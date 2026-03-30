package rs.raf.banka2_bek.order.scheduler;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.banka2_bek.order.service.OrderExecutionService;
import rs.raf.banka2_bek.order.service.StopOrderActivationService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderSchedulerTest {
    @Mock
    private StopOrderActivationService stopOrderActivationService;

    @Mock
    private OrderExecutionService orderExecutionService;

    @InjectMocks
    private OrderScheduler orderScheduler;

    @Test
    void processStopOrders_shouldCallService() {
        orderScheduler.processStopOrders();
        verify(stopOrderActivationService, times(1)).checkAndActivateStopOrders();
    }

    @Test
    void processStopOrders_shouldLogError_whenExceptionThrown() {
        doThrow(new RuntimeException("test error"))
                .when(stopOrderActivationService).checkAndActivateStopOrders();

        assertDoesNotThrow(() -> orderScheduler.processStopOrders());
        verify(stopOrderActivationService, times(1)).checkAndActivateStopOrders();
    }

    @Test
    void executeApprovedOrders_shouldCallService() {
        orderScheduler.executeApprovedOrders();
        verify(orderExecutionService, times(1)).executeOrders();
    }

    @Test
    void executeApprovedOrders_shouldLogError_whenExceptionThrown() {
        doThrow(new RuntimeException("test error"))
                .when(orderExecutionService).executeOrders();

        assertDoesNotThrow(() -> orderScheduler.executeApprovedOrders());
        verify(orderExecutionService, times(1)).executeOrders();
    }


}
