package com.softmotions.commons.mime;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Collection;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public class MimeDetectorTest {


    static MimeDetector detector = new MagicMimeMimeDetector();

    @Test
    public void testMimeDetector() throws Exception {
        try (InputStream in = new BufferedInputStream(getClass().getResourceAsStream("test-doc1.png"))) {
            Assert.assertNotNull(in);
            Collection<MimeType> mtypes = detector.getMimeTypes(in);
            Assert.assertEquals(mtypes.size(), 1);
            MimeType mtype = mtypes.iterator().next();
            Assert.assertEquals(mtype.getMediaType(), "image");
            Assert.assertEquals(mtype.getSubType(), "png");
        }
        try (InputStream in = new BufferedInputStream(getClass().getResourceAsStream("test-doc1.pdf"))) {
            Assert.assertNotNull(in);
            Collection<MimeType> mtypes = detector.getMimeTypes(in);
            Assert.assertEquals(mtypes.size(), 1);
            MimeType mtype = mtypes.iterator().next();
            Assert.assertEquals(mtype.getMediaType(), "application");
            Assert.assertEquals(mtype.getSubType(), "pdf");
        }
        try (InputStream in = new BufferedInputStream(getClass().getResourceAsStream("test-doc1.odt"))) {
            Assert.assertNotNull(in);
            Collection<MimeType> mtypes = detector.getMimeTypes(in);
            Assert.assertTrue(!mtypes.isEmpty());
            MimeType mtype = mtypes.iterator().next();
            Assert.assertEquals(mtype.getMediaType(), "application");
            Assert.assertEquals(mtype.getSubType(), "vnd.oasis.opendocument.text");
        }
    }
}
