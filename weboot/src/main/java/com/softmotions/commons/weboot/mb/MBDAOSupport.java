package com.softmotions.commons.weboot.mb;

import org.apache.ibatis.session.SqlSession;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

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

    public <T> T withinTransaction(MBAction<T> action) throws SQLException {
        return action.exec(sess, sess.getConnection());
    }

}
