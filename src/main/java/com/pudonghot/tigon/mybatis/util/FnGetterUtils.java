package com.pudonghot.tigon.mybatis.util;

import lombok.val;
import java.util.Map;
import lombok.SneakyThrows;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import java.lang.invoke.SerializedLambda;
import org.springframework.util.StringUtils;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Getter utils
 * get name of field by getter function
 *
 * @author Donghuang
 * @date Feb 02, 2025 22:50:12
 */
@Slf4j
public class FnGetterUtils {
    private static final Map<FnGetter<?, ?>, String> CACHE = new ConcurrentHashMap<>();
    private static final Pattern PATTERN_GETTER = Pattern.compile("^(?:get|is)(\\w+)$");

    /**
     * get field name from getter function
     *
     * @param getter getter function
     * @return field name
     * @param <T> entity type
     * @param <R> field type
     */
    public static <T, R> String getFieldName(final FnGetter<T, R> getter) {
        return CACHE.computeIfAbsent(getter, FnGetterUtils::doGetFieldName);
    }

    static <T, R> String doGetFieldName(final FnGetter<T, R> getter) {
        val methodName = getMethodName(getter);
        val matcher = PATTERN_GETTER.matcher(methodName);
        if (matcher.find()) {
            return StringUtils.uncapitalize(matcher.group(1));
        }

        throw new IllegalStateException(
                "Method [" + methodName + "] is not a valid getter function");
    }

    @SneakyThrows
    static <T, R> String getMethodName(final FnGetter<T, R> getter) {
        val method = getter.getClass().getDeclaredMethod("writeReplace");
        method.setAccessible(Boolean.TRUE);
        return ((SerializedLambda) method.invoke(getter)).getImplMethodName();
    }
}
