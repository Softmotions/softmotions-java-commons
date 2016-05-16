package com.softmotions.weboot.cayenne

import org.apache.cayenne.DataRow
import org.apache.cayenne.ObjectContext
import org.apache.cayenne.exp.Expression
import org.apache.cayenne.exp.ExpressionFactory
import org.apache.cayenne.query.ObjectSelect
import org.apache.cayenne.query.SQLSelect
import org.apache.cayenne.query.SelectById

inline fun <reified T : Any> ObjectContext.new(): T {
    return this.newObject(T::class.java)
}

inline fun <reified T : Any> objectSelect(exprStr: String? = null, expr: Expression? = null): ObjectSelect<T?> {
    if (exprStr != null) {
        return ObjectSelect.query(T::class.java, ExpressionFactory.exp(exprStr))
    } else if (expr != null) {
        return ObjectSelect.query(T::class.java, expr)
    } else {
        return ObjectSelect.query(T::class.java)
    }
}

inline fun <reified T : Any> objectDataRowSelect(exprStr: String? = null, expr: Expression? = null): ObjectSelect<DataRow?> {
    if (exprStr != null) {
        return ObjectSelect.dataRowQuery(T::class.java, ExpressionFactory.exp(exprStr))
    } else if (expr != null) {
        return ObjectSelect.dataRowQuery(T::class.java, expr)
    } else {
        return ObjectSelect.dataRowQuery(T::class.java)
    }
}

inline fun <reified T : Any> sqlScalarSelect(sql: String): SQLSelect<T?> {
    return SQLSelect.scalarQuery(T::class.java, sql)
}

inline fun <reified T : Any> sqlObjectSelect(sql: String): SQLSelect<T?> {
    return SQLSelect.query(T::class.java, sql)
}

fun sqlDataRowSelect(sql: String): SQLSelect<DataRow?> {
    return SQLSelect.dataRowQuery(sql)
}

inline fun <reified T : Any> selectById(id: Any): SelectById<T?> {
    return SelectById.query(T::class.java, id)
}

inline fun <reified T : Any> selectDataRowById(id: Any): SelectById<DataRow?> {
    return SelectById.dataRowQuery(T::class.java, id)
}


