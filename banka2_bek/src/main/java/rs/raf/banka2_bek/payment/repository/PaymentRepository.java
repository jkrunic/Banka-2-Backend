package rs.raf.banka2_bek.payment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.raf.banka2_bek.payment.model.Payment;
import rs.raf.banka2_bek.payment.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // NAPOMENA (PostgreSQL): cast(:param as tip) je neophodan jer PG JDBC
    // ne moze da zakljuci tip NULL parametra — na JPQL ":p is null" izraz
    // genrise "ERROR: could not determine data type of parameter". H2/MySQL
    // ne zahtevaju cast.
    @Query("""
           select p from Payment p
           where (p.fromAccount.client.id = :clientId
                  or p.toAccountNumber in (select a.accountNumber from Account a where a.client.id = :clientId))
             and (cast(:fromDate as timestamp) is null or p.createdAt >= :fromDate)
             and (cast(:toDate as timestamp) is null or p.createdAt <= :toDate)
             and (cast(:accountNumber as string) is null or p.fromAccount.accountNumber = :accountNumber or p.toAccountNumber = :accountNumber)
             and (cast(:minAmount as big_decimal) is null or p.amount >= :minAmount)
             and (cast(:maxAmount as big_decimal) is null or p.amount <= :maxAmount)
             and (cast(:status as string) is null or p.status = :status)
           """)
    Page<Payment> findByUserAccountsWithFilters(
            @Param("clientId") Long clientId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("accountNumber") String accountNumber,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("status") PaymentStatus status,
            Pageable pageable
    );
}
