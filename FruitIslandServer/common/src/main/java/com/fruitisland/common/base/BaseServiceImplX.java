package com.fruitisland.common.base;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * Base service implementation
 */
public abstract class BaseServiceImplX<M extends BaseMapperX<T>, T> extends ServiceImpl<M, T> implements BaseServiceX<T> {
}
