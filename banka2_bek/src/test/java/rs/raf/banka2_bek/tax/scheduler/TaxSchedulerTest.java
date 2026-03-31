package rs.raf.banka2_bek.tax.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.banka2_bek.tax.service.TaxService;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaxSchedulerTest {

    @Mock
    private TaxService taxService;

    @InjectMocks
    private TaxScheduler taxScheduler;

    @Nested
    @DisplayName("calculateMonthlyTax")
    class CalculateMonthlyTax {

        @Test
        @DisplayName("calls taxService.calculateTaxForAllUsers() exactly once")
        void callsTaxServiceOnce() {
            taxScheduler.calculateMonthlyTax();

            verify(taxService, times(1)).calculateTaxForAllUsers();
        }

        @Test
        @DisplayName("does not propagate exception from taxService")
        void doesNotPropagateException() {
            doThrow(new RuntimeException("DB error")).when(taxService).calculateTaxForAllUsers();

            taxScheduler.calculateMonthlyTax();

            verify(taxService, times(1)).calculateTaxForAllUsers();
        }

        @Test
        @DisplayName("catches all exception subtypes including RuntimeException")
        void catchesAllExceptionTypes() {
            doThrow(new IllegalStateException("bad state")).when(taxService).calculateTaxForAllUsers();

            taxScheduler.calculateMonthlyTax();

            verify(taxService, times(1)).calculateTaxForAllUsers();
        }
    }
}
