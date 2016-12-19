package com.softmotions.weboot.mb;

/**
 * MyBatis extra mappers supplier, contained
 * in Guice multibinding Set.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface WBMyBatisExtraConfigSupplier {

    /**
     * Extra XML mappers as set
     * of classpath resources.
     */
    String[] extraMappersXML();
}
