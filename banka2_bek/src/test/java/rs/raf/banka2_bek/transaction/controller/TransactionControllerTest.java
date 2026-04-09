package rs.raf.banka2_bek.transaction.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rs.raf.banka2_bek.transaction.dto.TransactionListItemDto;
import rs.raf.banka2_bek.transaction.dto.TransactionResponseDto;
import rs.raf.banka2_bek.transaction.dto.TransactionType;
import rs.raf.banka2_bek.transaction.service.TransactionService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock private TransactionService transactionService;
    @InjectMocks private TransactionController controller;

    @Test
    void getTransactions_returnsPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<TransactionListItemDto> page = new PageImpl<>(List.of(), pageable, 0);
        when(transactionService.getTransactions(any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<TransactionListItemDto>> response = controller.getTransactions(pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEmpty();
    }

    @Test
    void getTransactionById_returnsDto() {
        TransactionResponseDto dto = TransactionResponseDto.builder()
                .id(1L).accountNumber("111").toAccountNumber("222").currencyCode("RSD")
                .description("Test").type(TransactionType.PAYMENT)
                .debit(BigDecimal.valueOf(500)).credit(BigDecimal.ZERO)
                .balanceAfter(BigDecimal.valueOf(500)).availableAfter(BigDecimal.valueOf(500))
                .createdAt(LocalDateTime.of(2026, 4, 1, 10, 0))
                .build();

        when(transactionService.getTransactionById(1L)).thenReturn(dto);

        ResponseEntity<TransactionResponseDto> response = controller.getTransactionById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getAccountNumber()).isEqualTo("111");
    }
}
