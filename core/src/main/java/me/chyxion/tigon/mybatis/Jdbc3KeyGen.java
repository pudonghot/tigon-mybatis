package me.chyxion.tigon.mybatis;

import lombok.var;
import lombok.val;
import java.util.*;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import lombok.extern.slf4j.Slf4j;
import java.sql.ResultSetMetaData;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.mapping.MappedStatement;
import me.chyxion.tigon.mybatis.util.EntityUtils;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;

/**
 * JDBC3 key generator, supports multiple keys return
 *
 * @author Donghuang
 * @date 2017/1/9 14:46
 */
@Slf4j
class Jdbc3KeyGen extends Jdbc3KeyGenerator {
    static final Jdbc3KeyGen OBJECT = new Jdbc3KeyGen();
    static final String MS_KEY_GEN_FIELD = "keyGenerator";

    /**
     * {@inheritDoc}
     */
    @Override
    public void processBefore(Executor executor,
        MappedStatement ms,
        Statement stmt,
        Object parameter) {
        // noop
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processAfter(final Executor executor,
        final MappedStatement ms,
        final Statement stmt,
        final Object params) {

        val models = getModels(params);
        if (models.isEmpty()) {
            log.info("No models found in params, use default Jdbc3KeyGenerator#processAfter");
            super.processAfter(executor, ms, stmt, params);
            return;
        }

        try (val rs = stmt.getGeneratedKeys()) {
            var keyProps = ms.getKeyProperties();

            if (keyProps == null || keyProps.length == 0) {
                keyProps = new String[] { EntityUtils.ID };
            }

            val rsmd = rs.getMetaData();
            if (rsmd.getColumnCount() >= keyProps.length) {
                val config = ms.getConfiguration();
                val typeHandlerRegistry = config.getTypeHandlerRegistry();

                for (val model : models) {
                    log.debug("Process model [{}] key.", model);
                    // there should be one row for each statement (also one for each params)
                    if (rs.next()) {
                        val metaModel = config.newMetaObject(model);
                        populateKeys(rs, metaModel, keyProps,
                            getTypeHandlers(typeHandlerRegistry, metaModel, keyProps, rsmd));
                        log.debug("Populate model [{}] key result.", model);
                    }
                    else {
                        log.debug("Key result set end.");
                        break;
                    }
                }
            }
            else {
                log.warn("Generated keys' meta data columns' size less than keys, ignore.");
            }
        }
        catch (Exception e) {
            throw new ExecutorException(
                "Error getting generated key or setting result to params object", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> getModels(final Object objParams) {
        if (objParams instanceof MapperMethod.ParamMap) {
            val mapParams = (Map<String, Object>) objParams;

            if (mapParams.containsKey(SuperMapper.PARAM_MODEL_KEY)) {
                return Arrays.asList(mapParams.get(SuperMapper.PARAM_MODEL_KEY));
            }

            if (mapParams.containsKey(SuperMapper.PARAM_MODELS_KEY)) {
                val objModels = mapParams.get(SuperMapper.PARAM_MODELS_KEY);

                if (objModels instanceof Collection) {
                    return (Collection<Object>) objModels;
                }

                if (objModels.getClass().isArray()) {
                    return Arrays.asList((Object[]) objModels);
                }
            }
        }

        return Collections.emptyList();
    }

    private TypeHandler<?>[] getTypeHandlers(
        final TypeHandlerRegistry typeHandlerRegistry,
        final MetaObject metaParam,
        final String[] keyProps,
        final ResultSetMetaData rsmd) throws SQLException {

        val typeHandlers = new TypeHandler<?>[keyProps.length];

        int i = 0;
        for (val keyProp : keyProps) {
            if (metaParam.hasSetter(keyProp)) {
                typeHandlers[i] = typeHandlerRegistry.getTypeHandler(
                        metaParam.getSetterType(keyProp),
                        JdbcType.forCode(rsmd.getColumnType(++i)));
            }
        }

        return typeHandlers;
    }

    private void populateKeys(
        final ResultSet rs,
        final MetaObject metaParam,
        final String[] keyProps,
        final TypeHandler<?>[] typeHandlers) throws SQLException {

        int i = 0;
        for (val keyProp : keyProps) {
            val th = typeHandlers[i];
            if (th != null) {
                metaParam.setValue(keyProp, th.getResult(rs, ++i));
            }
        }
    }
}
