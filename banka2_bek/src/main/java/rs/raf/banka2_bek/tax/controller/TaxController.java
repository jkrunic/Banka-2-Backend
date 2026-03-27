package rs.raf.banka2_bek.tax.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.raf.banka2_bek.tax.dto.TaxRecordDto;

import java.util.Collections;
import java.util.List;

/**
 * Stub controller za porezne endpointe.
 * Frontend (TaxPortalPage) ocekuje ove endpointe — vracamo prazne podatke
 * dok se ne implementira puna logika.
 */
@RestController
@RequestMapping("/tax")
@RequiredArgsConstructor
public class TaxController {

    /**
     * GET /tax - Lista korisnika sa dugovanjima (supervizor portal).
     * Filtriranje po userType i name.
     */
    @GetMapping
    public ResponseEntity<List<TaxRecordDto>> getTaxRecords(
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String name) {
        // Stub: vraca praznu listu dok se ne implementira porezna logika
        return ResponseEntity.ok(Collections.emptyList());
    }

    /**
     * POST /tax/calculate - Pokreni obracun poreza za tekuci mesec.
     */
    @PostMapping("/calculate")
    public ResponseEntity<Void> triggerCalculation() {
        // Stub: prihvata zahtev ali nema efekta dok se ne implementira
        return ResponseEntity.ok().build();
    }
}
