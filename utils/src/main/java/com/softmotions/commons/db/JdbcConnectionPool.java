package com.softmotions.commons.db;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Connection pool datasource wrapper.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class JdbcConnectionPool implements DataSource, ConnectionEventListener, Closeable {

    public static final int DEFAULT_LOGIN_TIMEOUT_SEC = 10;

    public static final int DEFAULT_MAX_CONNECTIONS = 20;

    private final ArrayList<PooledConnection> recycledConnections = new ArrayList<>();

    private final ConnectionPoolDataSource dataSource;

    private final Set<PooledConnection> invalidConnections = new HashSet<>();

    private final Object lock = new Object();

    private int loginTimeout = DEFAULT_LOGIN_TIMEOUT_SEC;

    private int maxConnections = DEFAULT_MAX_CONNECTIONS;

    private int activeConnections;

    private boolean closed;

    public JdbcConnectionPool(ConnectionPoolDataSource dataSource) throws SQLException {
        this.dataSource = dataSource;
    }

    public void setMaxConnections(int val) {
        if (val < 1) {
            val = DEFAULT_MAX_CONNECTIONS;
        }
        synchronized (lock) {
            this.maxConnections = val;
            lock.notifyAll();
        }
    }

    public int getMaxConnection() {
        synchronized (lock) {
            return maxConnections;
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            invalidConnections.clear();
            for (PooledConnection pc : recycledConnections) {
                closeConnection(pc);
            }
        }
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return dataSource.getLogWriter();
    }

    private PrintWriter getLogWriterSilent() {
        PrintWriter lw = null;
        try {
            lw = getLogWriter();
        } catch (SQLException e) {
        }
        return lw;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        dataSource.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        synchronized (lock) {
            if (seconds < 1) {
                seconds = DEFAULT_LOGIN_TIMEOUT_SEC;
            }
            this.loginTimeout = seconds;
        }
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        synchronized (lock) {
            return loginTimeout;
        }
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return dataSource.getParentLogger();
    }

    @Override
    public Connection getConnection() throws SQLException {
        long max = System.currentTimeMillis() + loginTimeout * 1000;
        do {
            synchronized (this) {
                if (closed) {
                    throw new IllegalStateException("Connection pool has been disposed");
                }
                if (activeConnections < maxConnections) {
                    Connection conn;
                    PooledConnection pc;
                    if (!recycledConnections.isEmpty()) {
                        pc = recycledConnections.remove(recycledConnections.size() - 1);
                    } else {
                        pc = dataSource.getPooledConnection();
                    }
                    conn = pc.getConnection();
                    activeConnections++;
                    pc.addConnectionEventListener(this);
                    return conn;
                }
                try {
                    wait(100);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        } while (System.currentTimeMillis() <= max);
        throw new SQLException("Login timeout");
    }

    @Override
    public void connectionClosed(ConnectionEvent event) {
        PooledConnection pc = (PooledConnection) event.getSource();
        pc.removeConnectionEventListener(this);
        recycleConnection(pc);
    }

    @Override
    public void connectionErrorOccurred(ConnectionEvent event) {
        PooledConnection pc = (PooledConnection) event.getSource();
        if (pc != null && !invalidConnections.contains(pc)) {
            invalidConnections.add(pc);
        }
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        throw new UnsupportedOperationException("getConnection(username, password)");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException("unwrap");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    ///////////////////////////////////////////////////////////////////////////
    //                        Private staff                                  //
    ///////////////////////////////////////////////////////////////////////////


    private void closeConnection(PooledConnection pc) {
        try {
            pc.close();
        } catch (SQLException e) {
            PrintWriter lw = getLogWriterSilent();
            if (lw != null) {
                e.printStackTrace(lw);
            }
        }
    }

    void recycleConnection(PooledConnection pc) {
        synchronized (lock) {
            if (activeConnections <= 0) {
                throw new AssertionError();
            }
            activeConnections--;
            if (invalidConnections.remove(pc)) {
                closeConnection(pc);
            } else if (!closed && activeConnections < maxConnections) {
                recycledConnections.add(pc);
            } else {
                closeConnection(pc);
            }
            if (activeConnections >= maxConnections - 1) {
                notifyAll();
            }
        }
    }
}
