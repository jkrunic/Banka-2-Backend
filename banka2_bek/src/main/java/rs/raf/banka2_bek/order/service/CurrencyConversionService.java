package rs.raf.banka2_bek.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.raf.banka2_bek.exchange.ExchangeService;
import rs.raf.banka2_bek.exchange.dto.ExchangeRateDto;
import rs.raf.banka2_bek.order.exception.UnsupportedCurrencyException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Servis za konverziju iznosa izmedju valuta u okviru order flow-a.
 * Koristi postojeci {@link ExchangeService} kao izvor srednjih kurseva.
 *
 * Kurs u {@link ExchangeRateDto#getRate()} je "koliko jedinica target valute za 1 RSD".
 * Zato se konverzija iz A u B racuna kao: amount * (rateB / rateA).
 */
@Service
@RequiredArgsConstructor
public class CurrencyConversionService {

    private static final int SCALE = 4;

    private final ExchangeService exchangeService;

    /**
     * Konvertuje iznos iz jedne valute u drugu koristeci srednji kurs.
     * Ako su valute iste, vraca isti iznos bez ikakvih izmena.
     *
     * @param amount       iznos u izvornoj valuti (ne sme biti null)
     * @param fromCurrency izvorna valuta (ISO kod)
     * @param toCurrency   ciljna valuta (ISO kod)
     * @return iznos u ciljnoj valuti, zaokruzen na 4 decimale (HALF_UP)
     * @throws UnsupportedCurrencyException ako neka od valuta nije podrzana
     */
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }
        BigDecimal rate = getRate(fromCurrency, toCurrency);
        return amount.multiply(rate).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Vraca kurs za par valuta: koliko jedinica {@code toCurrency} se dobije za 1 {@code fromCurrency}.
     * Za isti par vraca {@link BigDecimal#ONE}.
     */
    public BigDecimal getRate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }

        List<ExchangeRateDto> rates = exchangeService.getAllRates();

        double fromRate = findRate(rates, fromCurrency);
        double toRate = findRate(rates, toCurrency);

        // rate[X] = jedinica X za 1 RSD; 1 from = (1 / fromRate) RSD = (toRate / fromRate) to
        double crossRate = toRate / fromRate;
        return BigDecimal.valueOf(crossRate).setScale(6, RoundingMode.HALF_UP);
    }

    private double findRate(List<ExchangeRateDto> rates, String currency) {
        return rates.stream()
                .filter(r -> r.getCurrency().equalsIgnoreCase(currency))
                .findFirst()
                .orElseThrow(() -> new UnsupportedCurrencyException(
                        "Valuta nije podrzana od strane exchange servisa: " + currency))
                .getRate();
    }
}
