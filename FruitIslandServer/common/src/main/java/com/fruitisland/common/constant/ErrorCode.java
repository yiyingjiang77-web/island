package com.fruitisland.common.constant;

/**
 * Unified error codes
 *
 * <pre>
 * 0     : success
 * 10xxx : user / auth
 * 20xxx : resource / item
 * 30xxx : map / land
 * </pre>
 */
public class ErrorCode {

    // ==================== Success ====================
    public static final int SUCCESS = 0;

    // ==================== User 10xxx ====================
    /** 用户不存在 */
    public static final int USER_NOT_FOUND = 10001;
    /** Token失效 */
    public static final int TOKEN_EXPIRED = 10002;

    // ==================== Resource 20xxx ====================
    /** 金币不足 */
    public static final int COIN_NOT_ENOUGH = 20001;

    // ==================== Map / Land 30xxx ====================
    /** 土地未解锁 */
    public static final int LAND_LOCKED = 30001;

}
