# 实现原理与扩展

## 模块边界

Tigon MyBatis 的核心代码可以分为四层：

| 模块 | 主要类型 | 职责 |
| --- | --- | --- |
| Mapper API | `BaseQueryMapper`、`BaseInsertMapper`、`BaseUpdateMapper`、`BaseDeleteMapper` | 定义业务 Mapper 可继承的方法和 MyBatis 参数名 |
| 查询模型 | `Search`、`Criterion`、`ProcArg`、`SqlParam` | 保存查询条件，并将条件拆成 SQL 片段与绑定参数 |
| 实体元数据 | `EntityUtils`、字段注解 | 解析表名、主键、列、插入值和更新值 |
| 启动期生成 | `TigonMyBatisConfiguration`、`XmlGenArg`、`MapperXmlEl`、`XmlContentProvider` | 找到 Mapper，生成 XML DOM，并注册 MyBatis statement |

`src/main/resources/tigon-mybatis.xml` 是通用 SQL 模板。Java 代码负责给每个 Mapper 生成 namespace 内的 `table`、`primaryKey`、`cols` 等片段和 CRUD statement，模板负责拼接查询条件、分页及具体 SQL 结构。

## 启动流程

Spring Boot 从 `AutoConfiguration.imports` 加载 `TigonMyBatisConfiguration`。Bean 初始化完成后执行以下流程：

1. 查找所有 `SqlSessionFactory`。没有工厂时跳过 Tigon 初始化。
2. 在每个 MyBatis `Configuration` 中筛选继承 `SuperMapper` 的 Mapper。
3. 首次发现目标 Mapper 时加载公共 `tigon-mybatis.xml`，并启用 `mapUnderscoreToCamelCase`。
4. 收集 Mapper 继承树上的 `@MapperXmlEl`，以及 `XmlGenCustomizer` 提供的扩展元素。
5. 解析 Mapper 的实体泛型和表名，生成 namespace 对应的 XML DOM。
6. 已存在或处于 incomplete 状态的 statement/fragment 会跳过，其余元素交给 `XMLMapperBuilder` 注册。
7. 将 Mapper namespace 的 MyBatis Cache 补到动态生成的 statement。
8. `startup-check=true` 时对 Query Mapper 执行一次 `list(Search.of().limit(1))`。
9. 发布 `TigonMyBatisReadyEvent`。

这套机制不会替换 MyBatis `Configuration`，因此原生 XML、注解 Mapper、拦截器、类型处理器和事务行为仍由 MyBatis 管理。

## 表与实体解析

表名按以下优先级确定：

1. Mapper 接口上的 `@Table`。
2. 实体类上的 `@Table`。
3. 实体简单类名从驼峰转为下划线。

主键默认查找不区分大小写的 `id` 字段。存在 `@PrimaryKey` 时优先使用注解字段；多个 `@PrimaryKey` 会在启动时失败。`@NoPrimaryKey` 会返回内部占位主键，因此这类 Mapper 应限制为适合无主键实体的操作。

普通持久化字段不包括：

- `static` 字段；
- Java `transient` 字段；
- `public` 字段；
- 标记 Tigon `@Transient` 的字段。

## Search 参数模型

`Search.assemble()` 返回按顺序排列的 `List<Object>`：SQL 结构片段使用 `SqlParam.rawVal(...)` 标记，业务值保持为普通对象。MyBatis XML 遍历该列表时，原始片段通过 `${}` 输出，业务值通过 `#{}` 绑定。

因此 `eq`、`between`、`in` 等 API 的值参数会走 PreparedStatement；列名、表达式和 `build` 生成的片段属于 SQL 结构，不具备同样的注入保护。

嵌套 `and/or` 会把子 `Search` 组装结果放入父条件。包含多个条件的子查询自动加括号，并继承父查询的 table 前缀。

## 覆盖与扩展生成内容

### 使用业务 XML

业务 XML 最适合增加领域查询。生成器会跳过已经存在的同名 statement，所以也可以通过定义 `find`、`list` 等同名 statement 覆盖默认实现。

业务 XML 可复用以下 namespace 内片段：

- `table`
- `primaryKey`
- `cols`
- `colsOfFind`
- `colsOfList`

### 自定义 XML 元素

需要向所有 Tigon Mapper 注入通用 fragment 或 statement 时：

1. 定义一个 Spring Bean，实现标记接口 `XmlGenCustomizer`。
2. 在实现类或其接口上添加一个或多个 `@MapperXmlEl`。
3. 简单元素使用 `include`；动态内容实现 `XmlContentProvider`，返回文本或 DOM Node。

测试目录中的 `OperatorSqlTagGen` 是最小示例。相同 `tag + id` 的元素会去重，Customizer 收集到的元素优先于 Mapper 继承树中的元素。

## 修改核心 SQL 时的检查点

修改 `tigon-mybatis.xml`、`Search`、`EntityUtils` 或动态 XML 生成逻辑时，至少验证：

- 无引用符、反引号和双引号配置；
- null、集合、数组及嵌套 AND/OR；
- 自定义 XML 与生成 statement 同名时的覆盖行为；
- `@PrimaryKey`、`@NoPrimaryKey`、`@Transient`、`@NotUpdate*`、`@RawValue`；
- 单条与批量 insert/update；
- 目标数据库的分页、批量更新和函数方言。

具体测试分层见 [测试指南](testing.md)。
