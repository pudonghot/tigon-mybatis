package com.pudonghot.tigon.mybatis.mapper;

import java.util.List;

import com.pudonghot.tigon.mybatis.BaseMapper;
import com.pudonghot.tigon.mybatis.Search;
import com.pudonghot.tigon.mybatis.Table;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import com.pudonghot.tigon.mybatis.entity.User;

/**
 * @author Donghuang
 * @date Nov 15, 2020 22:42:55
 */
@Mapper
@Table("${user.table:tb_user}")
public interface UserMapper extends BaseMapper<Integer, User> {

    /**
     * find user by account
     *
     * @param account account
     * @return user found
     */
    default User findByAccount(String account) {
        return find(new Search("account", account));
    }

    /**
     * list user by name
     * @param name name
     * @return users found
     */
    List<User> listByName(@Param("name") String name);
}
