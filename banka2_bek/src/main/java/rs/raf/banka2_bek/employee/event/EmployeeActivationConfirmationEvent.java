package rs.raf.banka2_bek.employee.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class EmployeeActivationConfirmationEvent extends ApplicationEvent {

    private final String email;
    private final String firstName;


    public EmployeeActivationConfirmationEvent(Object source, String email, String firstName) {
        super(source);
        this.email = email;
        this.firstName = firstName;
    }
}
