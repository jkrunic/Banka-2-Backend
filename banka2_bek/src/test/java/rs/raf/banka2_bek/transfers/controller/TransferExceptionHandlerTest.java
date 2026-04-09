package rs.raf.banka2_bek.transfers.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TransferExceptionHandlerTest {

    private TransferExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TransferExceptionHandler();
    }

    // ── RuntimeException routing ──────────────────────────────────

    @Test
    void handleRuntime_notFoundMessage_returns404() {
        RuntimeException ex = new RuntimeException("Account not found");

        ResponseEntity<Map<String, Object>> response = handler.handleRuntime(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "Account not found");
    }

    @Test
    void handleRuntime_insufficientFunds_returns400() {
        RuntimeException ex = new RuntimeException("Insufficient funds");

        ResponseEntity<Map<String, Object>> response = handler.handleRuntime(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleRuntime_notActive_returns400() {
        RuntimeException ex = new RuntimeException("Account is not active");

        ResponseEntity<Map<String, Object>> response = handler.handleRuntime(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleRuntime_mustBeDifferent_returns400() {
        RuntimeException ex = new RuntimeException("Accounts must be different");

        ResponseEntity<Map<String, Object>> response = handler.handleRuntime(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleRuntime_sameCurrency_returns400() {
        RuntimeException ex = new RuntimeException("Accounts must have same currency");

        ResponseEntity<Map<String, Object>> response = handler.handleRuntime(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleRuntime_differentCurrencies_returns400() {
        RuntimeException ex = new RuntimeException("Accounts have different currencies");

        ResponseEntity<Map<String, Object>> response = handler.handleRuntime(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleRuntime_doNotHaveAccess_returns400() {
        RuntimeException ex = new RuntimeException("You do not have access");

        ResponseEntity<Map<String, Object>> response = handler.handleRuntime(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleRuntime_notAuthenticated_returns401() {
        RuntimeException ex = new RuntimeException("User is not authenticated");

        ResponseEntity<Map<String, Object>> response = handler.handleRuntime(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handleRuntime_clientNotFoundForAuthenticated_returns404_becauseNotFoundMatchesFirst() {
        // "Client not found for authenticated" contains "not found" which matches first branch
        RuntimeException ex = new RuntimeException("Client not found for authenticated user");

        ResponseEntity<Map<String, Object>> response = handler.handleRuntime(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleRuntime_unknownError_returns500() {
        RuntimeException ex = new RuntimeException("Something completely unexpected");

        ResponseEntity<Map<String, Object>> response = handler.handleRuntime(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("status", 500);
    }

    @Test
    void handleRuntime_nullMessage_returns500WithDefault() {
        RuntimeException ex = new RuntimeException((String) null);

        ResponseEntity<Map<String, Object>> response = handler.handleRuntime(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("message", "Internal server error");
    }

    // ── MethodArgumentNotValidException ───────────────────────────

    @Test
    void handleValidation_returnsBadRequest() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "amount", "Amount is required"));

        MethodParameter param = new MethodParameter(
                TransferExceptionHandlerTest.class.getDeclaredMethod("setUp"), -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(param, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((String) response.getBody().get("message")).contains("amount: Amount is required");
    }

    // ── HttpMessageNotReadableException ───────────────────────────

    @Test
    void handleJsonParse_returnsInvalidFormat() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("bad json", (Throwable) null, null);

        ResponseEntity<Map<String, Object>> response = handler.handleJsonParse(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Invalid request format.");
    }
}
