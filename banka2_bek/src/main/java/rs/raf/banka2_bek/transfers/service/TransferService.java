package rs.raf.banka2_bek.transfers.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.banka2_bek.account.model.Account;
import rs.raf.banka2_bek.account.model.AccountStatus;
import rs.raf.banka2_bek.client.model.Client;
import rs.raf.banka2_bek.payment.model.PaymentStatus;
import rs.raf.banka2_bek.transfer.model.Transfer;
import rs.raf.banka2_bek.transfer.model.TransferType;
import rs.raf.banka2_bek.transfers.dto.TransferFxRequestDto;
import rs.raf.banka2_bek.transfers.dto.TransferInternalRequestDto;
import rs.raf.banka2_bek.transfers.dto.TransferResponseDto;
import rs.raf.banka2_bek.transfers.repository.AccountRepository;
import rs.raf.banka2_bek.transfers.repository.TransferRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;

    public TransferService(TransferRepository transferRepository, AccountRepository accountRepository) {
        this.transferRepository = transferRepository;
        this.accountRepository = accountRepository;
    }

    private TransferResponseDto mapToDto(Transfer transfer) {
        TransferResponseDto response = new TransferResponseDto();
        response.setFromAccountNumber(transfer.getFromAccount().getAccountNumber());
        response.setToAccountNumber(transfer.getToAccount().getAccountNumber());
        response.setAmount(transfer.getFromAmount());
        response.setExchangeRate(transfer.getExchangeRate());
        response.setCommission(transfer.getCommission());
        response.setClientFirstName(transfer.getCreatedBy().getFirstName());
        response.setClientLastName(transfer.getCreatedBy().getLastName());
        response.setStatus(transfer.getStatus());
        return response;
    }


    @Transactional
    public TransferResponseDto internalTransfer(TransferInternalRequestDto request) {

        Account fromAccount = accountRepository.findByAccountNumber(request.getFromAccountNumber())
                .orElseThrow(() -> new RuntimeException("From account not found"));

        Account toAccount = accountRepository.findByAccountNumber(request.getToAccountNumber())
                .orElseThrow(() -> new RuntimeException("To account not found"));

        // Provera: racuni moraju biti razliciti
        if (request.getFromAccountNumber().equals(request.getToAccountNumber())) {
            throw new RuntimeException("Accounts must be different");
        }

        // Provera: oba racuna moraju biti aktivna
        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Source account is not active");
        }
        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Destination account is not active");
        }

        // Provera: isti klijent
        if (!fromAccount.getClient().getId().equals(toAccount.getClient().getId())) {
            throw new RuntimeException("Accounts must belong to the same client");
        }

        // Provera: ista valuta
        if (!fromAccount.getCurrency().getId().equals(toAccount.getCurrency().getId())) {
            throw new RuntimeException("Accounts must have the same currency");
        }

        // Provera: dovoljno sredstava
        if (fromAccount.getAvailableBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        // Skini novac sa fromAccount, dodaj na toAccount
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        fromAccount.setAvailableBalance(fromAccount.getAvailableBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));
        toAccount.setAvailableBalance(toAccount.getAvailableBalance().add(request.getAmount()));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);


        // Kreiraj i sacuvaj transfer
        Transfer transfer = new Transfer();
        transfer.setOrderNumber(UUID.randomUUID().toString().replace("-", "").substring(0, 30));
        transfer.setFromAccount(fromAccount);
        transfer.setToAccount(toAccount);
        transfer.setFromAmount(request.getAmount());
        transfer.setToAmount(request.getAmount());
        transfer.setFromCurrency(fromAccount.getCurrency());
        transfer.setToCurrency(toAccount.getCurrency());
        transfer.setExchangeRate(null);
        transfer.setCommission(BigDecimal.ZERO);
        transfer.setTransferType(TransferType.INTERNAL_TRANSFER);
        transfer.setStatus(PaymentStatus.COMPLETED);
        transfer.setCreatedBy(fromAccount.getClient());

        transferRepository.save(transfer);

        return mapToDto(transfer);
    }

    public TransferResponseDto fxTransfer(TransferFxRequestDto request) {
        // TODO: implementirati kada stigne Stašin kod
        throw new RuntimeException("Not implemented yet");
    }

    public List<TransferResponseDto> getAllTransfers(Client client) {
        List<Transfer> transfers = transferRepository.findByCreatedByOrderByCreatedAtDesc(client);
        List<TransferResponseDto> result = new ArrayList<>();
        for (Transfer transfer : transfers) {
            result.add(mapToDto(transfer));
        }
        return result;
    }

    public TransferResponseDto getTransferById(Long id) {
        Transfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transfer not found"));
        return mapToDto(transfer);
    }
}