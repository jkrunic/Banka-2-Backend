package rs.raf.banka2_bek.actuary.mapper;

import org.junit.jupiter.api.Test;
import rs.raf.banka2_bek.actuary.dto.ActuaryInfoDto;
import rs.raf.banka2_bek.actuary.model.ActuaryInfo;
import rs.raf.banka2_bek.actuary.model.ActuaryType;
import rs.raf.banka2_bek.employee.model.Employee;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ActuaryMapperTest {

    @Test
    void toDto_null_returnsNull() {
        assertThat(ActuaryMapper.toDto(null)).isNull();
    }

    @Test
    void toDto_withEmployee_mapsAllFields() {
        Employee employee = Employee.builder()
                .id(10L)
                .firstName("Marko")
                .lastName("Petrovic")
                .email("marko@banka.rs")
                .position("Agent")
                .build();

        ActuaryInfo info = new ActuaryInfo();
        info.setId(1L);
        info.setEmployee(employee);
        info.setActuaryType(ActuaryType.AGENT);
        info.setDailyLimit(BigDecimal.valueOf(50000));
        info.setUsedLimit(BigDecimal.valueOf(10000));
        info.setNeedApproval(true);

        ActuaryInfoDto dto = ActuaryMapper.toDto(info);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getEmployeeId()).isEqualTo(10L);
        assertThat(dto.getEmployeeName()).isEqualTo("Marko Petrovic");
        assertThat(dto.getEmployeeEmail()).isEqualTo("marko@banka.rs");
        assertThat(dto.getEmployeePosition()).isEqualTo("Agent");
        assertThat(dto.getActuaryType()).isEqualTo("AGENT");
        assertThat(dto.getDailyLimit()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        assertThat(dto.getUsedLimit()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(dto.isNeedApproval()).isTrue();
    }

    @Test
    void toDto_withoutEmployee_employeeFieldsAreNull() {
        ActuaryInfo info = new ActuaryInfo();
        info.setId(2L);
        info.setActuaryType(ActuaryType.SUPERVISOR);
        info.setDailyLimit(null);
        info.setUsedLimit(null);
        info.setNeedApproval(false);

        ActuaryInfoDto dto = ActuaryMapper.toDto(info);

        assertThat(dto.getId()).isEqualTo(2L);
        assertThat(dto.getEmployeeId()).isNull();
        assertThat(dto.getEmployeeName()).isNull();
        assertThat(dto.getEmployeeEmail()).isNull();
        assertThat(dto.getEmployeePosition()).isNull();
        assertThat(dto.getActuaryType()).isEqualTo("SUPERVISOR");
        assertThat(dto.isNeedApproval()).isFalse();
    }

    @Test
    void toDto_nullActuaryType_returnsNullType() {
        ActuaryInfo info = new ActuaryInfo();
        info.setId(3L);
        info.setActuaryType(null);

        ActuaryInfoDto dto = ActuaryMapper.toDto(info);

        assertThat(dto.getActuaryType()).isNull();
    }
}
