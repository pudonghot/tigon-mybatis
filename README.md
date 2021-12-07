# Tigon MyBatis

## 简介

Tigon MyBatis为Spring工程中MyBatis的Mapper提供增强，主要有以下特点

- 代码又少又壮，绝不做多余的事情
- 仅需Mapper继承接口，实现`增` `删` `改` `查`，无额外配置，爽到没女朋友
- 用完即走，毫不留恋

### 开始使用

- 引入Maven依赖

```xml
<dependency>
  <groupId>me.chyxion.tigon</groupId>
  <artifactId>tigon-mybatis</artifactId>
  <version>0.0.8</version>
</dependency>
```

### 使用示例

下面是使用示例，可以在源代码中找到更详细的单元测试。**Talk is cheep，read the fine source code.**

##### 定义Entity

```java
package me.chyxion.tigon.mybatis.entity;

import lombok.Getter;
import lombok.Setter;
import java.util.Date;
import lombok.ToString;
import java.io.Serializable;
import me.chyxion.tigon.mybatis.Table;
import me.chyxion.tigon.mybatis.NotUpdate;
import me.chyxion.tigon.mybatis.NotUpdateWhenNull;

@Getter
@Setter
@ToString
@Table("tb_user")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    // 标记账户不被更新
    @NotUpdate
    private String account;
    // 当手机号为null不被更新
    @NotUpdateWhenNull
    private String mobile;
    private String name;
    private Gender gender;
    private String password;
    private Date birthDate;
    private String city;
    private String avatar;

    private Boolean active;
    private String remark;
    private String createdBy;
    private Date createdAt;
    private String updatedBy;
    private Date updatedAt;

    public enum Gender {
        MALE,
        FEMALE
    }
}
```

##### 定义Mapper

```java
package me.chyxion.tigon.mybatis.mapper;

import java.util.List;
import me.chyxion.tigon.mybatis.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import me.chyxion.tigon.mybatis.entity.User;

@Mapper
public interface UserMapper extends BaseMapper<Integer, User> {
}
```

##### 注入Mapper对象

```java
@Autowired
private UserMapper mapper;
```

##### I. 插入

```java
val user = new User();
user.setName("Donghuang");
user.setAccount("donghuang");
user.setMobile("137647788xx");
user.setPassword(RandomStringUtils.randomAlphanumeric(16));
user.setGender(User.Gender.MALE);
user.setBirthDate(DateUtils.parseDate("1994-04-04"));
user.setCity("Shanghai");
user.setActive(true);
user.setRemark("Uncle Donghuang");
user.setCreatedBy("donghuang");
user.setCreatedAt(new Date());

// 插入单条记录
mapper.insert(user);

val user1 = new User();
user1.setName("Gemily");
user1.setAccount("gemily");
user1.setMobile("15770780xxx");
user1.setPassword(RandomStringUtils.randomAlphanumeric(16));
user1.setGender(User.Gender.FEMALE);
user1.setBirthDate(DateUtils.parseDate("1990-06-06"));
user1.setCity("Hangzhou");
user1.setActive(true);
user1.setCreatedBy("donghuang");
user1.setCreatedAt(new Date());

val user2 = new User();
user2.setName("Luffy");
user2.setAccount("luffy");
user2.setMobile("137647799xx");
user2.setPassword(RandomStringUtils.randomAlphanumeric(16));
user2.setGender(User.Gender.MALE);
user2.setBirthDate(DateUtils.parseDate("1997-07-07"));
user2.setCity("East sea");
user2.setActive(true);
user2.setRemark("Luffy");
user2.setCreatedBy("donghuang");
user2.setCreatedAt(new Date());

// 批量插入记录
mapper.insert(Arrays.asList(user1, user2));
```

##### II. 查询

根据`ID`查询单个对象

```java
val id = 1154;
// 根据主键查询单条记录
val user = mapper.find(id);
```

根据属性查询单个对象

```java
// 根据属性account, mobile查询单个对象
val user = mapper.find(
    new Search("account", "donghuang")
        .eq("mobile", "137647788xx"));
```

根据属性查询列表

```java
// 根据属性birthDate, gender查询数据列表
// 查询结果根据属性birthDate升序排序
// 返回数据限制42条
val users = mapper.list(new Search()
    .between("birthDate",
        DateUtils.parseDate("1982-04-04"),
        DateUtils.parseDate("1994-04-04")
    )
    .eq("gender", User.Gender.MALE)
    .asc("birthDate")
    .limit(42));
```

`Search`对象支持的`API`

