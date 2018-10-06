package com.softmotions.xconfig

import org.slf4j.LoggerFactory
import org.testng.Assert
import org.testng.annotations.Test
import java.nio.file.Files

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
@Test
class TestXConfig {

    companion object {
        val log = LoggerFactory.getLogger(TestXConfig::class.java)
    }

    @Test
    fun testBasic1() {
        val f = Files.createTempFile(null, null).toFile()
        log.info("File: $f")
        f.deleteOnExit()
        val cfg = XConfigBuilder(f.toURI().toURL())
                .allowWrites()
                .autosave(true)
                .create()

        Assert.assertNull(cfg.text("foo.bar"))
        Assert.assertNull(cfg["foo.bar"])
        cfg["foo.bar"] = "baz"
        Assert.assertEquals(cfg["foo.bar"], "baz")
        Assert.assertEquals(cfg.textXPath("foo/bar"), "baz")
        cfg["foo.bar"] = cfg.attr("attr", "attrval")
        Assert.assertEquals(cfg.textXPath("foo/bar[@attr]/@attr"), "attrval")

        val sub = cfg.sub("foo").first()
        Assert.assertNotNull(sub.parent)
        Assert.assertEquals(sub["bar"], "baz")
        sub["bar2"] = "bar2val"
        Assert.assertEquals(sub.textXPath("bar2"), "bar2val")

        val sub2 = cfg.sub("unknown")
        Assert.assertTrue(sub2.isEmpty())

        cfg.setAttrsXPath("foo/bar|foo/bar2", "one" to 1, "two" to 2)

        val sub3 = cfg.sub("foo.bar").first()
        Assert.assertEquals(sub3.textXPath("@attr"), "attrval")
        Assert.assertEquals(sub3.text("."), "baz")
        Assert.assertEquals(cfg.text("foo.bar[@attr]"), "attrval")
        Assert.assertEquals(sub3.text("[@two]"), "2")

        cfg["foo.bar[@attr]"] = "val2"
        Assert.assertEquals(cfg.text("foo.bar[@attr]"), "val2")

        log.info("File: ${f.readText()}")
    }

    @Test
    fun testMaster() {
        val f = Files.createTempFile(null, null).toFile()
        log.info("File: $f")
        f.deleteOnExit()
        val masterUrl = javaClass.getResource("master-config.xml")
        Assert.assertNotNull(masterUrl)
        val cfg = XConfigBuilder(f.toURI().toURL())
                .master(masterUrl) { XConfigBuilder.basicSubstitutor(it) }
                .allowWrites()
                .autosave(true)
                .create()
        Assert.assertEquals(cfg["master.home"], System.getProperty("user.home"))

        cfg["master.slave.data"] = "data"
        Assert.assertEquals(cfg["master.slave.data"], "data")

        cfg["master.home"] = "newhome"
        Assert.assertEquals(cfg["master.home"], "newhome")

        log.info("File: ${f.readText()}")
    }

    @Test
    fun testListConfig() {
        val url = javaClass.getResource("list-config.xml")
        val cfg = XConfigBuilder(url).create()
        Assert.assertEquals(cfg.text("foo.bar"), "one")
        val lp = cfg.listPattern("foo.bar")
        Assert.assertEquals(lp.size, 2)
        Assert.assertEquals(lp[0], "one")
        Assert.assertEquals(lp[1], "two")
    }
}