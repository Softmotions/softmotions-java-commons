package com.softmotions.commons.re;

public class RegexpHelper {

    private RegexpHelper() {
    }

    public static String convertGlobToRegEx(String line) {

        line = line.trim();
        StringBuilder sb = new StringBuilder(line.length());

        boolean escaping = false;
        int incurlies = 0;
        char pc = 0;

        for (int i = 0, l = line.length(); i < l; ++i) {
            char cc = line.charAt(i);
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
