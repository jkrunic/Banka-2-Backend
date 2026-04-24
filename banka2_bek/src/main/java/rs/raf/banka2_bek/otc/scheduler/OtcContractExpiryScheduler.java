package rs.raf.banka2_bek.otc.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.raf.banka2_bek.otc.service.OtcService;

/**
 * Dnevno markiranje OTC ugovora kojima je prosao settlementDate kao EXPIRED.
 * Time se prodavcima oslobadja publicQuantity za nove ponude.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OtcContractExpiryScheduler {

    private final OtcService otcService;

    @Scheduled(cron = "0 5 1 * * *")
    public void expireContracts() {
        int count = otcService.expireSettledContracts();
        if (count > 0) {
            log.info("OTC: {} ugovora istekao i markiran EXPIRED", count);
        }
    }
}
