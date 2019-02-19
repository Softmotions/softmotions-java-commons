package com.softmotions.weboot.repository

import com.google.inject.Guice
import com.google.inject.Stage
import com.softmotions.kotlin.toFile
import org.apache.commons.io.FileUtils
import org.testng.Assert
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.URI

class TestFSRepository : BaseTest() {

    private val cfgLocation: String = "conf/fs-test-configuration.xml"
    private lateinit var root: File

    @BeforeClass
    fun before() {
        val env = TestEnv(cfgLocation)
        injector = Guice.createInjector(Stage.PRODUCTION, env)
        root = env.xcfg()["repository.fs.root"]!!.toFile()
    }

    @AfterClass
    fun after() {
        FileUtils.deleteQuietly(root)
    }

    @Test
    fun test1Base() {
        // 5bc12b3c-6569-44ca-9ce9-d1e791583278
        val rep = getRepositoryByName("fs")
        val data = "5bc12b3c-6569-44ca-9ce9-d1e791583278".toByteArray()
        val key = "5bc12b3c"

        // test persist, transferTo
        val uri = data.inputStream().use {
            rep.persist(it, key)
        }
        Assert.assertTrue(rep.acceptUri(uri))
        ByteArrayOutputStream().use { output ->
            rep.transferTo(uri, output)
            Assert.assertEquals(data, output.toByteArray())
        }

        // test uri
        Assert.assertFalse(rep.acceptUri(URI("aa", "5bc12b3c", null)))
        Assert.assertTrue(rep.acceptUri(URI("fs", "5bc12b3c", null)))
        Assert.assertEquals(key, rep.fetchFileName(uri))

        // test bad uri
        ByteArrayOutputStream().use { output ->
            Assert.assertThrows(IOException::class.java) {
                rep.transferTo(URI("fs", "5bc12b3c", null), output)
            }
        }

        // test files
        val f = root.resolve(uri.schemeSpecificPart)
        Assert.assertTrue(f.exists())
        f.inputStream().use { input ->
            Assert.assertEquals(data, input.readAllBytes())
        }
    }

    @Test
    fun test2Remove() {
        // a0f72a11-903d-4ef7-a659-cd815147278e
        val rep = getRepositoryByName("fs")
        val data = "a0f72a11-903d-4ef7-a659-cd815147278e".toByteArray()
        val key = "a0f72a11"

        val uri = data.inputStream().use {
            rep.persist(it, key)
        }
        ByteArrayOutputStream().use { output ->
            rep.transferTo(uri, output)
            Assert.assertEquals(data, output.toByteArray())
        }
        val f = root.resolve(uri.schemeSpecificPart)
        Assert.assertTrue(f.exists())
        rep.remove(uri)
        Assert.assertFalse(f.exists())
        ByteArrayOutputStream().use { output ->
            Assert.assertThrows(IOException::class.java) {
                rep.transferTo(uri, output)
            }
        }
    }
}