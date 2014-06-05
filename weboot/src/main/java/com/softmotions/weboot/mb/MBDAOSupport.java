package com.softmotions.weboot.mb;

import org.apache.commons.collections.map.Flat3Map;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class MBDAOSupport {

    protected String namespace;

    protected final SqlSession sess;

    public SqlSession getSession() {
        return sess;
    }

    public Connection getConnection() {
        return sess.getConnection();
    }

    public DataSource getDataSource() {
        return sess.getConfiguration().getEnvironment().getDataSource();
    }

    public MBDAOSupport(SqlSession sess) {
        this.sess = sess;
    }

    public MBDAOSupport(String namespace, SqlSession sess) {
        this.namespace = namespace;
        this.sess = sess;
    }

    public int insert(String stmtId, Object... params) {
        return sess.insert(toStatementId(stmtId), toParametersObj(params));
    }

    public int delete(String stmtId, Object... params) {
        return sess.delete(toStatementId(stmtId), toParametersObj(params));
    }

    public int update(String stmtId, Object... params) {
        return sess.update(toStatementId(stmtId), toParametersObj(params));
    }

    public void select(String stmtId, ResultHandler rh, Object... params) {
        sess.select(toStatementId(stmtId), toParametersObj(params), rh);
    }

    public void selectByCriteria(MBCriteriaQuery crit, ResultHandler rh, String defstmtId) {
        crit.finish();
        sess.select(crit.getStatement() != null ? crit.getStatement() : toStatementId(defstmtId),
                    crit,
                    rh);
    }

    public <E> List<E> select(String stmtId, Object... params) {
        return sess.selectList(toStatementId(stmtId), toParametersObj(params));
    }

    public <E> List<E> select(String stmtId, RowBounds rb, Object... params) {
        return sess.selectList(toStatementId(stmtId), toParametersObj(params), rb);
    }

    public <E> List<E> selectByCriteria(MBCriteriaQuery crit) {
        return selectByCriteria(crit, null);
    }

    public <E> List<E> selectByCriteria(MBCriteriaQuery crit, String defstmtId) {
        crit.finish();
        return sess.selectList(crit.getStatement() != null ? crit.getStatement() : toStatementId(defstmtId),
                               crit);
    }

    public <E> E selectOneByCriteria(MBCriteriaQuery crit) {
        return selectOneByCriteria(crit, null);
    }

    public <E> E selectOneByCriteria(MBCriteriaQuery crit, String defstmtId) {
        crit.finish();
        return sess.selectOne(crit.getStatement() != null ? crit.getStatement() : toStatementId(defstmtId), crit);
    }

    public int updateByCriteria(MBCriteriaQuery crit) {
        return updateByCriteria(crit, null);
    }

    public int updateByCriteria(MBCriteriaQuery crit, String defstmtId) {
        crit.finish();
        return sess.update(crit.getStatement() != null ? crit.getStatement() : toStatementId(defstmtId), crit);
    }

    public int insertByCriteria(MBCriteriaQuery crit) {
        return insertByCriteria(crit, null);
    }

    public int insertByCriteria(MBCriteriaQuery crit, String defstmtId) {
        crit.finish();
        return sess.insert(crit.getStatement() != null ? crit.getStatement() : toStatementId(defstmtId), crit);
    }

    public int deleteByCriteria(MBCriteriaQuery crit) {
        return deleteByCriteria(crit, null);
    }

    public int deleteByCriteria(MBCriteriaQuery crit, String defstmtId) {
        crit.finish();
        return sess.delete(crit.getStatement() != null ? crit.getStatement() : toStatementId(defstmtId), crit);
    }

    public <E> E selectOne(String stmtId, Object... params) {
        return sess.selectOne(toStatementId(stmtId), toParametersObj(params));
    }

    public <T> T withinTransaction(MBAction<T> action) throws SQLException {
        return action.exec(sess, sess.getConnection());
    }

    public MBCriteriaQuery createCriteria() {
        return new MBCriteriaQuery(this);
    }

    protected static Object toParametersObj(Object[] params) {
        if (params == null || params.length == 0) {
            return null;
        }
        if (params.length == 1) {
            return params[0];
        }
        Map<String, Object> pmap = (params.length / 2 > 3) ? new HashMap<>(params.length / 2) : new Flat3Map();
        String key = null;
        for (int i = 0; i < params.length; ++i) {
            if (i % 2 == 0) {
                key = String.valueOf(params[i]);
            } else if (key != null) {
                pmap.put(key, params[i]);
                key = null;
            }
        }
        return pmap;
    }

    protected String toStatementId(String stmtId) {
        if (stmtId == null) {
            throw new RuntimeException("MyBatis statement id cannot be null");
        }
        return (namespace == null ? stmtId :
                (stmtId.startsWith(namespace) ? stmtId : (namespace + "." + stmtId)));
    }
}