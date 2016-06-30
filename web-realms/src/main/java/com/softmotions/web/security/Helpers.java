package com.softmotions.web.security;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class Helpers {

    private static final Logger log = LoggerFactory.getLogger(Helpers.class);

    private Helpers() {
    }

    public static boolean equalsIgnoreCase(final CharSequence str1, final CharSequence str2) {
        if (str1 == null || str2 == null) {
            return str1 == str2;
        } else if (str1 == str2) {
            return true;
        } else if (str1.length() != str2.length()) {
            return false;
        } else {
            return regionMatches(str1, true, 0, str2, 0, str1.length());
        }
    }

    static boolean regionMatches(final CharSequence cs, final boolean ignoreCase, final int thisStart,
                                 final CharSequence substring, final int start, final int length) {
        if (cs instanceof String && substring instanceof String) {
            return ((String) cs).regionMatches(ignoreCase, thisStart, (String) substring, start, length);
        }
        int index1 = thisStart;
        int index2 = start;
        int tmpLen = length;

        while (tmpLen-- > 0) {
            final char c1 = cs.charAt(index1++);
            final char c2 = substring.charAt(index2++);

            if (c1 == c2) {
                continue;
            }

            if (!ignoreCase) {
                return false;
            }

            // The same check as in String.regionMatches():
            if (Character.toUpperCase(c1) != Character.toUpperCase(c2)
                && Character.toLowerCase(c1) != Character.toLowerCase(c2)) {
                return false;
            }
        }

        return true;
    }


    public static boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (Character.isWhitespace(cs.charAt(i)) == false) {
                return false;
            }
        }
        return true;
    }

    public static <T extends Comparable<? super T>> int compare(final T c1, final T c2) {
        if (c1 == c2) {
            return 0;
        } else if (c1 == null) {
            return -1;
        } else if (c2 == null) {
            return 1;
        }
        return c1.compareTo(c2);
    }


    public static <T> int indexOf(T[] array, T el) {
        for (int i = 0, l = array.length; i < l; ++i) {
            if (array[i] == el || array[i].equals(el)) {
                return i;
            }
        }
        return -1;
    }

    public static URL getResourceAsUrl(String location, Class owner) {
        if (location == null) {
            return null;
        }
        URL url = null;
        if (owner != null) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = owner.getClassLoader();
            }
            url = cl.getResource(location);
        }
        if (url == null) {
            InputStream is = null;
            try {
                url = new URL(location);
                is = url.openStream();
            } catch (IOException e) {
                url = null;
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
            }
            File cfgFile = new File(location);
            if (cfgFile.exists()) {
                try {
                    url = cfgFile.toURI().toURL();
                } catch (MalformedURLException e) {
                }
            }
        }
        return url;
    }
}
