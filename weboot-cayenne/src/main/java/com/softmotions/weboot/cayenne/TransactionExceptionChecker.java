package com.softmotions.weboot.cayenne;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface TransactionExceptionChecker {

    /**
     * Return true if transaction need to rollback on exception
     * specified by tr.
     */
    boolean needRollback(Throwable tr);
}
