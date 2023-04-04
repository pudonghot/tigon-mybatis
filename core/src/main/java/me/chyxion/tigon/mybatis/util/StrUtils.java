package me.chyxion.tigon.mybatis.util;

import lombok.val;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Donghuang
 * @date Oct 31, 2014 1:27:57 PM
 */
public class StrUtils {
    private static Pattern PATTERN_UNDERSCORE = Pattern.compile("_([a-zA-Z])");

    /**
     * split Camel Case
     * <pre>
     * nice - [nice]
     * World - [World]
     * MySQL - [My SQL]
     * HTML - [HTML]
     * JSONObject - [JSON Object]
     * JPanel - [J Panel]
     * toJSONString - [to JSON String]
     * Log4j - [Log4j]
     * 99Roses - [99 Roses]
     * DO178 - [DO178]
     * Do178 - [Do178]
     * </pre>
     * @param str word
     * @return split result
     */
    public static String[] splitCamel(final String str) {
        return StrUtils.isNotBlank(str) ?
                    // JSONObject - JSON Object
                    // 99Rose - 99 Rose
                str.split(new StringBuilder("(?<=[0-9A-Z])(?=[A-Z][a-z])")
                    // MySQL - My SQL
                    .append("|(?<=[a-z])(?=[A-Z])")
                    .toString()) :
                new String[0];
    }

    /**
     * FooBar - foo_bar
     *
     * @param str FooBar
     * @return foo_bar
     */
    public static String camelToUnderscore(final String str) {
        return Arrays.stream(splitCamel(str)).collect(Collectors.joining("_")).toLowerCase();
    }

    /**
     * foo_bar -> fooBar
     *
     * @param str foo_bar
     * @return FooBar
     */
    public static String underscoreToCamel(final String str) {
        val m = PATTERN_UNDERSCORE.matcher(str);
        val sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, m.group(1).toUpperCase());
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }
}
