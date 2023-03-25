package me.chyxion.tigon.mybatis.xmlgen.contentprovider;

import lombok.val;
import lombok.extern.slf4j.Slf4j;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;
import me.chyxion.tigon.mybatis.xmlgen.XmlGenArg;
import me.chyxion.tigon.mybatis.util.EntityUtils;
import me.chyxion.tigon.mybatis.TigonMyBatisProperties;

/**
 * @author Donghuang
 * @date Jan 09, 2020 18:05:43
 */
@Slf4j
public class ColsXmlContentProvider extends XmlContentProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public Content content(final XmlGenArg arg) {
        val entityClass = arg.getEntityClass();

        val props = arg.getBeanFactory().getBean(TigonMyBatisProperties.class);
        val quotationMark = props.getQuotationMark();

        if (StringUtils.hasText(quotationMark)) {
            return new Content(EntityUtils.cols(entityClass)
                        .stream().map(col -> quotationMark + col + quotationMark)
                        .collect(Collectors.joining(", ")));
        }

        return new Content(EntityUtils.cols(entityClass)
                    .stream().collect(Collectors.joining(", ")));
    }
}
