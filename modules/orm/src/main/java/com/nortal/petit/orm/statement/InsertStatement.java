/**
 *   Copyright 2014 Nortal AS
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.nortal.petit.orm.statement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.persistence.PersistenceException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.nortal.petit.beanmapper.Property;
import com.nortal.petit.core.util.ArgPreparedStatementSetter;

/**
 * @author Lauri Lättemäe (lauri.lattemae@nortal.com)
 * @created 29.04.2013
 * @param <B>
 */
public class InsertStatement<B> extends BeansStatement<B, InsertStatement<B>> {
    protected final static Logger log = LoggerFactory.getLogger(InsertStatement.class);
	
    public InsertStatement(JdbcOperations jdbcTemplate, StatementBuilder statementBuilder, Class<B> beanClass) {
        Assert.isTrue(beanClass != null, "InsertStatement.construct: beanClass is mandatory");
        init(jdbcTemplate, statementBuilder, beanClass);
    }

    @SuppressWarnings("unchecked")
	public InsertStatement(JdbcOperations jdbcTemplate, StatementBuilder statementBuilder, B... beans) {
        Assert.isTrue(ArrayUtils.isNotEmpty(beans), "InsertStatement.construct: beans are mandatory");
        init(jdbcTemplate, statementBuilder, (Class<B>) beans[0].getClass());

        this.beans = Arrays.asList(beans);
        // by default insert all properties to db
        setBy(StatementUtil.toStringArray(getWritableProps(getMapping())));
    }

    @Override
    protected void prepare() {
        getStatementBuilder().setPropertyNameMapper(getPropertyNameMapper(false));
        prepareSet();
        setSql(getStatementBuilder().getInsert());
    }

    @Override
    public void exec() {
        prepare();
        if (!CollectionUtils.isEmpty(getBeans())) {
            if (getMapping().id() == null) {
                execBatchUpdate();
            } else {
                Property<B, Object> idProperty = getMapping().id();
                InsertCallbackResult result = getJdbcTemplate().execute(new InsertStatementCreator(idProperty.column()), new InsertStatementCallback(idProperty));

                try {
                    for (int i = 0; i < getBeans().size(); i++) {
                        B bean = getBeans().get(i);
                        Object key = result.getKeyHolder().getKeyList().get(i).get(idProperty.column());
                        idProperty.write(bean, key);
                        result.getInterceptorCalls().setBeanId(bean, key);
                    }
                } catch (Exception e) {
                    throw new PersistenceException("InsertStatement.exec: unable to write bean primary key", e);
                }
                result.getInterceptorCalls().callInterceptor();
            }
        } else {
            getJdbcTemplate().update(getSql(), getParams(null));
        }
    }

    @Override
    protected StatementType getStatementType() {
        return StatementType.INSERT;
    }

    private final class InsertCallbackResult {
        private KeyHolder keyHolder;
        private InterceptorCalls interceptorCalls;
        
        public InsertCallbackResult(KeyHolder keyHolder, InterceptorCalls interceptorCalls) {
            this.keyHolder = keyHolder;
            this.interceptorCalls = interceptorCalls;
        }

        public KeyHolder getKeyHolder() {
            return keyHolder;
        }

        public InterceptorCalls getInterceptorCalls() {
            return interceptorCalls;
        }
    }
    
    private final class InsertStatementCallback implements PreparedStatementCallback<InsertCallbackResult> {
        private final InsertStatement<B>.InterceptorCalls interceptorCalls = new InterceptorCalls();
        private final KeyHolder keyHolder =  new GeneratedKeyHolder();
        private Property<B, Object> idProperty;
        
        
        private InsertStatementCallback(Property<B, Object> idProperty) {
            this.idProperty = idProperty;
        }

        @Override
        public InsertCallbackResult doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
            MappingParamFunction<B> paramFunction = new MappingParamFunction<B>(getMapping());

