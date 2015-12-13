package com.softmotions.weboot.cayenne;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Any method marked with this annotation will be considered for
 * transactionality.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Transactional {

    /**
     * Use current thread ObjectContext for commit or rollback operations.
     *
     * @see ObjectContextThreadHolder
     */
    boolean useCurrentObjectContext() default true;

    Class<? extends TransactionExceptionChecker> exceptionChecker() default DefaultTransactionExceptionChecker.class;
}
