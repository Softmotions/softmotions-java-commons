package com.softmotions.commons.string;

import org.apache.commons.lang3.StringUtils;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * String escape helper.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 * @version $Id$
 */
public final class EscapeHelper {

    public static String toUnicodeEscape(Object val) {
        String src = String.valueOf(val);
        int length = src.length();
        if (length == 0) return src;
        StringBuilder sb = new StringBuilder(6 * length);
        for (int i = 0; i < length; ++i) {
            sb.append("\\u");
            toUnsignedString((int) src.charAt(i), 4, sb);
        }
        return sb.toString();
    }

    private static void toUnsignedString(int i, int shift, StringBuilder sb) {

        char[] buf = new char[32];
        int charPos = 32;
        int radix = 1 << shift;
        int mask = radix - 1;
        do {
            buf[--charPos] = digits[i & mask];
            i >>>= shift;
        } while (i != 0);

        int len = (32 - charPos);
        if (len < 4) {
            for (int j = 0; j < 4 - len; ++j) {
                sb.append(0);
            }
        }
        sb.append(buf, charPos, len);
    }

    private static final char[] digits = {

            '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b',
            'c', 'd', 'e', 'f', 'g', 'h',
            'i', 'j', 'k', 'l', 'm', 'n',
            'o', 'p', 'q', 'r', 's', 't',
            'u', 'v', 'w', 'x', 'y', 'z'
    };

    public static String escapeVelocity(Object val) {
        String src = String.valueOf(val);
        if (src.contains(".")) return StringUtils.replace(src, ".", "_");
        return src;
    }

    private static final Map<Integer, String> XML_ENTRY_MAP;

    static {
        XML_ENTRY_MAP = new HashMap<Integer, String>(5);
        XML_ENTRY_MAP.put(34, "quot");
        XML_ENTRY_MAP.put(38, "amp");
        XML_ENTRY_MAP.put(60, "lt");
        XML_ENTRY_MAP.put(62, "gt");
        XML_ENTRY_MAP.put(39, "apos");
    }

    public static String escapeXML(String str) {
        if (str == null) {
            return null;
        }
        final StringBuilder buf = new StringBuilder((int) (str.length() * 1.2));
        final int length = str.length();
        for (int i = 0; i < length; ++i) {
            final int ch = str.charAt(i);
            final String entityName = XML_ENTRY_MAP.get(ch);
            if (entityName == null) {
                buf.append((char) ch);
            } else {
                buf.append('&');
                buf.append(entityName);
                buf.append(';');
            }
        }
        return buf.toString();
    }


    /**
     * Translates the given String into ASCII code.
     *
     * @param input the input which contains native characters like umlauts etc
     * @return the input in which native characters are replaced through ASCII code
     */
    public static String nativeToAscii(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder buffer = new StringBuilder(input.length() + 60);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c <= 0x7E) {
                buffer.append(c);
            } else {
                buffer.append("\\u");
                String hex = Integer.toHexString(c);
                for (int j = hex.length(); j < 4; j++) {
                    buffer.append('0');
                }
                buffer.append(hex);
            }
        }
        return buffer.toString();
    }


    /**
     * Translates the given String into ASCII code.
     *
     * @param input the input which contains native characters like umlauts etc
     * @return the input in which native characters are replaced through ASCII code
     */
    public static String asciiToNative(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder buffer = new StringBuilder(input.length());
        boolean precedingBackslash = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (precedingBackslash) {
                switch (c) {
                    case 'f':
                        c = '\f';
                        break;
                    case 'n':
                        c = '\n';
                        break;
                    case 'r':
                        c = '\r';
                        break;
                    case 't':
                        c = '\t';
                        break;
                    case 'u':
                        String hex = input.substring(i + 1, i + 5);
                        c = (char) Integer.parseInt(hex, 16);
                        i += 4;
                }
                precedingBackslash = false;
            } else {
                precedingBackslash = (c == '\\');
            }
            if (!precedingBackslash) {
                buffer.append(c);
            }
        }
        return buffer.toString();
    }


    public static String escapeJSON(String input) {

        if (input == null) {
            return "null";
        }

        StringWriter sw = (input.length() > 0) ? new StringWriter(input.length()) : new StringWriter();

        int length = input.length();
        for (int i = 0; i < length; i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"':
                    sw.write("\\\"");
                    break;
                case '\\':
                    sw.write("\\\\");
                    break;
                case '\b':
                    sw.write("\\b");
                    break;
                case '\f':
                    sw.write("\\f");
                    break;
                case '\n':
                    sw.write("\\n");
                    break;
                case '\r':
                    sw.write("\\r");
                    break;
                case '\t':
                    sw.write("\\t");
                    break;
                case '/':
                    sw.write("\\/");
                    break;
                default:
                    //Reference: http://www.unicode.org/versions/Unicode5.1.0/
                    if ((c >= '\u0000' && c <= '\u001F') || (c >= '\u007F' && c <= '\u009F') || (c >= '\u2000' && c <= '\u20FF')) {
                        String ss = Integer.toHexString(c);
                        sw.write("\\u");
                        for (int k = 0; k < 4 - ss.length(); k++) {
                            sw.write('0');
                        }
                        sw.write(ss.toUpperCase());
                    } else {
                        sw.write(c);
                    }
            }
        }

        return sw.toString();
    }


    private EscapeHelper() {
    }
}
