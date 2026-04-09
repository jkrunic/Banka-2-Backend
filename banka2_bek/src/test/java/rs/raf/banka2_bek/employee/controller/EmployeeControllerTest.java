package rs.raf.banka2_bek.employee.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rs.raf.banka2_bek.employee.controller.exception_handler.EmployeeExceptionHandler;
import rs.raf.banka2_bek.employee.dto.*;
import rs.raf.banka2_bek.employee.service.EmployeeService;

import java.time.LocalDate;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EmployeeControllerTest {

    private MockMvc mockMvc;
    @Mock private EmployeeService employeeService;
    @InjectMocks private EmployeeController controller;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new EmployeeExceptionHandler())
                .build();
    }

    private EmployeeResponseDto sampleResponse() {
        return EmployeeResponseDto.builder().id(1L).firstName("Marko").lastName("Petrovic")
                .dateOfBirth(LocalDate.of(1990, 1, 1)).gender("M").email("m@b.rs").phone("0601234567")
                .address("Belgrade").username("marko").position("Dev").department("IT")
                .active(false).permissions(Set.of("READ")).build();
    }

    @Test void createEmployee_returns201() throws Exception {
        when(employeeService.createEmployee(any(CreateEmployeeRequestDto.class))).thenReturn(sampleResponse());
        CreateEmployeeRequestDto req = new CreateEmployeeRequestDto();
        req.setFirstName("Marko"); req.setLastName("Petrovic"); req.setDateOfBirth(LocalDate.of(1990,1,1));
        req.setGender("M"); req.setEmail("m@b.rs"); req.setPhone("0601234567"); req.setAddress("Belgrade");
        req.setUsername("marko"); req.setPosition("Dev"); req.setDepartment("IT");
        mockMvc.perform(post("/employees").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1));
    }

    @Test void getEmployees_returnsPage() {
        Page<EmployeeResponseDto> page = new PageImpl<>(java.util.List.of(sampleResponse()));
        when(employeeService.getEmployees(eq(0), eq(10), nullable(String.class), nullable(String.class), nullable(String.class), nullable(String.class))).thenReturn(page);
        org.springframework.http.ResponseEntity<Page<EmployeeResponseDto>> response = controller.getEmployees(0, 10, null, null, null, null);
        org.assertj.core.api.Assertions.assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        org.assertj.core.api.Assertions.assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test void getEmployeeById_returnsEmployee() throws Exception {
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleResponse());
        mockMvc.perform(get("/employees/1")).andExpect(status().isOk()).andExpect(jsonPath("$.firstName").value("Marko"));
    }

    @Test void updateEmployee_returnsUpdated() throws Exception {
        EmployeeResponseDto updated = sampleResponse();
        when(employeeService.updateEmployee(eq(1L), any(UpdateEmployeeRequestDto.class))).thenReturn(updated);
        UpdateEmployeeRequestDto req = new UpdateEmployeeRequestDto();
        req.setFirstName("UpdatedMarko");
        mockMvc.perform(put("/employees/1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test void deactivateEmployee_returns200() throws Exception {
        doNothing().when(employeeService).deactivateEmployee(1L);
        mockMvc.perform(patch("/employees/1/deactivate")).andExpect(status().isOk());
    }

    @Test void getEmployeeById_notFound_returnsBadRequest() throws Exception {
        when(employeeService.getEmployeeById(999L)).thenThrow(new IllegalArgumentException("Employee with ID 999 not found."));
        mockMvc.perform(get("/employees/999")).andExpect(status().isBadRequest());
    }
}
