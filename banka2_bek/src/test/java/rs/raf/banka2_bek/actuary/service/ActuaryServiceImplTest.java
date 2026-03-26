package rs.raf.banka2_bek.actuary.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import rs.raf.banka2_bek.actuary.dto.ActuaryInfoDto;
import rs.raf.banka2_bek.actuary.dto.UpdateActuaryLimitDto;
import rs.raf.banka2_bek.actuary.model.ActuaryInfo;
import rs.raf.banka2_bek.actuary.model.ActuaryType;
import rs.raf.banka2_bek.actuary.repository.ActuaryInfoRepository;
import rs.raf.banka2_bek.actuary.service.implementation.ActuaryServiceImpl;
import rs.raf.banka2_bek.auth.model.User;
import rs.raf.banka2_bek.auth.repository.UserRepository;
import rs.raf.banka2_bek.employee.model.Employee;
import rs.raf.banka2_bek.employee.repository.EmployeeRepository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActuaryServiceImplTest {

    @Mock
    private ActuaryInfoRepository actuaryInfoRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ActuaryServiceImpl actuaryService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of())
        );
    }

    private Employee createEmployee(Long id, String firstName, String lastName, String email) {
        Employee emp = Employee.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .dateOfBirth(java.time.LocalDate.of(1990, 1, 1))
                .gender("M")
                .phone("123")
                .address("Adresa")
                .username(email)
                .password("pass")
                .saltPassword("salt")
                .position("Pozicija")
                .department("Dept")
                .active(true)
                .permissions(Set.of())
                .build();
        return emp;
    }

    private User createAdminUser(String email) {
        User admin = new User();
        admin.setEmail(email);
        admin.setPassword("pass");
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setActive(true);
        admin.setRole("ADMIN");
        return admin;
    }

    private ActuaryInfo createAgentInfo(Long id, Employee employee,
                                        BigDecimal dailyLimit, BigDecimal usedLimit,
                                        boolean needApproval) {
        ActuaryInfo info = new ActuaryInfo();
        info.setId(id);
        info.setEmployee(employee);
        info.setActuaryType(ActuaryType.AGENT);
        info.setDailyLimit(dailyLimit);
        info.setUsedLimit(usedLimit);
        info.setNeedApproval(needApproval);
        return info;
    }

    private ActuaryInfo createSupervisorInfo(Long id, Employee employee) {
        ActuaryInfo info = new ActuaryInfo();
        info.setId(id);
        info.setEmployee(employee);
        info.setActuaryType(ActuaryType.SUPERVISOR);
        info.setDailyLimit(null);
        info.setUsedLimit(null);
        info.setNeedApproval(false);
        return info;
    }

    @Nested
    @DisplayName("getAgents")
    class GetAgents {

        @Test
        @DisplayName("vraca sve agente bez filtera")
        void returnsAllAgents() {
            Employee marko = createEmployee(10L, "Marko", "Markovic", "marko.markovic@banka.rs");
            Employee jelena = createEmployee(11L, "Jelena", "Jovanovic", "jelena.jovanovic@banka.rs");

            ActuaryInfo agent1 = createAgentInfo(1L, marko,
                    new BigDecimal("100000"), new BigDecimal("15000"), false);
            ActuaryInfo agent2 = createAgentInfo(2L, jelena,
                    new BigDecimal("50000"), BigDecimal.ZERO, true);

            when(actuaryInfoRepository.findByTypeAndFilters(
                    ActuaryType.AGENT, null, null, null, null))
                    .thenReturn(List.of(agent1, agent2));

            List<ActuaryInfoDto> result = actuaryService.getAgents(null, null, null, null);

            assertEquals(2, result.size());
            assertEquals("Marko Markovic", result.get(0).getEmployeeName());
            assertEquals("Jelena Jovanovic", result.get(1).getEmployeeName());
        }

        @Test
        @DisplayName("vraca praznu listu ako nema agenata")
        void returnsEmptyList() {
            when(actuaryInfoRepository.findByTypeAndFilters(
                    ActuaryType.AGENT, null, null, null, null))
                    .thenReturn(Collections.emptyList());

            List<ActuaryInfoDto> result = actuaryService.getAgents(null, null, null, null);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getActuaryInfo")
    class GetActuaryInfo {

        @Test
        @DisplayName("vraca aktuarske podatke za postojeceg zaposlenog")
        void returnsActuaryInfo() {
            Employee marko = createEmployee(10L, "Marko", "Markovic", "marko.markovic@banka.rs");
            ActuaryInfo agentInfo = createAgentInfo(1L, marko,
                    new BigDecimal("100000"), new BigDecimal("15000"), false);

            when(actuaryInfoRepository.findByEmployeeId(10L)).thenReturn(Optional.of(agentInfo));

            ActuaryInfoDto result = actuaryService.getActuaryInfo(10L);

            assertEquals(10L, result.getEmployeeId());
            assertEquals("AGENT", result.getActuaryType());
        }

        @Test
        @DisplayName("baca izuzetak ako zapis ne postoji")
        void notFound() {
            when(actuaryInfoRepository.findByEmployeeId(999L)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> actuaryService.getActuaryInfo(999L));

            assertTrue(ex.getMessage().contains("999"));
        }
    }

    @Nested
    @DisplayName("updateAgentLimit")
    class UpdateAgentLimit {

        @Test
        @DisplayName("admin iz users tabele moze da promeni dailyLimit i needApproval")
        void adminUserCanUpdateAgent() {
            authenticateAs("admin@banka.rs");

            Employee agentEmployee = createEmployee(10L, "Marko", "Markovic", "marko@banka.rs");
            ActuaryInfo agentInfo = createAgentInfo(1L, agentEmployee,
                    new BigDecimal("100000"), new BigDecimal("15000"), false);

            UpdateActuaryLimitDto dto = new UpdateActuaryLimitDto();
            dto.setDailyLimit(new BigDecimal("250000"));
            dto.setNeedApproval(true);

            when(userRepository.findByEmail("admin@banka.rs")).thenReturn(Optional.of(createAdminUser("admin@banka.rs")));
            when(actuaryInfoRepository.findByEmployeeId(10L)).thenReturn(Optional.of(agentInfo));
            when(actuaryInfoRepository.save(any(ActuaryInfo.class))).thenAnswer(inv -> inv.getArgument(0));

            ActuaryInfoDto result = actuaryService.updateAgentLimit(10L, dto);

            assertEquals(new BigDecimal("250000"), result.getDailyLimit());
            assertTrue(result.isNeedApproval());
            assertEquals(new BigDecimal("15000"), result.getUsedLimit());
            verify(actuaryInfoRepository).save(agentInfo);
        }

        @Test
        @DisplayName("supervizor zaposleni moze da menja samo prosledjena polja")
        void supervisorEmployeeCanPartiallyUpdateAgent() {
            authenticateAs("supervisor@banka.rs");

            Employee supervisor = createEmployee(20L, "Nina", "Nikolic", "supervisor@banka.rs");
            supervisor.setPermissions(Set.of("SUPERVISOR"));

            Employee agentEmployee = createEmployee(10L, "Marko", "Markovic", "marko@banka.rs");
            ActuaryInfo agentInfo = createAgentInfo(1L, agentEmployee,
                    new BigDecimal("100000"), new BigDecimal("15000"), false);
            ActuaryInfo supervisorInfo = createSupervisorInfo(5L, supervisor);

            UpdateActuaryLimitDto dto = new UpdateActuaryLimitDto();
            dto.setNeedApproval(true);

            when(userRepository.findByEmail("supervisor@banka.rs")).thenReturn(Optional.empty());
            when(employeeRepository.findByEmail("supervisor@banka.rs")).thenReturn(Optional.of(supervisor));
            when(actuaryInfoRepository.findByEmployeeId(20L)).thenReturn(Optional.of(supervisorInfo));
            when(actuaryInfoRepository.findByEmployeeId(10L)).thenReturn(Optional.of(agentInfo));
            when(actuaryInfoRepository.save(any(ActuaryInfo.class))).thenAnswer(inv -> inv.getArgument(0));

            ActuaryInfoDto result = actuaryService.updateAgentLimit(10L, dto);

            assertEquals(new BigDecimal("100000"), result.getDailyLimit());
            assertTrue(result.isNeedApproval());
            verify(actuaryInfoRepository).save(agentInfo);
        }

        @Test
        @DisplayName("agent ne sme da menja tudje limite")
        void agentCannotUpdateAgentLimit() {
            authenticateAs("agent@banka.rs");

            Employee agent = createEmployee(20L, "A", "G", "agent@banka.rs");
            agent.setPermissions(Set.of("AGENT"));
            ActuaryInfo agentOwnInfo = createAgentInfo(5L, agent,
                    new BigDecimal("50000"), new BigDecimal("1000"), false);

            UpdateActuaryLimitDto dto = new UpdateActuaryLimitDto();
            dto.setDailyLimit(new BigDecimal("1"));

            when(userRepository.findByEmail("agent@banka.rs")).thenReturn(Optional.empty());
            when(employeeRepository.findByEmail("agent@banka.rs")).thenReturn(Optional.of(agent));
            when(actuaryInfoRepository.findByEmployeeId(20L)).thenReturn(Optional.of(agentOwnInfo));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> actuaryService.updateAgentLimit(10L, dto));

            assertEquals("Only supervisors or admins can perform this action.", ex.getMessage());
            verify(actuaryInfoRepository, never()).save(any());
        }

        @Test
        @DisplayName("nije dozvoljeno menjati supervizora")
        void cannotUpdateSupervisor() {
            authenticateAs("admin@banka.rs");

            Employee supervisor = createEmployee(30L, "Nina", "Nikolic", "nina@banka.rs");
            ActuaryInfo supervisorInfo = createSupervisorInfo(6L, supervisor);

            UpdateActuaryLimitDto dto = new UpdateActuaryLimitDto();
            dto.setDailyLimit(new BigDecimal("999"));

            when(userRepository.findByEmail("admin@banka.rs")).thenReturn(Optional.of(createAdminUser("admin@banka.rs")));
            when(actuaryInfoRepository.findByEmployeeId(30L)).thenReturn(Optional.of(supervisorInfo));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> actuaryService.updateAgentLimit(30L, dto));

            assertEquals("Only AGENT actuaries can be modified.", ex.getMessage());
            verify(actuaryInfoRepository, never()).save(any());
        }

        @Test
        @DisplayName("negativan dailyLimit nije dozvoljen")
        void negativeDailyLimitIsRejected() {
            authenticateAs("admin@banka.rs");

            Employee agentEmployee = createEmployee(10L, "Marko", "Markovic", "marko@banka.rs");
            ActuaryInfo agentInfo = createAgentInfo(1L, agentEmployee,
                    new BigDecimal("100000"), new BigDecimal("15000"), false);

            UpdateActuaryLimitDto dto = new UpdateActuaryLimitDto();
            dto.setDailyLimit(new BigDecimal("-1"));

            when(userRepository.findByEmail("admin@banka.rs")).thenReturn(Optional.of(createAdminUser("admin@banka.rs")));
            when(actuaryInfoRepository.findByEmployeeId(10L)).thenReturn(Optional.of(agentInfo));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> actuaryService.updateAgentLimit(10L, dto));

            assertEquals("Daily limit cannot be negative.", ex.getMessage());
            verify(actuaryInfoRepository, never()).save(any());
        }

        @Test
        @DisplayName("null dto se odbija")
        void nullDtoIsRejected() {
            authenticateAs("admin@banka.rs");
            when(userRepository.findByEmail("admin@banka.rs")).thenReturn(Optional.of(createAdminUser("admin@banka.rs")));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> actuaryService.updateAgentLimit(10L, null));

            assertEquals("Update payload is required.", ex.getMessage());
        }

        @Test
        @DisplayName("ako nema autentifikacije baca exception")
        void unauthenticatedUpdateFails() {
            UpdateActuaryLimitDto dto = new UpdateActuaryLimitDto();
            dto.setNeedApproval(true);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> actuaryService.updateAgentLimit(10L, dto));

            assertEquals("User is not authenticated.", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("resetUsedLimit")
    class ResetUsedLimit {

        @Test
        @DisplayName("admin moze rucno da resetuje usedLimit agenta")
        void adminCanResetAgentLimit() {
            authenticateAs("admin@banka.rs");

            Employee agentEmployee = createEmployee(10L, "Marko", "Markovic", "marko@banka.rs");
            ActuaryInfo agentInfo = createAgentInfo(1L, agentEmployee,
                    new BigDecimal("100000"), new BigDecimal("15000.50"), false);

            when(userRepository.findByEmail("admin@banka.rs")).thenReturn(Optional.of(createAdminUser("admin@banka.rs")));
            when(actuaryInfoRepository.findByEmployeeId(10L)).thenReturn(Optional.of(agentInfo));
            when(actuaryInfoRepository.save(any(ActuaryInfo.class))).thenAnswer(inv -> inv.getArgument(0));

            ActuaryInfoDto result = actuaryService.resetUsedLimit(10L);

            assertEquals(BigDecimal.ZERO, result.getUsedLimit());
            verify(actuaryInfoRepository).save(agentInfo);
        }

        @Test
        @DisplayName("supervizor ne moze da resetuje supervizora")
        void cannotResetSupervisor() {
            authenticateAs("supervisor@banka.rs");

            Employee supervisor = createEmployee(20L, "Nina", "Nikolic", "supervisor@banka.rs");
            supervisor.setPermissions(Set.of("SUPERVISOR"));
            ActuaryInfo supervisorInfo = createSupervisorInfo(5L, supervisor);

            when(userRepository.findByEmail("supervisor@banka.rs")).thenReturn(Optional.empty());
            when(employeeRepository.findByEmail("supervisor@banka.rs")).thenReturn(Optional.of(supervisor));
            when(actuaryInfoRepository.findByEmployeeId(20L)).thenReturn(Optional.of(supervisorInfo));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> actuaryService.resetUsedLimit(20L));

            assertEquals("Only AGENT actuaries can be modified.", ex.getMessage());
            verify(actuaryInfoRepository, never()).save(any());
        }

        @Test
        @DisplayName("reset je idempotentan kada je usedLimit vec nula")
        void resetIsIdempotentWhenAlreadyZero() {
            authenticateAs("admin@banka.rs");

            Employee agentEmployee = createEmployee(10L, "Marko", "Markovic", "marko@banka.rs");
            ActuaryInfo agentInfo = createAgentInfo(1L, agentEmployee,
                    new BigDecimal("100000"), BigDecimal.ZERO, false);

            when(userRepository.findByEmail("admin@banka.rs")).thenReturn(Optional.of(createAdminUser("admin@banka.rs")));
            when(actuaryInfoRepository.findByEmployeeId(10L)).thenReturn(Optional.of(agentInfo));
            when(actuaryInfoRepository.save(any(ActuaryInfo.class))).thenAnswer(inv -> inv.getArgument(0));

            ActuaryInfoDto result = actuaryService.resetUsedLimit(10L);

            assertEquals(BigDecimal.ZERO, result.getUsedLimit());
            verify(actuaryInfoRepository).save(agentInfo);
        }
    }

    @Nested
    @DisplayName("resetAllUsedLimits")
    class ResetAllUsedLimits {

        @Test
        @DisplayName("resetuje usedLimit na 0 za sve agente i cuva ih jednim saveAll pozivom")
        void resetsAllAgentsToZero() {
            Employee marko = createEmployee(10L, "Marko", "Markovic", "marko.markovic@banka.rs");
            Employee jelena = createEmployee(11L, "Jelena", "Jovanovic", "jelena.jovanovic@banka.rs");

            ActuaryInfo agent1 = createAgentInfo(1L, marko,
                    new BigDecimal("100000"), new BigDecimal("15000.50"), false);
            ActuaryInfo agent2 = createAgentInfo(2L, jelena,
                    new BigDecimal("50000"), new BigDecimal("999.99"), true);

            when(actuaryInfoRepository.findAllByActuaryType(ActuaryType.AGENT))
                    .thenReturn(List.of(agent1, agent2));

            actuaryService.resetAllUsedLimits();

            assertEquals(BigDecimal.ZERO, agent1.getUsedLimit());
            assertEquals(BigDecimal.ZERO, agent2.getUsedLimit());
            assertEquals(new BigDecimal("100000"), agent1.getDailyLimit());
            assertTrue(agent2.isNeedApproval());

            verify(actuaryInfoRepository).saveAll(List.of(agent1, agent2));
        }

        @Test
        @DisplayName("ako nema agenata ne baca exception i ne zove saveAll")
        void doesNothingWhenNoAgentsExist() {
            when(actuaryInfoRepository.findAllByActuaryType(ActuaryType.AGENT))
                    .thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> actuaryService.resetAllUsedLimits());

            verify(actuaryInfoRepository).findAllByActuaryType(ActuaryType.AGENT);
            verify(actuaryInfoRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("resetuje i null usedLimit na nulu")
        void resetsNullUsedLimitToZero() {
            Employee marko = createEmployee(10L, "Marko", "Markovic", "marko.markovic@banka.rs");
            ActuaryInfo agent = createAgentInfo(1L, marko,
                    new BigDecimal("100000"), null, false);

            when(actuaryInfoRepository.findAllByActuaryType(ActuaryType.AGENT)).thenReturn(List.of(agent));

            actuaryService.resetAllUsedLimits();

            assertEquals(BigDecimal.ZERO, agent.getUsedLimit());
            verify(actuaryInfoRepository).saveAll(List.of(agent));
        }

        @Test
        @DisplayName("uvek trazi samo AGENT zapise iz repository-ja")
        void queriesOnlyAgentType() {
            when(actuaryInfoRepository.findAllByActuaryType(ActuaryType.AGENT))
                    .thenReturn(Collections.emptyList());

            actuaryService.resetAllUsedLimits();

            verify(actuaryInfoRepository).findAllByActuaryType(ActuaryType.AGENT);
            verify(actuaryInfoRepository, never()).findByEmployeeId(anyLong());
        }
    }

    @Nested
    @DisplayName("scheduledResetAllUsedLimits")
    class ScheduledResetAllUsedLimits {

        @Test
        @DisplayName("scheduled wrapper poziva business reset metodu")
        void scheduledMethodCallsBusinessLogic() {
            ActuaryServiceImpl serviceSpy = spy(actuaryService);

            serviceSpy.scheduledResetAllUsedLimits();

            verify(serviceSpy).resetAllUsedLimits();
        }
    }
}