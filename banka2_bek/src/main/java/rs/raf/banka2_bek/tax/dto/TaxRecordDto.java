package rs.raf.banka2_bek.tax.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaxRecordDto {
    private Long userId;
    private String userName;
    private String userType; // CLIENT or EMPLOYEE
    private BigDecimal totalProfit;
    private BigDecimal taxOwed;
    private BigDecimal taxPaid;
    private String currency;
}
