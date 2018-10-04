package com.softmotions.aconfig

import org.slf4j.LoggerFactory
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
    fun testCreate() {
        val f = Files.createTempFile(null, null).toFile()
        f.deleteOnExit()
        log.info("File: ${f}")
        val ac = AConfig(f, true)
        ac["foo.bar.baz"] = "bar"
        log.info("foo.bar.baz=" + ac["foo.bar.baz"])
        log.info("foo/bar/baz/text()=" + ac.string("foo/bar/baz/text()"))

        // todo
        log.info("data:\n${f.readText()}")
    }

}