package com.fruitisland.game.util;

/**
 * Demo3.0 无限等级体系 — 经验公式与等级计算工具
 *
 * <p>经验公式: {@code expToNext(level) = 100 × level^1.3}
 *
 * <p>Lv1-20: 从 island_level_config 表读取累计经验阈值和奖励内容
 * <br>Lv21+: 使用公式运行时计算，不存表
 *
 * <p>三阶段设计:
 * <ul>
 *   <li>Lv1-20: 内容解锁期（每级解锁作物/配方/建筑等）</li>
 *   <li>Lv21-100: 精通成长期（数值提升，无新内容）</li>
 *   <li>Lv100+: 无限追求期（纯数值积累）</li>
 * </ul>
 *
 * <p><b>混合模式</b>: 因为 Lv1-20 的表配置经验值可能与纯公式不一致，
 * 所以 Lv21+ 的计算需要以表的 Lv20 阈值为起点，再用公式递推。
 */
public final class LevelFormulaUtil {

    /** 配置表覆盖的最高等级（Lv1-20 有表配置，Lv21+ 用公式） */
    public static final int MAX_TABLE_LEVEL = 20;

    private LevelFormulaUtil() {}

    /**
     * 从当前等级升到下一级所需的经验值。
     *
     * <p>公式: {@code 100 × level^1.3}
     *
     * @param level 当前等级 (≥1)
     * @return 升到 level+1 所需经验
     */
    public static long expToNext(int level) {
        if (level < 1) return 0;
        return (long) (100 * Math.pow(level, 1.3));
    }

    // ─── 纯公式模式（Lv21+ 使用） ───────────────────────────

    /**
     * 达到指定等级所需的累计经验（纯公式，从 Lv1 开始累加）。
     *
     * <p>注意：此方法不适用于 Lv1-20 的表配置等级，
     * 因为表的 cumulative_exp 可能与公式值不同。
     * Lv1-20 请从 island_level_config 表读取。
     *
     * @param level 目标等级 (≥1)
     * @return 纯公式计算的累计经验阈值
     */
    public static long cumulativeExpForLevel(int level) {
        if (level <= 1) return 0;
        long total = 0;
        for (int l = 1; l < level; l++) {
            total += expToNext(l);
        }
        return total;
    }

    /**
     * 根据累计经验计算等级（纯公式，不查表）。
     *
     * <p>注意：仅在 Lv21+ 场景使用。Lv1-20 应从表查找。
     *
     * @param cumulativeExp 累计经验
     * @return 纯公式计算的等级 (≥1)
     */
    public static int calculateLevel(long cumulativeExp) {
        if (cumulativeExp < 0) return 1;
        int level = 1;
        long needed = 0;
        while (needed + expToNext(level) <= cumulativeExp) {
            needed += expToNext(level);
            level++;
        }
        return level;
    }

    // ─── 混合模式（表 + 公式） ──────────────────────────────

    /**
     * 在表覆盖范围之外（Lv21+）计算等级。
     *
     * <p>以表的 Lv20 阈值为起点，用公式递推。
     *
     * @param cumulativeExp 当前累计经验
     * @param tableMaxThreshold 表中 Lv20 的累计经验阈值
     * @return 等级 (≥MAX_TABLE_LEVEL)
     */
    public static int calculateLevelBeyondTable(long cumulativeExp, long tableMaxThreshold) {
        int level = MAX_TABLE_LEVEL;
        long accumulated = tableMaxThreshold;
        while (accumulated + expToNext(level) <= cumulativeExp) {
            accumulated += expToNext(level);
            level++;
        }
        return level;
    }

    /**
     * 在表覆盖范围之外（Lv21+）计算下一级累计经验阈值。
     *
     * @param level 当前等级 (≥MAX_TABLE_LEVEL)
     * @param tableMaxThreshold 表中 Lv20 的累计经验阈值
     * @return 达到 level+1 所需的累计经验阈值
     */
    public static long nextThresholdBeyond(int level, long tableMaxThreshold) {
        long accumulated = tableMaxThreshold;
        for (int l = MAX_TABLE_LEVEL; l < level; l++) {
            accumulated += expToNext(l);
        }
        return accumulated + expToNext(level);
    }

    /**
     * 在表覆盖范围之外（Lv21+）计算当前等级的经验下限。
     *
     * @param level 当前等级 (≥MAX_TABLE_LEVEL)
     * @param tableMaxThreshold 表中 Lv20 的累计经验阈值
     * @return 达到 level 所需的累计经验阈值
     */
    public static long levelFloorBeyond(int level, long tableMaxThreshold) {
        if (level <= MAX_TABLE_LEVEL) return tableMaxThreshold;
        long accumulated = tableMaxThreshold;
        for (int l = MAX_TABLE_LEVEL; l < level; l++) {
            accumulated += expToNext(l);
        }
        return accumulated;
    }

    /**
     * 在表覆盖范围之外（Lv21+）计算距下一级还需多少经验。
     *
     * @param cumulativeExp 当前累计经验
     * @param level 当前等级
     * @param tableMaxThreshold 表中 Lv20 的累计经验阈值
     * @return 距离升级还差的经验
     */
    public static long expRemainingBeyond(long cumulativeExp, int level, long tableMaxThreshold) {
        long ceiling = nextThresholdBeyond(level, tableMaxThreshold);
        return ceiling - cumulativeExp;
    }
}
