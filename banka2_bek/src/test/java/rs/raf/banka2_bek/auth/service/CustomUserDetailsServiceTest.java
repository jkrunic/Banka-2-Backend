package rs.raf.banka2_bek.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import rs.raf.banka2_bek.auth.model.User;
import rs.raf.banka2_bek.auth.repository.UserRepository;
import rs.raf.banka2_bek.employee.model.Employee;
import rs.raf.banka2_bek.employee.repository.EmployeeRepository;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void loadUserByUsername_employeeFound_returnsEmployeeUserDetails() {
        Employee employee = Employee.builder()
                .id(1L)
                .email("admin@banka.rs")
                .password("hashed-pass")
                .active(true)
                .permissions(Set.of("ADMIN"))
                .build();

        when(employeeRepository.findByEmail("admin@banka.rs")).thenReturn(Optional.of(employee));

        UserDetails result = service.loadUserByUsername("admin@banka.rs");

        assertThat(result).isInstanceOf(EmployeeUserDetails.class);
        assertThat(result.getUsername()).isEqualTo("admin@banka.rs");
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void loadUserByUsername_employeeNotFound_fallsBackToUserRepo() {
        when(employeeRepository.findByEmail("client@banka.rs")).thenReturn(Optional.empty());

        User user = mock(User.class, withSettings().lenient());
        lenient().when(user.getEmail()).thenReturn("client@banka.rs");
        when(userRepository.findByEmail("client@banka.rs")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("client@banka.rs");

        assertThat(result).isNotNull();
        verify(userRepository).findByEmail("client@banka.rs");
    }

    @Test
    void loadUserByUsername_neitherFound_throwsUsernameNotFound() {
        when(employeeRepository.findByEmail("unknown@banka.rs")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("unknown@banka.rs")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("unknown@banka.rs"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: unknown@banka.rs");
    }
}
