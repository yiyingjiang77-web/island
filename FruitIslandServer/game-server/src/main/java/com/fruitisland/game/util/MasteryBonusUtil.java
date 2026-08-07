package com.fruitisland.game.util;

/**
 * Demo3.0 精通加成工具 — 等级驱动的数值提升（有上限）
 *
 * <p>三轴加成:
 * <ul>
 *   <li>产量: +floor(Lv/5)×5%, 上限 +100%（Lv100 封顶）</li>
 *   <li>售价: +floor(Lv/10)×10%, 上限 +50%（Lv50 封顶）</li>
 *   <li>生长: -Lv×0.5%, 下限 -40%（Lv80 封顶）</li>
 * </ul>
 *
 * <p>上限设计理由:
 * <ul>
 *   <li>产量上限 +100%（翻倍）: 足够有成就感但不会让材料溢出</li>
 *   <li>售价上限 +50%: 增长比产量慢，避免金币通胀</li>
 *   <li>生长下限 -40%（即 60%）: 保证作物仍有等待感</li>
 * </ul>
 */
public final class MasteryBonusUtil {

    private MasteryBonusUtil() {}

    /**
     * 产量倍率: 1 + min(1.0, floor(level/5) × 0.05)
     *
     * <p>Lv1-4: 1.0, Lv5-9: 1.05, Lv10-14: 1.10, ..., Lv100+: 2.0（封顶）
     *
     * @param level 玩家等级
     * @return 产量倍率 (1.0 ~ 2.0)
     */
    public static double yieldMultiplier(int level) {
        if (level < 1) return 1.0;
        return 1.0 + Math.min(1.0, Math.floor(level / 5.0) * 0.05);
    }

    /**
     * 售价倍率: 1 + min(0.5, floor(level/10) × 0.1)
     *
     * <p>Lv1-9: 1.0, Lv10-19: 1.1, Lv20-29: 1.2, ..., Lv50+: 1.5（封顶）
     *
     * @param level 玩家等级
     * @return 售价倍率 (1.0 ~ 1.5)
     */
    public static double priceMultiplier(int level) {
        if (level < 1) return 1.0;
        return 1.0 + Math.min(0.5, Math.floor(level / 10.0) * 0.1);
    }

    /**
     * 生长时间倍率: max(0.6, 1 - level × 0.005)
     *
     * <p>Lv1: 0.995, Lv10: 0.95, Lv40: 0.80, Lv80+: 0.60（封顶）
     *
     * @param level 玩家等级
     * @return 生长时间倍率 (0.6 ~ 1.0)
     */
    public static double growthMultiplier(int level) {
        if (level < 1) return 1.0;
        return Math.max(0.6, 1.0 - level * 0.005);
    }

    /**
     * 将基础产量按精通倍率放大（向下取整）。
     *
     * @param baseYield 基础产量
     * @param level 玩家等级
     * @return 精通后的产量
     */
    public static int applyYieldBonus(int baseYield, int level) {
        return (int) Math.floor(baseYield * yieldMultiplier(level));
    }

    /**
     * 将基础售价按精通倍率放大（向下取整）。
     *
     * @param basePrice 基础售价
     * @param level 玩家等级
     * @return 精通后的售价
     */
    public static int applyPriceBonus(int basePrice, int level) {
        return (int) Math.floor(basePrice * priceMultiplier(level));
    }

    /**
     * 将基础生长时间按精通倍率缩短（向上取整，保证至少 1 秒）。
     *
     * @param baseGrowthSeconds 基础生长时间（秒）
     * @param level 玩家等级
     * @return 精通后的生长时间（秒）
     */
    public static int applyGrowthBonus(int baseGrowthSeconds, int level) {
        return Math.max(1, (int) Math.ceil(baseGrowthSeconds * growthMultiplier(level)));
    }
}
