package com.softmotions.weboot.cayenne

import org.apache.cayenne.DataRow
import org.apache.cayenne.ObjectContext
import org.apache.cayenne.configuration.server.ServerRuntime
import org.apache.cayenne.exp.Expression
import org.apache.cayenne.exp.ExpressionFactory
import org.apache.cayenne.exp.Property
import org.apache.cayenne.query.ColumnSelect
import org.apache.cayenne.query.ObjectSelect
import org.apache.cayenne.query.SQLSelect
import org.apache.cayenne.query.SelectById
import org.apache.cayenne.tx.BaseTransaction
import org.apache.cayenne.tx.TransactionDescriptor
import org.apache.cayenne.tx.TransactionManager
import org.apache.cayenne.tx.TransactionPropagation
import java.sql.Connection

inline fun <reified T : Any> ObjectContext.new(): T {
    return this.newObject(T::class.java)
}

inline fun <reified T : Any> objectSelect(expr: Expression? = null, exprStr: String? = null): ObjectSelect<T?> {
    return when {
        exprStr != null -> ObjectSelect.query<T>(T::class.java, ExpressionFactory.exp(exprStr))
        expr != null -> ObjectSelect.query<T>(T::class.java, expr)
        else -> ObjectSelect.query<T>(T::class.java)
    }
}

inline fun <reified T : Any> objectDataRowSelect(expr: Expression? = null, exprStr: String? = null): ObjectSelect<DataRow?> {
    return when {
        exprStr != null -> ObjectSelect.dataRowQuery(T::class.java, ExpressionFactory.exp(exprStr))
        expr != null -> ObjectSelect.dataRowQuery(T::class.java, expr)
        else -> ObjectSelect.dataRowQuery(T::class.java)
    }
}

inline fun <reified T : Any, E> singleColumnSelect(column: Property<E>): ColumnSelect<E?> {
    return ObjectSelect.columnQuery(T::class.java, column)
}

inline fun <reified T : Any> columnSelect(column: Property<*>, vararg restof: Property<*>): ColumnSelect<Array<Any?>> {
    return ObjectSelect.columnQuery(T::class.java, column, *restof)
}

inline fun <reified T : Any> sqlScalarSelect(sql: String): SQLSelect<T?> {
    return SQLSelect.scalarQuery<T>(T::class.java, sql)
}

inline fun <reified T : Any> sqlScalarSelectOne(octx: ObjectContext, sql: String): T? {
    return SQLSelect.scalarQuery<T>(T::class.java, sql).selectOne(octx)
}

inline fun <reified T : Any> sqlScalarSelectFirst(octx: ObjectContext, sql: String): T? {
    return SQLSelect.scalarQuery<T>(T::class.java, sql).selectFirst(octx)
}

inline fun <reified T : Any> sqlObjectSelect(sql: String): SQLSelect<T?> {
    return SQLSelect.query<T>(T::class.java, sql)
}

inline fun <reified T : Any> sqlObjectSelectOne(octx: ObjectContext, sql: String): T? {
    return SQLSelect.query<T>(T::class.java, sql).selectOne(octx)
}

inline fun <reified T : Any> sqlObjectSelectFirst(octx: ObjectContext, sql: String): T? {
    return SQLSelect.query<T>(T::class.java, sql).selectFirst(octx)
}

fun sqlDataRowSelect(sql: String): SQLSelect<DataRow?> {
    return SQLSelect.dataRowQuery(sql)
}

fun sqlDataRowSelectOne(octx: ObjectContext, sql: String): DataRow? {
    return SQLSelect.dataRowQuery(sql).selectOne(octx)
}

fun sqlDataRowSelectFirst(octx: ObjectContext, sql: String): DataRow? {
    return SQLSelect.dataRowQuery(sql).selectFirst(octx)
}

inline fun <reified T : Any> selectById(id: Any): SelectById<T?> {
    return SelectById.query<T>(T::class.java, id)
}

inline fun <reified T : Any> selectOneById(octx: ObjectContext, id: Any): T? {
    return SelectById.query<T>(T::class.java, id).selectOne(octx)
}

inline fun <reified T : Any> selectDataRowById(id: Any): SelectById<DataRow?> {
    return SelectById.dataRowQuery(T::class.java, id)
}

inline fun <reified T : Any> selectOneDataRowById(octx: ObjectContext, id: Any): DataRow? {
    return SelectById.dataRowQuery(T::class.java, id).selectOne(octx)
}


///////////////////////////////////////////////////////////////////////////
//                           Server runtime                              //
///////////////////////////////////////////////////////////////////////////

val NEW_TX_DESCRIPTOR = TransactionDescriptor(
        Connection.TRANSACTION_SERIALIZABLE,
        TransactionPropagation.REQUIRES_NEW  // require new transaction for every operation
)

val MANDATORY_TX_DESCRIPTOR = TransactionDescriptor(
        Connection.TRANSACTION_SERIALIZABLE,
        TransactionPropagation.MANDATORY
)

fun <T> ServerRuntime.performInNewTransaction(action: () -> T): T {
    val txm = this.injector.getInstance(TransactionManager::class.java)
    return txm.performInTransaction(action, NEW_TX_DESCRIPTOR)
}

fun <T> ServerRuntime.performInMandatoryTransaction(action: () -> T): T {
    val txm = this.injector.getInstance(TransactionManager::class.java)
    return txm.performInTransaction(action, MANDATORY_TX_DESCRIPTOR)
}

fun ServerRuntime.isInTransaction(): Boolean {
    val tx = BaseTransaction.getThreadTransaction()
    return tx != null
}

