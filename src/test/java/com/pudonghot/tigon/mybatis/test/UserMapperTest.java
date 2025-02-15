package com.pudonghot.tigon.mybatis.test;

import lombok.val;
import java.util.*;
import lombok.var;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import com.pudonghot.tigon.mybatis.Search;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.RandomUtils;
import com.pudonghot.tigon.mybatis.TestDriver;
import com.pudonghot.tigon.mybatis.UpdateObj;
import com.pudonghot.tigon.mybatis.entity.User;
import com.pudonghot.tigon.mybatis.util.FnGetter;
import org.apache.commons.lang3.RandomStringUtils;
import com.pudonghot.tigon.mybatis.mapper.UserMapper;
import com.pudonghot.tigon.mybatis.util.FnGetterUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

/**
 * @author Donghuang
 * @date Sep 03, 2020 14:37:38
 */
@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestDriver.class)
public class UserMapperTest extends AbstractTransactionalJUnit4SpringContextTests {
    @Autowired
    private UserMapper mapper;
    private final int testCaseSize = 17;
    private final String donghuang = "donghuang";

    @Before
    public void setup() {
        Assert.state("tb_user".equals(mapper.getTable()), "Table name assert failed");

        val userList = new ArrayList<User>(10);

        // birthdate from 1980 to 2000
        val dateFrom = Calendar.getInstance();
        dateFrom.set(1980, 1, 1, 0, 0);
        val dateTo = Calendar.getInstance();
        dateTo.set(2000, 1, 1, 0, 0);

        for (int i = 0; i < testCaseSize - 1; ++i) {
            val user = new User();
            user.setName("User " + i);
            user.setAccount("account" + i);
            user.setMobile("1376" + RandomStringUtils.randomNumeric(7));
            user.setPassword(RandomStringUtils.randomAlphanumeric(16));
            user.setGender(RandomUtils.nextInt(0, 2) > 0 ?
                User.Gender.MALE : User.Gender.FEMALE);

            user.setBirthDate(new Date(
                RandomUtils.nextLong(dateFrom.getTimeInMillis(),
                    dateTo.getTimeInMillis())));

            user.setCity(RandomUtils.nextInt(0, 2) > 0 ?
                "Hangzhou" : "Shanghai");

            user.setActive(true);
            user.setRemark("Init remark");
            user.setCreatedBy(donghuang);
            user.setCreatedAt(new Date());
            userList.add(user);
        }
        mapper.insert(userList);

        val user = new User();
        user.setName(StringUtils.capitalize(donghuang));
        user.setAccount(donghuang);
        user.setMobile("1376" + RandomStringUtils.randomNumeric(7));
        user.setPassword(RandomStringUtils.randomAlphanumeric(16));
        user.setGender(User.Gender.MALE);
        user.setBirthDate(new Date(
            RandomUtils.nextLong(dateFrom.getTimeInMillis(),
                dateTo.getTimeInMillis())));
        user.setCity("Shanghai");

        user.setActive(true);
        user.setRemark("Uncle Donghuang");
        user.setCreatedBy("donghuang");
        user.setCreatedAt(new Date());
        mapper.insert(user);
        // Assert.state(user.getId() != null, "Test @UseGeneratedKeys failed");
    }

