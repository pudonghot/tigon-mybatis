package com.pudonghot.tigon.test;

import com.pudonghot.tigon.mybatis.entity.User;
import com.pudonghot.tigon.mybatis.util.FnGetter;
import com.pudonghot.tigon.mybatis.util.FnGetterUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Donghuang
 * @date Sep 02, 2020 11:55:43
 */
class FnGetterUtilsTest {

    @Test
    void extractsFieldsFromGetAndIsMethods() {
        assertEquals(User.Fields.gender, getFieldName(User::getGender));
        assertEquals(User.Fields.active, getFieldName(User::getActive));
    }

    @Test
    void rejectsFunctionsThatAreNotGetterMethodReferences() {
        FnGetter<User, String> function = user -> user.getName();

        assertThrows(IllegalStateException.class, () -> getFieldName(function));
    }

    private <T, R> String getFieldName(final FnGetter<T, R> getter) {
        return FnGetterUtils.getFieldName(getter);
    }
}
