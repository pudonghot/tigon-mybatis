package com.pudonghot.tigon.mybatis;

import lombok.*;
import java.util.*;
import org.w3c.dom.Element;
import java.io.InputStream;
import org.w3c.dom.Document;
import lombok.extern.slf4j.Slf4j;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.xml.transform.OutputKeys;
import org.apache.ibatis.parsing.XNode;
import javax.xml.transform.dom.DOMSource;
import org.springframework.util.StringUtils;
import org.springframework.core.io.Resource;
import org.apache.ibatis.parsing.XPathParser;
import javax.xml.transform.TransformerFactory;
import org.springframework.util.CollectionUtils;
import org.apache.ibatis.session.Configuration;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.DocumentBuilderFactory;
import com.pudonghot.tigon.mybatis.util.StrUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import com.pudonghot.tigon.mybatis.util.AssertUtils;
import com.pudonghot.tigon.mybatis.xmlgen.XmlGenArg;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.context.ApplicationContext;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.springframework.aop.framework.AopProxyUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.beans.factory.InitializingBean;
import com.pudonghot.tigon.mybatis.xmlgen.XmlGenCustomizer;
import org.springframework.core.annotation.AnnotationUtils;
import org.apache.ibatis.builder.xml.XMLMapperEntityResolver;
import org.springframework.beans.factory.annotation.Autowired;
import com.pudonghot.tigon.mybatis.event.TigonMyBatisReadyEvent;
import com.pudonghot.tigon.mybatis.xmlgen.annotation.MapperXmlEl;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import static com.pudonghot.tigon.mybatis.xmlgen.annotation.MapperXmlEl.Tag;
import com.pudonghot.tigon.mybatis.xmlgen.contentprovider.XmlContentProvider;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @author Donghuang
 * @date Jul 12, 2014 3:36:13 PM
 */
@Slf4j
@EnableConfigurationProperties({ TigonMyBatisProperties.class })
public class TigonMyBatisConfiguration implements InitializingBean {

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired(required = false)
    private SqlSessionFactory[] sqlSessionFactories;
    @Autowired(required = false)
    private List<XmlGenCustomizer> customizers;
    /**
     * execute #afterPropertiesSet after Mappers initialized
     */
    @Autowired(required = false)
    private SuperMapper<?>[] mappers;
    @Getter
    private final Map<Class<SuperMapper<?>>, String> tables = new HashMap<>();
    @Getter
    @Autowired
    private TigonMyBatisProperties properties;
    private static TigonMyBatisConfiguration STATIC_INSTANCE;

