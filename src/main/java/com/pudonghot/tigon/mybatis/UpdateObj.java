package com.pudonghot.tigon.mybatis;

import lombok.val;
import java.util.HashMap;
import com.pudonghot.tigon.mybatis.util.FnGetter;
import com.pudonghot.tigon.mybatis.util.FnGetterUtils;

/**
 * Object for update
 * @see BaseUpdateMapper#update(java.util.Map, Search)
 *
 * @author Donghuang
 * @date Feb 03, 2025 17:17:22
 */
public class UpdateObj extends HashMap<String, Object> {
    private static final long serialVersionUID = 1L;

    /**
     * Update builder
     *
     * @param field field
     * @param value value
     * @return update
     * @param <T>
     * @param <R>
     */
    public static <T, R> UpdateObj of(final FnGetter<T, R> field, final Object value) {
        val update = new UpdateObj();
        return update.set(field, value);
    }

    /**
     * set field
     *
     * @param field field
     * @param value value
     * @return this
     * @param <T>
     * @param <R>
     */
    public <T, R> UpdateObj set(final FnGetter<T, R> field, final Object value) {
        put(FnGetterUtils.getFieldName(field), value);
        return this;
    }
}
