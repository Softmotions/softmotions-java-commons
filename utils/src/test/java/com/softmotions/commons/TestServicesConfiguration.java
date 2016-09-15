package com.softmotions.commons;

import java.net.URL;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.testng.annotations.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.testng.Assert;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class TestServicesConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TestServicesConfiguration.class);

    @Test
    public void test1() throws Exception {
        URL res = getClass().getClassLoader().getResource("com/softmotions/commons/services1.xml");
        Assert.assertNotNull(res);
        ServicesConfiguration scfg = new ServicesConfiguration(res);
        HierarchicalConfiguration<ImmutableNode> xcfg = scfg.xcfg();
        Assert.assertNotNull(xcfg);
        Assert.assertEquals(System.getProperty("user.dir"), xcfg.getString("cwd"));
        Assert.assertEquals(System.getProperty("user.home"), xcfg.getString("home"));
        Assert.assertEquals(System.getProperty("user.home"), xcfg.getString("sys1"));
        Assert.assertEquals(System.getProperty("user.home"), xcfg.getString("env1"));
        Assert.assertEquals(scfg.getTmpdir().getAbsolutePath(), xcfg.getString("tmp"));
        Assert.assertEquals("{zzz}", xcfg.getString("zzz"));

    }
}
