package com.softmotions.weboot.cayenne;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class DefaultTransactionExceptionChecker implements TransactionExceptionChecker {

    @Override
    public boolean needRollback(Throwable tr) {
        return true;
    }
}
