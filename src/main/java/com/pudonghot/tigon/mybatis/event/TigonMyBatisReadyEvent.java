package com.pudonghot.tigon.mybatis.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationContext;
import com.pudonghot.tigon.mybatis.TigonMyBatisConfiguration;

/**
 * @author Donghuang
 * @date Nov 23, 2020 21:28:02
 */
public class TigonMyBatisReadyEvent extends ApplicationEvent {
    private static final long serialVersionUID = 1L;

    @Getter
    private final TigonMyBatisConfiguration configuration;

    /**
     * Create a new {@link TigonMyBatisReadyEvent} instance.
     *
     * @param context the context that was being created
     */
    public TigonMyBatisReadyEvent(final ApplicationContext context,
                                  final TigonMyBatisConfiguration configuration) {
        super(context);
        this.configuration = configuration;
    }

    public ApplicationContext getContext() {
        return (ApplicationContext) source;
    }
}
