package com.softmotions.commons;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class TestJVMResources {

    @Test
    public void test1() throws Exception {
        JVMResources.set("foo", "bar");
        URL res = new URL("jvmr:foo");
        InputStream is = res.openStream();
        assertNotNull(is);
        String val = IOUtils.toString(is, "UTF-8");
        assertEquals("bar", val);
    }

    @Test
    public void test2() throws Exception {
        URL res = getClass().getClassLoader().getResource("com/softmotions/commons/services1.xml");
        assertNotNull(res);
        String data;
        try (InputStream is = res.openStream()) {
            data = IOUtils.toString(is, "UTF-8");
        }
        assertTrue(data.contains("<hello>kitty</hello>"));
        data = data.replace("kitty", "pitty");
        JVMResources.set(res.toString(), data);
        URL res2 = new URL("jvmr:" + res.toString());
        try (InputStream is = res2.openStream()) {
            data = IOUtils.toString(is, "UTF-8");
        }
        assertTrue(data.contains("<hello>pitty</hello>"));

        Exception err = null;
        JVMResources.remove(res.toString());
        try {
            res2.openStream();
        } catch (IOException e) {
            err = e;
        }
        assertNotNull(err);
    }
}
