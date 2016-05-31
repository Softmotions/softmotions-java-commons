package com.softmotions.weboot.jaxrs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.codec.net.BCodec;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class FileUploadUtils {

    private static final Pattern B64_TEST =
            Pattern.compile("^=\\?([a-zA-Z\\-0-9]+)\\?B\\?([a-zA-Z0-9\\+/=]+)\\?=$");

    private FileUploadUtils() {
    }

    @Nullable
    public static String getFileName(MultivaluedMap<String, String> header) {
        String[] contentDisposition = header.getFirst("Content-Disposition").split(";");
        for (String filename : contentDisposition) {
            filename = filename.trim();
            String fname = null;
            if ((filename.startsWith("filename="))) {
                fname = filename.substring("filename=".length()).replace("\"", "");
            } else if ((filename.startsWith("filename*="))) {
                fname = filename.substring("filename*=".length()).replace("\"", "");
            }
            if (fname != null) {
                Matcher m = B64_TEST.matcher(fname);
                if (m.matches()) {
                    try {
                        return new BCodec().decode(fname);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    return fname;
                }
            }
        }
        return null;
    }

}

