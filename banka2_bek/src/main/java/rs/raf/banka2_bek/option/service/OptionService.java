package rs.raf.banka2_bek.option.service;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.banka2_bek.account.model.Account;
import rs.raf.banka2_bek.account.repository.AccountRepository;
import rs.raf.banka2_bek.actuary.repository.ActuaryInfoRepository;
import rs.raf.banka2_bek.auth.util.UserRole;
import rs.raf.banka2_bek.employee.model.Employee;
import rs.raf.banka2_bek.employee.repository.EmployeeRepository;
import rs.raf.banka2_bek.option.dto.OptionChainDto;
import rs.raf.banka2_bek.option.dto.OptionDto;
import rs.raf.banka2_bek.option.mapper.OptionMapper;
import rs.raf.banka2_bek.option.model.Option;
import rs.raf.banka2_bek.option.model.OptionType;
import rs.raf.banka2_bek.option.repository.OptionRepository;
import rs.raf.banka2_bek.portfolio.model.Portfolio;
import rs.raf.banka2_bek.portfolio.repository.PortfolioRepository;
import rs.raf.banka2_bek.stock.model.Listing;
import rs.raf.banka2_bek.stock.repository.ListingRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OptionService {

    private static final Logger log = LoggerFactory.getLogger(OptionService.class);

    private final OptionRepository optionRepository;
    private final ListingRepository listingRepository;
    private final EmployeeRepository employeeRepository;
    private final ActuaryInfoRepository actuaryInfoRepository;
    private final AccountRepository accountRepository;
    private final PortfolioRepository portfolioRepository;
    private final String bankRegistrationNumber;

    public OptionService(
            OptionRepository optionRepository,
            ListingRepository listingRepository,
            EmployeeRepository employeeRepository,
            ActuaryInfoRepository actuaryInfoRepository,
            AccountRepository accountRepository,
            PortfolioRepository portfolioRepository,
            @Value("${bank.registration-number}") String bankRegistrationNumber) {
        this.optionRepository = optionRepository;
        this.listingRepository = listingRepository;
        this.employeeRepository = employeeRepository;
        this.actuaryInfoRepository = actuaryInfoRepository;
        this.accountRepository = accountRepository;
        this.portfolioRepository = portfolioRepository;
        this.bankRegistrationNumber = bankRegistrationNumber;
    }

    public List<OptionChainDto> getOptionsForStock(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new EntityNotFoundException("Listing id: " + listingId + " not found."));

        List<Option> options = optionRepository.findByStockListingId(listingId);
        BigDecimal currentPrice = listing.getPrice();

        Map<LocalDate, List<Option>> grouped = options.stream()
                .collect(Collectors.groupingBy(Option::getSettlementDate));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    OptionChainDto chain = new OptionChainDto();
                    chain.setSettlementDate(entry.getKey());
                    chain.setCurrentStockPrice(currentPrice);

                    List<OptionDto> calls = entry.getValue().stream()
                            .filter(o -> o.getOptionType() == OptionType.CALL)
                            .sorted(Comparator.comparing(Option::getStrikePrice))
                            .map(o -> OptionMapper.toDto(o, currentPrice))
                            .toList();
                    chain.setCalls(calls);

                    List<OptionDto> puts = entry.getValue().stream()
                            .filter(o -> o.getOptionType() == OptionType.PUT)
                            .sorted(Comparator.comparing(Option::getStrikePrice))
                            .map(o -> OptionMapper.toDto(o, currentPrice))
                            .toList();
                    chain.setPuts(puts);

                    return chain;
                })
                .toList();
    }

    public OptionDto getOptionById(Long optionId) {
        Option option = optionRepository.findById(optionId)
                .orElseThrow(() -> new EntityNotFoundException("Option id: " + optionId + " not found."));

        BigDecimal currentPrice = option.getStockListing().getPrice();
        return OptionMapper.toDto(option, currentPrice);
    }

    @Transactional
    public void exerciseOption(Long optionId, String userEmail) {
        Employee employee = ensureUserCanExerciseOptions(userEmail);

        Option option = optionRepository.findById(optionId)
                .orElseThrow(() -> new EntityNotFoundException("Option id: " + optionId + " not found."));

        if (option.getSettlementDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Opcija je istekla (settlement: " + option.getSettlementDate() + ")"
            );
        }

        BigDecimal currentPrice = option.getStockListing().getPrice();
        BigDecimal strikePrice = option.getStrikePrice();

        if (option.getOptionType() == OptionType.CALL && currentPrice.compareTo(strikePrice) <= 0) {
            throw new IllegalArgumentException(
                    "CALL opcija nije in-the-money (stock: " + currentPrice + ", strike: " + strikePrice + ")"
            );
        }

        if (option.getOptionType() == OptionType.PUT && currentPrice.compareTo(strikePrice) >= 0) {
            throw new IllegalArgumentException(
                    "PUT opcija nije in-the-money (stock: " + currentPrice + ", strike: " + strikePrice + ")"
            );
        }

        if (option.getOpenInterest() <= 0) {
            throw new IllegalArgumentException("Opcija nema otvorenih ugovora za izvrsavanje.");
        }

        Listing stockListing = option.getStockListing();
        int contractSize = option.getContractSize();
        BigDecimal totalCost = strikePrice.multiply(BigDecimal.valueOf(contractSize))
                .setScale(4, RoundingMode.HALF_UP);

        // Find bank account (Company ID = 3) — same pattern as OrderExecutionService
        Account bankAccount = getBankAccount();

        if (option.getOptionType() == OptionType.CALL) {
            // CALL exercise: user pays strikePrice * contractSize, receives shares
            // Debit the bank account
            if (bankAccount.getAvailableBalance().compareTo(totalCost) < 0) {
                throw new IllegalStateException(
                        "Nedovoljno sredstava na racunu banke za izvrsavanje CALL opcije. Potrebno: " + totalCost
                );
            }
            bankAccount.setBalance(bankAccount.getBalance().subtract(totalCost));
            bankAccount.setAvailableBalance(bankAccount.getAvailableBalance().subtract(totalCost));
            accountRepository.save(bankAccount);

            // Add shares to portfolio
            updatePortfolioBuy(employee.getId(), UserRole.EMPLOYEE, stockListing, contractSize, currentPrice);

        } else {
            // PUT exercise: user sells shares at strikePrice, receives cash
            // Remove shares from portfolio (must own them)
            updatePortfolioSell(employee.getId(), UserRole.EMPLOYEE, stockListing, contractSize);

            // Credit the bank account
            bankAccount.setBalance(bankAccount.getBalance().add(totalCost));
            bankAccount.setAvailableBalance(bankAccount.getAvailableBalance().add(totalCost));
            accountRepository.save(bankAccount);
        }

        // Decrement open interest
        option.setOpenInterest(option.getOpenInterest() - 1);
        optionRepository.save(option);

        log.info(
                "Opcija {} (id={}) izvrsena od strane {}. Tip={}, strike={}, contractSize={}, totalCost={}. Novi openInterest={}",
                option.getTicker(),
                option.getId(),
                userEmail,
                option.getOptionType(),
                strikePrice,
                contractSize,
                totalCost,
                option.getOpenInterest()
        );
    }

    /**
     * Adds shares to portfolio after CALL exercise.
     * Same pattern as OrderExecutionService.updatePortfolio for BUY.
     */
    private void updatePortfolioBuy(Long userId, String userRole, Listing listing, int quantity, BigDecimal price) {
        Optional<Portfolio> existing = portfolioRepository.findByUserIdAndUserRole(userId, userRole)
                .stream()
                .filter(p -> p.getListingId().equals(listing.getId()))
                .findFirst();

        if (existing.isPresent()) {
            Portfolio portfolio = existing.get();
            int oldQty = portfolio.getQuantity();
            BigDecimal oldTotal = portfolio.getAverageBuyPrice().multiply(BigDecimal.valueOf(oldQty));
            BigDecimal newFillTotal = price.multiply(BigDecimal.valueOf(quantity));
            int newQty = oldQty + quantity;

            BigDecimal newAvg = oldTotal.add(newFillTotal)
                    .divide(BigDecimal.valueOf(newQty), 4, RoundingMode.HALF_UP);

            portfolio.setQuantity(newQty);
            portfolio.setAverageBuyPrice(newAvg);
            portfolioRepository.save(portfolio);
        } else {
            Portfolio portfolio = new Portfolio();
            portfolio.setUserId(userId);
            portfolio.setUserRole(userRole);
            portfolio.setListingId(listing.getId());
            portfolio.setListingTicker(listing.getTicker());
            portfolio.setListingName(listing.getName());
            portfolio.setListingType(listing.getListingType().name());
            portfolio.setQuantity(quantity);
            portfolio.setAverageBuyPrice(price);
            portfolio.setPublicQuantity(0);
            portfolioRepository.save(portfolio);
        }
    }

    /**
     * Removes shares from portfolio after PUT exercise.
     * Same pattern as OrderExecutionService.updatePortfolio for SELL.
     */
    private void updatePortfolioSell(Long userId, String userRole, Listing listing, int quantity) {
        Portfolio portfolio = portfolioRepository.findByUserIdAndUserRole(userId, userRole)
                .stream()
                .filter(p -> p.getListingId().equals(listing.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Korisnik nema dovoljno akcija " + listing.getTicker() + " u portfoliju za PUT exercise."
                ));

        if (portfolio.getQuantity() < quantity) {
            throw new IllegalStateException(
                    "Nedovoljno akcija " + listing.getTicker() + " u portfoliju. Potrebno: " + quantity +
                            ", dostupno: " + portfolio.getQuantity()
            );
        }

        int newQty = portfolio.getQuantity() - quantity;
        if (newQty <= 0) {
            portfolioRepository.delete(portfolio);
        } else {
            portfolio.setQuantity(newQty);
            portfolioRepository.save(portfolio);
        }
    }

    /**
     * Finds the bank's USD account using the configured bank registration number.
     * Same pattern as OrderExecutionService.getBankAccount.
     */
    private Account getBankAccount() {
        return accountRepository.findBankAccountByCurrency(bankRegistrationNumber, "USD")
                .orElseThrow(() -> new IllegalStateException("Bank USD account not found!"));
    }

    private Employee ensureUserCanExerciseOptions(String userEmail) {
        Employee employee = employeeRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AccessDeniedException("Samo aktuar moze da izvrsi opciju."));

        if (!Boolean.TRUE.equals(employee.getActive())) {
            throw new AccessDeniedException("Samo aktivan aktuar moze da izvrsi opciju.");
        }

        boolean adminEmployee = employee.getPermissions() != null && employee.getPermissions().contains(UserRole.ADMIN);
        boolean actuaryExists = actuaryInfoRepository.findByEmployeeId(employee.getId()).isPresent();

        if (!adminEmployee && !actuaryExists) {
            throw new AccessDeniedException("Samo aktuar moze da izvrsi opciju.");
        }

        return employee;
    }
}