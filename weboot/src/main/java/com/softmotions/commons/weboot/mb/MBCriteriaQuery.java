package com.softmotions.commons.weboot.mb;

import com.softmotions.commons.cont.CollectionUtils;
import com.softmotions.commons.cont.Stack;

import org.apache.ibatis.session.RowBounds;

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

    private Integer rowOffset;

    private Integer rowLimit;

    private String namespace;

    public MBCriteriaQuery() {
        this(null, DEFAULT_PREFIX);
    }

    public MBCriteriaQuery(String namespace) {
        this(namespace, DEFAULT_PREFIX);
    }

    public MBCriteriaQuery(String namespace, String cqPrefix) {
        this(namespace, cqPrefix, null);
    }

    public MBCriteriaQuery(String namespace, Map<String, Object> params) {
        this(namespace, DEFAULT_PREFIX, params);
    }

    public MBCriteriaQuery(String namespace, String cqPrefix, Map<String, Object> params) {
        this.cqPrefix = cqPrefix;
        this.namespace = namespace;
        if (params != null) {
            putAll(params);
        }
    }

    public void clear() {
        finished = false;
        columnPrefix = null;
        orderBySpec = null;
        rowLimit = null;
        rowOffset = null;
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

    public T params(Object... params) {
        String key = null;
        for (int i = 0; i < params.length; ++i) {
            if (i % 2 == 0) {
                key = String.valueOf(params[i]);
            } else if (key != null) {
                put(key, params[i]);
                key = null;
            }
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

    public T limit(int val) {
        rowLimit = val;
        return putQ("LIMIT", val);
    }

    public T offset(int val) {
        rowOffset = val;
        return putQ("OFFSET", val);
    }

    public String getNamespace() {
        return namespace;
    }

    public RowBounds getRowBounds() {
        if (rowLimit == null && rowOffset == null) {
            return RowBounds.DEFAULT;
        }
        return new RowBounds(rowOffset != null ? rowOffset : 0,
                             rowLimit != null ? rowLimit : 0);

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
        if (namespace != null) {
            this.statement = namespace + "." + this.statement;
        }
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
