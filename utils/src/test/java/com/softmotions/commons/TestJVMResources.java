package com.softmotions.commons;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.testng.annotations.Test;

import org.testng.Assert;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class TestJVMResources {

    @Test
    public void test1() throws Exception {
        JVMResources.set("foo", "bar");
        URL res = new URL("jvmr:foo");
        InputStream is = res.openStream();
        Assert.assertNotNull(is);
        String val = IOUtils.toString(is, "UTF-8");
        Assert.assertEquals("bar", val);
    }
}
