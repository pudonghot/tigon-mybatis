# 测试指南

## 运行测试

项目要求 JDK 17 或更高版本：

```bash
mvn test
```

测试数据库是 `src/test/resources/tigon-mybatis.db` 中的空 SQLite 表。测试运行时 Maven 将它复制到 `target/test-classes`，Spring 集成测试在事务中插入固定数据，并在每个用例结束后回滚。

## 测试分层

| 层次 | 当前测试 | 适用变更 |
| --- | --- | --- |
| 纯单元测试 | `SearchTest`、`EntityUtilsTest`、`BaseQueryMapperTest`、工具类测试 | 条件组装、字段注解、分页算法、字符串和 Getter 解析 |
| Spring + SQLite 集成测试 | `UserMapperTest` | 动态 statement 注册、公共 XML、CRUD SQL、自定义 Mapper XML |
| 目标数据库测试 | 当前仓库未内置 | MySQL 批量更新、`FIELD` 排序、Oracle 方言、驱动差异 |

优先把不需要 MyBatis/Spring 的行为放进纯单元测试。只有需要验证 XML 动态注册、OGNL、参数绑定或真实 SQL 时，才扩展 `UserMapperTest`。

## 新增功能时怎么补测试

### 修改 Search

- 在 `SearchTest` 中断言 `assemble()` 的完整顺序。
- 同时覆盖字符串字段和 Getter 方法引用。
- 条件涉及 null、集合、数组或嵌套查询时分别增加边界用例。
- 新增 SQL 片段后，在集成测试中至少执行一次真实查询。

### 修改实体注解或字段过滤

- 在 `EntityUtilsTest` 的测试实体上组合注解。
- 分别断言 `cols`、`insertMap`、`updateMap` 和 `batchUpdateMap`。
- `SqlParam` 需要同时检查 `value`、`raw` 和 `ignoreNull`，不能只检查 Map 中是否存在字段。

### 修改 Mapper 或公共 XML

- 在 `UserMapperTest` 中确认 statement 已注册。
- 使用确定性 fixture 断言受影响行数和查询结果。
- 自定义 XML 应同时验证自定义方法仍存在，避免生成器覆盖业务定义。
- SQL 使用数据库方言时，应在对应真实数据库增加独立集成测试；SQLite 通过不能证明 MySQL/Oracle 行为正确。

### 修改分页扫描

- 在 `BaseQueryMapperTest` 中记录每次请求的 `offset/limit`。
- 覆盖总数为 0、整页和非整页尾页。
- 明确断言传入 `Search` 是否会被修改，避免调用方契约悄然变化。

## 用例约定

- 测试数据必须固定，禁止随机手机号、日期、枚举或字符串。
- 每个测试独立准备前置状态，不依赖方法执行顺序。
- 集成测试使用 `@Transactional` 回滚，不向仓库内 SQLite 文件写入持久数据。
- 使用 JUnit Assertions，并让测试方法名描述可观察行为。
- 一次测试尽量只覆盖一个功能域；批量断言同一条业务链路可以放在同一方法中。
- 修复缺陷时先增加能复现问题的失败测试，再修改生产代码。

## 当前覆盖边界

现有测试覆盖通用 CRUD、动态 statement 注册、业务 XML 共存、`Search` 主流条件、实体字段注解和分页扫描。下列能力仍需要后续按数据库环境补充：

- MySQL `update(Collection<Entity>)`；
- MySQL `orderBy(col, values)`；
- Oracle 专用 list 分页片段；
- 多 `SqlSessionFactory` 的隔离和事件发布；
- MyBatis 二级缓存向动态 statement 的回填。
