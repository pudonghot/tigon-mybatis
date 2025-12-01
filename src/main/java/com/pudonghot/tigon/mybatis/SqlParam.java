package com.pudonghot.tigon.mybatis;

import lombok.Getter;
import java.io.Serializable;
import lombok.RequiredArgsConstructor;

/**
 * @author Donghuang
 * @date 2017/2/6 9:48
 */
@Getter
@RequiredArgsConstructor
public class SqlParam implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean raw;
    private final Object value;
    private final boolean ignoreNull;

    public static SqlParam rawVal(final Object val) {
        return new SqlParam(true, val, false);
    }

    public static SqlParam val(final Object val) {
        return new SqlParam(false, val, false);
    }

    public static SqlParam val(final Object val, boolean ignoreNull) {
        return new SqlParam(false, val, ignoreNull);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.valueOf(raw ? value : "[" + value + "]");
    }
}
