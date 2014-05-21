package com.softmotions.commons.ctype;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class CTypeUtils {

    @SuppressWarnings("StaticCollection")
    static final Set<String> TXT_MTYPES;

    static {
        TXT_MTYPES = new HashSet<>();
        TXT_MTYPES.add("application/atom+xml");
        TXT_MTYPES.add("application/rdf+xml");
        TXT_MTYPES.add("application/rss+xml");
        TXT_MTYPES.add("application/soap+xml");
        TXT_MTYPES.add("application/xop+xml");
        TXT_MTYPES.add("application/xhtml+xml");
        TXT_MTYPES.add("application/json");
        TXT_MTYPES.add("application/javascript");
        TXT_MTYPES.add("application/xml");
        TXT_MTYPES.add("application/xml-dtd");
        TXT_MTYPES.add("application/x-tex");
        TXT_MTYPES.add("application/x-latex");
        TXT_MTYPES.add("application/x-javascript");
        TXT_MTYPES.add("application/ecmascript");
    }


    private CTypeUtils() {
    }


    public static boolean isTextualContentType(String ctype) {
        return ctype != null && (ctype.startsWith("text/") || TXT_MTYPES.contains(ctype));
    }
}
