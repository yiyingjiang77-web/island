package com.fruitisland.game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fruitisland.game.dto.SatisfactionStatusVO;
import com.fruitisland.game.entity.*;
import com.fruitisland.game.mapper.*;
import com.fruitisland.game.service.CustomerOrderService;
import com.fruitisland.game.service.SatisfactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SatisfactionServiceImpl implements SatisfactionService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private final CustomerOrderService customerOrderService;
    private final SatisfactionGiftConfigMapper giftConfigMapper;
    private final DailySatisfactionMapper dailyMapper;
    private final GamePlayerMapper playerMapper;
    private final Clock clock;

    @Override
    @Transactional
    public SatisfactionStatusVO getStatus(Long playerId) {
        List<SatisfactionStatusVO.History> rewards = settle(playerId);
        LocalDate today = LocalDate.now(clock.withZone(BUSINESS_ZONE));
        Stats todayStats = statsFor(orders(playerId), today);
        List<SatisfactionStatusVO.GiftRule> rules = rulesFor(today);
        Gift preview = tierFor(rules, todayStats.percent());
        SatisfactionStatusVO.GiftRule nextRule = rules.stream()
                .filter(rule -> rule.minimumPercent() > todayStats.percent())
                .findFirst().orElse(null);
        int minimumQuantity = preview == Gift.NONE
                ? rules.stream().mapToInt(SatisfactionStatusVO.GiftRule::minimumDeliveredQuantity)
                        .min().orElse(0)
                : preview.minimumQuantity;
        var todayView = new SatisfactionStatusVO.Today(today, todayStats.deliveredOrders,
                todayStats.rejectedOrders, todayStats.closedOrders, todayStats.deliveredQuantity,
                todayStats.percent(), preview.tier, preview.gold,
                Math.max(0, minimumQuantity - todayStats.deliveredQuantity),
                nextRule == null ? null : nextRule.tierCode(),
                nextRule == null ? 0 : nextRule.minimumPercent() - todayStats.percent());
        List<SatisfactionStatusVO.History> history = dailyMapper.selectList(
                        new LambdaQueryWrapper<DailySatisfaction>()
                                .eq(DailySatisfaction::getPlayerId, playerId)
                                .ge(DailySatisfaction::getBusinessDate, today.minusDays(29))
                                .orderByDesc(DailySatisfaction::getBusinessDate))
                .stream().map(this::history).toList();
        return new SatisfactionStatusVO(todayView, rules, history, rewards);
    }

    @Override
    @Transactional
    public List<SatisfactionStatusVO.History> settlePastDays(Long playerId) {
        return settle(playerId);
    }

    private List<SatisfactionStatusVO.History> settle(Long playerId) {
        LocalDate today = LocalDate.now(clock.withZone(BUSINESS_ZONE));
        List<CustomerOrder> orders = orders(playerId);
        TreeSet<LocalDate> dates = new TreeSet<>();
        orders.forEach(order -> { if (order.getCloseTime() != null) dates.add(order.getCloseTime().toLocalDate()); });
        dates.removeIf(date -> !date.isBefore(today));
        List<SatisfactionStatusVO.History> rewards = new ArrayList<>();
        for (LocalDate date : dates) {
            GamePlayer player = playerMapper.selectForUpdate(playerId);
            if (player == null) throw new IllegalArgumentException("玩家不存在");
            DailySatisfaction existing = dailyMapper.selectOne(new LambdaQueryWrapper<DailySatisfaction>()
                    .eq(DailySatisfaction::getPlayerId, playerId)
                    .eq(DailySatisfaction::getBusinessDate, date));
            if (existing != null) continue;
            Stats stats = statsFor(orders, date);
            if (stats.closedOrders == 0) continue;
            Gift gift = giftFor(date, stats);
            DailySatisfaction daily = new DailySatisfaction();
            daily.setPlayerId(playerId); daily.setBusinessDate(date);
            daily.setDeliveredOrders(stats.deliveredOrders); daily.setRejectedOrders(stats.rejectedOrders);
            daily.setClosedOrders(stats.closedOrders); daily.setDeliveredQuantity(stats.deliveredQuantity);
            daily.setSatisfactionPercent(stats.percent()); daily.setGiftTierSnapshot(gift.tier);
            daily.setRewardGoldSnapshot(gift.gold);
            daily.setRewardStatus(gift.gold > 0 ? "GRANTED" : "NOT_ELIGIBLE");
            daily.setSettledAt(LocalDateTime.ofInstant(clock.instant(), BUSINESS_ZONE));
            dailyMapper.insert(daily);
            if (gift.gold > 0) {
                player.setGold((player.getGold() == null ? 0 : player.getGold()) + gift.gold);
                playerMapper.updateById(player);
                rewards.add(history(daily));
            }
        }
        return rewards;
    }

    private List<CustomerOrder> orders(Long playerId) {
        return customerOrderService.lambdaQuery().eq(CustomerOrder::getPlayerId, playerId)
                .isNotNull(CustomerOrder::getCloseTime)
                .in(CustomerOrder::getStatus, List.of("DELIVERED", "OUT_OF_STOCK")).list();
    }

    private Stats statsFor(List<CustomerOrder> orders, LocalDate date) {
        int deliveredOrders = 0, rejected = 0, quantity = 0;
        for (CustomerOrder order : orders) {
            if (order.getCloseTime() == null || !order.getCloseTime().toLocalDate().equals(date)) continue;
            if ("DELIVERED".equals(order.getStatus())) {
                deliveredOrders++;
                quantity += order.getQuantity() == null ? 0 : Math.max(0, order.getQuantity());
            } else if ("OUT_OF_STOCK".equals(order.getStatus())) rejected++;
        }
        return new Stats(deliveredOrders, rejected, deliveredOrders + rejected, quantity);
    }

    private Gift giftFor(LocalDate date, Stats stats) {
        if (stats.closedOrders == 0) return Gift.NONE;
        Gift candidate = tierFor(date, stats.percent());
        return candidate != Gift.NONE && stats.deliveredQuantity >= candidate.minimumQuantity
                ? candidate : Gift.NONE;
    }

    private Gift tierFor(LocalDate date, int satisfactionPercent) {
        return tierFor(rulesFor(date), satisfactionPercent);
    }

    private Gift tierFor(List<SatisfactionStatusVO.GiftRule> rules, int satisfactionPercent) {
        return rules.stream()
                .filter(rule -> rule.minimumPercent() <= satisfactionPercent)
                .max(Comparator.comparingInt(SatisfactionStatusVO.GiftRule::minimumPercent))
                .map(rule -> new Gift(rule.tierCode(), rule.rewardGold(), rule.minimumDeliveredQuantity()))
                .orElse(Gift.NONE);
    }

    private List<SatisfactionStatusVO.GiftRule> rulesFor(LocalDate date) {
        Map<String, SatisfactionGiftConfig> latestByTier = new LinkedHashMap<>();
        giftConfigMapper.selectList(new LambdaQueryWrapper<SatisfactionGiftConfig>()
                        .eq(SatisfactionGiftConfig::getEnabled, 1)
                        .le(SatisfactionGiftConfig::getEffectiveFrom, date)
                        .orderByDesc(SatisfactionGiftConfig::getConfigVersion)
                        .orderByDesc(SatisfactionGiftConfig::getId))
                .forEach(config -> latestByTier.putIfAbsent(config.getTierCode(), config));
        return latestByTier.values().stream()
                .map(config -> new SatisfactionStatusVO.GiftRule(config.getTierCode(),
                        config.getMinimumPercent(), config.getMinimumDeliveredQuantity(),
                        config.getRewardGold()))
                .sorted(Comparator.comparingInt(SatisfactionStatusVO.GiftRule::minimumPercent))
                .toList();
    }

    private SatisfactionStatusVO.History history(DailySatisfaction d) {
        return new SatisfactionStatusVO.History(d.getBusinessDate(), d.getDeliveredOrders(),
                d.getRejectedOrders(), d.getClosedOrders(), d.getDeliveredQuantity(),
                d.getSatisfactionPercent(), d.getGiftTierSnapshot(),
                d.getRewardGoldSnapshot() == null ? 0 : d.getRewardGoldSnapshot(), d.getRewardStatus());
    }

    private record Stats(int deliveredOrders, int rejectedOrders, int closedOrders, int deliveredQuantity) {
        int percent() { return closedOrders == 0 ? 0 : deliveredOrders * 100 / closedOrders; }
    }
    private record Gift(String tier, long gold, int minimumQuantity) {
        static final Gift NONE = new Gift(null, 0, 0);
    }
}
