package com.pudonghot.tigon.test;

import com.pudonghot.tigon.mybatis.util.StrUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrUtilsTest {

    @Test
    void splitsAcronymsAndCamelCaseWords() {
        assertArrayEquals(new String[] {"JSON", "Object"}, StrUtils.splitCamel("JSONObject"));
        assertArrayEquals(new String[] {"My", "SQL"}, StrUtils.splitCamel("MySQL"));
        assertArrayEquals(new String[] {"99", "Roses"}, StrUtils.splitCamel("99Roses"));
        assertArrayEquals(new String[0], StrUtils.splitCamel(" "));
    }

    @Test
    void convertsBetweenCamelCaseAndUnderscoreNames() {
        assertEquals("created_at", StrUtils.camelToUnderscore("createdAt"));
        assertEquals("json_object", StrUtils.camelToUnderscore("JSONObject"));
        assertEquals("createdAt", StrUtils.underscoreToCamel("created_at"));
    }

    @Test
    void detectsBlankCharacterSequences() {
        assertTrue(StrUtils.isBlank(null));
        assertTrue(StrUtils.isBlank(" \t\n"));
        assertFalse(StrUtils.isBlank(" value "));
        assertTrue(StrUtils.isNotBlank("value"));
    }
}
