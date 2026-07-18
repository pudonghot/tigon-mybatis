# Tigon MyBatis

**简体中文** | [English](README_EN.md)

[![Maven Central](https://img.shields.io/maven-central/v/com.pudonghot.tigon/tigon-mybatis.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.pudonghot.tigon/tigon-mybatis)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-007396)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![MyBatis 3.5](https://img.shields.io/badge/MyBatis-3.5-cb2d30)](https://mybatis.org/mybatis-3/)
[![License MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

> 把通用 CRUD 做成 MyBatis 的一组可组合能力，而不是再造一套 ORM。

Tigon MyBatis 是一个面向 Spring / Spring Boot 的轻量 MyBatis Mapper 增强库。业务 Mapper 只需继承一个接口，就能获得完整或按需组合的增、删、改、查能力；已有的 XML、注解 SQL、插件、类型处理器和事务配置继续按原来的方式工作。

它关注一件具体的事：**用尽可能少的代码，消除重复 Mapper CRUD，同时保留原生 MyBatis 的控制力。**

## 为什么是 Tigon MyBatis

### 1. 小而清晰，代码可以读懂

当前主代码只有 36 个 Java 文件、不到 4,000 行源码（包含空行与注释），核心 SQL 集中在一份公共 XML 模板中。项目没有代码生成器、运行时实体代理或自建数据库抽象层。

核心实现只有四个部分：

| 模块 | 职责 |
| --- | --- |
| `Base*Mapper` | 声明可组合的 CRUD 能力 |
| `Search` | 表达条件、组合、排序和分页 |
| `EntityUtils` | 解析实体字段、主键和注解 |
| `xmlgen` | 启动时生成并注册 MyBatis Statement |

代码少不是为了追求数字，而是为了让行为可定位、扩展点可理解、升级风险可评估。需要深入实现时，可以从 [实现原理与扩展](docs/architecture.md) 直接进入核心路径。

### 2. 方法名少，但表达能力完整

Tigon MyBatis 按操作意图命名 API：查一条就是 `find`，查多条就是 `list`，不同输入通过 Java 重载表达。无需在 `selectById`、`selectOne`、`selectList` 等相近名称之间切换。

```java
// 同一个 find，既接受主键，也接受任意查询条件
User byId = userMapper.find(1L);
User byAccount = userMapper.find(
    Search.of(User::getAccount, "aiden")
);

// 同一个 list，既接受 Search，也接受主键集合或数组
List<User> activeUsers = userMapper.list(
    Search.of(User::getActive, true).limit(20)
);
List<User> selectedUsers = userMapper.list(List.of(1L, 2L, 3L));
```

更新同样遵循这一原则：

```java
userMapper.update(user);                                  // 按实体主键更新
userMapper.update(updateMap, 1L);                         // Map + 主键
userMapper.update(updateMap, search);                     // Map + 条件
userMapper.update(User::getName, "Aiden", 1L);            // 单列 + 主键
userMapper.update(User::getName, "Aiden", search);        // 单列 + 条件
userMapper.setNull(User::getRemark, search);              // 明确写入 NULL
```

这套 API 保持了较小的记忆面：`find / list / count / exists / insert / update / delete`，参数决定操作范围，方法名始终贴近业务语言。

### 3. 无侵入，接入后仍然是 MyBatis

- 实体不需要继承基类，也不要求实现特定接口。
- 不替换 `SqlSessionFactory`，不接管事务，不改变 Mapper 扫描方式。
- 不拦截或包装业务 Mapper 调用，而是在启动阶段注册标准 MyBatis `MappedStatement`。
- 已有 XML 和注解 SQL 可以继续使用；同名 Statement 已存在时，Tigon MyBatis 不会覆盖。
- MyBatis 插件、Cache、TypeHandler、`databaseId` 和自定义 SQL 保持有效。
- Spring Boot 自动配置即可启用；普通 Spring 项目只需注册一个配置 Bean。

因此 Tigon MyBatis 可以先接入一个 Mapper，再逐步推广，不要求一次性改造实体或删除已有 XML。

### 4. 能力可组合，扩展也是声明式的

`BaseMapper` 只是四种能力的组合：

```java
public interface BaseMapper<PrimaryKey, Entity>
    extends BaseQueryMapper<PrimaryKey, Entity>,
            BaseInsertMapper<Entity>,
            BaseUpdateMapper<PrimaryKey, Entity>,
            BaseDeleteMapper<PrimaryKey, Entity> {
}
```

只读 Mapper 可以只继承 `BaseQueryMapper`，只写组件也可以只选择需要的接口。每种能力通过 `@MapperXmlEl` 声明自己需要生成的 SQL 元素；启动时，生成器遍历接口继承树并组装最终 Mapper XML。

这意味着扩展遵循与内置能力相同的机制：实现 `XmlGenCustomizer`，添加一个配置注解，就可以为所有 Tigon Mapper 注册新的 SQL fragment 或 Statement。详细示例见 [高级扩展：xmlgen](#高级扩展xmlgen)。

### 5. 细节能力不是附加层

- Getter 方法引用：`User::getCreatedAt` 会解析为字段名，减少字符串硬编码，也能跟随重构。
- 启动检查：默认对 Query Mapper 执行一次轻量查询，尽早暴露表名或字段映射错误。
- XML 共存：业务 XML 可以复用自动生成的 `table`、`primaryKey`、`cols` 等片段。
- 生命周期事件：全部 Mapper 注册完成后发布 `TigonMyBatisReadyEvent`。
- 多 `SqlSessionFactory`：每个 MyBatis Configuration 独立完成 Statement 注册。
- 实体回调与数据库表达式：支持 `BasicEntity`、`@RawValue` 和生成主键配置。

## 30 秒接入

### 1. 添加依赖

```xml
<dependency>
  <groupId>com.pudonghot.tigon</groupId>
  <artifactId>tigon-mybatis</artifactId>
  <version>1.0.4</version>
</dependency>
```

当前版本是 `1.0.4`。源码要求 Java 17，使用 Spring Boot 4、Spring Framework 7、MyBatis 3.5 和 MyBatis-Spring 4 构建与测试。

Tigon MyBatis 不负责数据源、事务和 Mapper 扫描。项目仍按正常 MyBatis 方式配置 `DataSource`、`SqlSessionFactory`，并使用 `@Mapper`、`@MapperScan` 或等价配置注册 Mapper。

### 2. 定义实体

实体字段默认按驼峰转下划线映射，例如 `createdAt` 对应 `created_at`。默认使用名为 `id` 的字段作为主键，也可以用 `@PrimaryKey` 指定其他字段。

```java
public class User {
    private Long id;
    private String account;
    private String name;
    private Boolean active;
    private LocalDateTime createdAt;

    // getter / setter
}
```

### 3. 继承 Mapper

```java
@Mapper
@Table("tb_user")
public interface UserMapper extends BaseMapper<Long, User> {
}
```

到这里已经可以使用：

```java
User user = userMapper.find(1L);
List<User> users = userMapper.list(
    Search.of(User::getActive, true)
        .desc(User::getCreatedAt)
        .limit(20)
);
```

Spring Boot 会通过 Auto Configuration 自动注册 TigonMyBatisConfiguration。非 Spring Boot 项目显式注册 `TigonMyBatisConfiguration` Bean 即可。

## 从基础 CRUD 到组合查询

### Mapper 能力

| 接口 | 主要方法 |
| --- | --- |
| `BaseQueryMapper` | `count`、`exists`、`find`、`findCol`、`list`、`listCol`、`select`、`scan`、`batchScan` |
| `BaseInsertMapper` | 单条、集合、数组 `insert` |
| `BaseUpdateMapper` | 实体更新、Map 更新、单列更新、`setNull` |
| `BaseDeleteMapper` | 按主键或 `Search` 删除 |
| `BaseMapper` | 组合以上全部能力 |

`scan` 和 `batchScan` 会先统计总数，再按 `offset/limit` 分批读取，适合离线处理和大结果集消费。扫描会修改传入 `Search` 的分页状态；需要保留原条件时使用 `Search.clone(search)`。`pageSize` 必须大于 0。

### Search

`Search` 只负责表达查询，不隐藏 SQL 的基本概念：

```java
Search search = Search.of(User::getActive, true)
    .and(
        Search.of(User::getCity, "Shanghai")
            .or(User::getCity, "Hangzhou")
    )
    .between(User::getCreatedAt, start, end)
    .contains(User::getName, "Aiden")
    .asc(User::getName)
    .offset(0)
    .limit(50);
```

| 类别 | 方法 |
| --- | --- |
| 比较 | `eq`、`ne`、`gt`、`gte`、`lt`、`lte` |
| 空值/布尔 | `isNull`、`notNull`、`isTrue`、`isFalse` |
| 集合/范围 | `in`、`notIn`、`between`、`notBetween` |
| 模糊匹配 | `like`、`notLike`、`contains`、`startsWith`、`endsWith` 及对应的 `not...` 方法 |
| 组合 | `and`、`or`、`build` |
| 结果控制 | `distinct`、`asc`、`desc`、`orderBy`、`offset`、`limit` |
| 辅助状态 | `attr`、`hasCriterion`、`hasOrder`、`clearCriteria`、`clearOrders`、`clone` |

几个有意设计的转换规则：

- `eq(col, null)` 转换为 `isNull(col)`。
- `ne(col, null)` 转换为 `notNull(col)`。
- `eq/ne` 接收集合或数组时，转换为 `in/notIn`。
- 包含多个条件的子 `Search` 自动添加括号。
- 查询值使用 MyBatis `#{}` 绑定；SQL 结构片段与业务参数在内部保持分离。

优先使用 Getter 方法引用。字符串列名、`select` 表达式、`orderBy` 自定义列和 `build` 中的 SQL 属于原生 SQL 能力，只能使用可信内容。

## 实体映射与注解

注解是可选能力，不使用时实体仍然是普通 Java Bean。

```java
@UseGeneratedKeys
public class User {
    private Long id;

    @NotUpdate
    private String account;

    @NotUpdateWhenNull
    private String mobile;

    @RawValue("CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Transient
    private String displayName;
}
```

| 注解 | 作用 |
| --- | --- |
| `@Table` | 指定 Mapper 或实体对应的表名，Mapper 配置优先 |
| `@PrimaryKey` | 指定非 `id` 主键字段 |
| `@NoPrimaryKey` | 标记无主键实体，适合只读视图等受限场景 |
| `@UseGeneratedKeys` | 配置 MyBatis `useGeneratedKeys`、`keyProperty` 和 `keyColumn` |
| `@Transient` | 字段不参与列清单、插入和更新 |
| `@NotUpdate` | 字段可插入，但实体更新和批量更新时忽略 |
| `@NotUpdateWhenNull` | 单实体更新时 null 字段不更新；MySQL 批量更新时保留原值 |
| `@RawValue` | 插入或更新时使用数据库表达式，例如 `uuid()`、`CURRENT_TIMESTAMP` |

Java `transient`、`static`、`public` 字段也不会作为普通持久化字段。实体可选实现 `BasicEntity`，通过 `beforeInsert()` 和 `beforeUpdate()` 在取值前执行回调。

`@Table` 支持 Spring 占位符：

```java
@Table("${user.table:tb_user}")
public interface UserMapper extends BaseMapper<Long, User> {
}
```

Mapper 和实体都未声明 `@Table` 时，表名由实体类名转换得到，例如 `UserProfile` 对应 `user_profile`。

## 与业务 XML 共存

通用 CRUD 不应该限制领域查询。业务 Mapper 可以继续声明方法并提供同 namespace 的 XML：

```java
List<User> listByName(@Param("name") String name);
```

```xml
<mapper namespace="com.example.UserMapper">
  <select id="listByName" resultType="com.example.User">
    select <include refid="cols" />
    from <include refid="table" />
    where name = #{name}
  </select>
</mapper>
```

生成器遵循“只补充、不抢占”：

- 已存在的 Statement 或 SQL fragment 不会被重新生成。
- 业务 XML 可以复用 `table`、`primaryKey`、`cols`、`colsOfFind`、`colsOfList`。
- 业务 XML 可以预先定义同名内容，覆盖默认 CRUD 或列清单。
- 原生 MyBatis XML 仍是处理复杂 SQL、数据库特性和性能优化的首选位置。

## 高级扩展：xmlgen

Tigon MyBatis 的 CRUD 本身就是通过注解声明和 XML 内容提供器生成的，扩展能力与内置能力使用同一条路径。

例如，为所有 Tigon Mapper 注册一个名为 `operator` 的 SQL fragment：

```java
@Component
@MapperXmlEl(
    tag = MapperXmlEl.Tag.SQL,
    id = "operator",
    contentProvider = OperatorContentProvider.class
)
public class CommonSqlExtension implements XmlGenCustomizer {
}
```

内容提供器既可以返回文本，也可以直接构造 XML DOM 节点，因此能够生成 `<bind>`、`<if>`、`<foreach>` 等动态 MyBatis 元素：

```java
public class OperatorContentProvider extends XmlContentProvider {
    @Override
    public Content content(XmlGenArg arg) {
        var bind = arg.getDocument().createElement("bind");
        bind.setAttribute("name", "__operator__");
        bind.setAttribute(
            "value",
            "@com.example.SecurityContextHolder@currentUserId()"
        );
        var value = arg.getDocument().createTextNode("#{__operator__}");
        return new Content(List.of(bind, value));
    }
}
```

上面的 Provider 会生成等价于以下内容的 SQL fragment：

```xml
<sql id="operator">
  <bind
      name="__operator__"
      value="@com.example.SecurityContextHolder@currentUserId()" />
  #{__operator__}
</sql>
```

fragment 会注册到每个 Tigon Mapper 自己的 namespace 中，因此业务 Mapper XML 可以像引用 `table`、`cols` 一样直接引用它：

```xml
<mapper namespace="com.example.UserMapper">
  <select id="listByCurrentOperator" resultType="com.example.User">
    select <include refid="cols" />
    from <include refid="table" />
    where operator_id = <include refid="operator" />
  </select>
</mapper>
```

对应的 Mapper 方法保持原生 MyBatis 写法，不需要传入当前操作人参数：

```java
List<User> listByCurrentOperator();
```

应用启动时，`operator` fragment 与业务 XML 中尚未完成解析的 Statement 会一起完成注册；运行查询时，`<bind>` 获取当前操作人并通过 `#{__operator__}` 作为 PreparedStatement 参数绑定。

启动时，Tigon MyBatis 会收集 `XmlGenCustomizer` Bean 上的 `@MapperXmlEl`，再与 Mapper 接口继承树上的声明合并、去重并注册。这个机制适合实现组织级公共能力，例如：

- 审计字段和当前操作人；
- 软删除或数据权限 SQL fragment；
- 数据库方言适配；
- 团队统一的查询或写入 Statement；
- 根据实体、表名或 Spring 配置动态生成 SQL。

完整处理流程、覆盖优先级和扩展检查点见 [实现原理与扩展](docs/architecture.md)，可运行示例见 [`OperatorSqlTagGen`](src/test/java/com/pudonghot/tigon/mybatis/customizer/OperatorSqlTagGen.java)。

## 配置

```yaml
tigon:
  mybatis:
    startup-check: true
    quotation-mark: "`"
    insert-default-instead-null: true
```

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `tigon.mybatis.startup-check` | `true` | 启动时对每个 Query Mapper 执行一次 `limit 1` 查询，尽早发现映射问题 |
| `tigon.mybatis.quotation-mark` | 空 | 表名和简单列名的引用符；MySQL 常用反引号，Oracle/PostgreSQL 常用双引号 |
| `tigon.mybatis.insert-default-instead-null` | `true` | 插入时用 SQL `DEFAULT` 代替 null；数据库不支持时设为 `false` |

## 明确的边界

Tigon MyBatis 不试图覆盖 MyBatis 的所有生态能力。目前不内置分页对象、逻辑删除、乐观锁、租户隔离或代码生成器；这些能力可以通过业务 XML、MyBatis 插件或 `xmlgen` 扩展实现。保持边界是项目代码量小、兼容性高的重要原因。

使用时还需要注意：

- `update(..., Search.of())`、`setNull(..., Search.of())` 和 `delete(Search.of())` 会作用于全表，应在业务层检查 `search.hasCriterion()`。
- `update(Collection<Entity>)` 使用 MySQL `UPDATE ... JOIN` 和 `IFNULL`，目前仅支持 MySQL。
- `orderBy(col, values)` 使用 MySQL `FIELD` 排序。
- 默认分页使用 `limit offset, size`；Oracle list 有专用片段，其他数据库应先运行集成测试。
- 空集合会生成 `in ()` / `not in ()`，调用方应在进入 Mapper 前处理。
- `@RawValue`、`Search.build`、字符串列名和 `select` 表达式只能使用可信的服务端内容。
- 当前自动化集成测试使用 SQLite；MySQL、Oracle 及多 `SqlSessionFactory` 场景仍需要持续补充真实环境测试。

## 开发与验证

```bash
mvn test
```

当前测试包含无数据库单元测试和 Spring + SQLite 集成测试，固定数据、事务回滚，不依赖执行顺序。新增能力时的测试分层和覆盖要求见 [测试指南](docs/testing.md)。

## 文档与源码入口

- [实现原理与扩展](docs/architecture.md)
- [测试指南](docs/testing.md)
- [通用 CRUD 接口](src/main/java/com/pudonghot/tigon/mybatis/BaseMapper.java)
- [Search 实现](src/main/java/com/pudonghot/tigon/mybatis/Search.java)
- [启动期 XML 生成](src/main/java/com/pudonghot/tigon/mybatis/TigonMyBatisConfiguration.java)
- [完整 CRUD 集成测试](src/test/java/com/pudonghot/tigon/mybatis/test/UserMapperTest.java)

## 参与共建

如果你认同“小而清晰、增强但不接管”的方向，欢迎为[项目点亮 Star](https://gitee.com/chyxion/tigon-mybatis)、提交 [Issue](https://gitee.com/chyxion/tigon-mybatis/issues)、测试用例、数据库方言验证和 Pull Request。

对于核心行为变更，建议先增加能够锁定现有契约的测试，再调整实现；项目希望长期保持“小、清晰、可组合、可退出”。

## License

[MIT](LICENSE)