- `asc` Order ASC 列升序排序
- `desc` Order DSC 列降序排序
- `orderBy` Order by 列属性排序
- `between` Between two values 属性列属于2个值之间
- `build` Build query criterion 自定义构建一个属性列查询条件
- `startsWith` Value starts with string 属性列以字符串开头，等同于`col like 'val%'`
- `endsWith` Value ends with string 属性列以字符串结尾，等同于`col like '%val'`
- `contains` Value contains string 属性列包含字符串，等同于`col like '%val%'`
- `like` Value like 属性列与字符串相似
- `eq` Equals 属性列等于
- `ne` Not equals 属性列小于或等于
- `gt` Greater than 属性列大于
- `gte` Equals or greater than 属性列大于或等于
- `lt` Less than 属性列小于
- `lte` Equals or less than 属性列小于
- `in` In values 属性列属于集合
- `notIn` Not in values 属性列不属于集合
- `isNull` Value is null 属性列为null
- `notNull` Value is not null 属性列不为null
- `isTrue` Value is true 属性列为true
- `isFalse` Value is false 属性列为false
- `limit` Return rows limit 查询/更新结果行数限制
- `offset` Return rows offset 查询结果偏移行数
- `and` And another `Search` 且另外一个Search对象
- `or` Or another `Search` 或另外一个Search对象

##### III. 更新

通过`Entity`根据`ID`更新

```java
// 根据主键查询记录
val user = mapper.find(1);

user.setName("东皇大叔");
user.setUpdatedBy("SYS");
user.setUpdatedAt(new Date());
// 更新单个实体对象
mapper.update(user);
```

通过`Map<String, Object>`更新

```java
val update = new HashMap<String, Object>(6);
update.put("name", "东皇大叔");
update.put("updatedBy", "SYS");
update.put("updatedAt", new Date());
// 通过Map更新ID为1的记录
mapper.update(update, 1);
// 效果同上
// mapper.update(update, new Search("id", 1));
// mapper.update(update, new Search(1));
```

更新列为`NULL`

```java
// 更新id为274229记录的属性列remark为null
mapper.setNull("remark", 274229);
// 更新id为1154记录的属性列remark为null
mapper.setNull("remark", new Search("id", 1154));
// 更新全表的属性列remark为null，小心操作！！！
mapper.setNull("remark", new Search());
```

##### IV. 删除

通过`ID`删除数据

```java
// 根据主键删除记录
mapper.delete(1);
```

通过`Search`对象删除数据

```java
// 根据属性ID删除记录
mapper.delete(new Search("id", 1));
// 等同于 mapper.delete(1);
```

##### V. 杂项

除了上面说到的一些基础增删改查操作，还有一些实用功能，如`@Transient` `@UseGeneratedKeys` `@NoPrimaryKey` `@NotUpdateWhenNull` `@RawValue`等注解，插入、更新前回调，以及支持扩展自定义的方法等。

### 配置说明

- SpringBoot项目，无需其他操作，引入依赖即可
- Spring项目，注册Bean *me.chyxion.tigon.mybatis.TigonMyBatisConfiguration*
- 业务Mapper继承*me.chyxion.tigon.mybatis.BaseMapper*或相关衍生Mapper，*Base(Query, Insert, Update, Delete)Mapper*

### 原理

Tigon MyBatis并**不改变**MyBatis相关功能，所做的只是在程序**启动期间**检测业务Mapper接口，如果继承了相关`BaseMapper.java`，则注入相关方法`MappedStatement`，具体逻辑参见源码，超简单，超幼稚。

### 其他

在前面使用`Search`的例子中，我们需要一些`User`的属性常量字符串，比如

```java
val user = mapper.find(new Search("account", "donghuang"));
```

可以将这些常量定义在`User`类中，如

```java
public static final String ACCOUNT = "account";
```

使用过程中可以使用属性常量，如

```java
val user = mapper.find(new Search(User.ACCOUNT, "donghuang"));
```

也可以使用`Lombok`的`@FieldNameConstants`注解生成，只是这个注解还处于试验阶段，有一定不稳定风险。

### 最后

为什么要有这个项目，其实这些代码本人从2014年就陆续在写在用，在自己参与的一些项目里默默奉献。

有想过开源，奈何一直忙着修福报，此外很重要的一点是，觉得方案并不完善，还是比较长比较臭。

开源界已经有很多MyBatis相关的项目了，包括官方出品的`mybatis-dynamic-sql`，说实话，都挺恶心的。

欢迎有兴趣的同学一起共建。
