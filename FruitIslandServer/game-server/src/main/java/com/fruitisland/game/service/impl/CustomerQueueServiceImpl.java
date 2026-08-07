package com.fruitisland.game.service.impl;

import com.fruitisland.game.dto.CustomerQueueVO;
import com.fruitisland.game.entity.*;
import com.fruitisland.game.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerQueueServiceImpl implements CustomerQueueService {
    private final CustomerOrderService customerOrderService;
    private final CustomerArrivalStateService arrivalStateService;
    private final CustomerTemplateService customerTemplateService;
    private final PlayerRecipeService playerRecipeService;
    private final RecipeConfigService recipeConfigService;
    private final OrderQuantityWeightService quantityWeightService;
    private final Clock clock;
    private final RandomGenerator random;
    private final DrinkShopService drinkShopService;

    @Override
    @Transactional
    public CustomerQueueVO getQueue(Long playerId) {
        CustomerArrivalState state = arrivalStateService.lockOrCreate(playerId);
        DrinkShopLevelConfig config = drinkShopService.getActiveConfig(playerId);
        int capacity = config.getQueueCapacity();
        int arrivalInterval = config.getArrivalIntervalSeconds();
        List<CustomerOrder> waiting = new ArrayList<>(customerOrderService.listWaiting(playerId));
        if (waiting.isEmpty() && state.getNextArrivalAt() == null) {
            List<CustomerOrder> initial = generateOrders(playerId, capacity, 1);
            customerOrderService.saveBatch(initial);
            waiting.addAll(initial);
        } else {
            reconcileArrivals(playerId, waiting, state, capacity, arrivalInterval);
        }
        return toView(waiting, state, config);
    }

    @Override
    @Transactional
    public void recordDeparture(Long playerId) {
        CustomerArrivalState state = arrivalStateService.lockOrCreate(playerId);
        DrinkShopLevelConfig config = drinkShopService.getActiveConfig(playerId);
        List<CustomerOrder> waiting = customerOrderService.listWaiting(playerId);
        for (int index = 0; index < waiting.size(); index++) {
            waiting.get(index).setQueuePosition(index + 1);
        }
        if (!waiting.isEmpty()) customerOrderService.updateBatchById(waiting);
        LocalDateTime nextArrivalAt = waiting.size() < config.getQueueCapacity()
                ? LocalDateTime.now(clock).plusSeconds(config.getArrivalIntervalSeconds())
                : null;
        state.setNextArrivalAt(nextArrivalAt);
        arrivalStateService.setNextArrivalAt(playerId, nextArrivalAt);
    }

    private void reconcileArrivals(
            Long playerId,
            List<CustomerOrder> waiting,
            CustomerArrivalState state,
            int capacity,
            int arrivalInterval
    ) {
        boolean changed = false;
        if (waiting.size() >= capacity) {
            if (state.getNextArrivalAt() != null) {
                state.setNextArrivalAt(null);
                changed = true;
            }
        } else {
            LocalDateTime now = LocalDateTime.now(clock);
            if (state.getNextArrivalAt() == null) {
                state.setNextArrivalAt(now.plusSeconds(arrivalInterval));
                changed = true;
            }
            while (state.getNextArrivalAt() != null
                    && !now.isBefore(state.getNextArrivalAt())
                    && waiting.size() < capacity) {
                CustomerOrder order = generateOrders(playerId, 1, waiting.size() + 1).get(0);
                customerOrderService.save(order);
                waiting.add(order);
                state.setNextArrivalAt(
                        state.getNextArrivalAt().plusSeconds(arrivalInterval));
                changed = true;
            }
            if (waiting.size() >= capacity && state.getNextArrivalAt() != null) {
                state.setNextArrivalAt(null);
                changed = true;
            }
        }
        if (changed) arrivalStateService.setNextArrivalAt(playerId, state.getNextArrivalAt());
    }

    private List<CustomerOrder> generateOrders(Long playerId, int count, int firstPosition) {
        List<CustomerTemplate> customers = customerTemplateService.list();
        if (customers.isEmpty()) throw new IllegalStateException("顾客模板未配置");
        List<RecipeConfig> recipes = playerRecipeService.listByPlayer(playerId).stream()
                .map(qualification -> recipeConfigService.getById(qualification.getRecipeId()))
                .filter(recipe -> recipe != null && Integer.valueOf(1).equals(recipe.getEnabled()))
                .toList();
        if (recipes.isEmpty()) throw new IllegalStateException("没有可生成订单的有效配方");
        List<OrderQuantityWeight> quantities = quantityWeightService.listEnabled();
        if (quantities.isEmpty()) throw new IllegalStateException("订单数量权重未配置");

        LocalDateTime now = LocalDateTime.now(clock);
        List<CustomerOrder> orders = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            CustomerTemplate customer = customers.get(random.nextInt(customers.size()));
            RecipeConfig recipe = weighted(
                    recipes, value -> valueOrDefault(value.getOrderWeight(), 1));
            OrderQuantityWeight quantity = weighted(quantities, value -> valueOrDefault(value.getWeight(), 0));
            CustomerOrder order = new CustomerOrder();
            order.setPlayerId(playerId);
            order.setCustomerId(customer.getId());
            order.setRecipeId(recipe.getId());
            order.setItemId(recipe.getOutputItem());
            order.setQuantity(quantity.getQuantity());
            order.setUnitGoldSnapshot(valueOrDefault(recipe.getSaleGold(), 0));
            order.setUnitExpSnapshot(valueOrDefault(recipe.getSaleExp(), 0));
            order.setQueuePosition(firstPosition + index);
            order.setStatus("WAITING");
            order.setCreateTime(now);
            orders.add(order);
        }
        return orders;
    }

    private <T> T weighted(List<T> values, ToIntFunction<T> weight) {
        int total = values.stream().mapToInt(weight).sum();
        if (total <= 0) throw new IllegalStateException("随机权重必须为正数");
        int draw = random.nextInt(total);
        for (T value : values) {
            draw -= weight.applyAsInt(value);
            if (draw < 0) return value;
        }
        return values.get(values.size() - 1);
    }

    private CustomerQueueVO toView(List<CustomerOrder> orders, CustomerArrivalState state,
                                   DrinkShopLevelConfig config) {
        Map<String, CustomerTemplate> customers = customerTemplateService.list().stream()
                .collect(Collectors.toMap(CustomerTemplate::getId, value -> value));
        List<CustomerQueueVO.CustomerView> views = orders.stream().map(order -> {
            CustomerTemplate customer = customers.get(order.getCustomerId());
            int quantity = valueOrDefault(order.getQuantity(), 0);
            int gold = valueOrDefault(order.getUnitGoldSnapshot(), 0);
            int exp = valueOrDefault(order.getUnitExpSnapshot(), 0);
            return new CustomerQueueVO.CustomerView(
                    order.getId(),
                    valueOrDefault(order.getQueuePosition(), 0),
                    order.getCustomerId(),
                    customer == null ? order.getCustomerId() : customer.getName(),
                    customer == null ? "" : customer.getAvatar(),
                    order.getRecipeId(),
                    order.getItemId(),
                    quantity,
                    gold,
                    exp,
                    gold * quantity,
                    exp * quantity,
                    order.getStatus(),
                    order.getCreateTime()
            );
        }).toList();
        return new CustomerQueueVO(views, state.getNextArrivalAt(),
                config.getArrivalIntervalSeconds(), config.getLevel(), config.getQueueCapacity());
    }

    private static int valueOrDefault(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}
