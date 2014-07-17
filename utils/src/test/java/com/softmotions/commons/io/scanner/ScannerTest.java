package com.softmotions.commons.io.scanner;

import com.softmotions.commons.cont.ArrayUtils;
import com.softmotions.commons.cont.Pair;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import static org.junit.Assert.*;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class ScannerTest {

    private static final Logger log = LoggerFactory.getLogger(ScannerTest.class);

    @Test
    public void testScanner() throws Exception {
        final String bdir = System.getProperty("project.basedir");
        assertNotNull(bdir);
        final Path baseDir = Paths.get(bdir, "src/test/test-data");
        assertTrue(Files.isDirectory(baseDir));

        DirectoryScannerFactory dsf = new DirectoryScannerFactory(baseDir);

        dsf.include("com/softmotions/**/dat?/*/{b,c}.txt");
        try (DirectoryScanner scanner = dsf.createScanner()) {
            DirectoryScannerCollectVisitor cv = new DirectoryScannerCollectVisitor();
            scanner.scan(cv);
            assertEquals(2, cv.getMatches().size());
            assertEquals(0, cv.getErrors().size());
            for (Pair<Path, BasicFileAttributes> p : cv.getMatches()) {
                Path path = p.getOne();
                String fname = path.getName(path.getNameCount() - 1).toString();
                assertTrue("b.txt".equals(fname) || "c.txt".equals(fname));
            }
            scanner.scan(cv);
        }

        dsf.resetExcludes().resetIncludes();
        dsf.include("com/softmotions/**/dat?/*/{b,c}.txt");
        dsf.exclude("**/*/b.txt");
        try (DirectoryScanner scanner = dsf.createScanner()) {
            DirectoryScannerCollectVisitor cv = new DirectoryScannerCollectVisitor();
            scanner.scan(cv);
            assertEquals(1, cv.getMatches().size());
            assertEquals(0, cv.getErrors().size());
            for (Pair<Path, BasicFileAttributes> p : cv.getMatches()) {
                Path path = p.getOne();
                String fname = path.getName(path.getNameCount() - 1).toString();
                assertTrue("c.txt".equals(fname));
            }
            scanner.scan(cv);
        }

        dsf.resetExcludes().resetIncludes();
        dsf.include("com/softmotions/**/dat?/*/{b,c}.txt");
        dsf.exclude("**/*");
        try (DirectoryScanner scanner = dsf.createScanner()) {
            DirectoryScannerCollectVisitor cv = new DirectoryScannerCollectVisitor();
            scanner.scan(cv);
            assertEquals(0, cv.getMatches().size());
            assertEquals(0, cv.getErrors().size());
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
            assertEquals(expected.length, cv.getMatches().size());
            assertEquals(0, cv.getErrors().size());
            for (Pair<Path, BasicFileAttributes> p : cv.getMatches()) {
                Path path = p.getOne();
                assertTrue(-1 != ArrayUtils.indexOf(expected, path.toString()));
            }
        }
    }
}