    @Test
    public void testRun() {
        val userListFound = mapper.list(new Search());
        Assert.state(userListFound.size() == testCaseSize, "Test list failed");
        Assert.state(mapper.list(Arrays.asList(1, 2, 3)).size() == 3,
            "Test list by id collection failed");
        Assert.state(mapper.list(new Integer[]{1, 2, 3}).size() == 3,
            "Test list by id array failed");
        Assert.state(mapper.count(null) == testCaseSize, "Test count failed");

        Assert.state(mapper.count(Search.of(User::getAccount, donghuang)) == 1, "Test count failed");
        Assert.state(mapper.find(Search.of(User::getAccount, donghuang)).getAccount().equals(donghuang), "Test find failed");
        Assert.state(mapper.exists(Search.of(User::getAccount, donghuang)), "Test exists failed");

        for (val user : userListFound) {
            log.info("User [{}] found.", user);
            user.setUpdatedBy(donghuang);
            user.setUpdatedAt(new Date());
            mapper.update(user);

            val update = UpdateObj.of(User::getRemark, user.getRemark() + " Updated");
            mapper.update(update, user.getId());
        }

        val userDonghuang = mapper.findByAccount(donghuang);
        Assert.state(userDonghuang != null, "Test find failed");
        Assert.state(userDonghuang.getAccessKey().length() == 16, "Test @RawValue failed");
        userDonghuang.setCity("Beijing");
        userDonghuang.setAccount("UpdateWillBeIgnored");
        mapper.update(userDonghuang);

        val account10Updated = mapper.find(userDonghuang.getId());
        Assert.state(userDonghuang.getCity().equals(
                account10Updated.getCity()),
            "Test update failed");
        Assert.state(donghuang.equals(account10Updated.getAccount()),
            "Account should not be updated");

        Assert.state(mapper.listByName("Donghuang").size() == 1, "Test listByName failed");

        val updatedBy = "donghuang.cxn";
        val update = UpdateObj.of(User::getRemark, "update remark id gt 3 and lt 6")
            .set(User::getUpdatedBy, updatedBy)
            .set(User::getUpdatedAt, new Date());
        mapper.update(update, Search.of().gt(User::getId, 3).lt(User::getId, 6));
        Assert.state(mapper.find(4).getUpdatedBy().equals(updatedBy),
            "Test update map failed");
        Assert.state(mapper.find(5).getUpdatedBy().equals(updatedBy),
            "Test update map failed");

        mapper.update(User::getCity, "SHANGHAI", Search.of(3));
        Assert.state(mapper.find(3).getCity().equals("SHANGHAI"), "Test update field failed");

        mapper.setNull(User::getRemark, new Search(3));
        var user3 = mapper.find(3);
        Assert.state(user3.getRemark() == null, "Test setNull failed");
        mapper.setNull(Arrays.asList((FnGetter<User, ?>) User::getRemark, User::getUpdatedBy), Search.of(3));

        user3 = mapper.find(3);
        Assert.state(user3.getRemark() == null &&
            user3.getUpdatedBy() == null, "Test setNull failed");
        Assert.state(mapper.find(Search.of(3)
                        .isNull(User::getUpdatedBy)).getRemark() == null,
            "Test isNull failed");
        Assert.state(mapper.find(Search.of(3)
                        .eq(User::getUpdatedBy, null)).getRemark() == null,
            "Test eq NULL failed");
        Assert.state(mapper.find(
                Search.of(10).notNull(User::getUpdatedBy)).getRemark() != null,
            "Test notNull failed");
        Assert.state(mapper.find(
                Search.of(10).ne(User::getUpdatedBy, null)).getRemark() != null,
            "Test notNull failed");

        mapper.setNull(FnGetterUtils.getFieldName(User::getRemark), Search.of().between(User::getId, 6, 8));

        mapper.update(FnGetterUtils.getFieldName(User::getRemark), "AA", 6);
        Assert.state(mapper.find(6).getRemark().equals("AA"), "Test update col failed");
        mapper.update(FnGetterUtils.getFieldName(User::getRemark), "BB", new Search(7));
        Assert.state(mapper.find(7).getRemark().equals("BB"), "Test update col failed");

        val id = mapper.findCol(User::getId, Search.of(1));
        Assert.state(Integer.valueOf(1).equals(id), "Test findCol failed");
        final List<Integer> idList = mapper.listCol("`tb_user`.id", Search.of(1));
        Assert.state(Integer.valueOf(1).equals(idList.get(0)), "Test listCol failed");
        val idList2 = mapper.listCol(User::getId, Search.of(1));
        Assert.state(Integer.valueOf(1).equals(idList2.get(0)), "Test listCol failed");

        val userList13 = mapper.list(Search.of(
                arg -> arg.addSql("id % 2 in ").addParamList(1, 3)));
        for (val user : userList13) {
            val idMod2 = user.getId() % 2;
            Assert.state(idMod2 == 1 || idMod2 == 3, "Test Search#build failed");
        }

        val deleteId = 7;
        mapper.delete(deleteId);
        Assert.state(mapper.find(deleteId) == null, "Test delete failed");

        val deleteId2 = 9;
        mapper.delete(new Search(deleteId2));
        Assert.state(mapper.find(deleteId2) == null, "Test delete failed");
    }

    @Test
    public void testScan() {
        List<Integer> ids = Arrays.asList(10, 11, 13, 15);
        mapper.scan(3, new Search(ids), user -> {
            log.info("Scan user [{}].", user);
            Assert.state(ids.contains(user.getId()), "Test scan failed");
        });
    }
}
