package rs.raf.banka2_bek.transaction.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.raf.banka2_bek.transaction.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByAccountClientId(Long clientId, Pageable pageable);

    @Query("""
            select t from Transaction t
            left join fetch t.account a
            left join fetch t.currency c
            left join fetch t.payment p
            left join fetch p.fromAccount fa
            left join fetch t.transfer tr
            where t.id = :transactionId
              and a.client.id = :clientId
            """)
    Optional<Transaction> findReceiptTransactionForClient(
            @Param("transactionId") Long transactionId,
            @Param("clientId") Long clientId
    );

    // PostgreSQL: cast(:param as tip) je neophodan kad parametar moze biti null
    // (vidi napomenu u PaymentRepository).
    @Query("""
       select t from Transaction t
       left join t.payment p
       left join t.transfer tr
       where t.account.client.id = :clientId
         and (cast(:fromDate as timestamp) is null or t.createdAt >= :fromDate)
         and (cast(:toDate as timestamp) is null or t.createdAt <= :toDate)
         and (cast(:minAmount as big_decimal) is null or
              (case
                   when p is not null then p.amount
                   when tr is not null then tr.fromAmount
                   else null
               end) >= :minAmount)
         and (cast(:maxAmount as big_decimal) is null or
              (case
                   when p is not null then p.amount
                   when tr is not null then tr.fromAmount
                   else null
               end) <= :maxAmount)
         and (cast(:type as string) is null or
              (:type = 'PAYMENT' and p is not null) or
              (:type = 'TRANSFER' and tr is not null))
       """)
    Page<Transaction> findTransactionsByAccountUserIdAndFilters(
            @Param("clientId") Long clientId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("type") String type,
            Pageable pageable
    );
}
