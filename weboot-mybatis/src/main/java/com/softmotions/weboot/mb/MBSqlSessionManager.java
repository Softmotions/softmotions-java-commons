package com.softmotions.weboot.mb;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionException;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.softmotions.commons.ThreadUtils;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class MBSqlSessionManager implements SqlSessionFactory, SqlSession {

    private static final Logger log = LoggerFactory.getLogger(MBSqlSessionManager.class);
    private final SqlSessionFactory sqlSessionFactory;
    private final SqlSession sqlSessionProxy;
    private final ThreadLocal<SqlSession> localSqlSession = ThreadUtils.createThreadLocal();
    private final ThreadLocal<ArrayList<MBSqlSessionListener>> localSqlSessionListeners = ThreadUtils.createThreadLocal();


    public static MBSqlSessionManager newInstance(SqlSessionFactory sqlSessionFactory) {
        return new MBSqlSessionManager(sqlSessionFactory);
    }

    private MBSqlSessionManager(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.sqlSessionProxy = (SqlSession) Proxy.newProxyInstance(
                SqlSessionFactory.class.getClassLoader(),
                new Class[]{SqlSession.class},
                new SqlSessionInterceptor());
    }

    public void startManagedSession() {
        this.localSqlSession.set(openSession());
    }

    public void startManagedSession(boolean autoCommit) {
        this.localSqlSession.set(openSession(autoCommit));
    }

    public void startManagedSession(Connection connection) {
        this.localSqlSession.set(openSession(connection));
    }

    public void startManagedSession(TransactionIsolationLevel level) {
        this.localSqlSession.set(openSession(level));
    }

    public void startManagedSession(ExecutorType execType) {
        this.localSqlSession.set(openSession(execType));
    }

    public void startManagedSession(ExecutorType execType, boolean autoCommit) {
        this.localSqlSession.set(openSession(execType, autoCommit));
    }

    public void startManagedSession(ExecutorType execType, TransactionIsolationLevel level) {
        this.localSqlSession.set(openSession(execType, level));
    }

    public void startManagedSession(ExecutorType execType, Connection connection) {
        this.localSqlSession.set(openSession(execType, connection));
    }

    public boolean isManagedSessionStarted() {
        return this.localSqlSession.get() != null;
    }

    @Override
    public SqlSession openSession() {
        return sqlSessionFactory.openSession();
    }

    @Override
    public SqlSession openSession(boolean autoCommit) {
        return sqlSessionFactory.openSession(autoCommit);
    }

    @Override
    public SqlSession openSession(Connection connection) {
        return sqlSessionFactory.openSession(connection);
    }

    @Override
    public SqlSession openSession(TransactionIsolationLevel level) {
        return sqlSessionFactory.openSession(level);
    }

    @Override
    public SqlSession openSession(ExecutorType execType) {
        return sqlSessionFactory.openSession(execType);
    }

    @Override
    public SqlSession openSession(ExecutorType execType, boolean autoCommit) {
        return sqlSessionFactory.openSession(execType, autoCommit);
    }

    @Override
    public SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level) {
        return sqlSessionFactory.openSession(execType, level);
    }

    @Override
    public SqlSession openSession(ExecutorType execType, Connection connection) {
        return sqlSessionFactory.openSession(execType, connection);
    }

    @Override
    public Configuration getConfiguration() {
        return sqlSessionFactory.getConfiguration();
    }

    @Override
    public <T> T selectOne(String statement) {
        return sqlSessionProxy.<T>selectOne(statement);
    }

    @Override
    public <T> T selectOne(String statement, Object parameter) {
        return sqlSessionProxy.<T>selectOne(statement, parameter);
    }

    @Override
    public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
        return sqlSessionProxy.<K, V>selectMap(statement, mapKey);
    }

    @Override
    public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
        return sqlSessionProxy.<K, V>selectMap(statement, parameter, mapKey);
    }

    @Override
    public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
        return sqlSessionProxy.<K, V>selectMap(statement, parameter, mapKey, rowBounds);
    }

    @Override
    public <E> List<E> selectList(String statement) {
        return sqlSessionProxy.<E>selectList(statement);
    }

    @Override
    public <E> List<E> selectList(String statement, Object parameter) {
        return sqlSessionProxy.<E>selectList(statement, parameter);
    }

    @Override
    public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
        return sqlSessionProxy.<E>selectList(statement, parameter, rowBounds);
    }

    @Override
    public void select(String statement, ResultHandler handler) {
        sqlSessionProxy.select(statement, handler);
    }

    @Override
    public void select(String statement, Object parameter, ResultHandler handler) {
        sqlSessionProxy.select(statement, parameter, handler);
    }

    @Override
    public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
        sqlSessionProxy.select(statement, parameter, rowBounds, handler);
    }

    @Override
    public int insert(String statement) {
        return sqlSessionProxy.insert(statement);
    }

    @Override
    public int insert(String statement, Object parameter) {
        return sqlSessionProxy.insert(statement, parameter);
    }

    @Override
    public int update(String statement) {
        return sqlSessionProxy.update(statement);
    }

    @Override
    public int update(String statement, Object parameter) {
        return sqlSessionProxy.update(statement, parameter);
    }

    @Override
    public int delete(String statement) {
        return sqlSessionProxy.delete(statement);
    }

    @Override
    public int delete(String statement, Object parameter) {
        return sqlSessionProxy.delete(statement, parameter);
    }

    @Override
    public <T> T getMapper(Class<T> type) {
        return getConfiguration().getMapper(type, this);
    }

    @Override
    public <T> Cursor<T> selectCursor(String s) {
        return sqlSessionProxy.selectCursor(s);
    }

    @Override
    public <T> Cursor<T> selectCursor(String s, Object o) {
        return sqlSessionProxy.selectCursor(s, o);
    }

    @Override
    public <T> Cursor<T> selectCursor(String s, Object o, RowBounds rowBounds) {
        return sqlSessionProxy.selectCursor(s, o, rowBounds);
    }

    @Override
    public Connection getConnection() {
        final SqlSession sqlSession = localSqlSession.get();
        if (sqlSession == null)
            throw new SqlSessionException("Error:  Cannot get connection.  No managed session is started.");
        return sqlSession.getConnection();
    }

    @Override
    public void clearCache() {
        final SqlSession sqlSession = localSqlSession.get();
        if (sqlSession == null)
            throw new SqlSessionException("Error:  Cannot clear the cache.  No managed session is started.");
        sqlSession.clearCache();
    }

    @Override
    public void commit() {
        final SqlSession sqlSession = localSqlSession.get();
        if (sqlSession == null) throw new SqlSessionException("Error:  Cannot commit.  No managed session is started.");
        boolean succes = false;
        try {
            sqlSession.commit();
            succes = true;
        } finally {
            fireCommit(succes, true);
        }
    }

    @Override
    public void commit(boolean force) {
        final SqlSession sqlSession = localSqlSession.get();
        if (sqlSession == null) throw new SqlSessionException("Error:  Cannot commit.  No managed session is started.");
        boolean succes = false;
        try {
            sqlSession.commit(force);
            succes = true;
        } finally {
            fireCommit(succes, true);
        }
    }

    @Override
    public void rollback() {
        final SqlSession sqlSession = localSqlSession.get();
        if (sqlSession == null)
            throw new SqlSessionException("Error:  Cannot rollback.  No managed session is started.");
        try {
            sqlSession.rollback();
        } finally {
            fireRollback(true);
        }
    }

    @Override
    public void rollback(boolean force) {
        final SqlSession sqlSession = localSqlSession.get();
        if (sqlSession == null)
            throw new SqlSessionException("Error:  Cannot rollback.  No managed session is started.");
        try {
            sqlSession.rollback(force);
        } finally {
            fireRollback(true);
        }
    }

    @Override
    public List<BatchResult> flushStatements() {
        final SqlSession sqlSession = localSqlSession.get();
        if (sqlSession == null)
            throw new SqlSessionException("Error:  Cannot rollback.  No managed session is started.");
        return sqlSession.flushStatements();
    }

    @Override
    public void close() {
        final SqlSession sqlSession = localSqlSession.get();
        if (sqlSession == null) throw new SqlSessionException("Error:  Cannot close.  No managed session is started.");
        boolean success = false;
        try {
            sqlSession.close();
            success = true;
        } finally {
            try {
                localSqlSession.remove();
            } finally {
                fireClose(success, true);
            }
        }
    }

    public void registerNextEventSessionListener(MBSqlSessionListener lsnr) {
        if (lsnr == null) throw new NullPointerException();
        if (!isManagedSessionStarted()) {
            throw new SqlSessionException("No managed session started, uanable to register next-event listener");
        }
        ArrayList<MBSqlSessionListener> lsnrs = localSqlSessionListeners.get();
        if (lsnrs == null) {
            lsnrs = new ArrayList<>();
            localSqlSessionListeners.set(lsnrs);
        }
        lsnrs.add(lsnr);
    }


    private void fireCommit(boolean success, boolean clear) {
        ArrayList<MBSqlSessionListener> lsnrs = localSqlSessionListeners.get();
        if (lsnrs == null || lsnrs.isEmpty()) {
            return;
        }
        try {
            for (MBSqlSessionListener l : lsnrs) {
                try {
                    l.commit(success);
                } catch (Exception e) {
                    log.error("", e);
                }
            }
        } finally {
            if (clear) {
                localSqlSessionListeners.remove();
            }
        }
    }


    private void fireRollback(boolean clear) {
        ArrayList<MBSqlSessionListener> lsnrs = localSqlSessionListeners.get();
        if (lsnrs == null || lsnrs.isEmpty()) {
            return;
        }
        try {
            for (MBSqlSessionListener l : lsnrs) {
                try {
                    l.rollback();
                } catch (Exception e) {
                    log.error("", e);
                }
            }
        } finally {
            if (clear) {
                lsnrs.clear();
            }
        }
    }

    private void fireClose(boolean succes, boolean clear) {
        ArrayList<MBSqlSessionListener> lsnrs = localSqlSessionListeners.get();
        if (lsnrs == null || lsnrs.isEmpty()) {
            return;
        }
        try {
            for (MBSqlSessionListener l : lsnrs) {
                try {
                    l.close(succes);
                } catch (Exception e) {
                    log.error("", e);
                }
            }
        } finally {
            if (clear) {
                lsnrs.clear();
            }
        }
    }

    private class SqlSessionInterceptor implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            final SqlSession sqlSession = MBSqlSessionManager.this.localSqlSession.get();
            if (sqlSession != null) {
                try {
                    return method.invoke(sqlSession, args);
                } catch (Throwable t) {
                    throw ExceptionUtil.unwrapThrowable(t);
                }
            } else {
                final SqlSession autoSqlSession = openSession();
                try {
                    final Object result = method.invoke(autoSqlSession, args);
                    autoSqlSession.commit();
                    return result;
                } catch (Throwable t) {
                    autoSqlSession.rollback();
                    throw ExceptionUtil.unwrapThrowable(t);
                } finally {
                    autoSqlSession.close();
                }
            }
        }
    }
}
