package rs.raf.banka2_bek.payment.controller.exception_handler;

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

class PaymentRecipientExceptionHandlerTest {

    private PaymentRecipientExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PaymentRecipientExceptionHandler();
    }

    @Test
    void handleIllegalArgument_returnsBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid recipient");

        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Invalid recipient");
        assertThat(response.getBody()).containsEntry("status", 400);
    }

    @Test
    void handleIllegalState_returnsForbidden() {
        IllegalStateException ex = new IllegalStateException("Payment forbidden");

        ResponseEntity<Map<String, Object>> response = handler.handleIllegalState(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("message", "Payment forbidden");
        assertThat(response.getBody()).containsEntry("status", 403);
    }

    @Test
    void handleValidation_returnsBadRequest() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "amount", "Amount is required"));

        MethodParameter param = new MethodParameter(
                PaymentRecipientExceptionHandlerTest.class.getDeclaredMethod("setUp"), -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(param, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((String) response.getBody().get("message")).contains("amount: Amount is required");
    }

    @Test
    void handleJsonParse_withProblemMarker_extractsProblem() {
        String innerMessage = "some prefix problem: Invalid enum value\nother stuff";
        RuntimeException cause = new RuntimeException(innerMessage);
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Parse error", cause, null);

        ResponseEntity<Map<String, Object>> response = handler.handleJsonParse(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((String) response.getBody().get("message")).isEqualTo("Invalid enum value");
    }

    @Test
    void handleJsonParse_withProblemMarkerButBlank_returnsFullMessage() {
        String innerMessage = "some prefix problem:   \nother stuff";
        RuntimeException cause = new RuntimeException(innerMessage);
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Parse error", cause, null);

        ResponseEntity<Map<String, Object>> response = handler.handleJsonParse(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        // Falls through to return the full message when problem part is blank
        assertThat(response.getBody().get("message")).isNotNull();
    }

    @Test
    void handleJsonParse_noProblemMarker_returnsFullMessage() {
        RuntimeException cause = new RuntimeException("Just a plain error message");
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Parse error", cause, null);

        ResponseEntity<Map<String, Object>> response = handler.handleJsonParse(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((String) response.getBody().get("message")).isEqualTo("Just a plain error message");
    }

    @Test
    void handleJsonParse_causeMessageNull_returnsDefaultMessage() {
        RuntimeException cause = new RuntimeException((String) null);
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Parse error", cause, null);

        ResponseEntity<Map<String, Object>> response = handler.handleJsonParse(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((String) response.getBody().get("message")).isEqualTo("Invalid request format.");
    }

    @Test
    void handleJsonParse_withProblemMarkerNoNewline_extractsProblem() {
        String innerMessage = "some prefix problem: Invalid value here";
        RuntimeException cause = new RuntimeException(innerMessage);
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Parse error", cause, null);

        ResponseEntity<Map<String, Object>> response = handler.handleJsonParse(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((String) response.getBody().get("message")).isEqualTo("Invalid value here");
    }
}
