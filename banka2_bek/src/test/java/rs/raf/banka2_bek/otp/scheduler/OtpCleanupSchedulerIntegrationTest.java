package rs.raf.banka2_bek.otp.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import rs.raf.banka2_bek.otp.model.OtpVerification;
import rs.raf.banka2_bek.otp.repository.OtpVerificationRepository;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;

import rs.raf.banka2_bek.IntegrationTestCleanup;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OtpCleanupSchedulerIntegrationTest {

    @Autowired
    private OtpCleanupScheduler otpCleanupScheduler;

    @Autowired
    private OtpVerificationRepository otpVerificationRepository;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void cleanDatabase() {
        IntegrationTestCleanup.truncateAllTables(dataSource);
    }

    private OtpVerification buildOtp(String email, LocalDateTime createdAt, boolean used) {
        return OtpVerification.builder()
                .email(email)
                .code("123456")
                .expiresAt(createdAt.plusMinutes(5))
                .createdAt(createdAt)
                .used(used)
                .build();
    }

    @Test
    @DisplayName("deletes OTP records older than 24h")
    void deletesOtpsOlderThan24h() {
        otpVerificationRepository.save(buildOtp("old@test.com", LocalDateTime.now().minusHours(25), false));
        otpVerificationRepository.save(buildOtp("new@test.com", LocalDateTime.now().minusMinutes(10), false));

        otpCleanupScheduler.cleanupExpiredOtps();

        List<OtpVerification> remaining = otpVerificationRepository.findAll();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getEmail()).isEqualTo("new@test.com");
    }

    @Test
    @DisplayName("deletes used OTP records older than 1h")
    void deletesUsedOtpsOlderThan1h() {
        otpVerificationRepository.save(buildOtp("used-old@test.com", LocalDateTime.now().minusHours(2), true));
        otpVerificationRepository.save(buildOtp("used-fresh@test.com", LocalDateTime.now().minusMinutes(30), true));

        otpCleanupScheduler.cleanupExpiredOtps();

        List<OtpVerification> remaining = otpVerificationRepository.findAll();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getEmail()).isEqualTo("used-fresh@test.com");
    }

    @Test
    @DisplayName("keeps fresh unused OTP records")
    void keepsFreshUnusedOtps() {
        otpVerificationRepository.save(buildOtp("fresh@test.com", LocalDateTime.now().minusMinutes(5), false));

        otpCleanupScheduler.cleanupExpiredOtps();

        assertThat(otpVerificationRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("deletes both expired and used records in one call, keeps the rest")
    void deletesBothTypesKeepsRest() {
        otpVerificationRepository.save(buildOtp("expired@test.com", LocalDateTime.now().minusHours(30), false));
        otpVerificationRepository.save(buildOtp("used-old@test.com", LocalDateTime.now().minusHours(2), true));
        otpVerificationRepository.save(buildOtp("fresh@test.com", LocalDateTime.now().minusMinutes(5), false));
        otpVerificationRepository.save(buildOtp("used-fresh@test.com", LocalDateTime.now().minusMinutes(20), true));

        otpCleanupScheduler.cleanupExpiredOtps();

        List<OtpVerification> remaining = otpVerificationRepository.findAll();
        assertThat(remaining).hasSize(2);
        assertThat(remaining).extracting(OtpVerification::getEmail)
                .containsExactlyInAnyOrder("fresh@test.com", "used-fresh@test.com");
    }

    @Test
    @DisplayName("no exception when table is empty")
    void noErrorOnEmptyTable() {
        otpCleanupScheduler.cleanupExpiredOtps();

        assertThat(otpVerificationRepository.count()).isZero();
    }
}
