package rs.raf.banka2_bek.portfolio.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.raf.banka2_bek.portfolio.dto.PortfolioItemDto;
import rs.raf.banka2_bek.portfolio.dto.PortfolioSummaryDto;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Stub controller za portfolio endpointe.
 * Frontend ocekuje ove endpointe — vracamo prazne/podrazumevane podatke
 * dok se ne implementira puna logika.
 */
@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    /**
     * GET /portfolio/my - Lista hartija u vlasnistvu korisnika.
     */
    @GetMapping("/my")
    public ResponseEntity<List<PortfolioItemDto>> getMyPortfolio() {
        // Stub: vraca praznu listu dok se ne implementira portfolio logika
        return ResponseEntity.ok(Collections.emptyList());
    }

    /**
     * GET /portfolio/summary - Ukupna vrednost, profit, porez.
     */
    @GetMapping("/summary")
    public ResponseEntity<PortfolioSummaryDto> getSummary() {
        // Stub: vraca nule dok se ne implementira portfolio logika
        PortfolioSummaryDto summary = new PortfolioSummaryDto(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
        return ResponseEntity.ok(summary);
    }

    /**
     * PATCH /portfolio/{id}/public - Postavi broj akcija u javnom rezimu.
     */
    @PatchMapping("/{id}/public")
    public ResponseEntity<Map<String, String>> setPublicQuantity(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {
        // Stub: prihvata zahtev ali nema efekta dok se ne implementira
        return ResponseEntity.ok(Map.of("message", "Public quantity updated (stub)"));
    }
}
