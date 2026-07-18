package com.pudonghot.tigon.mybatis.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.pudonghot.tigon.mybatis.Search;
import com.pudonghot.tigon.mybatis.TestDriver;
import com.pudonghot.tigon.mybatis.UpdateObj;
import com.pudonghot.tigon.mybatis.entity.User;
import com.pudonghot.tigon.mybatis.mapper.UserMapper;
import com.pudonghot.tigon.mybatis.util.FnGetter;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
@SpringBootTest(classes = TestDriver.class)
class UserMapperTest {
    private static final int TEST_CASE_SIZE = 17;
    private static final String SPECIAL_ACCOUNT = "donghuang";

    @Autowired
    private UserMapper mapper;

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @BeforeEach
    void insertDeterministicFixtures() {
        assertEquals("tb_user", mapper.getTable());

        var users = new ArrayList<User>(TEST_CASE_SIZE);
        for (int i = 1; i <= TEST_CASE_SIZE; i++) {
            var user = new User();
            user.setName(i == TEST_CASE_SIZE ? "Donghuang" : "User " + i);
            user.setAccount(i == TEST_CASE_SIZE ? SPECIAL_ACCOUNT : "account-" + i);
            user.setMobile(String.format("1376000%04d", i));
            user.setPassword("password-" + i);
            user.setGender(i % 2 == 0 ? User.Gender.FEMALE : User.Gender.MALE);
            user.setBirthDate(new Date(315_532_800_000L + i * 86_400_000L));
            user.setCity(i % 2 == 0 ? "Hangzhou" : "Shanghai");
            user.setActive(true);
            user.setRemark("Remark " + i);
            user.setCreatedBy(SPECIAL_ACCOUNT);
            user.setCreatedAt(new Date(1_577_836_800_000L + i * 1_000L));
            users.add(user);
        }

        assertEquals(TEST_CASE_SIZE, mapper.insert(users));
    }

    @Test
    void registersGeneratedStatementsAndKeepsCustomMapperXml() {
        var configuration = sqlSessionFactory.getConfiguration();
        var namespace = UserMapper.class.getName() + ".";

        for (var id : List.of("count", "exists", "find", "findCol", "list", "listCol",
                "select", "insert", "update", "setNull", "delete", "listByName")) {
            assertTrue(configuration.hasStatement(namespace + id), id + " should be registered");
        }

        assertEquals(1, mapper.listByName("Donghuang").size());
    }

    @Test
    void queriesByPrimaryKeySearchAndSelectedColumns() {
        assertEquals(TEST_CASE_SIZE, mapper.list(Search.of()).size());
        assertEquals(TEST_CASE_SIZE, mapper.count(null));
        assertEquals(3, mapper.list(List.of(1, 2, 3)).size());
        assertEquals(3, mapper.list(new Integer[] {1, 2, 3}).size());

        var special = mapper.findByAccount(SPECIAL_ACCOUNT);
        assertNotNull(special);
        assertEquals(SPECIAL_ACCOUNT, special.getAccount());
        assertEquals(SPECIAL_ACCOUNT,
            mapper.find(Search.of(User::getAccount, SPECIAL_ACCOUNT).distinct(true)).getAccount());
        assertTrue(mapper.exists(Search.of(User::getAccount, SPECIAL_ACCOUNT)));
        assertFalse(mapper.exists(Search.of(User::getAccount, "missing")));

        assertEquals(1, mapper.findCol(User::getId, Search.of(1)));
        assertEquals(List.of(1, 2, 3),
            mapper.listCol(User::getId, Search.of(List.of(1, 2, 3)).asc(User::getId)));

        var oddUsers = mapper.list(Search.of(
            arg -> arg.addSql("id % 2 = ").addParam(1)));
        assertEquals(9, oddUsers.size());
        assertTrue(oddUsers.stream().allMatch(user -> user.getId() % 2 == 1));

        assertEquals(1, mapper.select("ifnull(max(id), 0)", Search.of().limit(1)).size());
    }

    @Test
    void updatesEntitiesMapsAndIndividualColumns() {
        var user = mapper.findByAccount(SPECIAL_ACCOUNT);
        assertEquals(16, user.getAccessKey().length());

        user.setCity("Beijing");
        user.setAccount("must-not-change");
        assertEquals(1, mapper.update(user));

        var updated = mapper.find(user.getId());
        assertEquals("Beijing", updated.getCity());
        assertEquals(SPECIAL_ACCOUNT, updated.getAccount());

        var update = UpdateObj.of(User::getRemark, "updated by map")
            .set(User::getUpdatedBy, "tester")
            .set(User::getUpdatedAt, new Date(1_600_000_000_000L));
        assertEquals(2, mapper.update(update, Search.of().gt(User::getId, 3).lt(User::getId, 6)));
        assertEquals("tester", mapper.find(4).getUpdatedBy());
        assertEquals("tester", mapper.find(5).getUpdatedBy());

        assertEquals(1, mapper.update(User::getCity, "SHANGHAI", 3));
        assertEquals("SHANGHAI", mapper.find(3).getCity());
        assertEquals(1, mapper.update("remark", "changed", Search.of(6)));
        assertEquals("changed", mapper.find(6).getRemark());
    }

    @Test
    void setsOneOrMoreColumnsToNull() {
        assertEquals(1, mapper.setNull(User::getRemark, Search.of(3)));
        assertNull(mapper.find(3).getRemark());

        assertEquals(1, mapper.setNull(
            List.of((FnGetter<User, ?>) User::getRemark, User::getUpdatedBy),
            Search.of(4)));
        var user = mapper.find(4);
        assertNull(user.getRemark());
        assertNull(user.getUpdatedBy());
        assertNotNull(mapper.find(Search.of(4).isNull(User::getUpdatedBy)));

        mapper.update(User::getUpdatedBy, "tester", 5);
        assertNotNull(mapper.find(Search.of(5).ne(User::getUpdatedBy, null)));
    }

    @Test
    void deletesByPrimaryKeyAndSearch() {
        assertEquals(1, mapper.delete(7));
        assertNull(mapper.find(7));

        assertEquals(1, mapper.delete(Search.of(9)));
        assertNull(mapper.find(9));
        assertEquals(TEST_CASE_SIZE - 2, mapper.count(null));
    }

    @Test
    void scansMatchingRowsInStablePages() {
        var ids = List.of(10, 11, 13, 15);
        var scanned = new ArrayList<Integer>();

        assertTrue(mapper.scan(3, Search.of(ids).asc(User::getId),
            user -> scanned.add(user.getId())));

        assertEquals(ids, scanned);
        assertEquals(Set.copyOf(ids), new HashSet<>(scanned));
    }
}
