package rs.raf.banka2_bek.loan.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import rs.raf.banka2_bek.loan.model.InterestType;
import rs.raf.banka2_bek.loan.model.Loan;
import rs.raf.banka2_bek.loan.model.LoanStatus;
import rs.raf.banka2_bek.loan.model.LoanType;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByInterestTypeAndStatusIn(InterestType interestType, List<LoanStatus> statuses);

    Page<Loan> findByClientId(Long clientId, Pageable pageable);

    @Query("SELECT l FROM Loan l WHERE " +
            "(:loanType IS NULL OR l.loanType = :loanType) AND " +
            "(:status IS NULL OR l.status = :status) AND " +
            "(:accountNumber IS NULL OR l.account.accountNumber = :accountNumber)")
    Page<Loan> findWithFilters(LoanType loanType, LoanStatus status, String accountNumber, Pageable pageable);
}