    /**
     * get config static instance
     *
     * @return config
     */
    public static TigonMyBatisConfiguration getStaticInstance() {
        return STATIC_INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() {
        STATIC_INSTANCE = this;

        if (sqlSessionFactories == null || sqlSessionFactories.length == 0) {
            log.warn("No 'org.apache.ibatis.session.SqlSessionFactory' bean found, Tigon MyBatis ignored.");
            return;
        }

        log.info("Register tigon-mybatis.xml.");
        val path = "classpath*:tigon-mybatis.xml";
        val resources = findResources(path);
        AssertUtils.state(resources.length == 1,
            () -> "No unique resource [" + path + "] found");
        val resource = resources[0];
        log.info("Tigon namespace XML resource [{}] found.", resource);

        val customXmlEls = getCustomXmlEls();

        for (val sqlSessionFactory : sqlSessionFactories) {
            val config = sqlSessionFactory.getConfiguration();
            val sqlFragments = config.getSqlFragments();

            val argGenXml = new ArgGenXml(
                    new XMLMapperEntityResolver(),
                    config,
                    ((GenericApplicationContext) applicationContext).getBeanFactory());

            final boolean[] mapperFound = { false };

            eachMapper(config, mapper -> {
                if (!mapperFound[0]) {
                    mapperFound[0] = true;

                    log.info("Register SQL session factory [{}] 'tigon-mybatis.xml'", sqlSessionFactory);
                    new XMLMapperBuilder(
                            resourceInputStream(resource),
                            config,
                            resource.toString(),
                            sqlFragments).parse();

                    log.info("Update SQL session factory [{}] 'MapUnderscoreToCamelCase' to true", sqlSessionFactory);
                    config.setMapUnderscoreToCamelCase(true);
                }

                log.info("Generate mapper class [{}].", mapper);
                argGenXml.setMapperClass(mapper);
                argGenXml.setMapperXmlEls(getMapperXmlEls(customXmlEls, mapper));

                val bytesMapper = genMapperXml(argGenXml);

                if (bytesMapper != null) {
                    log.debug("Mapper XML [{}] generated.", new String(bytesMapper));
                    new XMLMapperBuilder(
                            new ByteArrayInputStream(bytesMapper),
                            config,
                            "[Tigon]" + mapper.getName() + ".xml",
                            sqlFragments).parse();
                }
            });

            if (!mapperFound[0]) {
                continue;
            }

            // add cache
            eachMapper(config, mapper -> {
                val mapperName = mapper.getName();
                val mapperPrefix = mapperName + ".";
                for (val msName : config.getMappedStatementNames()) {
                    if (msName.startsWith(mapperPrefix)) {
                        val mappedStatement = config.getMappedStatement(msName);
                        // add cache
                        if (config.hasCache(mapperName) && mappedStatement.getCache() == null) {
                            SystemMetaObject.forObject(mappedStatement)
                                .setValue("cache", config.getCache(mapperName));
                        }
                    }
                }
            });
        }

        // add get table proxy
        if (mappers != null && mappers.length > 0) {
            if (properties.isStartupCheck()) {
                log.info("Startup check is on, run database table check.");
                val search = Search.of().limit(1);
                for (val mapper : mappers) {
                    val mapperInterfaces = mapper.getClass().getInterfaces();
                    if (mapper instanceof BaseQueryMapper) {
                        log.info("Mapper [{}] is instance of BaseQueryMapper, run #list to check database table.", mapperInterfaces);
                        ((BaseQueryMapper) mapper).list(search);
                    }
                    else {
                        log.debug("Mapper [{}] is not instance of BaseQueryMapper, ignore.", mapperInterfaces);
                    }
                }
            }
        }

        applicationContext.publishEvent(
            new TigonMyBatisReadyEvent(applicationContext, this));
    }

    /**
     * find resource by path
     *
     * @param path path
     * @return resources
     */
    @SneakyThrows
    Resource[] findResources(final String path) {
        return new PathMatchingResourcePatternResolver().getResources(path);
    }

    @SneakyThrows
    InputStream resourceInputStream(final Resource resource) {
        return resource.getInputStream();
    }

    byte[] genMapperXml(final ArgGenXml argGenXml) {
        val mapperClass = argGenXml.getMapperClass();

        val doc = genDocument(argGenXml);
        val xPathParser = new XPathParser(
                doc, true, null, argGenXml.getXmlMapperEntityResolver());

        val beanFactory = argGenXml.getBeanFactory();
        val xmlProcessArg = new XmlGenArg(beanFactory, xPathParser, doc, mapperClass);
        tables.put(mapperClass, xmlProcessArg.getTable());

        val configuration = argGenXml.getConfiguration();
        val namespacePrefix = mapperClass.getName() + ".";
        val sqlFragments = configuration.getSqlFragments();
        val docEl = doc.getDocumentElement();
        boolean updated = false;

        for (val element : argGenXml.getMapperXmlEls()) {
            var id = namespacePrefix + element.id();
            log.debug("Generate SQL fragment [{}].", id);

            // SQL
            if (element.tag() == Tag.SQL) {
                if (sqlFragments.containsKey(id)) {
                    log.info("SQL fragment [{}] existed, ignore generate.", id);
                    continue;
                }
            }
            // CRUD
            else {
                if (configuration.hasStatement(id, false)) {
                    log.info("SQL statement [{}] existed, ignore generate.", id);
                    continue;
                }

                if (isIncompleteStatement(configuration, id)) {
                    log.info("Incomplete SQL statement [{}] existed, ignore generate.", id);
                    continue;
                }
            }

            docEl.appendChild(xmlEl(element, xmlProcessArg));
            updated = true;
        }

        if (updated) {
            return toBytes(doc);
        }

        return null;
    }

    boolean isIncompleteStatement(
            final Configuration configuration,
            final String id) {

        for (val statementBuilder : configuration.getIncompleteStatements()) {
            val metaObj = SystemMetaObject.forObject(statementBuilder);
            val assistant = (MapperBuilderAssistant) metaObj.getValue("builderAssistant");
            val context = (XNode) metaObj.getValue("context");

            if (id.equals(assistant.getCurrentNamespace() + "." +
                    context.getStringAttribute("id"))) {
                return true;
            }
        }

        return false;
    }

    @SneakyThrows
    Document genDocument(final ArgGenXml arg) {
        val mapperClass = arg.getMapperClass();
        val doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .newDocument();

        val root = doc.createElement("mapper");
        root.setAttribute("namespace", mapperClass.getName());
        doc.appendChild(root);
        return doc;
    }

    @SneakyThrows
    byte[] toBytes(final Document doc) {
        val transformer = TransformerFactory.newInstance().newTransformer();

        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        val doctype = doc.getImplementation().createDocumentType(
                "DOCTYPE",
                "-//mybatis.org//DTD Mapper 3.0//EN",
                "http://mybatis.org/dtd/mybatis-3-mapper.dtd");

        transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());

        val baos = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(baos));
        return baos.toByteArray();
    }

    @SneakyThrows
    XmlContentProvider newProvider(final MapperXmlEl el) {
        return el.contentProvider().getDeclaredConstructor().newInstance();
    }

    List<MapperXmlEl> getCustomXmlEls() {
        if (CollectionUtils.isEmpty(customizers)) {
            return Collections.emptyList();
        }

        val interfaces = new LinkedHashSet<Class<?>>(16);
        customizers.forEach(bean -> getAllInterfaces(interfaces, AopProxyUtils.ultimateTargetClass(bean)));

        val xmlEls = new ArrayList<MapperXmlEl>(16);

        for (val it : interfaces) {
            for (val xmlEl : it.getAnnotationsByType(MapperXmlEl.class)) {
                xmlEls.add(xmlEl);
            }
        }

        val setEls = new HashSet<String>(xmlEls.size());
        xmlEls.removeIf(e -> !setEls.add(e.tag() + e.id()));

        return xmlEls;
    }

    List<MapperXmlEl> getMapperXmlEls(final List<MapperXmlEl> customXmlEls, final Class<?> clazz) {
        val interfaces = new LinkedHashSet<Class<?>>(8);
        getAllInterfaces(interfaces, clazz);
        val xmlEls = new ArrayList<>(customXmlEls);

        for (val it : interfaces) {
            for (val xmlEl : it.getAnnotationsByType(MapperXmlEl.class)) {
                xmlEls.add(xmlEl);
            }
        }

        val setEls = new HashSet<String>(xmlEls.size());
        xmlEls.removeIf(e -> !setEls.add(e.tag() + e.id()));

        return xmlEls;
    }

    void getAllInterfaces(final Set<Class<?>> interfaces, final Class<?> clazz) {
        interfaces.add(clazz);
        for (val it : clazz.getInterfaces()) {
            getAllInterfaces(interfaces, it);
        }
    }

    Element xmlEl(final MapperXmlEl element, final XmlGenArg arg) {
        val doc = arg.getDocument();
        val tag = element.tag();
        val elId = element.id();
        val el = doc.createElement(tag.name().toLowerCase());
        el.setAttribute("id", elId);

        // result type
        val resultType = element.resultType();
        if (StrUtils.isNotBlank(resultType)) {
            el.setAttribute("resultType",
                    MapperXmlEl.RESULT_TYPE_ENTITY.equals(resultType) ?
                            arg.getEntityClass().getName() : resultType);
        }

        // insert
        if (tag == Tag.INSERT) {
            val entityClass = arg.getEntityClass();
            val ugkAnnotation =
                    AnnotationUtils.findAnnotation(
                        entityClass, UseGeneratedKeys.class);

            if (ugkAnnotation != null) {
                el.setAttribute("useGeneratedKeys", "true");

                String keyProp = "id";
                String keyCol = "id";
                val valueCfg = ugkAnnotation.value();
                if (valueCfg != null && valueCfg.length > 0) {
                    val txt = Arrays.stream(valueCfg).collect(Collectors.joining(","));
                    keyProp = txt;
                    keyCol = txt;
                }

                val keyPropCfg = ugkAnnotation.props();
                if (keyPropCfg != null && keyPropCfg.length > 0) {
                    keyProp = Arrays.stream(keyPropCfg).collect(Collectors.joining(","));
                }

                val keyColCfg = ugkAnnotation.cols();
                if (keyColCfg != null && keyColCfg.length > 0) {
                    keyCol = Arrays.stream(keyColCfg).collect(Collectors.joining(","));
                }

                el.setAttribute("keyProperty", keyProp);
                el.setAttribute("keyColumn", keyCol);
            }
        }

        val contentProvider = element.contentProvider();

        if (contentProvider != MapperXmlEl.EmptyProvider.class) {
            val content = newProvider(element).content(arg);

            val textNode = content.getContent();
            if (StringUtils.hasText(textNode)) {
                el.setTextContent(textNode);
                return el;
            }

            content.getNodes().forEach(el::appendChild);
            return el;
        }

        val includeEl = doc.createElement("include");
        val include = element.include();
        includeEl.setAttribute("refid",
            StrUtils.isNotBlank(include) ?
                include : "Tigon." + elId);
        el.appendChild(includeEl);
        return el;
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    static class ArgGenXml {
        private final XMLMapperEntityResolver xmlMapperEntityResolver;
        private final Configuration configuration;
        private final ConfigurableBeanFactory beanFactory;
        private Class<SuperMapper<?>> mapperClass;
        private List<MapperXmlEl> mapperXmlEls;
    }

    void eachMapper(final Configuration config, final Consumer<Class<SuperMapper<?>>> consumer) {
        for (val mapper : config.getMapperRegistry().getMappers()) {
            if (SuperMapper.class.isAssignableFrom(mapper)) {
                consumer.accept((Class<SuperMapper<?>>) mapper);
            }
        }
    }
}
