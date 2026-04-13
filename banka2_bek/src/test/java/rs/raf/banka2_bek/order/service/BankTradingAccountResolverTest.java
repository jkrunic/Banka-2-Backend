package rs.raf.banka2_bek.order.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.banka2_bek.account.model.Account;
import rs.raf.banka2_bek.account.model.AccountCategory;
import rs.raf.banka2_bek.account.repository.AccountRepository;
import rs.raf.banka2_bek.order.exception.UnsupportedCurrencyException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BankTradingAccountResolver")
class BankTradingAccountResolverTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private BankTradingAccountResolver resolver;

    private Account bankAccount(String number) {
        Account a = new Account();
        a.setAccountNumber(number);
        a.setAccountCategory(AccountCategory.BANK_TRADING);
        return a;
    }

    @Test
    @DisplayName("resolve vraca bankin trading racun za podrzane valute (RSD, USD, EUR)")
    void resolve_returnsAccountForSupportedCurrency() {
        Account rsd = bankAccount("999000000000000001");
        Account usd = bankAccount("999000000000000002");
        Account eur = bankAccount("999000000000000003");

        when(accountRepository.findFirstByAccountCategoryAndCurrency_Code(AccountCategory.BANK_TRADING, "RSD"))
                .thenReturn(Optional.of(rsd));
        when(accountRepository.findFirstByAccountCategoryAndCurrency_Code(AccountCategory.BANK_TRADING, "USD"))
                .thenReturn(Optional.of(usd));
        when(accountRepository.findFirstByAccountCategoryAndCurrency_Code(AccountCategory.BANK_TRADING, "EUR"))
                .thenReturn(Optional.of(eur));

        assertThat(resolver.resolve("RSD")).isSameAs(rsd);
        assertThat(resolver.resolve("USD")).isSameAs(usd);
        assertThat(resolver.resolve("EUR")).isSameAs(eur);
    }

    @Test
    @DisplayName("resolve baca UnsupportedCurrencyException za valutu koju banka ne podrzava")
    void resolve_throwsUnsupportedCurrencyException_forUnknownCurrency() {
        when(accountRepository.findFirstByAccountCategoryAndCurrency_Code(AccountCategory.BANK_TRADING, "JPY"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve("JPY"))
                .isInstanceOf(UnsupportedCurrencyException.class)
                .hasMessageContaining("JPY");
    }

    @Test
    @DisplayName("resolve vraca prvi matchovani racun kada repo vrati findFirst rezultat")
    void resolve_returnsFirstMatchingAccount() {
        Account first = bankAccount("222000100000000110");

        when(accountRepository.findFirstByAccountCategoryAndCurrency_Code(AccountCategory.BANK_TRADING, "RSD"))
                .thenReturn(Optional.of(first));

        Account result = resolver.resolve("RSD");

        assertThat(result).isSameAs(first);
        assertThat(result.getAccountCategory()).isEqualTo(AccountCategory.BANK_TRADING);
    }
}
