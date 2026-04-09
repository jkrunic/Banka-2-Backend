package rs.raf.banka2_bek.payment.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentCodeTest {

    @Test void getCode_returnsCodeString() {
        assertThat(PaymentCode.CODE_220.getCode()).isEqualTo("220");
        assertThat(PaymentCode.CODE_290.getCode()).isEqualTo("290");
    }

    @Test void isSupported_validCode_returnsTrue() {
        assertThat(PaymentCode.isSupported("220")).isTrue();
        assertThat(PaymentCode.isSupported("290")).isTrue();
        assertThat(PaymentCode.isSupported("253")).isTrue();
    }

    @Test void isSupported_invalidCode_returnsFalse() {
        assertThat(PaymentCode.isSupported("999")).isFalse();
        assertThat(PaymentCode.isSupported("000")).isFalse();
    }

    @Test void isSupported_null_returnsFalse() {
        assertThat(PaymentCode.isSupported(null)).isFalse();
    }

    @Test void isSupported_withWhitespace_returnsTrue() {
        assertThat(PaymentCode.isSupported(" 220 ")).isTrue();
    }

    @Test void fromCode_validCode_returnsEnum() {
        assertThat(PaymentCode.fromCode("220")).isEqualTo(PaymentCode.CODE_220);
        assertThat(PaymentCode.fromCode("290")).isEqualTo(PaymentCode.CODE_290);
    }

    @Test void fromCode_withWhitespace_returnsEnum() {
        assertThat(PaymentCode.fromCode(" 220 ")).isEqualTo(PaymentCode.CODE_220);
    }

    @Test void fromCode_invalidCode_throwsException() {
        assertThatThrownBy(() -> PaymentCode.fromCode("999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid payment code");
    }

    @Test void fromCode_null_throwsException() {
        assertThatThrownBy(() -> PaymentCode.fromCode(null))
                .isInstanceOf(Exception.class);
    }

    @Test void values_containsExpectedCount() {
        PaymentCode[] values = PaymentCode.values();
        assertThat(values.length).isGreaterThanOrEqualTo(46);
    }

    @Test void fromCode_allValues_roundTrip() {
        for (PaymentCode code : PaymentCode.values()) {
            assertThat(PaymentCode.fromCode(code.getCode())).isEqualTo(code);
            assertThat(PaymentCode.isSupported(code.getCode())).isTrue();
        }
    }
}
