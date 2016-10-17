package com.softmotions.weboot.mb;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.mybatis.guice.transactional.Transactional;

import javax.inject.Inject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;

import com.softmotions.commons.ClassUtils;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class MBTransactionalMethodInterceptor implements MethodInterceptor {

    private static final Class<?>[] CAUSE_TYPES = new Class[]{Throwable.class};

    private static final Class<?>[] MESSAGE_CAUSE_TYPES = new Class[]{String.class, Throwable.class};

    /**
     * This class logger.
     */
    private final Log log = LogFactory.getLog(getClass());

    /**
     * The {@code SqlSessionManager} reference.
     */
    @Inject
    private MBSqlSessionManager sqlSessionManager;

    /**
     * Sets the SqlSessionManager instance.
     *
     * @param sqlSessionManager the SqlSessionManager instance.
     */
    public void setSqlSessionManager(MBSqlSessionManager sqlSessionManager) {
        this.sqlSessionManager = sqlSessionManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method interceptedMethod = invocation.getMethod();
        Transactional transactional = ClassUtils.getAnnotation(interceptedMethod, Transactional.class);

        // The annotation may be present at the class level instead
        if (transactional == null) {
            transactional = interceptedMethod.getDeclaringClass().getAnnotation(Transactional.class);
        }

        String debugPrefix = null;
        if (this.log.isDebugEnabled()) {
            debugPrefix = format("[Intercepted method: %s]", interceptedMethod.toGenericString());
        }

        boolean isSessionInherited = this.sqlSessionManager.isManagedSessionStarted();

        if (isSessionInherited) {
            if (log.isDebugEnabled()) {
                log.debug(format("%s - SqlSession already set for thread: %s",
                                 debugPrefix,
                                 currentThread().getId()));
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(format("%s - SqlSession not set for thread: %s, creating a new one",
                                 debugPrefix,
                                 currentThread().getId()));
            }

            sqlSessionManager.startManagedSession(transactional.executorType(), transactional.isolation().getTransactionIsolationLevel());
        }

        Object object = null;
        boolean needsRollback = transactional.rollbackOnly();
        try {
            object = invocation.proceed();
        } catch (Throwable t) {
            needsRollback = true;
            throw convertThrowableIfNeeded(invocation, transactional, t);
        } finally {
            if (!isSessionInherited) {
                try {
                    if (needsRollback) {
                        if (log.isDebugEnabled()) {
                            log.debug(debugPrefix
                                      + " - SqlSession of thread: "
                                      + currentThread().getId()
                                      + " rolling back");
                        }

                        sqlSessionManager.rollback(true);
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug(debugPrefix
                                      + " - SqlSession of thread: "
                                      + currentThread().getId()
                                      + " committing");
                        }

                        sqlSessionManager.commit(transactional.force());
                    }
                } finally {
                    if (log.isDebugEnabled()) {
                        log.debug(format("%s - SqlSession of thread: %s terminated its life-cycle, closing it",
                                         debugPrefix,
                                         currentThread().getId()));
                    }

                    sqlSessionManager.close();
                }
            } else if (log.isDebugEnabled()) {
                log.debug(format("%s - SqlSession of thread: %s is inherited, skipped close operation",
                                 debugPrefix,
                                 currentThread().getId()));
            }
        }

        return object;
    }

    private Throwable convertThrowableIfNeeded(MethodInvocation invocation, Transactional transactional, Throwable t) {
        Method interceptedMethod = invocation.getMethod();

        // check the caught exception is declared in the invoked method
        for (Class<?> exceptionClass : interceptedMethod.getExceptionTypes()) {
            if (exceptionClass.isAssignableFrom(t.getClass())) {
                return t;
            }
        }

        // check the caught exception is of same rethrow type
        if (transactional.rethrowExceptionsAs().isAssignableFrom(t.getClass())) {
            return t;
        }

        // rethrow the exception as new exception
        String errorMessage;
        Object[] initargs;
        Class<?>[] initargsType;

        if (!transactional.exceptionMessage().isEmpty()) {
            errorMessage = format(transactional.exceptionMessage(), invocation.getArguments());
            initargs = new Object[]{errorMessage, t};
            initargsType = MESSAGE_CAUSE_TYPES;
        } else {
            initargs = new Object[]{t};
            initargsType = CAUSE_TYPES;
        }

        Constructor<? extends Throwable> exceptionConstructor = getMatchingConstructor(transactional.rethrowExceptionsAs(), initargsType);
        Throwable rethrowEx = null;
        if (exceptionConstructor != null) {
            try {
                rethrowEx = exceptionConstructor.newInstance(initargs);
            } catch (Exception e) {
                errorMessage = format("Impossible to re-throw '%s', it needs the constructor with %s argument(s).",
                                      transactional.rethrowExceptionsAs().getName(),
                                      Arrays.toString(initargsType));
                log.error(errorMessage, e);
                rethrowEx = new RuntimeException(errorMessage, e);
            }
        } else {
            errorMessage = format("Impossible to re-throw '%s', it needs the constructor with %s or %s argument(s).",
                                  transactional.rethrowExceptionsAs().getName(),
                                  Arrays.toString(CAUSE_TYPES),
                                  Arrays.toString(MESSAGE_CAUSE_TYPES));
            log.error(errorMessage);
            rethrowEx = new RuntimeException(errorMessage);
        }

        return rethrowEx;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> Constructor<E> getMatchingConstructor(Class<E> type,
                                                                               Class<?>[] argumentsType) {
        Class<? super E> currentType = type;
        while (Object.class != currentType) {
            for (Constructor<?> constructor : currentType.getConstructors()) {
                if (Arrays.equals(argumentsType, constructor.getParameterTypes())) {
                    return (Constructor<E>) constructor;
                }
            }
            currentType = currentType.getSuperclass();
        }
        return null;
    }

}
