package me.chyxion.tigon.mybatis.xmlgen.contentprovider;

import lombok.val;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.ArrayList;
import org.w3c.dom.Element;
import java.util.Collections;
import org.springframework.util.StringUtils;
import me.chyxion.tigon.mybatis.xmlgen.XmlGenArg;
import me.chyxion.tigon.mybatis.TigonMyBatisProperties;

/**
 * @author Donghuang
 * @date Feb 21, 2017 17:42:03
 */
public abstract class XmlContentProvider {

    /**
     * sql tag content
     *
     * @param arg arg
     * @return sql tag content
     */
    public abstract Content content(final XmlGenArg arg);

    /**
     * wrap content quotation mark
     *
     * @param arg arg
     * @param content content
     * @return content
     */
    protected Content wrapQuotationMark(final XmlGenArg arg, final String content) {
        val props = arg.getBeanFactory().getBean(TigonMyBatisProperties.class);
        val quotationMark = props.getQuotationMark();
        return new Content(StringUtils.hasText(quotationMark) ?
                    quotationMark + content + quotationMark : content);
    }

    @Getter
    @Setter
    public static class Content {
        private final boolean text;
        private final String content;
        private final List<Element> elements;

        public Content(final String content) {
            this.text = true;
            this.content = content;
            this.elements = Collections.emptyList();
        }

        public Content(final List<Element> elements) {
            this.text = false;
            this.content = null;
            this.elements = elements;
        }

        public Content(final Element element) {
            this(new ArrayList<>());
            elements.add(element);
        }
    }
}
