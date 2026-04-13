package rs.raf.banka2_bek.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.raf.banka2_bek.account.model.Account;
import rs.raf.banka2_bek.account.model.AccountCategory;
import rs.raf.banka2_bek.account.repository.AccountRepository;
import rs.raf.banka2_bek.order.exception.UnsupportedCurrencyException;

/**
 * Resolver koji vraca bankin trading racun u trazenoj valuti.
 * Koristi se u order flow-u kada treba odrediti sa kog bankinog racuna
 * agent trguje (rezervacija, isplata provizije, settlement).
 */
@Service
@RequiredArgsConstructor
public class BankTradingAccountResolver {

    private final AccountRepository accountRepository;

    /**
     * Vraca prvi bankin trading racun u datoj valuti.
     *
     * @param listingCurrency ISO kod valute (npr. "RSD", "USD", "EUR")
     * @return {@link Account} sa {@code accountCategory = BANK_TRADING} i matchovanom valutom
     * @throws UnsupportedCurrencyException ako banka nema racun u trazenoj valuti
     */
    public Account resolve(String listingCurrency) {
        return accountRepository
                .findFirstByAccountCategoryAndCurrency_Code(AccountCategory.BANK_TRADING, listingCurrency)
                .orElseThrow(() -> new UnsupportedCurrencyException(
                        "Banka ne podrzava trgovinu u valuti: " + listingCurrency));
    }
}
