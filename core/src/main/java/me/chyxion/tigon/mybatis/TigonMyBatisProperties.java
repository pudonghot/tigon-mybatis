package me.chyxion.tigon.mybatis;

import lombok.Setter;
import lombok.Getter;
import lombok.ToString;
import java.io.Serializable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Donghuang
 * @date Mar 25, 2023 16:34:51
 */
@Getter
@Setter
@ToString
@ConfigurationProperties("tigon.mybatis")
public class TigonMyBatisProperties implements Serializable {
    /**
     * startup check config
     */
    private boolean startupCheck = true;

    /**
     * database quotation mark，for example：MySQL `，Oracle "
     */
    private String quotationMark;
}
