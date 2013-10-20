package com.softmotions.commons.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Pattern;

/**
 * Mongodb constrains.
 *
 * @author Adamansky Anton (anton@adamansky.com)
 * @version $Id$
 */
public class Constraints implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, Object> query;

    private final Map<String, Integer> fields;

    private BasicDBObject dbquery, dbfields, dborderby;

    private Sort sort;

    private int startIndex, resultSize;

    private Stack<String> fieldStack = new Stack<>();

    private Map<String, Object> elementMatch;

    public Constraints() {
        startIndex = -1;
        resultSize = -1;
        query = new HashMap<>();
        fields = new HashMap<>();
    }

    public Constraints(String name, Object value) {
        this();
        field(name).equalTo(value);
    }

    public Constraints orderByDesc(String name) {
        return orderBy(name, false);
    }

    public Constraints orderBy(String name) {
        return orderBy(name, true);
    }

    public Constraints orderBy(String name, boolean ascending) {
        dborderby = null;
        if (sort == null) {
            sort = new Sort();
        }
        sort.add(name, ascending);
        return this;
    }

    public Constraints skip(int resultsToSkip) {
        startIndex = resultsToSkip;
        return this;
    }

    public Constraints limit(int batchSize) {
        resultSize = batchSize;
        return this;
    }

    public Constraints field(String name) {
        fieldStack.push(name);
        return this;
    }

    public Constraints id() {
        return field("_id");
    }

    public Constraints equalTo(Object value) {
        return addField(value);
    }

    public Constraints notEqualTo(Object value) {
        return addMapField(FilterOperator.NOT_EQUAL, value);
    }

    public Constraints lessThan(Object value) {
        return addMapField(FilterOperator.LESS_THAN, value);
    }

    public Constraints lessThanOrEqualTo(Object value) {
        return addMapField(FilterOperator.LESS_THAN_OR_EQUAL, value);
    }

    public Constraints greaterThan(Object value) {
        return addMapField(FilterOperator.GREATER_THAN, value);
    }

    public Constraints greaterThanOrEqualTo(Object value) {
        return addMapField(FilterOperator.GREATER_THAN_OR_EQUAL, value);
    }

    public Constraints exists() {
        return addMapField(FilterOperator.EXISTS, true);
    }

    public Constraints notExists() {
        return addMapField(FilterOperator.EXISTS, false);
    }

    public Constraints notExistsOrNull() {
        return addField(null);
    }

    public Constraints matches(String regExp, boolean caseInsensitive) {
        Pattern pattern = caseInsensitive
                          ? Pattern.compile(regExp, Pattern.CASE_INSENSITIVE)
                          : Pattern.compile(regExp);
        return addField(pattern);
    }

    public Constraints size(int size) {
        return addMapField(FilterOperator.SIZE, size);
    }

    public Constraints mod(int modulo, int result) {
        List<Integer> value = new ArrayList<>();
        value.add(modulo);
        value.add(result);
        return addMapField(FilterOperator.MOD, value);
    }

    @SuppressWarnings("unchecked")
    public Constraints hasAnyOf(List values) {
        return addMapField(FilterOperator.IN, values);
    }

    @SuppressWarnings("unchecked")
    public Constraints hasNoneOf(List values) {
        return addMapField(FilterOperator.NOT_IN, values);
    }

    public Constraints or(Constraints... values) {
        field("$or");
        List<Map> orvals = new ArrayList<>(values.length);
        for (final Constraints c : values) {
            orvals.add(c.createDBQuery());
        }
        addField(orvals);
        return this;
    }

    @SuppressWarnings("unchecked")
    public Constraints hasAllOf(List values) {
        return addMapField(FilterOperator.ALL, values);
    }

    public Constraints elementMatch() {
        Map<String, Object> em = this.elementMatch != null ? this.elementMatch : new HashMap();
        this.elementMatch = null;
        addMapField(FilterOperator.ELEMENT_MATCH, em);
        return this;
    }

    public Constraints addElementMatchField(String key) {
        if (this.elementMatch == null) {
            this.elementMatch = new HashMap<>();
        }
        fieldStack.push(key);
        return this;
    }

    public Constraints where(String clause) {
        query.put("$where", clause);
        return this;
    }

    /**
     * Specifiy that the field with the name supplied should be
     * returned in the results.
     *
     * @param names names of fields to include
     * @return
     */
    public Constraints include(String... names) {
        dbfields = null;
        for (String name : names) {
            fields.put(name, 1);
        }
        return this;
    }

    /**
     * Specifiy that the field with the name supplied should NOT be
     * returned in the results.
     *
     * @param names names of fields to exclude
     * @return
     */
    public Constraints exclude(String... names) {
        dbfields = null;
        for (String name : names) {
            fields.put(name, 0);
        }
        return this;
    }

    private Constraints addField(Object value) {
        dbquery = null;
        validateField();
        if (elementMatch != null) {
            elementMatch.put(fieldStack.pop(), valueOf(value));
        } else {
            query.put(fieldStack.pop(), valueOf(value));
        }
        return this;
    }

    private Constraints addMapField(FilterOperator op, Object value) {
        dbquery = null;
        validateField();
        if (elementMatch != null) {
            elementMatch.put(fieldStack.pop(), map(op.val(), valueOf(value)));
        } else {
            query.put(fieldStack.pop(), map(op.val(), valueOf(value)));
        }
        return this;
    }

    private void validateField() {
        if (fieldStack.isEmpty()) {
            throw new ConstraintsException("Must specify field first");
        }
    }

    @SuppressWarnings("unchecked")
    static Object valueOf(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj.getClass().isEnum()) {
            return ((Enum) obj).name();
        } else if (obj instanceof Constraints) {
            return ((Constraints) obj).createDBQuery();
        } else if (obj instanceof Locale) {
            return ((Locale) obj).toString();
        } else {
            return obj;
        }
    }

    static Map<String, Object> map(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    public BasicDBObject createDBQuery() {
        if (dbquery != null) {
            return dbquery;
        }
        if (!fieldStack.isEmpty()) {
            throw new ConstraintsException("Invalid constraints, found unused fields: " + fieldStack);
        }
        dbquery = new BasicDBObject(query);
        return dbquery;
    }

    public BasicDBObject createDBFields() {
        if (dbfields != null) {
            return dbfields;
        }
        dbfields = (fields != null && !fields.isEmpty()) ? new BasicDBObject(fields) : null;
        return dbfields;
    }

    public BasicDBObject createDBOrderBy() {
        if (dborderby != null) {
            return dborderby;
        }
        if (sort != null && !sort.getFields().isEmpty()) {
            dborderby = new BasicDBObject();
            for (Sort.SortField s : sort.getFields()) {
                dborderby.put(s.getName(), s.isAscending() ? 1 : -1);
            }
        }
        return dborderby;
    }

    public DBCursor applyToCursor(DBCursor cursor) {
        BasicDBObject orderBy = createDBOrderBy();
        if (orderBy != null) {
            cursor.sort(orderBy);
        }
        if (startIndex > 0) {
            cursor.skip(startIndex);
        }
        if (resultSize > 0) {
            cursor.limit(resultSize);
        }
        return cursor;
    }

    public DBObject findOne(DBCollection coll) {
        return coll.findOne(createDBQuery(), createDBFields(), createDBOrderBy());
    }

    public DBCursor find(DBCollection coll) {
        DBCursor cur = coll.find(createDBQuery(), createDBFields());
        return applyToCursor(cur);
    }

    public long count(DBCollection coll) {
        return coll.count(createDBQuery());
    }

    public List distinct(DBCollection coll, String key) {
        return coll.distinct(key, createDBQuery());
    }

    public int getResultSize() {
        return resultSize;
    }

    public Sort getSort() {
        return sort;
    }

    public int getStartIndex() {
        return startIndex;
    }

}
