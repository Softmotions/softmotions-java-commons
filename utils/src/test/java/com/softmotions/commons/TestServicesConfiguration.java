package com.softmotions.commons;

import java.io.InputStream;
import java.net.URL;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class TestServicesConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TestServicesConfiguration.class);

    @Test
    public void test1() throws Exception {
        URL res = getClass().getClassLoader().getResource("com/softmotions/commons/services1.xml");
        assertNotNull(res);
        ServicesConfiguration scfg = new ServicesConfiguration(res);
        HierarchicalConfiguration<ImmutableNode> xcfg = scfg.xcfg();
        assertNotNull(xcfg);
        assertEquals(System.getProperty("user.dir"), xcfg.getString("cwd"));
        assertEquals(System.getProperty("user.home"), xcfg.getString("home"));
        assertEquals(System.getProperty("user.home"), xcfg.getString("sys1"));
        assertEquals(System.getProperty("user.home"), xcfg.getString("env1"));
        assertEquals(scfg.getTmpdir().getAbsolutePath(), xcfg.getString("tmp"));
        assertEquals("{zzz}", xcfg.getString("zzz"));

    }
}
