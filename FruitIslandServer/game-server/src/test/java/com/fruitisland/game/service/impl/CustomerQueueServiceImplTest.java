package com.fruitisland.game.service.impl;

import com.fruitisland.game.dto.CustomerQueueVO;
import com.fruitisland.game.entity.*;
import com.fruitisland.game.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class CustomerQueueServiceImplTest {
    private CustomerOrderService customerOrderService;
    private CustomerArrivalStateService arrivalStateService;
    private CustomerTemplateService customerTemplateService;
    private PlayerRecipeService playerRecipeService;
    private RecipeConfigService recipeConfigService;
    private OrderQuantityWeightService quantityWeightService;
    private RandomGenerator random;
    private CustomerQueueServiceImpl service;
    private MutableClock clock;
    private DrinkShopService drinkShopService;

    @BeforeEach
    void setUp() {
        customerOrderService = mock(CustomerOrderService.class);
        arrivalStateService = mock(CustomerArrivalStateService.class);
        customerTemplateService = mock(CustomerTemplateService.class);
        playerRecipeService = mock(PlayerRecipeService.class);
        recipeConfigService = mock(RecipeConfigService.class);
        quantityWeightService = mock(OrderQuantityWeightService.class);
        random = mock(RandomGenerator.class);
        clock = new MutableClock(Instant.parse("2026-07-29T08:00:00Z"));
        drinkShopService = mock(DrinkShopService.class);
        service = new CustomerQueueServiceImpl(
                customerOrderService,
                arrivalStateService,
                customerTemplateService,
                playerRecipeService,
                recipeConfigService,
                quantityWeightService,
                clock,
                random,
                drinkShopService
        );

        DrinkShopLevelConfig levelConfig = new DrinkShopLevelConfig();
        levelConfig.setLevel(1);
        levelConfig.setQueueCapacity(5);
        levelConfig.setArrivalIntervalSeconds(120);
        when(drinkShopService.getActiveConfig(7L)).thenReturn(levelConfig);

        when(customerOrderService.listWaiting(7L)).thenReturn(List.of());
        when(arrivalStateService.lockOrCreate(7L)).thenReturn(new CustomerArrivalState());
        when(customerTemplateService.list()).thenReturn(List.of(customer("berry", "莓莓", "👧")));
        when(playerRecipeService.listByPlayer(7L)).thenReturn(List.of(qualification(7L, "strawberry_juice")));
        when(recipeConfigService.getById("strawberry_juice"))
                .thenReturn(recipe("strawberry_juice", "草莓汁", 30, 5, 100));
        when(quantityWeightService.listEnabled()).thenReturn(List.of(
                quantity(1, 60), quantity(2, 30), quantity(3, 10)));
        when(random.nextInt(anyInt())).thenReturn(0);

        AtomicLong ids = new AtomicLong(10);
        doAnswer(invocation -> {
            List<CustomerOrder> orders = invocation.getArgument(0);
            orders.forEach(order -> order.setId(ids.getAndIncrement()));
            return true;
        }).when(customerOrderService).saveBatch(anyList());
    }

    @Test
    void firstVisitImmediatelyCreatesFiveWaitingCustomersWithSnapshottedOrders() {
        CustomerQueueVO queue = service.getQueue(7L);

        assertEquals(5, queue.getCustomers().size());
        assertNull(queue.getNextCustomerArrivalAt());
        assertEquals(List.of(1, 2, 3, 4, 5),
                queue.getCustomers().stream().map(CustomerQueueVO.CustomerView::getQueuePosition).toList());
        assertTrue(queue.getCustomers().stream().allMatch(customer ->
                customer.getRecipeId().equals("strawberry_juice")
                        && customer.getQuantity() == 1
                        && customer.getUnitGold() == 30
                        && customer.getUnitExp() == 5
                        && customer.getExpectedGold() == 30
                        && customer.getExpectedExp() == 5
                        && customer.getStatus().equals("WAITING")));
        verify(customerOrderService).saveBatch(anyList());
        verify(arrivalStateService).lockOrCreate(7L);
    }

    @Test
    void vacancyWaitsTwoMinutesThenAddsOnlyOneCustomer() {
        CustomerArrivalState state = arrivalState(7L, "2026-07-29T08:02:00");
        List<CustomerOrder> fourWaiting = waitingOrders(7L, 4);
        when(arrivalStateService.lockOrCreate(7L)).thenReturn(state);
        when(customerOrderService.listWaiting(7L)).thenReturn(fourWaiting);

        clock.advanceSeconds(119);
        CustomerQueueVO beforeDue = service.getQueue(7L);
        assertEquals(4, beforeDue.getCustomers().size());
        verify(customerOrderService, never()).save(any(CustomerOrder.class));

        clock.advanceSeconds(1);
        CustomerQueueVO atDue = service.getQueue(7L);
        assertEquals(5, atDue.getCustomers().size());
        assertNull(atDue.getNextCustomerArrivalAt());
        verify(customerOrderService).save(any(CustomerOrder.class));
    }

    @Test
    void offlineCatchUpUsesTwoMinuteRoundsAndNeverExceedsFiveCustomers() {
        CustomerArrivalState state = arrivalState(7L, "2026-07-29T08:02:00");
        when(arrivalStateService.lockOrCreate(7L)).thenReturn(state);
        when(customerOrderService.listWaiting(7L)).thenReturn(waitingOrders(7L, 2));
        clock.advanceSeconds(361);

        CustomerQueueVO queue = service.getQueue(7L);

        assertEquals(5, queue.getCustomers().size());
        assertNull(queue.getNextCustomerArrivalAt());
        verify(customerOrderService, times(3)).save(any(CustomerOrder.class));
    }

    @Test
    void fullQueueStopsTimerAndDepartureStartsANewFullInterval() {
        CustomerArrivalState state = arrivalState(7L, "2026-07-29T07:00:00");
        when(arrivalStateService.lockOrCreate(7L)).thenReturn(state);
        when(customerOrderService.listWaiting(7L)).thenReturn(waitingOrders(7L, 5));

        CustomerQueueVO full = service.getQueue(7L);
        assertNull(full.getNextCustomerArrivalAt());
        verify(arrivalStateService).setNextArrivalAt(7L, null);
        verify(customerOrderService, never()).save(any(CustomerOrder.class));

        List<CustomerOrder> afterMiddleDeparture = new java.util.ArrayList<>(waitingOrders(7L, 4));
        afterMiddleDeparture.get(1).setQueuePosition(3);
        afterMiddleDeparture.get(2).setQueuePosition(4);
        afterMiddleDeparture.get(3).setQueuePosition(5);
        when(customerOrderService.listWaiting(7L)).thenReturn(afterMiddleDeparture);
        service.recordDeparture(7L);

        assertEquals(LocalDateTime.parse("2026-07-29T08:02:00"), state.getNextArrivalAt());
        verify(arrivalStateService).setNextArrivalAt(
                7L, LocalDateTime.parse("2026-07-29T08:02:00"));
        assertEquals(List.of(1, 2, 3, 4),
                afterMiddleDeparture.stream().map(CustomerOrder::getQueuePosition).toList());
        verify(customerOrderService).updateBatchById(afterMiddleDeparture);
    }

    @Test
    void configuredQuantityWeightSelectsTwoAtTheSixtyPercentBoundary() {
        CustomerArrivalState state = arrivalState(7L, "2026-07-29T08:00:00");
        when(arrivalStateService.lockOrCreate(7L)).thenReturn(state);
        when(customerOrderService.listWaiting(7L)).thenReturn(waitingOrders(7L, 4));
        when(recipeConfigService.getById("strawberry_juice"))
                .thenReturn(recipe("strawberry_juice", "草莓汁", 30, 5, 101));
        when(random.nextInt(1)).thenReturn(0);
        when(random.nextInt(101)).thenReturn(0);
        when(random.nextInt(100)).thenReturn(60);

        CustomerQueueVO queue = service.getQueue(7L);

        assertEquals(2, queue.getCustomers().get(4).getQuantity());
    }

    @Test
    void configuredQuantityWeightSelectsThreeAtTheNinetyPercentBoundary() {
        CustomerArrivalState state = arrivalState(7L, "2026-07-29T08:00:00");
        when(arrivalStateService.lockOrCreate(7L)).thenReturn(state);
        when(customerOrderService.listWaiting(7L)).thenReturn(waitingOrders(7L, 4));
        when(recipeConfigService.getById("strawberry_juice"))
                .thenReturn(recipe("strawberry_juice", "草莓汁", 30, 5, 101));
        when(random.nextInt(1)).thenReturn(0);
        when(random.nextInt(101)).thenReturn(0);
        when(random.nextInt(100)).thenReturn(90);

        CustomerQueueVO queue = service.getQueue(7L);

        assertEquals(3, queue.getCustomers().get(4).getQuantity());
    }

    @Test
    void queueSerializesOnArrivalStateBeforeReadingWaitingOrders() {
        CustomerArrivalState state = arrivalState(7L, "2026-07-29T08:02:00");
        when(arrivalStateService.lockOrCreate(7L)).thenReturn(state);
        when(customerOrderService.listWaiting(7L)).thenReturn(waitingOrders(7L, 4));

        service.getQueue(7L);

        var calls = inOrder(arrivalStateService, customerOrderService);
        calls.verify(arrivalStateService).lockOrCreate(7L);
        calls.verify(customerOrderService).listWaiting(7L);
    }

    @Test
    void expandedQueueWaitsOneConfiguredIntervalInsteadOfFillingImmediately() {
        DrinkShopLevelConfig levelTwo = new DrinkShopLevelConfig();
        levelTwo.setLevel(2);
        levelTwo.setQueueCapacity(6);
        levelTwo.setArrivalIntervalSeconds(120);
        when(drinkShopService.getActiveConfig(7L)).thenReturn(levelTwo);
        CustomerArrivalState state = arrivalState(7L, null);
        when(arrivalStateService.lockOrCreate(7L)).thenReturn(state);
        when(customerOrderService.listWaiting(7L)).thenReturn(waitingOrders(7L, 5));

        CustomerQueueVO expanded = service.getQueue(7L);
        assertEquals(5, expanded.getCustomers().size());
        assertEquals(6, expanded.getCapacity());
        assertEquals(2, expanded.getShopLevel());
        assertEquals(LocalDateTime.parse("2026-07-29T08:02:00"), expanded.getNextCustomerArrivalAt());

        clock.advanceSeconds(120);
        assertEquals(6, service.getQueue(7L).getCustomers().size());
    }

    @Test
    void lowerConfiguredCapacityKeepsExistingCustomersAndStopsRefill() {
        DrinkShopLevelConfig lowered = new DrinkShopLevelConfig();
        lowered.setLevel(1);
        lowered.setQueueCapacity(5);
        lowered.setArrivalIntervalSeconds(120);
        when(drinkShopService.getActiveConfig(7L)).thenReturn(lowered);
        CustomerArrivalState state = arrivalState(7L, "2026-07-29T08:00:00");
        when(arrivalStateService.lockOrCreate(7L)).thenReturn(state);
        when(customerOrderService.listWaiting(7L)).thenReturn(waitingOrders(7L, 6));

        CustomerQueueVO queue = service.getQueue(7L);
        assertEquals(6, queue.getCustomers().size());
        assertNull(queue.getNextCustomerArrivalAt());
        verify(customerOrderService, never()).save(any(CustomerOrder.class));
    }

    private CustomerTemplate customer(String id, String name, String avatar) {
        CustomerTemplate value = new CustomerTemplate();
        value.setId(id);
        value.setName(name);
        value.setAvatar(avatar);
        return value;
    }

    private PlayerRecipe qualification(Long playerId, String recipeId) {
        PlayerRecipe value = new PlayerRecipe();
        value.setPlayerId(playerId);
        value.setRecipeId(recipeId);
        return value;
    }

    private RecipeConfig recipe(String id, String name, int gold, int exp, int weight) {
        RecipeConfig value = new RecipeConfig();
        value.setId(id);
        value.setName(name);
        value.setOutputItem(id);
        value.setSaleGold(gold);
        value.setSaleExp(exp);
        value.setOrderWeight(weight);
        value.setEnabled(1);
        return value;
    }

    private OrderQuantityWeight quantity(int quantity, int weight) {
        OrderQuantityWeight value = new OrderQuantityWeight();
        value.setQuantity(quantity);
        value.setWeight(weight);
        value.setEnabled(1);
        return value;
    }

    private CustomerArrivalState arrivalState(Long playerId, String nextArrivalAt) {
        CustomerArrivalState state = new CustomerArrivalState();
        state.setId(1L);
        state.setPlayerId(playerId);
        state.setNextArrivalAt(nextArrivalAt == null ? null : LocalDateTime.parse(nextArrivalAt));
        return state;
    }

    private List<CustomerOrder> waitingOrders(Long playerId, int count) {
        return java.util.stream.IntStream.rangeClosed(1, count).mapToObj(position -> {
            CustomerOrder order = new CustomerOrder();
            order.setId((long) position);
            order.setPlayerId(playerId);
            order.setCustomerId("berry");
            order.setRecipeId("strawberry_juice");
            order.setItemId("strawberry_juice");
            order.setQuantity(1);
            order.setUnitGoldSnapshot(30);
            order.setUnitExpSnapshot(5);
            order.setQueuePosition(position);
            order.setStatus("WAITING");
            order.setCreateTime(LocalDateTime.parse("2026-07-29T07:00:00"));
            return order;
        }).toList();
    }

    private static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        void advanceSeconds(long seconds) {
            current = current.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
