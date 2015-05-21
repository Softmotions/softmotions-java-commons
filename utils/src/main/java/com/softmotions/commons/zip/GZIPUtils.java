package com.softmotions.commons.zip;


import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * GZIP Utils
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 * @version $Id: GZIPUtils.java 17713 2011-09-26 10:06:20Z adam $
 */
public class GZIPUtils {

    private static final int GZIP_MAGIC_NUMBER_BYTE_1 = 31;
    private static final int GZIP_MAGIC_NUMBER_BYTE_2 = -117;
    private static final int EMPTY_GZIPPED_CONTENT_SIZE = 20;

    private GZIPUtils() {
    }

    public static boolean isGzipped(byte[] candidate) {
        return !(candidate == null || candidate.length < 2) &&
               (candidate[0] == GZIP_MAGIC_NUMBER_BYTE_1 && candidate[1] == GZIP_MAGIC_NUMBER_BYTE_2);
    }


    public static void gzip(InputStream is, OutputStream os) throws IOException {
        GZIPOutputStream gzos = new GZIPOutputStream(os);
        IOUtils.copy(is, gzos);
        gzos.flush();
        gzos.finish();
    }

    public static void gunzip(InputStream is, OutputStream os) throws IOException {
        GZIPInputStream gzin = new GZIPInputStream(is);
        IOUtils.copy(gzin, os);
        gzin.close();
    }

    public static byte[] gzip(byte[] indata) throws IOException {
        if (isGzipped(indata)) {
            throw new IllegalArgumentException("Byte data already gzipped");
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream(indata.length / 2);
        gzip(new ByteArrayInputStream(indata), bos);
        return bos.toByteArray();
    }

    public static byte[] gunzip(byte[] indata) throws IOException {
        if (!isGzipped(indata)) {
            throw new IllegalArgumentException("Byte data not gzipped");
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream(indata.length << 1);
        gunzip(new ByteArrayInputStream(indata), bos);
        return bos.toByteArray();
    }


    /**
     * Возвращает true если gzip данные являются пустыми
     */
    public static boolean isEmptyGZIP(byte[] data) {
        return isGzipped(data) && (data.length == EMPTY_GZIPPED_CONTENT_SIZE);
    }
}
