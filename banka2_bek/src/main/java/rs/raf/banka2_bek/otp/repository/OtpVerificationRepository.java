package rs.raf.banka2_bek.otp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.raf.banka2_bek.otp.model.OtpVerification;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    Optional<OtpVerification> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(String email);

    @Modifying
    @Query("DELETE FROM OtpVerification o WHERE o.createdAt < :cutoff")
    int deleteAllOlderThan(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("DELETE FROM OtpVerification o WHERE o.used = true AND o.createdAt < :cutoff")
    int deleteUsedOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
