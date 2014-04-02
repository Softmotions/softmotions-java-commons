package com.softmotions.commons.weboot.mb;

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

    public int insert(String stmtId, Object... params) {
        return sess.insert(stmtId, toParametersMap(params));
    }

    public int update(String stmtId, Object... params) {
        return sess.update(stmtId, toParametersMap(params));
    }

    public void select(String stmtId, ResultHandler rh, Object... params) {
        sess.select(stmtId, toParametersMap(params), rh);
    }

    public <E> List<E> select(String stmtId, Object... params) {
        return sess.selectList(stmtId, toParametersMap(params));
    }

    public <E> List<E> select(String stmtId, RowBounds rb, Object... params) {
        return sess.selectList(stmtId, toParametersMap(params), rb);
    }

    public <E> List<E> selectByCriteria(MBCriteriaQuery crit, String defstmtId) {
        crit.finish();
        return sess.selectList(crit.getStatement() != null ? crit.getStatement() : defstmtId,
                               crit,
                               crit.getRowBounds());
    }

    public <E> E selectOneByCriteria(MBCriteriaQuery crit, String defstmtId) {
        crit.finish();
        return sess.selectOne(crit.getStatement() != null ? crit.getStatement() : defstmtId, crit);
    }

    public int updateByCriteria(MBCriteriaQuery crit, String defstmtId) {
        crit.finish();
        return sess.update(crit.getStatement() != null ? crit.getStatement() : defstmtId, crit);
    }

    public int insertByCriteria(MBCriteriaQuery crit, String defstmtId) {
        crit.finish();
        return sess.insert(crit.getStatement() != null ? crit.getStatement() : defstmtId, crit);
    }

    public <E> E selectOne(String stmtId, Object... params) {
        return sess.selectOne(stmtId, toParametersMap(params));
    }

    public <T> T withinTransaction(MBAction<T> action) throws SQLException {
        return action.exec(sess, sess.getConnection());
    }

    protected static Map<String, Object> toParametersMap(Object[] params) {
        if (params == null || params.length == 0) {
            return null;
        }
        Map<String, Object> pmap = new HashMap<>(params.length / 2);
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
}
