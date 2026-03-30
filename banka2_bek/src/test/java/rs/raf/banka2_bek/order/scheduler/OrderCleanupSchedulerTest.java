package rs.raf.banka2_bek.order.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.raf.banka2_bek.order.model.Order;
import rs.raf.banka2_bek.order.model.OrderStatus;
import rs.raf.banka2_bek.order.repository.OrderRepository;
import rs.raf.banka2_bek.stock.model.Listing;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderCleanupSchedulerTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderCleanupScheduler orderCleanupScheduler;


    @Test
    void cleanupExpiredOrders_shouldDeclineExpiredOrder() {
        Order order = mock(Order.class);
        Listing listing = mock(Listing.class);
        when(order.getLastModification()).thenReturn(LocalDateTime.now().minusDays(2));
        when(order.getListing()).thenReturn(listing);
        when(orderRepository.findByStatusAndIsDoneFalse(OrderStatus.APPROVED))
                .thenReturn(List.of(order));

        orderCleanupScheduler.cleanupExpiredOrders();

        verify(order).setStatus(OrderStatus.DECLINED);
        verify(order).setApprovedBy("SYSTEM - Settlement date expired");
        verify(orderRepository).save(order);
    }

    @Test
    void cleanupExpiredOrders_shouldNotDeclineRecentOrder() {
        Order order = mock(Order.class);
        when(order.getLastModification()).thenReturn(LocalDateTime.now());
        when(orderRepository.findByStatusAndIsDoneFalse(OrderStatus.APPROVED))
                .thenReturn(List.of(order));

        orderCleanupScheduler.cleanupExpiredOrders();

        verify(order, never()).setStatus(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cleanupExpiredOrders_shouldDoNothing_whenNoApprovedOrders() {
        when(orderRepository.findByStatusAndIsDoneFalse(OrderStatus.APPROVED))
                .thenReturn(List.of());

        orderCleanupScheduler.cleanupExpiredOrders();

        verify(orderRepository, never()).save(any());
    }
}
