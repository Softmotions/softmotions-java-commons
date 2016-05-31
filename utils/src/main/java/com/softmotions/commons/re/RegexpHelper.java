package com.softmotions.commons.re;

import javax.annotation.Nonnull;

public class RegexpHelper {

    private RegexpHelper() {
    }

    /**
     * Convert a `glob` http://en.wikipedia.org/wiki/Glob_(programming)
     * to the regular expression counterpart
     *
     * @param glob The glob to be converted
     */
    @Nonnull
    public static String convertGlobToRegEx(String glob) {
        glob = glob.trim();
        StringBuilder sb = new StringBuilder(glob.length());

        boolean escaping = false;
        int incurlies = 0;
        char pc = 0;

        for (int i = 0, l = glob.length(); i < l; ++i) {
            char cc = glob.charAt(i);
            switch (cc) {
                case '*':
                    if (escaping) {
                        sb.append("\\*");
                    } else {
                        sb.append(".*");
                    }
                    escaping = false;
                    break;
                case '?':
                    if (escaping) {
                        sb.append("\\?");
                    } else {
                        sb.append('.');
                    }
                    escaping = false;
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                    sb.append('\\');
                    sb.append(cc);
                    escaping = false;
                    break;
                case '\\':
                    if (escaping) {
                        sb.append("\\\\");
                        escaping = false;
                    } else {
                        escaping = true;
                    }
                    break;
                case '{':
                    if (escaping) {
                        sb.append("\\{");
                    } else {
                        sb.append('(');
                        incurlies++;
                    }
                    escaping = false;
                    break;
                case '}':
                    if (incurlies > 0 && !escaping) {
                        sb.append(')');
                        incurlies--;
                    } else if (escaping) {
                        sb.append("\\}");
                    } else {
                        sb.append('}');
                    }
                    escaping = false;
                    break;
                case ',':
                    if (incurlies > 0 && !escaping) {
                        sb.append('|');
                    } else {
                        sb.append(cc);
                    }
                    escaping = false;
                    break;
                default:
                    if (!escaping && Character.isWhitespace(cc)) {
                        if (pc != 0 && !Character.isWhitespace(pc)) {
                            sb.append("\\s*");
                        }
                    } else {
                        sb.append(cc);
                    }
                    escaping = false;
                    break;
            }
            pc = cc;
        }
        return sb.toString();
    }
}
