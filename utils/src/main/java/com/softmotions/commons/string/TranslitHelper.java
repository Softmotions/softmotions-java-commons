package com.softmotions.commons.string;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class TranslitHelper {

    private static final String RU_LANG = new Locale("ru").getLanguage();


    private static final String[] ARRAY_EN = new String[]{
            "sh", "sch", "y", "ya", "zh", "ch", "yu", "yo",
            "i", "u", "k", "e", "n", "g", "z", "h", "", "f",
            "v", "a", "p", "r", "o", "l", "d", "e", "s", "m",
            "i", "t", "'", "b", "c",
            "SH", "SCH", "Y", "YA", "ZH", "CH", "YU", "YO",
            "Y", "U", "K", "E", "N", "G", "Z", "H", "", "F",
            "V", "A", "P", "R", "O", "L", "D", "E", "S", "M",
            "I", "T", "'", "B", "C"
    };

    private static final Character[] ARRAY_RU = new Character[]{
            'ш', 'щ', 'ы', 'я', 'ж', 'ч', 'ю', 'ё', 'й', 'у',
            'к', 'е', 'н', 'г', 'з', 'х', 'ъ', 'ф', 'в', 'а',
            'п', 'р', 'о', 'л', 'д', 'э', 'с', 'м', 'и', 'т',
            'ь', 'б', 'ц',
            'Ш', 'Щ', 'Ы', 'Я', 'Ж', 'Ч', 'Ю', 'Ё', 'Й', 'У',
            'К', 'Е', 'Н', 'Г', 'З', 'Х', 'Ъ', 'Ф', 'В', 'А',
            'П', 'Р', 'О', 'Л', 'Д', 'Э', 'С', 'М', 'И', 'Т',
            'Ь', 'Б', 'Ц'
    };

    @SuppressWarnings("StaticCollection")
    private static final Map<Character, String> RU_EN_MAP = new HashMap<>();

    static {
        int i = -1;
        while (++i < ARRAY_RU.length) {
            if (!RU_EN_MAP.containsKey(ARRAY_RU[i])) {
                RU_EN_MAP.put(ARRAY_RU[i], ARRAY_EN[i]);
            }
        }
    }

    private TranslitHelper() {
    }


    public static String translit(Locale locale, String str) {
        if (RU_LANG.equals(locale.getLanguage())) {
            return translitRussian(str);
        } else {
            return str;
        }
    }

    /**
     * Преобразует русский текст в транслитерованную форму английскими буквами
     */
    public static String translitRussian(String src) {
        StringBuilder sb = new StringBuilder(src.length());
        for (char c : src.toCharArray()) {
            if (RU_EN_MAP.containsKey(c)) {
                sb.append(RU_EN_MAP.get(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
