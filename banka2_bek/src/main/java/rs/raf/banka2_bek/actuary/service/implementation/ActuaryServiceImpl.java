package rs.raf.banka2_bek.actuary.service.implementation;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.raf.banka2_bek.actuary.dto.ActuaryInfoDto;
import rs.raf.banka2_bek.actuary.dto.UpdateActuaryLimitDto;
import rs.raf.banka2_bek.actuary.mapper.ActuaryMapper;
import rs.raf.banka2_bek.actuary.model.ActuaryInfo;
import rs.raf.banka2_bek.actuary.model.ActuaryType;
import rs.raf.banka2_bek.actuary.repository.ActuaryInfoRepository;
import rs.raf.banka2_bek.actuary.service.ActuaryService;
import rs.raf.banka2_bek.auth.model.User;
import rs.raf.banka2_bek.auth.repository.UserRepository;
import rs.raf.banka2_bek.employee.model.Employee;
import rs.raf.banka2_bek.employee.repository.EmployeeRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActuaryServiceImpl implements ActuaryService {

    private final ActuaryInfoRepository actuaryInfoRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    @Override
    public List<ActuaryInfoDto> getAgents(String email, String firstName, String lastName, String position) {
        List<ActuaryInfo> agents = actuaryInfoRepository.findByTypeAndFilters(
                ActuaryType.AGENT, email, firstName, lastName, position
        );
        return agents.stream()
                .map(ActuaryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ActuaryInfoDto getActuaryInfo(Long employeeId) {
        ActuaryInfo info = actuaryInfoRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Actuary info for employee with ID " + employeeId + " not found."
                ));

        return ActuaryMapper.toDto(info);
    }

    @Override
    @Transactional
    public ActuaryInfoDto updateAgentLimit(Long employeeId, UpdateActuaryLimitDto dto) {
        ensureSupervisorOrAdmin();

        if (dto == null) {
            throw new IllegalArgumentException("Update payload is required.");
        }

        ActuaryInfo agentInfo = getAgentActuaryInfo(employeeId);

        if (dto.getDailyLimit() != null) {
            if (dto.getDailyLimit().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Daily limit cannot be negative.");
            }
            agentInfo.setDailyLimit(dto.getDailyLimit());
        }

        if (dto.getNeedApproval() != null) {
            agentInfo.setNeedApproval(dto.getNeedApproval());
        }

        ActuaryInfo saved = actuaryInfoRepository.save(agentInfo);
        return ActuaryMapper.toDto(saved);
    }

    @Override
    @Transactional
    public ActuaryInfoDto resetUsedLimit(Long employeeId) {
        ensureSupervisorOrAdmin();

        ActuaryInfo agentInfo = getAgentActuaryInfo(employeeId);
        agentInfo.setUsedLimit(BigDecimal.ZERO);

        ActuaryInfo saved = actuaryInfoRepository.save(agentInfo);
        return ActuaryMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void resetAllUsedLimits() {
        List<ActuaryInfo> agents = actuaryInfoRepository.findAllByActuaryType(ActuaryType.AGENT);

        if (agents.isEmpty()) {
            return;
        }

        agents.forEach(agent -> agent.setUsedLimit(BigDecimal.ZERO));
        actuaryInfoRepository.saveAll(agents);
    }

    @Scheduled(cron = "0 59 23 * * *")
    @Transactional
    public void scheduledResetAllUsedLimits() {
        resetAllUsedLimits();
    }

    private ActuaryInfo getAgentActuaryInfo(Long employeeId) {
        ActuaryInfo info = actuaryInfoRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Actuary info for employee with ID " + employeeId + " not found."
                ));

        if (info.getActuaryType() != ActuaryType.AGENT) {
            throw new IllegalStateException("Only AGENT actuaries can be modified.");
        }

        return info;
    }

    private void ensureSupervisorOrAdmin() {
        String email = getAuthenticatedEmail();

        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null && "ADMIN".equalsIgnoreCase(user.getRole()) && user.isActive()) {
            return;
        }

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated employee not found."));

        if (!Boolean.TRUE.equals(employee.getActive())) {
            throw new IllegalStateException("Authenticated employee is not active.");
        }

        if (employee.getPermissions() != null && employee.getPermissions().contains("ADMIN")) {
            return;
        }

        boolean hasSupervisorPermission = employee.getPermissions() != null
                && employee.getPermissions().contains("SUPERVISOR");

        boolean hasSupervisorActuaryType = actuaryInfoRepository.findByEmployeeId(employee.getId())
                .map(info -> info.getActuaryType() == ActuaryType.SUPERVISOR)
                .orElse(false);

        if (!hasSupervisorPermission && !hasSupervisorActuaryType) {
            throw new IllegalStateException("Only supervisors or admins can perform this action.");
        }
    }

    private String getAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated.");
        }

        String email = authentication.getName();
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Unable to determine authenticated user email.");
        }

        return email;
    }
}