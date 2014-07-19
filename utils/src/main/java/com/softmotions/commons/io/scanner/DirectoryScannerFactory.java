package com.softmotions.commons.io.scanner;

import com.softmotions.commons.io.watcher.FSWatcher;
import com.softmotions.commons.io.watcher.FSWatcherCreateEvent;
import com.softmotions.commons.io.watcher.FSWatcherDeleteEvent;
import com.softmotions.commons.io.watcher.FSWatcherEventHandler;
import com.softmotions.commons.io.watcher.FSWatcherModifyEvent;
import com.softmotions.commons.io.watcher.FSWatcherRegisterEvent;
import com.softmotions.commons.re.RegexpHelper;

import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Factory for directory scanners.
 * <p/>
 * This implementation is thread-safe but
 * scanner instances produces by {@link #createScanner()}
 * may not be thread-safe.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
@ThreadSafe
public class DirectoryScannerFactory {

    private static final Logger log = LoggerFactory.getLogger(DirectoryScannerFactory.class);

    public static final String[] DEFAULT_EXCLUDES = new String[]{
            "**/*~",
            "**/#*#",
            "**/.#*",
            "**/%*%",
            "**/._*",
            "**/CVS",
            "**/CVS/**",
            "**/.cvsignore",
            "**/SCCS",
            "**/SCCS/**",
            "**/vssver.scc",
            "**/.svn",
            "**/.svn/**",
            "**/.DS_Store",
            "**/.git",
            "**/.git/**",
            "**/.gitattributes",
            "**/.gitignore",
            "**/.gitmodules",
            "**/.hg",
            "**/.hg/**",
            "**/.hgignore",
            "**/.hgsub",
            "**/.hgsubstate",
            "**/.hgtags",
            "**/.bzr",
            "**/.bzr/**",
            "**/.bzrignore"
    };

    private final Object lock = new Object();

    private final Path basedir;

    private ArrayList<String> excludes;

    private ArrayList<String> includes;

    private boolean useDefaultExcludes;

    public Path getBasedir() {
        return basedir;
    }

    public List<String> getExcludes() {
        synchronized (lock) {
            return excludes != null ?
                   Collections.unmodifiableList(excludes) :
                   Collections.EMPTY_LIST;
        }
    }

    public List<String> getIncludes() {
        synchronized (lock) {
            return includes != null ?
                   Collections.unmodifiableList(includes) :
                   Collections.EMPTY_LIST;
        }
    }

    public DirectoryScannerFactory(Path basedir) throws IOException {
        if (!Files.isDirectory(basedir)) {
            throw new IOException("File: " + basedir.toAbsolutePath() +
                                  "is not a directory");
        }
        this.basedir = basedir.toAbsolutePath().normalize();
        this.useDefaultExcludes = true;
    }

    public DirectoryScannerFactory exclude(String pattern) {
        synchronized (lock) {
            if (excludes == null) {
                excludes = new ArrayList<>();
            }
            excludes.add(pattern);
        }
        return this;
    }

    public DirectoryScannerFactory include(String pattern) {
        synchronized (lock) {
            if (includes == null) {
                includes = new ArrayList<>();
            }
            includes.add(pattern);
        }
        return this;
    }

    public DirectoryScannerFactory resetIncludes() {
        synchronized (lock) {
            if (includes != null) {
                includes.clear();
            }
        }
        return this;
    }

    public DirectoryScannerFactory resetExcludes() {
        synchronized (lock) {
            if (excludes != null) {
                excludes.clear();
            }
        }
        return this;
    }

    public boolean isUseDefaultExcludes() {
        synchronized (lock) {
            return useDefaultExcludes;
        }
    }

    public DirectoryScannerFactory setUseDefaultExcludes(boolean val) {
        synchronized (lock) {
            useDefaultExcludes = val;
        }
        return this;
    }

    public DirectoryScanner createScanner() throws IOException {
        return new DirectoryScannerImpl();
    }

    @NotThreadSafe
    private class DirectoryScannerImpl
            implements DirectoryScanner, FileVisitor<Path>, FSWatcherEventHandler {

        private final AntPatternMatcher matcher;

        private DirectoryScannerVisitor visitor;

        private FSWatcher watcher;

        private FSWatcherEventHandler handler;

        private Object userData;

        private DirectoryScannerImpl() {
            List<String> iList;
            List<String> eList;
            synchronized (lock) {
                iList = (includes != null) ? (List<String>) includes.clone() : new ArrayList<String>();
                eList = (excludes != null) ? (List<String>) excludes.clone() : new ArrayList<String>();
                if (useDefaultExcludes) {
                    eList.addAll(Arrays.asList(DEFAULT_EXCLUDES));
                }
            }
            if (iList.isEmpty()) {
                iList.add("**/*");
            }
            matcher = new AntPatternMatcher(iList, eList);
        }


        public <T> T getUserData() {
            return (T) userData;
        }

        public <T> void setUserData(T data) {
            userData = data;
        }

        public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) throws IOException {
            path = basedir.relativize(path);
            String spath = path.toString();
            if (spath.isEmpty() || accept(path, attrs)) {
                return FileVisitResult.CONTINUE;
            } else {
                return FileVisitResult.SKIP_SUBTREE;
            }
        }

        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
            path = basedir.relativize(path);
            if (accept(path, attrs)) {
                visitor.visit(path, attrs);
            }
            return FileVisitResult.CONTINUE;
        }

        public FileVisitResult visitFileFailed(Path path, IOException exc) throws IOException {
            visitor.error(path, exc);
            return FileVisitResult.CONTINUE;
        }

        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        private boolean accept(Path path, BasicFileAttributes attrs) {
            final String[] segments = new String[path.getNameCount()];
            for (int i = 0; i < segments.length; ++i) {
                segments[i] = path.getName(i).toString();
            }
            return matcher.voteAll(segments, (attrs != null && attrs.isDirectory()));
        }

        public void init(FSWatcher w) {
        }


        public void handlePollTimeout(FSWatcher watcher) throws Exception {
            handler.handlePollTimeout(watcher);
        }

        public void handleRegisterEvent(FSWatcherRegisterEvent e) throws Exception {
            if (!Files.isDirectory(e.getFullPath()) && acceptWatcherEvent(e.getFullPath())) {
                handler.handleRegisterEvent(e);
            }
        }

        public void handleCreateEvent(FSWatcherCreateEvent e) throws Exception {
            if (!Files.isDirectory(e.getFullPath()) && acceptWatcherEvent(e.getFullPath())) {
                handler.handleCreateEvent(e);
            }
        }

        public void handleDeleteEvent(FSWatcherDeleteEvent e) throws Exception {
            if (acceptWatcherEvent(e.getFullPath())) {
                handler.handleDeleteEvent(e);
            }
        }

        public void handleModifyEvent(FSWatcherModifyEvent e) throws Exception {
            if (!Files.isDirectory(e.getFullPath()) && acceptWatcherEvent(e.getFullPath())) {
                handler.handleModifyEvent(e);
            }
        }

        private boolean acceptWatcherEvent(Path path) {
            path = basedir.relativize(path);
            String spath = path.toString();
            return !spath.isEmpty() && accept(path, null);
        }

        public void close() throws IOException {
            if (watcher != null) {
                watcher.close();
            }
            visitor = null;
        }

        public FSWatcher activateFileSystemWatcher(FSWatcherEventHandler handler) throws IOException {
            return activateFileSystemWatcher(handler, 0, null);
        }


        public FSWatcher activateFileSystemWatcher(FSWatcherEventHandler handler,
                                                   long pollTimeoutMills,
                                                   Object userData) throws IOException {
            if (watcher == null) {
                watcher = new FSWatcher(basedir.toString(), basedir.getFileSystem(), this, pollTimeoutMills);
            }
            this.handler = handler;
            this.watcher.setUserData(userData);
            this.watcher.register(basedir, true);
            return watcher;
        }

        public Path getBasedir() {
            return basedir;
        }

        public void scan(DirectoryScannerVisitor visitor) throws IOException {
            this.visitor = visitor;
            Files.walkFileTree(basedir, this);
        }
    }

    private static final class AntPatternMatcher {

        private Pattern[][] includes;

        private Pattern[][] excludes;

        private AntPatternMatcher(List<String> includes, List<String> excludes) {
            this.includes = new Pattern[includes.size()][];
            this.excludes = new Pattern[excludes.size()][];
            for (int i = 0, l = includes.size(); i < l; ++i) {
                this.includes[i] = normalizePattern(includes.get(i));
            }
            for (int i = 0, l = excludes.size(); i < l; ++i) {
                this.excludes[i] = normalizePattern(excludes.get(i));
            }
        }

        private Pattern[] normalizePattern(String pattern) {
            if (StringUtils.isBlank(pattern)) {
                pattern = "**";
            }
            ArrayList<Pattern> parts = new ArrayList<>();
            pattern = pattern.replace('\\', '/');
            if ('/' != File.separatorChar) {
                pattern = pattern.replace(File.separatorChar, '/');
            }
            pattern = StringUtils.strip(pattern, "/ ");
            boolean inmd = false;
            StringTokenizer st = new StringTokenizer(pattern, "/");

            while (st.hasMoreElements()) {
                String pitem = st.nextToken().trim();
                if ("**".equals(pitem)) {
                    if (inmd) {
                        continue;
                    }
                    inmd = true;
                    parts.add(null);
                } else if ("*".equals(pitem) && inmd) {
                    ;//noop
                } else {
                    inmd = false;
                    parts.add(Pattern.compile(RegexpHelper.convertGlobToRegEx(pitem)));
                }
            }
            return parts.toArray(new Pattern[parts.size()]);
        }

        private boolean matchSegment(String val, Pattern pattern) {
            return (pattern == null || pattern.matcher(val).matches());
        }

        private boolean vote(String[] val, Pattern[] match, boolean prefixOnly) {
            if (val.length == 0 || match.length == 0) {
                return false;
            }
            int mind = 0;
            int expectNextInd = -1;
            Pattern mv;
            Pattern expectNext = null;

            for (final String el : val) {
                if (mind >= match.length) {
                    return false;
                }
                mv = match[mind];
                if (mv == null) {  // '**'
                    if (expectNextInd == -1 && match.length > mind + 1) {
                        expectNextInd = mind + 1;
                        expectNext = match[expectNextInd];
                    }
                }
                if (!matchSegment(el, mv)) {
                    return false;
                }
                if (expectNextInd != -1) {
                    if (matchSegment(el, expectNext)) {
                        expectNext = null;
                        expectNextInd = -1;
                        mind += 2;
                    }
                } else if (mv != null) {
                    mind += 1;
                }
            }
            if (!prefixOnly) {
                for (int i = mind; i < match.length; ++i) {
                    if (match[i] != null) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean voteAll(String[] val, boolean prefixOnly) {
            if (!prefixOnly) {
                for (final Pattern[] p : excludes) {
                    if (vote(val, p, false)) {
                        return false;
                    }
                }
            }
            for (final Pattern[] p : includes) {
                if (vote(val, p, prefixOnly)) {
                    return true;
                }
            }
            return false;
        }
    }


}
