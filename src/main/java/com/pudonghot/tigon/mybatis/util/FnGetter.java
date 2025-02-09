package com.pudonghot.tigon.mybatis.util;

import java.io.Serializable;

/**
 * Function Getter
 *
 * @author Donghuang
 * @date Feb 03, 2025 11:31:29
 */
@FunctionalInterface
public interface FnGetter<T, R> extends Serializable {

    R get(T obj);
}
