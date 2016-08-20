package com.softmotions.web;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.map.Flat3Map;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.softmotions.commons.cl.ClassLoaderUtils;
import com.softmotions.commons.ctype.CTypeUtils;

/**
 * Servlet provides access to the set of resources
 * stored in jar files in the classpath.
 * Supports automatic content reloading if
 * jar file updated.
 * <p/>
 * <p/>
 * Servlet parameters in the following format:
 * <pre>
 *      {prefix} => {injar path} [,watch=yes|no]
 * </pre>
 * <p/>
 * Example:
 * <pre>
 *  &lt;servlet&gt;
 *      &lt;servlet-name&gt;JarResourcesServlet&lt;/servlet-name&gt;
 *      &lt;servlet-class&gt;com.softmotions.web.JarResourcesServlet&lt;/servlet-class&gt;
 *      &lt;init-param&gt;
 *          &lt;param-name&gt;ncms&lt;/param-name&gt;
 *          &lt;param-value&gt;ncms-engine-qx/ncms, watch=yes&lt;/param-value&gt;
 *      &lt;/init-param&gt;
 *  &lt;/servlet&gt;
 * </pre>
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class JarResourcesFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(JarResourcesFilter.class);

    List<MappingSlot> mslots;

    String stripPefix;

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest sreq = (HttpServletRequest) req;
        boolean ret = getContent(sreq, (HttpServletResponse) resp, !"HEAD".equals(sreq.getMethod()));
        if (!ret && !resp.isCommitted()) {
            chain.doFilter(req, resp);
        }
    }

    @Override
    public void init(FilterConfig cfg) throws ServletException {
        mslots = new ArrayList<>();
        Enumeration<String> pnames = cfg.getInitParameterNames();
        while (pnames.hasMoreElements()) {
            String pname = pnames.nextElement();
            if ("strip-prefix".equals(pname)) {
                stripPefix = cfg.getInitParameter(pname);
                continue;
            }
            try {
                handleJarMapping(pname, cfg.getInitParameter(pname));
            } catch (ServletException e) {
                throw e;
            } catch (Exception e) {
                throw new ServletException(e);
            }
        }
        if (stripPefix == null) {
            stripPefix = "";
        }
    }

    @Override
    public void destroy() {
        for (MappingSlot ms : mslots) {
            try {
                ms.close();
            } catch (IOException ignored) {
            }
        }
        mslots.clear();
    }

    boolean getContent(HttpServletRequest req, HttpServletResponse resp, boolean transfer) throws ServletException, IOException {
        ContentDescriptor cd = getContentDescriptor(req, resp);
        if (cd == null) {
            return false;
        }
        if (cd.getMimeType() != null) {
            resp.setContentType(cd.getMimeType());
            if (resp.getCharacterEncoding() == null && CTypeUtils.isTextualContentType(cd.getMimeType())) {
                resp.setCharacterEncoding("UTF-8");
            }
        }
        MappingSlot ms = cd.mappingSlot;
        if (!ms.headers.isEmpty()) {
            for (Map.Entry<String, String> he : ms.headers.entrySet()) {
                resp.setHeader(he.getKey(), he.getValue());
            }
        }

        URL url = cd.getUrl();
        try (InputStream is = url.openStream()) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            IOUtils.copy(is, output);
            resp.setContentLength(output.size());
            if (transfer) {
                output.writeTo(resp.getOutputStream());
                resp.getOutputStream().flush();
            }
        }
        resp.flushBuffer();
        return true;
    }

    private ContentDescriptor getContentDescriptor(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String uri = req.getRequestURI();
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("", e);
        }
        String path = uri.substring(stripPefix.length());
        MappingSlot ms = findMatchingSlot(path);
        if (ms == null) {
            return null;
        }
        path = path.substring(ms.prefix.length());
        if (path.isEmpty()) { //redirect to the slash
            resp.sendRedirect(ms.prefix + '/');
            return null;
        }
        URL url = ms.getResourceUrl(path);
        if (url == null) {
            return null;
        }
        return new ContentDescriptor(ms, url, req.getServletContext().getMimeType(path));
    }

    MappingSlot findMatchingSlot(String path) {
        for (final MappingSlot ms : mslots) {
            if (path.startsWith(ms.prefix)) {
                return ms;
            }
        }
        return null;
    }

    void handleJarMapping(String prefix, String spec) throws Exception {
        if (prefix.isEmpty()) {
            throw new Exception("Empty config param name");
        }
        mslots.add(new MappingSlot(prefix, spec));
    }

    private static class MappingSlot implements Closeable {

        final String prefix;

        final String path;

        final Object lock = new Object();

        final Map<String, String> headers = new Flat3Map<>();

        boolean watch;

        long lastLoadMtime;

        volatile File jarFile;

        volatile JarResourcesClassLoader loader;

        private MappingSlot(String prefix, String spec) throws Exception {
            if (prefix.charAt(0) != '/') {
                prefix = "/" + prefix;
            }
            String[] parts = spec.split(",");
            if (parts.length == 0) {
                throw new ServletException(String.format("Invalid configuration param %s: %s", prefix, spec));
            }
            this.path = StringUtils.strip(parts[0].trim(), "/");
            //Parse options
            for (int i = 1; i < parts.length; ++i) {
                String p = parts[i];
                String[] pp = p.split("=");
                if (pp.length == 2) {
                    p = pp[0].trim();
                    switch (p) {
                        case "watch":
                        case "watching":
                            this.watch = BooleanUtils.toBoolean(pp[1]);
                            break;
                    }
                    String lp = p.toLowerCase();
                    if (lp.startsWith("x-")) {
                        headers.put(p, pp[1].trim());
                    }
                }
            }
            this.prefix = prefix;
            log.info("Registered JAR resources mapping: {} => {}", prefix, spec);
            if (!headers.isEmpty()) {
                log.info("Response headers: {}", headers);
            }
        }

        URL getResourceUrl(String resource) {
            URL resourceUrl;
            if ("/".equals(resource)) {
                resource = "/index.html";
            }
            String resourceTranslated = path + ((resource.charAt(0) != '/') ? ("/" + resource) : resource);
            ClassLoader loaderRef = null;
            long mtime;
            synchronized (lock) {
                mtime = (jarFile != null) ? jarFile.lastModified() : 0;
                if (loader != null) {
                    if (lastLoadMtime < mtime) {
                        try {
                            log.info("Closing old loader");
                            loader.close();
                            Thread.sleep(1000);
                        } catch (InterruptedException | IOException e) {
                            log.error("", e);
                        }
                        loader = null;
                    }
                    loaderRef = loader;
                }
            }
            if (loaderRef != null) {
                return loaderRef.getResource(resourceTranslated);
            }

            ClassLoader baseLoader =
                    ObjectUtils.firstNonNull(Thread.currentThread().getContextClassLoader(),
                                             getClass().getClassLoader());

            synchronized (lock) {
                if (jarFile != null) {
                    try {
                        log.info("Reloading jar file: {}", jarFile.toURI());
                        lastLoadMtime = jarFile.lastModified();
                        loader = new JarResourcesClassLoader(jarFile.toURI().toURL(), baseLoader);
                        loaderRef = loader;
                    } catch (MalformedURLException e) {
                        log.error("", e);
                    }
                }
            }

            if (loaderRef != null) {
                return loaderRef.getResource(resourceTranslated);
            }

            resourceUrl = baseLoader.getResource(resourceTranslated);
            if (resourceUrl != null && "jar".equals(resourceUrl.getProtocol())) {
                synchronized (lock) {
                    if (loader != null) {
                        return loader.getResource(resourceTranslated);
                    }
                    String p = resourceUrl.getFile();
                    URL baseJar;
                    try {
                        baseJar = new URL(p.substring(0, p.indexOf('!')));
                    } catch (MalformedURLException e) {
                        log.error("", e);
                        return resourceUrl;
                    }
                    if (watch) {
                        try {
                            log.info("Start watching jar file: {}", baseJar);
                            jarFile = new File(baseJar.toURI());
                            lastLoadMtime = jarFile.lastModified();
                        } catch (URISyntaxException e) {
                            log.error("", e);
                        }
                    }
                    loader = new JarResourcesClassLoader(baseJar, baseLoader);
                    loaderRef = loader;
                }
                return loaderRef.getResource(resourceTranslated);
            }
            return resourceUrl;
        }

        public String toString() {
            return "MappingSlot{" + prefix + " => " + path + '}';
        }

        public int hashCode() {
            return prefix.hashCode();
        }

        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        public boolean equals(Object obj) {
            return prefix.equals(obj);
        }


        @Override
        public void close() throws IOException {
            if (loader != null) {
                loader.close();
                loader = null;
            }
        }
    }

    private static final class JarResourcesClassLoader extends URLClassLoader {

        private JarResourcesClassLoader(URL url, ClassLoader parent) {
            super(new URL[]{url}, parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class clazz;
            synchronized (getClassLoadingLock(name)) {
                clazz = findLoadedClass(name);
                if (clazz == null) {
                    clazz = findClass(name);
                }
                if (resolve) resolveClass(clazz);
                return clazz;
            }
        }

        @Override
        public URL getResource(String name) {
            return findResource(name);
        }

        @Override
        public void close() throws IOException {
            ClassLoaderUtils.destroyClassLoader(this);
            super.close();
        }

        public String toString() {
            return "JarResourcesClassLoader{ " + Arrays.asList(getURLs()) + '}';
        }
    }

    private static final class ContentDescriptor {

        private final URL url;

        private final String mimeType;

        private final MappingSlot mappingSlot;

        public URL getUrl() {
            return url;
        }

        public String getMimeType() {
            return mimeType;
        }

        private ContentDescriptor(MappingSlot mappingSlot, URL url, String mimeType) {
            this.mappingSlot = mappingSlot;
            this.url = url;
            this.mimeType = mimeType;
        }
    }
}
