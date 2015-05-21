package com.softmotions.web;

import org.apache.commons.codec.net.BCodec;

/**
 * Кодирует имя файла для HTTP хедера: Content-Disposition в правильной кодировке.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class ResponseUtils {

    private ResponseUtils() {
    }

    public static String encodeContentDisposition(String fileName, boolean isInline)
            throws Exception {
        if (fileName == null) throw new IllegalArgumentException("Value of the \"filename\" parameter cannot be null!");
        String contentDisposition = isInline ? "inline; " : "attachment; ";
        contentDisposition += "filename=\"" + new BCodec("UTF-8").encode(fileName) + "\"";
        return contentDisposition;
    }
}
