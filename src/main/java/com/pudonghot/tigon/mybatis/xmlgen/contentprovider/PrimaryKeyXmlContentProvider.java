package com.pudonghot.tigon.mybatis.xmlgen.contentprovider;

import com.pudonghot.tigon.mybatis.xmlgen.XmlGenArg;
import lombok.extern.slf4j.Slf4j;
import com.pudonghot.tigon.mybatis.util.EntityUtils;

/**
 * @author Donghuang
 * @date Jan 09, 2020 18:05:43
 */
@Slf4j
public class PrimaryKeyXmlContentProvider extends XmlContentProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public Content content(final XmlGenArg arg) {
        return new Content(EntityUtils.primaryKeyCol(arg.getEntityClass()));
    }
}
