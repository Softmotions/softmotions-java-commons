package com.softmotions.weboot.jaxrs;

import javax.annotation.Nullable;
import javax.ws.rs.core.MultivaluedMap;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class FileUploadUtils {

    private FileUploadUtils() {
    }

    @Nullable
    public static String getFileName(MultivaluedMap<String, String> header) {
        String[] contentDisposition = header.getFirst("Content-Disposition").split(";");
        for (String filename : contentDisposition) {
            if ((filename.trim().startsWith("filename"))) {
                String[] name = filename.split("=");
                return name[1].trim().replaceAll("\"", "");
            }
        }
        return null;
    }

}

