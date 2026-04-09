package rs.raf.banka2_bek.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import rs.raf.banka2_bek.employee.model.Employee;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeUserDetailsTest {

    @Test
    void getAuthorities_withAdminPermission_containsRoleAdmin() {
        Employee employee = Employee.builder()
                .email("admin@banka.rs")
                .password("hashed")
                .active(true)
                .permissions(Set.of("ADMIN", "READ_USERS"))
                .build();

        EmployeeUserDetails details = new EmployeeUserDetails(employee);
        Collection<? extends GrantedAuthority> authorities = details.getAuthorities();
        Set<String> authorityNames = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertThat(authorityNames).contains("ROLE_ADMIN", "ROLE_EMPLOYEE", "ADMIN", "READ_USERS");
    }

    @Test
    void getAuthorities_withoutAdminPermission_doesNotContainRoleAdmin() {
        Employee employee = Employee.builder()
                .email("agent@banka.rs")
                .password("hashed")
                .active(true)
                .permissions(Set.of("TRADE"))
                .build();

        EmployeeUserDetails details = new EmployeeUserDetails(employee);
        Set<String> authorityNames = details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertThat(authorityNames).contains("ROLE_EMPLOYEE", "TRADE");
        assertThat(authorityNames).doesNotContain("ROLE_ADMIN");
    }

    @Test
    void getAuthorities_nullPermissions_onlyRoleEmployee() {
        Employee employee = Employee.builder()
                .email("emp@banka.rs")
                .password("hashed")
                .active(true)
                .permissions(null)
                .build();

        EmployeeUserDetails details = new EmployeeUserDetails(employee);
        Set<String> authorityNames = details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertThat(authorityNames).containsExactly("ROLE_EMPLOYEE");
    }

    @Test
    void getPassword_returnsEmployeePassword() {
        Employee employee = Employee.builder()
                .email("test@banka.rs")
                .password("secret-hash")
                .active(true)
                .build();

        EmployeeUserDetails details = new EmployeeUserDetails(employee);

        assertThat(details.getPassword()).isEqualTo("secret-hash");
    }

    @Test
    void getUsername_returnsEmployeeEmail() {
        Employee employee = Employee.builder()
                .email("test@banka.rs")
                .password("hash")
                .active(true)
                .build();

        EmployeeUserDetails details = new EmployeeUserDetails(employee);

        assertThat(details.getUsername()).isEqualTo("test@banka.rs");
    }

    @Test
    void isEnabled_activeEmployee_returnsTrue() {
        Employee employee = Employee.builder()
                .email("test@banka.rs")
                .password("hash")
                .active(true)
                .build();

        EmployeeUserDetails details = new EmployeeUserDetails(employee);

        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void isEnabled_inactiveEmployee_returnsFalse() {
        Employee employee = Employee.builder()
                .email("test@banka.rs")
                .password("hash")
                .active(false)
                .build();

        EmployeeUserDetails details = new EmployeeUserDetails(employee);

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void isEnabled_nullActive_returnsFalse() {
        Employee employee = Employee.builder()
                .email("test@banka.rs")
                .password("hash")
                .active(null)
                .build();

        EmployeeUserDetails details = new EmployeeUserDetails(employee);

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void accountAndCredentialFlags_alwaysTrue() {
        Employee employee = Employee.builder()
                .email("test@banka.rs")
                .password("hash")
                .active(true)
                .build();

        EmployeeUserDetails details = new EmployeeUserDetails(employee);

        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
    }
}
