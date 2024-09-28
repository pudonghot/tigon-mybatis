package com.pudonghot.tigon.mybatis.xmlgen.contentprovider;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import org.w3c.dom.Node;
import java.util.ArrayList;
import java.util.Collections;
import com.pudonghot.tigon.mybatis.xmlgen.XmlGenArg;

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

    @Getter
    @Setter
    public static class Content {
        private final String content;
        private final List<Node> nodes;

        public Content(final String content) {
            this.content = content;
            this.nodes = Collections.emptyList();
        }

        public Content(final List<Node> nodes) {
            this.content = null;
            this.nodes = nodes;
        }

        public Content(final Node element) {
            this(new ArrayList<>());
            nodes.add(element);
        }
    }
}