            for (B bean : getBeans()) {
                paramFunction.setBean(bean);
                Object[] params = getParams(paramFunction);
                Object[] queryParams = params.length == 1 && params[0] instanceof Object[] ? (Object[]) params[0]
                        : params;
                interceptorCalls.setBeanValues(bean, queryParams);
                ArgPreparedStatementSetter.setValues(ps, queryParams, 1);

                ps.executeUpdate();
                extractKeys(ps);
            }
            return new InsertCallbackResult(keyHolder, interceptorCalls);
        }

        /**
         * @param ps
         * @throws SQLException
         */
        private void extractKeys(PreparedStatement ps) throws SQLException {
            ResultSet keys = ps.getGeneratedKeys();
            if (keys != null) {
                try {
                    RowMapperResultSetExtractor<Map<String, Object>> rse = new RowMapperResultSetExtractor<Map<String, Object>>(
                            new InsertKeyColumnRowMapper(TypeUtils.getRawType(idProperty.type(), null)), 1);
                    keyHolder.getKeyList().addAll(rse.extractData(keys));
                } finally {
                    JdbcUtils.closeResultSet(keys);
                }
            }
        }
    }

    private final class InsertKeyColumnRowMapper extends ColumnMapRowMapper {
        private Class<?> idColumnTypeClass;
        
        public InsertKeyColumnRowMapper(Class<?> type) {
            this.idColumnTypeClass = type;
        }

        @Override
        protected Object getColumnValue(ResultSet rs, int index) throws SQLException {
            return JdbcUtils.getResultSetValue(rs, index, idColumnTypeClass);
        }
    }
    
    /**
     * Sets up the statement to return created keys.
     * When ID generation strategies are implemented this class is responsible for correctly initializing statement
     * 
     * @author Alrik Peets
     */
    private final class InsertStatementCreator implements PreparedStatementCreator {
        private String idColumn;
        
        public InsertStatementCreator(String idColumn) {
            this.idColumn = idColumn;
        }
        
        @Override
        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            //Statement.RETURN_GENERATED_KEYS works fine in PostgreSQL, MySQL. In Oracle this returns ROWID
            return con.prepareStatement(getSql(), new String[]{idColumn});
        }
    }

    /**
     * Collects data for interceptor parameters. One insert statement can use
     * batch insert to insert multiple entities, each insert
     * results in separate interceptor call.
     */
    private class InterceptorCalls {
        private String table;
        private String[] columns;
        private List<BeanDataHolder> dataHolders = new ArrayList<>();

        public InterceptorCalls() {
            this.table = getMapping().table();
            this.columns = Iterables.toArray(Lists.transform(setBy, getPropertyNameMapper(true)), String.class);
        }

        private BeanDataHolder findDataHolder(B bean) {
        	for (BeanDataHolder bdh : dataHolders) {
        		if (bdh.isSameBean(bean)) {
        			return bdh;
        		}
        	}
        	BeanDataHolder bdh = new BeanDataHolder(bean);
        	dataHolders.add(bdh);
        	return bdh;
        }
        
        public void setBeanId(B bean, Object id) {
        	BeanDataHolder bdh = findDataHolder(bean);
        	bdh.setId(id);
        }

        public void setBeanValues(B bean, Object[] values) {
        	BeanDataHolder bdh = findDataHolder(bean);
        	bdh.setValues(values);
        }

        public void callInterceptor() {
            if (getStatementBuilder().getInterceptor() != null) {
                for (B bean : getBeans()) {
                	BeanDataHolder bdh = findDataHolder(bean);
                    getStatementBuilder().getInterceptor().afterInsert(table, bdh.getId(), bdh.getValues(), columns);
                }
            }
        }
    }
    
    private class BeanDataHolder {
    	private B bean;
    	private Object id;
    	private Object[] values;

    	public BeanDataHolder(B bean) {
    		this.bean = bean;
    	}
    	
    	public boolean isSameBean(B other) {
    		return other != null && other.equals(bean);
    	}
		public Object getId() {
			return id;
		}
		public void setId(Object id) {
			this.id = id;
		}
		public Object[] getValues() {
			return values;
		}
		public void setValues(Object[] values) {
			this.values = values;
		}
    }
}
