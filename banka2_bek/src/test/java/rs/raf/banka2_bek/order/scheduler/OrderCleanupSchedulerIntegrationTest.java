package rs.raf.banka2_bek.order.scheduler;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import rs.raf.banka2_bek.order.model.Order;
import rs.raf.banka2_bek.order.model.OrderDirection;
import rs.raf.banka2_bek.order.model.OrderStatus;
import rs.raf.banka2_bek.order.model.OrderType;
import rs.raf.banka2_bek.order.repository.OrderRepository;
import rs.raf.banka2_bek.stock.model.Listing;
import rs.raf.banka2_bek.stock.model.ListingType;
import rs.raf.banka2_bek.stock.repository.ListingRepository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import rs.raf.banka2_bek.IntegrationTestCleanup;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class OrderCleanupSchedulerIntegrationTest {
    @Autowired
    private OrderCleanupScheduler orderCleanupScheduler;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void clean() {
        IntegrationTestCleanup.truncateAllTables(dataSource);
    }

    private Listing savedListing(java.time.LocalDate settlementDate) {
        Listing l = new Listing();
        l.setTicker("AAPL");
        l.setName("Apple Inc.");
        l.setListingType(ListingType.STOCK);
        l.setPrice(BigDecimal.valueOf(150));
        l.setAsk(BigDecimal.valueOf(151));
        l.setBid(BigDecimal.valueOf(149));
        l.setExchangeAcronym("NASDAQ");
        l.setLastRefresh(LocalDateTime.now());
        l.setSettlementDate(settlementDate);
        return listingRepository.save(l);
    }

    private Order savedOrder(OrderStatus status, LocalDateTime lastModification) {
        return savedOrder(status, lastModification, null);
    }

    private Order savedOrder(OrderStatus status, LocalDateTime lastModification,
                             java.time.LocalDate settlementDate) {
        Order o = new Order();
        o.setUserId(1L);
        o.setUserRole("CLIENT");
        o.setListing(savedListing(settlementDate));
        o.setOrderType(OrderType.MARKET);
        o.setDirection(OrderDirection.BUY);
        o.setQuantity(5);
        o.setContractSize(1);
        o.setPricePerUnit(BigDecimal.valueOf(150));
        o.setApproximatePrice(BigDecimal.valueOf(750));
        o.setStatus(status);
        o.setDone(false);
        o.setRemainingPortions(5);
        o.setAfterHours(false);
        o.setAllOrNone(false);
        o.setMargin(false);
        o.setCreatedAt(LocalDateTime.now());
        o.setLastModification(lastModification);
        return orderRepository.save(o);
    }

    @Test
    void cleanupExpiredOrders_shouldDeclineExpiredOrder() {
        Order order = savedOrder(OrderStatus.APPROVED,
                LocalDateTime.now().minusDays(2),
                java.time.LocalDate.now().minusDays(1));

        orderCleanupScheduler.cleanupExpiredOrders();

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.DECLINED);
        assertThat(updated.getApprovedBy()).isEqualTo("SYSTEM - Settlement date expired");
    }

    @Test
    void cleanupExpiredOrders_shouldNotDeclineRecentOrder() {
        Order order = savedOrder(OrderStatus.APPROVED, LocalDateTime.now());

        orderCleanupScheduler.cleanupExpiredOrders();

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.APPROVED);
    }

    @Test
    void cleanupExpiredOrders_shouldDoNothing_whenNoOrders() {
        orderCleanupScheduler.cleanupExpiredOrders();

        assertThat(orderRepository.findAll()).isEmpty();
    }
}
