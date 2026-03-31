package rs.raf.banka2_bek.tax.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import rs.raf.banka2_bek.tax.service.TaxService;

import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class TaxSchedulerIntegrationTest {

    @Autowired
    private TaxScheduler taxScheduler;

    @MockitoBean
    private TaxService taxService;

    @Test
    @DisplayName("scheduler calls taxService within Spring context")
    void schedulerCallsTaxServiceInContext() {
        taxScheduler.calculateMonthlyTax();

        verify(taxService, times(1)).calculateTaxForAllUsers();
    }

    @Test
    @DisplayName("scheduler does not propagate exception from service within Spring context")
    void schedulerSwallowsExceptionInContext() {
        doThrow(new RuntimeException("simulirana greska")).when(taxService).calculateTaxForAllUsers();

        taxScheduler.calculateMonthlyTax();

        verify(taxService, times(1)).calculateTaxForAllUsers();
    }
}
