package com.softmotions.weboot.repository

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Stage
import io.findify.s3mock.S3Mock
import org.testng.Assert
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.io.ByteArrayOutputStream
import java.lang.NullPointerException
import java.net.URI

@Test
class TestS3Repository {

    private val cfgLocation: String = "conf/repository-test-configuration.xml"
    private lateinit var injector: Injector
    private lateinit var env: TestEnv
    private lateinit var s3Mock: S3Mock

    @BeforeClass
    fun before() {
        s3Mock = S3Mock.Builder().withPort(8001).withInMemoryBackend().build()
        s3Mock.start()
        env = TestEnv(cfgLocation)
        injector = Guice.createInjector(Stage.PRODUCTION, env)
    }

    @Test
    fun test1Upload() {
        val rep = injector.getInstance(WBRepository::class.java)
        val data = "TEST TEXT"
        val key = "key1.txt"

        val uri = data.byteInputStream().use { input ->
            rep.persist(input, key)
        }

        // test filename
        Assert.assertEquals(key, rep.fetchFileName(uri))

        ByteArrayOutputStream().use { output ->
            rep.transferTo(uri, output)
            Assert.assertEquals(data.toByteArray(), output.toByteArray())
        }

        // test uri
        Assert.assertTrue(rep.acceptUri(uri))
        Assert.assertTrue(rep.acceptUri(URI("s3", env.xcfg()["repository.s3.bucket"], "/badobject", null)))
        Assert.assertFalse(rep.acceptUri(URI("s3", "badbucket", "/badobject", null)))
    }

    @Test
    fun test2Replace() {
        val rep = injector.getInstance(WBRepository::class.java)
        val data1 = "TEST TEXT"
        val data2 = "TEST TEXT 2"
        val key = "key2.txt"

        val uri1 = data1.byteInputStream().use { input ->
            rep.persist(input, key)
        }

        ByteArrayOutputStream().use { output ->
            rep.transferTo(uri1, output)
            Assert.assertEquals(data1.toByteArray(), output.toByteArray())
        }

        val uri2 = data2.byteInputStream().use { input ->
            rep.persist(input, key)
        }

        // test, uri no change
        Assert.assertEquals(uri1, uri2)

        ByteArrayOutputStream().use { output ->
            rep.transferTo(uri1, output)
            val res = output.toByteArray()
            Assert.assertNotEquals(data1.toByteArray(), res)
            Assert.assertEquals(data2.toByteArray(), res)
        }
    }

    @Test
    fun test3Remove() {
        val rep = injector.getInstance(WBRepository::class.java)
        val data = "TEST TEXT"
        val key = "key3.txt"

        val uri = data.byteInputStream().use { input ->
            rep.persist(input, key)
        }

        // check data
        ByteArrayOutputStream().use { output ->
            rep.transferTo(uri, output)
            Assert.assertEquals(data.toByteArray(), output.toByteArray())
        }

        // remove data
        Assert.assertTrue(rep.remove(uri))

        // check data
        ByteArrayOutputStream().use { output ->
            Assert.assertThrows(NullPointerException::class.java) {
                rep.transferTo(uri, output)
            }
        }
    }

    @AfterClass
    fun after() {
        s3Mock.stop()
    }
}