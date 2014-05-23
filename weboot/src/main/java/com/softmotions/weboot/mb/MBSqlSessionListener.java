package com.softmotions.weboot.mb;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface MBSqlSessionListener {

    void commit(boolean success);

    void rollback();

    void close(boolean success);
}
