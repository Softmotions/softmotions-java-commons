package com.softmotions.aconfig

import org.slf4j.LoggerFactory
import org.testng.Assert
import org.testng.annotations.Test
import java.nio.file.Files

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
@Test
class TestAConfig {

    companion object {
        val log = LoggerFactory.getLogger(TestAConfig::class.java)
    }

    @Test
    fun testBasic1() {
        val f = Files.createTempFile(null, null).toFile()
        log.info("File: $f")
        f.deleteOnExit()
        val cfg = AConfigBuilder(f.toURL())
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

        log.info("File: ${f.readText()}")
    }

    @Test
    fun testMaster() {
        val f = Files.createTempFile(null, null).toFile()
        log.info("File: $f")
        f.deleteOnExit()
        val masterUrl = javaClass.getResource("master-config.xml")
        Assert.assertNotNull(masterUrl)
        val cfg = AConfigBuilder(f.toURL())
                .master(masterUrl) { AConfigBuilder.basicSubstitutor(it) }
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
}