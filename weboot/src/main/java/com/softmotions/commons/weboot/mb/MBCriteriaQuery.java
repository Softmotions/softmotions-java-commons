package com.softmotions.commons.weboot.mb;

import com.softmotions.commons.cont.CollectionUtils;
import com.softmotions.commons.cont.Stack;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for MyBatis criteria queries.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
@SuppressWarnings("unchecked")
public class MBCriteriaQuery<T extends MBCriteriaQuery> extends HashMap<String, Object> {

    public static final String DEFAULT_PREFIX = "CQ_";

    private final String cqPrefix;

    private String columnPrefix;

    private Stack<String> orderBySpec;

    private boolean finished;

    private String statement;

    public MBCriteriaQuery() {
        this(DEFAULT_PREFIX);
    }

    public MBCriteriaQuery(String cqPrefix) {
        this(cqPrefix, null);
    }

    public MBCriteriaQuery(Map<String, Object> params) {
        this(DEFAULT_PREFIX, params);
    }

    public MBCriteriaQuery(String cqPrefix, Map<String, Object> params) {
        this.cqPrefix = cqPrefix;
        if (params != null) {
            putAll(params);
        }
    }

    public void clear() {
        finished = false;
        columnPrefix = null;
        orderBySpec = null;
        super.clear();
    }

    public boolean isFinished() {
        return finished;
    }

    public T finish() {
        if (finished) {
            return (T) this;
        }
        this.finished = true;
        if (orderBySpec != null) {
            put(cqPrefix + "ORDERBY", CollectionUtils.join(", ", orderBySpec));
            orderBySpec = null;
        }
        return (T) this;
    }

    public T prefixedBy(String prefix) {
        preActionCheck();
        this.columnPrefix = prefix;
        return (T) this;
    }

    public T orderBy(String... columns) {
        preActionCheck();
        if (orderBySpec == null) {
            orderBySpec = new Stack<>();
        }
        for (String c : columns) {
            orderBySpec.push(toColumnName(c));
        }
        return (T) this;
    }

    public T asc() {
        preActionCheck();
        addOrderByMod(true);
        return (T) this;
    }

    public T desc() {
        preActionCheck();
        addOrderByMod(false);
        return (T) this;
    }

    public T limit(long val) {
        return putQ("LIMIT", val);
    }

    public T offset(long val) {
        return putQ("OFFSET", val);
    }

    public T pk(Object val) {
        return putQ("PK", val);
    }

    public T param(String name, Object val) {
        put(name, val);
        return (T) this;
    }

    public String getStatement() {
        return statement;
    }

    public T withStatement(String statement) {
        this.statement = statement;
        return (T) this;
    }

    protected void addOrderByMod(boolean asc) {
        if (orderBySpec == null || orderBySpec.isEmpty()) {
            throw new IllegalStateException("Please specify 'order by' columns before");
        }
        String last = orderBySpec.pop();
        last += (asc ? " ASC" : " DESC");
        orderBySpec.push(last);
    }

    protected String toColumnName(String col) {
        return columnPrefix != null ? (columnPrefix + col) : col;
    }

    protected T putQ(String key, Object val) {
        preActionCheck();
        put(cqPrefix + key, val);
        return (T) this;
    }

    protected void preActionCheck() {
        if (finished) {
            throw new IllegalStateException(getClass().getSimpleName() + " already finished");
        }
    }
}
