package rs.raf.banka2_bek.auth.util;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import rs.raf.banka2_bek.client.repository.ClientRepository;
import rs.raf.banka2_bek.employee.repository.EmployeeRepository;

import java.util.Optional;

/**
 * Centralno razresavanje identiteta korisnika koji salje zahtev.
 *
 * Pre ekstrakcije, isti kod je bio duplikovan u {@code OrderServiceImpl},
 * {@code OtcService}, {@code PortfolioService} i drugim servisima. Ovde su
 * sva tri: trenutni korisnik (iz SecurityContext-a), rezolucija imena, i
 * rezolucija uloge iz ID-ja.
 */
@Component
@RequiredArgsConstructor
public class UserResolver {

    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * Pronalazi internal user-ID i ulogu na osnovu email-a iz JWT-a.
     * Prvo se pretrazuje {@code clients} pa {@code employees} tabela.
     *
     * @throws IllegalStateException ako nijedna tabela nema korisnika
     */
    public UserContext resolveCurrent() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<Long> clientId = clientRepository.findByEmail(email).map(c -> c.getId());
        if (clientId.isPresent()) {
            return new UserContext(clientId.get(), UserRole.CLIENT);
        }
        Long employeeId = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Korisnik nije pronadjen: " + email))
                .getId();
        return new UserContext(employeeId, UserRole.EMPLOYEE);
    }

    /**
     * Formatira ime + prezime za datu kombinaciju (userId, userRole).
     * Ako korisnik nije pronadjen, vraca "{uloga} #{id}" fallback.
     */
    public String resolveName(Long userId, String userRole) {
        if (UserRole.isClient(userRole)) {
            return clientRepository.findById(userId)
                    .map(c -> c.getFirstName() + " " + c.getLastName())
                    .orElse("Klijent #" + userId);
        }
        return employeeRepository.findById(userId)
                .map(e -> e.getFirstName() + " " + e.getLastName())
                .orElse("Zaposleni #" + userId);
    }

    /**
     * Odreduje ulogu korisnika na osnovu cistog user-ID-ja. Prvo se proveri
     * da li postoji klijent sa tim id-em, inace pretpostavlja employee.
     */
    public String resolveRole(Long userId) {
        return clientRepository.findById(userId).isPresent() ? UserRole.CLIENT : UserRole.EMPLOYEE;
    }
}
