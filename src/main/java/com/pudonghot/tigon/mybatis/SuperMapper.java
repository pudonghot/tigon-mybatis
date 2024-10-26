package com.pudonghot.tigon.mybatis;

import lombok.val;
import java.util.Arrays;
import java.util.stream.Collectors;
import com.pudonghot.tigon.mybatis.xmlgen.annotation.MapperXmlEl;
import com.pudonghot.tigon.mybatis.xmlgen.contentprovider.ColsXmlContentProvider;
import com.pudonghot.tigon.mybatis.xmlgen.contentprovider.TableXmlContentProvider;
import com.pudonghot.tigon.mybatis.xmlgen.contentprovider.PrimaryKeyXmlContentProvider;

/**
 * @author Donghuang
 * @date Dec 13, 2018 19:08:58
 */
@MapperXmlEl(tag = MapperXmlEl.Tag.SQL, id = "table", contentProvider = TableXmlContentProvider.class)
@MapperXmlEl(tag = MapperXmlEl.Tag.SQL, id = "primaryKey", contentProvider = PrimaryKeyXmlContentProvider.class)
@MapperXmlEl(tag = MapperXmlEl.Tag.SQL, id = "cols", contentProvider = ColsXmlContentProvider.class)
@MapperXmlEl(tag = MapperXmlEl.Tag.SQL, id = "colsOfFind", include = "cols")
@MapperXmlEl(tag = MapperXmlEl.Tag.SQL, id = "colsOfList", include = "cols")
public interface SuperMapper<Enity> {
    String PARAM_MODEL_KEY = "__model__";
    String PARAM_MODELS_KEY = "__models__";
    String PARAM_SEARCH_KEY = "__search__";
    String PARAM_COL_KEY = "__col__";
    String PARAM_COLS_KEY = "__cols__";
    String PARAM_VAL_KEY = "__val__";

    /**
     * get mapper table name
     *
     * @return table name
     */
    default String getTable() {
        val interfaces = getClass().getInterfaces();
        for (val mapperInterface : interfaces) {
            if (SuperMapper.class.isAssignableFrom(mapperInterface)) {
                return TigonMyBatisConfiguration.getStaticInstance().getTables().get(mapperInterface);
            }
        }

        throw new IllegalStateException("No table got from mapper ["
                + Arrays.asList(interfaces).stream().map(Class::getName).collect(Collectors.joining(", "))
                + "]");
    }
}
