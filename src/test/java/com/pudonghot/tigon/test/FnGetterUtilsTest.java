package com.pudonghot.tigon.test;

import lombok.val;
import lombok.extern.slf4j.Slf4j;
import com.pudonghot.tigon.mybatis.entity.User;
import com.pudonghot.tigon.mybatis.util.FnGetter;
import com.pudonghot.tigon.mybatis.util.FnGetterUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Donghuang
 * @date Sep 02, 2020 11:55:43
 */
@Slf4j
public class FnGetterUtilsTest {

    @Test
    public void testGetFieldName() {
        val fieldName = getFieldName(User::getGender);
        log.info("Field name [{}].", fieldName);
        Assertions.assertEquals(fieldName, User.Fields.gender);
    }

    public <T, R> String getFieldName(final FnGetter<T, R> getter) {
        return FnGetterUtils.getFieldName(getter);
    }
}
