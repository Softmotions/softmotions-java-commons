package com.softmotions.weboot.cayenne;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.tx.BaseTransaction;
import org.apache.cayenne.tx.Transaction;
import org.apache.cayenne.tx.TransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public final class TransactionalInterceptor implements MethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TransactionalInterceptor.class);

    @SuppressWarnings("StaticCollection")
    private static final Map<Class<? extends TransactionExceptionChecker>, TransactionExceptionChecker>
            EX_CHECKERS = new ConcurrentHashMap<>();

    private TransactionFactory txFactory;

    private JdbcEventLogger jdbcEventLogger;

    @Inject
    public void setServerRuntime(ServerRuntime serverRuntime) {
        log.info("Activating @Transactional interceptor {}", getClass().getName());
        txFactory = serverRuntime.getInjector().getInstance(TransactionFactory.class);
        jdbcEventLogger = serverRuntime.getInjector().getInstance(JdbcEventLogger.class);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {

        // join existing tx if it is in progress... in such case do not try to
        // commit or roll it back
        Transaction currentTx = BaseTransaction.getThreadTransaction();
        if (currentTx != null) {
            if (log.isDebugEnabled()) {
                log.debug("We are nested transaction, delegating invocation method: {}", invocation.getMethod().getName());
            }
            return invocation.proceed();
        }

        Object ret = null;
        Method interceptedMethod = invocation.getMethod();
        Transactional transactional = interceptedMethod.getAnnotation(Transactional.class);
        if (transactional == null) {
            transactional = interceptedMethod.getDeclaringClass().getAnnotation(Transactional.class);
        }
        if (log.isDebugEnabled()) {
            log.debug("@Transactional annotation: {}", transactional);
        }

        // start a new tx and manage it till the end
        Throwable thrown = null;
        Throwable thrown2 = null;
        Transaction tx = txFactory.createTransaction();
        if (log.isDebugEnabled()) {
            log.debug("Created tx: {}", tx);
        }
        BaseTransaction.bindThreadTransaction(tx);
        try {
            if (log.isDebugEnabled()) {
                log.debug("Calling transactional method: {}", invocation.getMethod().getName());
            }
            ret = invocation.proceed();
        } catch (Throwable tr) {
            if (log.isDebugEnabled()) {
                log.debug("Got exception: {}", tr.toString());
            }
            thrown = tr;
            TransactionExceptionChecker checker = getExceptionChecker(transactional);
            if (checker.needRollback(tr)) {
                if (log.isDebugEnabled()) {
                    log.debug("Exception checker voted for rollback");
                }
                tx.setRollbackOnly();
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Exception checker voted for commit");
                }
            }
            throw tr;
        } finally {
            ObjectContext octx = null;
            if (transactional.useCurrentObjectContext()) {
                octx = ObjectContextThreadHolder.getObjectContext();
                ObjectContextThreadHolder.removeObjectContext();
            }
            if (log.isDebugEnabled()) {
                log.debug("Perform {}", (tx.isRollbackOnly() ? "rollback" : "commit"));
            }
            tx = BaseTransaction.getThreadTransaction();
            if (log.isDebugEnabled()) {
                log.debug("finishing tx: {}", tx);
            }
            if (tx != null) {
                if (tx.isRollbackOnly()) {
                    try {
                        if (octx != null) {
                            octx.rollbackChanges();
                        }
                    } catch (Throwable e) {
                        thrown2 = e;
                        jdbcEventLogger.logQueryError(e);
                    } finally {
                        if (log.isDebugEnabled()) {
                            log.debug("BaseTransaction.bindThreadTransaction(null)");
                        }
                        BaseTransaction.bindThreadTransaction(null);
                    }
                    try {
                        if (log.isDebugEnabled()) {
                            log.debug("Do tx.rollback() tx: {}", tx);
                        }
                        tx.rollback();
                    } catch (Throwable e) {
                        thrown2 = e;
                        jdbcEventLogger.logQueryError(e);
                    }
                } else {
                    try {
                        if (octx != null) {
                            octx.commitChanges();
                        }
                    } catch (Throwable e) {
                        thrown2 = e;
                        jdbcEventLogger.logQueryError(e);
                    } finally {
                        if (log.isDebugEnabled()) {
                            log.debug("BaseTransaction.bindThreadTransaction(null)");
                        }
                        BaseTransaction.bindThreadTransaction(null);
                    }
                    try {
                        if (log.isDebugEnabled()) {
                            log.debug("Do tx.commit() tx: {}", tx);
                        }
                        if (tx.isRollbackOnly()) {
                            tx.rollback();
                        } else {
                            tx.commit();
                        }
                    } catch (Throwable e) {
                        thrown2 = e;
                        jdbcEventLogger.logQueryError(e);
                    }
                }
            }
            if (thrown == null && thrown2 != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Throw exception occurred during commit or rollback: {}", thrown2.toString());
                }
                //noinspection ThrowFromFinallyBlock
                throw thrown2;
            }
        }
        return ret;
    }

    private TransactionExceptionChecker getExceptionChecker(Transactional transactional) throws Exception {
        TransactionExceptionChecker checker = EX_CHECKERS.get(transactional.exceptionChecker());
        if (checker != null) {
            return checker;
        }
        checker = transactional.exceptionChecker().newInstance();
        EX_CHECKERS.put(transactional.exceptionChecker(), checker);
        return checker;
    }
}


