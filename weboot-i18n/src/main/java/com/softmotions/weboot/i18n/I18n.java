package com.softmotions.weboot.i18n;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.resourceloading.AggregateResourceBundleLocator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Localization support.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
@Singleton
@ThreadSafe
public class I18n {

    public static final String LNG_COOKIE_NAME = "NG_TRANSLATE_LANG_KEY";

    public static final String REQ_LOCALE_ATTR_NAME = "LNG_REQ_LOCALE";

    public static final String REQ_LOCALE_PARAM_NAME = "lang";

    private static final ThreadLocal<Map<String, SimpleDateFormat>> LOCAL_SDF_CACHE = new ThreadLocal<>();

    @SuppressWarnings("StaticCollection")
    private static final Map<String, String[]> LNG_MONTHS = new HashMap<>();

    private static final String[] ISO2_LANGUAGES;

    static {
        ISO2_LANGUAGES = Locale.getISOLanguages();
        Arrays.sort(ISO2_LANGUAGES);
    }

    static {
        LNG_MONTHS.put("ru", new String[]{
                "января", "февраля", "марта", "апреля", "мая",
                "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"
        });
    }

    private AggregateResourceBundleLocator bundleLocator;

    private Map<Locale, ResourceBundle> bundleCache;

    private boolean forceDefaultLocaleForRequests;

    @Inject
    public I18n(HierarchicalConfiguration<ImmutableNode> xcfg) {
        forceDefaultLocaleForRequests = xcfg.getBoolean("force-default-locale-for-requests", false);
        List<Object> blist = xcfg.getList("messages.bundle");
        ArrayList<String> rnames = new ArrayList<>();
        for (Object v : blist) {
            String sv = (v != null) ? v.toString() : null;
            if (StringUtils.isBlank(sv)) {
                continue;
            }
            rnames.add(sv);
        }
        bundleLocator = new AggregateResourceBundleLocator(rnames);
        bundleCache = new ConcurrentHashMap<>();

    }

    @Nonnull
    public ResourceBundle getResourceBundle(Locale locale) {
        ResourceBundle bundle = bundleCache.get(locale);
        if (bundle == null) {
            bundle = bundleLocator.getResourceBundle(locale);
            if (bundle == null) {
                bundle = bundleLocator.getResourceBundle(Locale.ENGLISH);
            }
            if (bundle == null) {
                throw new RuntimeException("Unable to locate any resource bundle for locale: " + locale);
            }
            bundleCache.put(locale, bundle);
        }
        return bundle;
    }

    @Nonnull
    public ResourceBundle getResourceBundle(HttpServletRequest req) {
        return getResourceBundle(getLocale(req));
    }

    @Nonnull
    public String get(String key, String... params) throws MissingResourceException {
        return get(key, Locale.getDefault(), params);
    }

    @Nonnull
    public String get(String key, Locale locale, Object... params) throws MissingResourceException {
        return String.format(locale, getResourceBundle(locale).getString(key), params);
    }

    @Nonnull
    public String get(String key, HttpServletRequest req, Object... params) throws MissingResourceException {
        return get(key, getLocale(req), params);
    }

    @Nonnull
    public Locale getLocale(@Nullable HttpServletRequest req) {
        if (req == null || forceDefaultLocaleForRequests) {
            return Locale.getDefault();
        }
        Locale l = (Locale) req.getAttribute(REQ_LOCALE_ATTR_NAME);
        if (l == null) {
            l = new Locale(fetchRequestLanguage(req));
            req.setAttribute(REQ_LOCALE_ATTR_NAME, l);
        }
        return l;
    }

    public boolean isValidISO2Language(String lang) {
        return (lang != null && Arrays.binarySearch(ISO2_LANGUAGES, lang) >= 0);
    }

    @Nonnull
    private String fetchRequestLanguage(HttpServletRequest req) {
        if (forceDefaultLocaleForRequests) {
            return Locale.getDefault().getLanguage();
        }
        String lang = req.getParameter(REQ_LOCALE_PARAM_NAME);
        if (StringUtils.isBlank(lang)) {
            Cookie[] cookies = req.getCookies();
            if (cookies != null) {
                for (final Cookie c : cookies) {
                    if (LNG_COOKIE_NAME.equals(c.getName())) {
                        lang = c.getValue();
                        //strip quotes if presented
                        if (lang.length() > 1
                                && lang.charAt(0) == '\"'
                                && lang.charAt(lang.length() - 1) == '\"') {
                            lang = lang.substring(1, lang.length() - 1);
                        }
                        break;
                    }
                }
            }
        }
        if (lang != null) {
            lang = lang.toLowerCase();
        }
        if (!isValidISO2Language(lang)) {
            lang = req.getLocale().getLanguage();
        }
        //noinspection ConstantConditions
        return isValidISO2Language(lang) ? lang : Locale.getDefault().getLanguage();
    }

    public void initRequestI18N(HttpServletRequest req, HttpServletResponse resp) {
        if (req == null || forceDefaultLocaleForRequests) {
            return;
        }
        String lang = fetchRequestLanguage(req);
        String qlang = '\"' + lang  + '\"';
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (final Cookie c : cookies) {
                if (LNG_COOKIE_NAME.equals(c.getName())) {
                    String clang = c.getValue();
                    if (Objects.equals(qlang, clang) || Objects.equals(lang, clang)) {
                        return;
                    }
                    break;
                }
            }
        }
        Cookie c = new Cookie(LNG_COOKIE_NAME, qlang);
        c.setMaxAge(60 * 60 * 24 * 7); //1 week todo configurable
        resp.addCookie(c);
    }


    public void saveRequestLang(String lang, HttpServletRequest req, HttpServletResponse resp) {
        if (forceDefaultLocaleForRequests) {
            return;
        }
        lang = lang.toLowerCase();
        if (!isValidISO2Language(lang)) {
            return;
        }
        String qlang = '\"' + lang + '\"';
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (final Cookie c : cookies) {
                if (LNG_COOKIE_NAME.equals(c.getName())) {
                    String clang = c.getValue();
                    if (Objects.equals(qlang, clang) || Objects.equals(lang, clang)) {
                        return;
                    }
                    break;
                }
            }
        }
        Cookie c = new Cookie(LNG_COOKIE_NAME, qlang);
        c.setMaxAge(60 * 60 * 24 * 7); //1 week todo configurable
        resp.addCookie(c);
        req.setAttribute(REQ_LOCALE_ATTR_NAME, new Locale(lang));
    }


    public String format(Date date,
                         String format,
                         Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        if ("LMMMMM".equals(format)) {
            return getLocaleAwareMonth(date, locale);
        }
        Map<String, SimpleDateFormat> formatters = LOCAL_SDF_CACHE.get();
        if (formatters == null) {
            formatters = new HashMap<>();
            LOCAL_SDF_CACHE.set(formatters);
        }
        String key = locale.toString() + '@' + format;
        SimpleDateFormat sdf = formatters.get(key);
        if (sdf == null) {
            sdf = new SimpleDateFormat(format, locale);
            formatters.put(key, sdf);
        }
        return sdf.format(date);
    }

    private String getLocaleAwareMonth(Date date,
                                       Locale locale) {
        Calendar cal = Calendar.getInstance(locale);
        cal.setTime(date);
        String lng = locale.getLanguage();
        String[] months = LNG_MONTHS.get(lng);
        if (months != null) {
            return months[cal.get(Calendar.MONTH)];
        }
        return format(cal.getTime(), "MMMMM", locale);
    }
}
