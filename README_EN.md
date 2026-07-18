# Tigon MyBatis

[简体中文](README.md) | **English**

[![Maven Central](https://img.shields.io/maven-central/v/com.pudonghot.tigon/tigon-mybatis.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.pudonghot.tigon/tigon-mybatis)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-007396)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![MyBatis 3.5](https://img.shields.io/badge/MyBatis-3.5-cb2d30)](https://mybatis.org/mybatis-3/)
[![License MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

> Turn common CRUD operations into composable MyBatis capabilities, without building another ORM.

Tigon MyBatis is a lightweight MyBatis Mapper enhancement library for Spring and Spring Boot. A business Mapper only needs to extend one interface to gain a complete, or selectively composed, set of create, read, update, and delete operations. Existing XML, annotation-based SQL, plugins, type handlers, and transaction configuration continue to work as before.

It focuses on one specific goal: **eliminate repetitive Mapper CRUD with as little code as possible while preserving the control of native MyBatis.**

## Why Tigon MyBatis

### 1. Small, clear, and readable

The main codebase currently contains only 36 Java files and fewer than 4,000 source lines, including blank lines and comments. Core SQL is centralized in one shared XML template. There is no code generator, runtime entity proxy, or custom database abstraction layer.

The implementation has four core parts:

| Module | Responsibility |
| --- | --- |
| `Base*Mapper` | Declares composable CRUD capabilities |
| `Search` | Expresses conditions, boolean groups, ordering, and pagination |
| `EntityUtils` | Resolves entity fields, primary keys, and annotations |
| `xmlgen` | Generates and registers MyBatis statements at startup |

The small codebase is not about chasing a number. It makes behavior easier to locate, extension points easier to understand, and upgrade risk easier to assess. For a deeper look, start with [Architecture and extensions](docs/architecture.md) (Chinese).

### 2. Fewer method names, complete expressiveness

Tigon MyBatis names APIs by intent: use `find` for one row and `list` for multiple rows. Java overloads express different inputs, so there is no need to switch among similar names such as `selectById`, `selectOne`, and `selectList`.

```java
// The same find method accepts either a primary key or arbitrary conditions.
User byId = userMapper.find(1L);
User byAccount = userMapper.find(
    Search.of(User::getAccount, "aiden")
);

// The same list method accepts a Search, a key collection, or a key array.
List<User> activeUsers = userMapper.list(
    Search.of(User::getActive, true).limit(20)
);
List<User> selectedUsers = userMapper.list(List.of(1L, 2L, 3L));
```

Updates follow the same principle:

```java
userMapper.update(user);                                  // Entity primary key
userMapper.update(updateMap, 1L);                         // Map + primary key
userMapper.update(updateMap, search);                     // Map + conditions
userMapper.update(User::getName, "Aiden", 1L);            // One column + primary key
userMapper.update(User::getName, "Aiden", search);        // One column + conditions
userMapper.setNull(User::getRemark, search);              // Explicitly write NULL
```

The API keeps a small vocabulary: `find / list / count / exists / insert / update / delete`. Parameters determine the scope while method names remain close to business language.

### 3. Non-invasive: it remains MyBatis

- Entities do not need to extend a base class or implement a framework interface.
- Tigon MyBatis does not replace `SqlSessionFactory`, manage transactions, or change Mapper scanning.
- It does not intercept or wrap business Mapper calls. It registers standard MyBatis `MappedStatement` instances at startup.
- Existing XML and annotation-based SQL continue to work. Tigon MyBatis does not overwrite a statement that already exists.
- MyBatis plugins, Cache, TypeHandler, `databaseId`, and custom SQL remain available.
- Spring Boot auto-configuration enables it automatically. A regular Spring project only needs to register one configuration bean.

You can therefore adopt Tigon MyBatis in one Mapper first and expand gradually, without rewriting entities or deleting existing XML.

### 4. Composable capabilities and declarative extensions

`BaseMapper` is simply a composition of four capabilities:

```java
public interface BaseMapper<PrimaryKey, Entity>
    extends BaseQueryMapper<PrimaryKey, Entity>,
            BaseInsertMapper<Entity>,
            BaseUpdateMapper<PrimaryKey, Entity>,
            BaseDeleteMapper<PrimaryKey, Entity> {
}
```

A read-only Mapper can extend only `BaseQueryMapper`; a write-only component can select only the interfaces it needs. Each capability declares its generated SQL elements through `@MapperXmlEl`. At startup, the generator walks the interface hierarchy and assembles the final Mapper XML.

Extensions use the same mechanism as built-in capabilities. Implement `XmlGenCustomizer` and add one configuration annotation to register a new SQL fragment or statement for every Tigon Mapper. See [Advanced extension: xmlgen](#advanced-extension-xmlgen) for a complete example.

### 5. Important details are built in

- Getter method references: `User::getCreatedAt` resolves to a field name, reducing string literals and following refactors.
- Startup validation: a lightweight query runs for each Query Mapper by default, exposing table or column mapping problems early.
- XML coexistence: business XML can reuse generated fragments such as `table`, `primaryKey`, and `cols`.
- Lifecycle event: `TigonMyBatisReadyEvent` is published after all Mappers have been registered.
- Multiple `SqlSessionFactory` instances: statements are registered independently in each MyBatis Configuration.
- Entity callbacks and database expressions: `BasicEntity`, `@RawValue`, and generated-key configuration are supported.

## Get Started in 30 Seconds

### 1. Add the dependency

```xml
<dependency>
  <groupId>com.pudonghot.tigon</groupId>
  <artifactId>tigon-mybatis</artifactId>
  <version>1.0.4</version>
</dependency>
```

The current version is `1.0.4`. The source requires Java 17 and is built and tested with Spring Boot 4, Spring Framework 7, MyBatis 3.5, and MyBatis-Spring 4.

Tigon MyBatis does not configure your data source, transactions, or Mapper scanning. Configure `DataSource` and `SqlSessionFactory` normally, then register Mappers with `@Mapper`, `@MapperScan`, or equivalent MyBatis configuration.

### 2. Define an entity

Entity fields use camelCase-to-snake_case mapping by default, so `createdAt` maps to `created_at`. A field named `id` is the default primary key; use `@PrimaryKey` to select another field.

```java
public class User {
    private Long id;
    private String account;
    private String name;
    private Boolean active;
    private LocalDateTime createdAt;

    // getters / setters
}
```

### 3. Extend a Mapper

```java
@Mapper
@Table("tb_user")
public interface UserMapper extends BaseMapper<Long, User> {
}
```

The Mapper is ready to use:

```java
User user = userMapper.find(1L);
List<User> users = userMapper.list(
    Search.of(User::getActive, true)
        .desc(User::getCreatedAt)
        .limit(20)
);
```

Spring Boot registers `TigonMyBatisConfiguration` through auto-configuration. In a non-Boot Spring project, register `TigonMyBatisConfiguration` as a bean explicitly.

## From Basic CRUD to Composed Queries

### Mapper capabilities

| Interface | Main methods |
| --- | --- |
| `BaseQueryMapper` | `count`, `exists`, `find`, `findCol`, `list`, `listCol`, `select`, `scan`, `batchScan` |
| `BaseInsertMapper` | Single, collection, and array `insert` |
| `BaseUpdateMapper` | Entity update, Map update, single-column update, and `setNull` |
| `BaseDeleteMapper` | Delete by primary key or `Search` |
| `BaseMapper` | Composes all capabilities above |

`scan` and `batchScan` count rows first and then read them in `offset/limit` batches. They are useful for offline processing and consuming large result sets. Scanning mutates the pagination state of the supplied `Search`; use `Search.clone(search)` when the original state must be preserved. `pageSize` must be greater than zero.

### Search

`Search` expresses a query without hiding its SQL fundamentals:

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

| Category | Methods |
| --- | --- |
| Comparison | `eq`, `ne`, `gt`, `gte`, `lt`, `lte` |
| Null / boolean | `isNull`, `notNull`, `isTrue`, `isFalse` |
| Set / range | `in`, `notIn`, `between`, `notBetween` |
| Pattern matching | `like`, `notLike`, `contains`, `startsWith`, `endsWith`, and their `not...` variants |
| Composition | `and`, `or`, `build` |
| Result control | `distinct`, `asc`, `desc`, `orderBy`, `offset`, `limit` |
| State helpers | `attr`, `hasCriterion`, `hasOrder`, `clearCriteria`, `clearOrders`, `clone` |

Several conversions are intentional:

- `eq(col, null)` becomes `isNull(col)`.
- `ne(col, null)` becomes `notNull(col)`.
- `eq/ne` with a collection or array becomes `in/notIn`.
- A nested `Search` with multiple conditions is automatically wrapped in parentheses.
- Query values use MyBatis `#{}` bindings; SQL structure and business parameters remain separate internally.

Prefer Getter method references. String column names, `select` expressions, custom `orderBy` columns, and SQL supplied through `build` are raw SQL capabilities and must only use trusted input.

## Entity Mapping and Annotations

Annotations are optional. Without them, an entity remains a regular JavaBean.

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

| Annotation | Purpose |
| --- | --- |
| `@Table` | Selects the table for a Mapper or entity; Mapper configuration takes precedence |
| `@PrimaryKey` | Selects a primary key field other than `id` |
| `@NoPrimaryKey` | Marks an entity without a primary key, for constrained use cases such as read-only views |
| `@UseGeneratedKeys` | Configures MyBatis `useGeneratedKeys`, `keyProperty`, and `keyColumn` |
| `@Transient` | Excludes a field from column lists, inserts, and updates |
| `@NotUpdate` | Allows insertion but excludes the field from entity and batch updates |
| `@NotUpdateWhenNull` | Skips null fields in a single-entity update; preserves the database value in a MySQL batch update |
| `@RawValue` | Uses a database expression during insert or update, such as `uuid()` or `CURRENT_TIMESTAMP` |

Java `transient`, `static`, and `public` fields are also excluded from normal persistence fields. An entity may optionally implement `BasicEntity` and use `beforeInsert()` or `beforeUpdate()` callbacks before values are read.

`@Table` supports Spring placeholders:

```java
@Table("${user.table:tb_user}")
public interface UserMapper extends BaseMapper<Long, User> {
}
```

If neither the Mapper nor the entity declares `@Table`, the entity class name determines the table name. For example, `UserProfile` maps to `user_profile`.

## Coexisting with Business XML

Generic CRUD should not constrain domain-specific queries. A business Mapper can continue to declare methods with XML in the same namespace:

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

The generator follows a fill-in, never-take-over rule:

- Existing statements and SQL fragments are not regenerated.
- Business XML can reuse `table`, `primaryKey`, `cols`, `colsOfFind`, and `colsOfList`.
- Business XML can define matching IDs in advance to override default CRUD or column lists.
- Native MyBatis XML remains the preferred place for complex SQL, database-specific behavior, and performance tuning.

## Advanced Extension: xmlgen

Tigon MyBatis CRUD is itself generated through declarative annotations and XML content providers. Extensions use exactly the same path as built-in capabilities.

For example, register an `operator` SQL fragment for every Tigon Mapper:

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

A content provider may return text or construct XML DOM nodes directly, so it can generate dynamic MyBatis elements such as `<bind>`, `<if>`, and `<foreach>`:

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

The provider above generates a fragment equivalent to:

```xml
<sql id="operator">
  <bind
      name="__operator__"
      value="@com.example.SecurityContextHolder@currentUserId()" />
  #{__operator__}
</sql>
```

The fragment is registered in each Tigon Mapper namespace, so business Mapper XML can reference it in the same way as `table` and `cols`:

```xml
<mapper namespace="com.example.UserMapper">
  <select id="listByCurrentOperator" resultType="com.example.User">
    select <include refid="cols" />
    from <include refid="table" />
    where operator_id = <include refid="operator" />
  </select>
</mapper>
```

The corresponding Mapper method remains standard MyBatis and does not require a current-operator parameter:

```java
List<User> listByCurrentOperator();
```

At startup, the `operator` fragment is registered together with business XML statements that were waiting for unresolved fragments. At query time, `<bind>` resolves the current operator and `#{__operator__}` binds it as a PreparedStatement parameter.

Tigon MyBatis collects `@MapperXmlEl` declarations from `XmlGenCustomizer` beans, merges them with declarations from the Mapper interface hierarchy, removes duplicates, and registers the result. This mechanism is suitable for organization-wide capabilities such as:

- Audit columns and the current operator
- Soft-delete or data-permission SQL fragments
- Database dialect adaptation
- Standardized query or write statements
- SQL generated from entity metadata, table names, or Spring configuration

See [Architecture and extensions](docs/architecture.md) (Chinese) for the full processing flow, precedence rules, and extension checklist. A runnable example is available in [`OperatorSqlTagGen`](src/test/java/com/pudonghot/tigon/mybatis/customizer/OperatorSqlTagGen.java).

## Configuration

```yaml
tigon:
  mybatis:
    startup-check: true
    quotation-mark: "`"
    insert-default-instead-null: true
```

| Property | Default | Description |
| --- | --- | --- |
| `tigon.mybatis.startup-check` | `true` | Runs one `limit 1` query for each Query Mapper at startup to expose mapping problems early |
| `tigon.mybatis.quotation-mark` | empty | Quotation mark for table and simple column names; MySQL commonly uses a backtick, while Oracle/PostgreSQL commonly use a double quote |
| `tigon.mybatis.insert-default-instead-null` | `true` | Uses SQL `DEFAULT` instead of null during insert; set it to `false` when the database does not support this syntax |

## Explicit Boundaries

Tigon MyBatis does not try to cover the entire MyBatis ecosystem. It does not currently include a page object, logical deletion, optimistic locking, tenant isolation, or a code generator. These capabilities can be implemented through business XML, MyBatis plugins, or `xmlgen` extensions. Maintaining this boundary is an important reason the project remains small and compatible.

Keep the following constraints in mind:

- `update(..., Search.of())`, `setNull(..., Search.of())`, and `delete(Search.of())` affect the entire table. Check `search.hasCriterion()` in the business layer.
- `update(Collection<Entity>)` uses MySQL `UPDATE ... JOIN` and `IFNULL` syntax and currently supports MySQL only.
- `orderBy(col, values)` uses MySQL `FIELD` ordering.
- Default pagination uses `limit offset, size`; Oracle list queries have a dedicated fragment, while other databases should be verified with integration tests.
- An empty collection produces `in ()` / `not in ()`; handle it before calling the Mapper.
- `@RawValue`, `Search.build`, string column names, and `select` expressions must only use trusted server-side input.
- Current automated integration tests use SQLite. Real MySQL, Oracle, and multiple-`SqlSessionFactory` scenarios still need broader coverage.

## Development and Verification

```bash
mvn test
```

The current suite includes database-free unit tests and Spring + SQLite integration tests. Fixtures are deterministic, transactions roll back, and tests do not depend on execution order. See the [Testing guide](docs/testing.md) (Chinese) for test layers and coverage expectations.

## Documentation and Source Entry Points

- [Architecture and extensions](docs/architecture.md) (Chinese)
- [Testing guide](docs/testing.md) (Chinese)
- [Generic CRUD interfaces](src/main/java/com/pudonghot/tigon/mybatis/BaseMapper.java)
- [`Search` implementation](src/main/java/com/pudonghot/tigon/mybatis/Search.java)
- [Startup XML generation](src/main/java/com/pudonghot/tigon/mybatis/TigonMyBatisConfiguration.java)
- [Complete CRUD integration tests](src/test/java/com/pudonghot/tigon/mybatis/test/UserMapperTest.java)

## Contributing

If you agree with the direction of staying small, clear, and additive rather than taking over, please [star the project](https://gitee.com/chyxion/tigon-mybatis), open an [Issue](https://gitee.com/chyxion/tigon-mybatis/issues), contribute test cases, verify database dialects, or submit a Pull Request.

For changes to core behavior, add tests that lock down the existing contract before changing the implementation. The project intends to remain small, clear, composable, and easy to remove.

## License

[MIT](LICENSE)
