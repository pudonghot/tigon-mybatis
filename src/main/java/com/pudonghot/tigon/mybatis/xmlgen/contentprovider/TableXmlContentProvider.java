package com.pudonghot.tigon.mybatis.xmlgen.contentprovider;

import lombok.val;
import lombok.extern.slf4j.Slf4j;
import com.pudonghot.tigon.mybatis.xmlgen.XmlGenArg;
import com.pudonghot.tigon.mybatis.util.EntityUtils;

/**
 * @author Donghuang
 * @date Feb 21, 2017 17:42:03
 */
@Slf4j
public class TableXmlContentProvider extends XmlContentProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public Content content(final XmlGenArg arg) {
        val tableName = arg.getTable();
        val resolvedTableName = arg.getBeanFactory().resolveEmbeddedValue(tableName);
        log.info("Table content [{}] generated from [{}].", resolvedTableName, tableName);
        return new Content(EntityUtils.quotationWrap(resolvedTableName));
    }
}
