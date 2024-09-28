package com.pudonghot.tigon.mybatis.customizer;

import lombok.val;
import java.util.Arrays;
import org.springframework.stereotype.Component;
import com.pudonghot.tigon.mybatis.xmlgen.XmlGenArg;
import com.pudonghot.tigon.mybatis.xmlgen.XmlGenCustomizer;
import com.pudonghot.tigon.mybatis.xmlgen.annotation.MapperXmlEl;
import com.pudonghot.tigon.mybatis.xmlgen.contentprovider.XmlContentProvider;

/**
 * @author Donghuang
 * @date Sep 28, 2024 11:09:29
 */
@Component
@MapperXmlEl(tag = MapperXmlEl.Tag.SQL, id = "operator", contentProvider = OperatorSqlTagGen.OperatorXmlContentProvider.class)
public class OperatorSqlTagGen implements XmlGenCustomizer {

    public static class OperatorXmlContentProvider extends XmlContentProvider {

        /**
         * {@inheritDoc}
         */
        @Override
        public Content content(final XmlGenArg arg) {
            val doc = arg.getDocument();
            val bindEl = doc.createElement("bind");
            bindEl.setAttribute("name", "__operator__");
            bindEl.setAttribute("value", "123");
            val textNode = doc.createTextNode("#{__operator__}");
            return new Content(Arrays.asList(bindEl, textNode));
        }
    }
}
