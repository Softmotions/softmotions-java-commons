package com.softmotions.commons.io.scanner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.softmotions.commons.cont.ArrayUtils;
import com.softmotions.commons.cont.Pair;
import com.softmotions.commons.io.watcher.FSWatcher;
import com.softmotions.commons.io.watcher.FSWatcherCollectEventHandler;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class ScannerTest {

    private static final Logger log = LoggerFactory.getLogger(ScannerTest.class);

    @Test
    public void testScanner() throws Exception {
        final String bdir = System.getProperty("project.basedir");
        Assert.assertNotNull(bdir);
        final Path baseDir = Paths.get(bdir, "src/test/test-data");
        Assert.assertTrue(Files.isDirectory(baseDir));

        DirectoryScannerFactory dsf = new DirectoryScannerFactory(baseDir);

        dsf.include("com/softmotions/**/dat?/*/{b,c}.txt");
        try (DirectoryScanner scanner = dsf.createScanner()) {
            DirectoryScannerCollectVisitor cv = new DirectoryScannerCollectVisitor();
            scanner.scan(cv);
            Assert.assertEquals(2, cv.getMatches().size());
            Assert.assertEquals(0, cv.getErrors().size());
            for (Pair<Path, BasicFileAttributes> p : cv.getMatches()) {
                Path path = p.getOne();
                String fname = path.getName(path.getNameCount() - 1).toString();
                Assert.assertTrue("b.txt".equals(fname) || "c.txt".equals(fname));
            }
            scanner.scan(cv);
        }

        dsf.resetExcludes().resetIncludes();
        dsf.include("com/softmotions/**/dat?/*/{b,c}.txt");
        dsf.exclude("**/*/b.txt");
        try (DirectoryScanner scanner = dsf.createScanner()) {
            DirectoryScannerCollectVisitor cv = new DirectoryScannerCollectVisitor();
            scanner.scan(cv);
            Assert.assertEquals(1, cv.getMatches().size());
            Assert.assertEquals(0, cv.getErrors().size());
            for (Pair<Path, BasicFileAttributes> p : cv.getMatches()) {
                Path path = p.getOne();
                String fname = path.getName(path.getNameCount() - 1).toString();
                Assert.assertTrue("c.txt".equals(fname));
            }
            scanner.scan(cv);
        }

        dsf.resetExcludes().resetIncludes();
        dsf.include("com/softmotions/**/dat?/*/{b,c}.txt");
        dsf.exclude("**/*");
        try (DirectoryScanner scanner = dsf.createScanner()) {
            DirectoryScannerCollectVisitor cv = new DirectoryScannerCollectVisitor();
            scanner.scan(cv);
            Assert.assertEquals(0, cv.getMatches().size());
            Assert.assertEquals(0, cv.getErrors().size());
        }

        dsf.resetExcludes().resetIncludes();
        dsf.include("com/softmotions/**/dat?/*/{b,c}.txt");
        dsf.include("**/ef*.txt");
        dsf.include("com/**/a/*.cp?");

        String[] expected = new String[]{
                "com/softmotions/commons/io/scanner/data/b/b.txt",
                "com/softmotions/commons/io/scanner/data/b/c.txt",
                "com/softmotions/commons/io/scanner/data/b/c/efg.txt",
                "com/softmotions/commons/io/scanner/data/a/b.cpp"
        };
        try (DirectoryScanner scanner = dsf.createScanner()) {
            DirectoryScannerCollectVisitor cv = new DirectoryScannerCollectVisitor();
            scanner.scan(cv);
            Assert.assertEquals(expected.length, cv.getMatches().size());
            Assert.assertEquals(0, cv.getErrors().size());
            for (Pair<Path, BasicFileAttributes> p : cv.getMatches()) {
                Path path = p.getOne();
                Assert.assertTrue(-1 != ArrayUtils.indexOf(expected, path.toString().replaceAll("\\\\", "/")));
            }
        }
    }

    @Test
    public void testWatcher() throws Exception {
        String bdir = System.getProperty("project.basedir");
        Assert.assertNotNull(bdir);
        Path baseDir = Paths.get(bdir, "src/test/test-data");
        Assert.assertTrue(Files.isDirectory(baseDir));
        Path tdir = Files.createTempDirectory("ScannerTest-");
        Assert.assertTrue(Files.exists(tdir));
        FileUtils.copyDirectory(baseDir.toFile(), tdir.toFile());

        //log.info("tdir=" + tdir);

        DirectoryScannerFactory dsf = new DirectoryScannerFactory(tdir);
        dsf.include("com/softmotions/commons/io/scanner/data/b/b.txt");
        try (DirectoryScanner scanner = dsf.createScanner()) {
            DirectoryScannerCollectVisitor cv = new DirectoryScannerCollectVisitor();
            scanner.scan(cv);
            Assert.assertEquals(1, cv.getMatches().size());
            Assert.assertEquals(0, cv.getErrors().size());
            Assert.assertEquals("com/softmotions/commons/io/scanner/data/b/b.txt",
                         cv.getMatches().get(0).getOne().toString().replaceAll("\\\\", "/"));
        }

        dsf.resetIncludes();
        dsf.resetExcludes();

        dsf.include("**/*.txt")
                .include("**/a/b/foo.cpp")
                .exclude("a.txt");

        DirectoryScanner scanner = dsf.createScanner();
        FSWatcherCollectEventHandler cv = new FSWatcherCollectEventHandler(tdir);
        FSWatcher watcher = scanner.activateFileSystemWatcher(cv, 0, null);
        Assert.assertNotNull(watcher);

        Files.createFile(tdir.resolve("foo.txt"));
        Files.createFile(tdir.resolve("foo.php"));
        Files.createDirectories(tdir.resolve("h/a/b"));
        Files.createFile(tdir.resolve("h/a/b/foo.cpp"));
        Files.createFile(tdir.resolve("h/a/b/bar.cpp"));
        Files.createFile(tdir.resolve("h/a/b/bar.txt"));

        Thread.sleep(500);

        Files.createFile(tdir.resolve("h/a/b/bar2.txt"));

        Thread.sleep(500);

        Files.delete(tdir.resolve("h/a/b/bar2.txt"));
        FileUtils.writeStringToFile(tdir.resolve("foo.txt").toFile(), "message", "utf-8");
        FileUtils.writeStringToFile(tdir.resolve("foo.php").toFile(), "message", "utf-8");


        FileUtils.deleteDirectory(tdir.resolve("h/a").toFile());

        Thread.sleep(1000);

        scanner.close();

        /*log.info("c=" + cv.getCreated());
        log.info("m=" + cv.getModified());
        log.info("d=" + cv.getDeleted());
        log.info("r=" + cv.getRegistered());*/

        Assert.assertTrue(cv.getCreated().contains(Paths.get("foo.txt")) ||
                   cv.getRegistered().contains(Paths.get("foo.txt")));

        Assert.assertTrue(cv.getCreated().contains(Paths.get("h/a/b/bar2.txt")) ||
                   cv.getRegistered().contains(Paths.get("h/a/b/bar2.txt")));

        Assert.assertTrue(cv.getCreated().contains(Paths.get("h/a/b/bar.txt")) ||
                   cv.getRegistered().contains(Paths.get("h/a/b/bar.txt")));


        Assert.assertTrue(cv.getModified().contains(Paths.get("foo.txt")));

        Assert.assertTrue(cv.getDeleted().contains(Paths.get("h/a/b/bar2.txt")));
        Assert.assertTrue(cv.getDeleted().contains(Paths.get("h/a/b/foo.cpp")));
        Assert.assertTrue(cv.getDeleted().contains(Paths.get("h/a/b/bar.txt")));


        Assert.assertTrue(cv.getRegistered().contains(Paths.get("com/softmotions/commons/io/scanner/data/a/abc.txt")));
        Assert.assertTrue(cv.getRegistered().contains(Paths.get("com/softmotions/commons/io/scanner/data/b/b.txt")));
        Assert.assertTrue(cv.getRegistered().contains(Paths.get("com/softmotions/commons/io/scanner/data/b/c.txt")));
        Assert.assertTrue(cv.getRegistered().contains(Paths.get("com/softmotions/commons/io/scanner/data/b/c/efg.txt")));

        //watcher.join();
        FileUtils.deleteDirectory(tdir.toFile());
    }

}
