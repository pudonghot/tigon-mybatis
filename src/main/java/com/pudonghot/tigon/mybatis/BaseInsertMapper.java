package com.pudonghot.tigon.mybatis;

import java.util.Collection;
import org.apache.ibatis.annotations.Param;
import com.pudonghot.tigon.mybatis.xmlgen.annotation.MapperXmlEl;

/**
 * @author Donghuang
 * @date Oct 17, 2015 2:09:20 PM
 */
@SuppressWarnings("hiding")
@MapperXmlEl(tag = MapperXmlEl.Tag.INSERT, id = "insert")
public interface BaseInsertMapper<Entity> extends SuperMapper<Entity> {

    /**
     * insert entity
     *
     * @param entity entity
     * @return insert result
     */
    int insert(@Param(PARAM_MODEL_KEY) Entity entity);

    /**
     * insert entities
     *
     * @param entities entities
     * @return insert result
     */
    int insert(@Param(PARAM_MODELS_KEY) Collection<Entity> entities);

    /**
     * insert entities
     *
     * @param entities entities
     * @return insert result
     */
    int insert(@Param(PARAM_MODELS_KEY) Entity[] entities);
}
