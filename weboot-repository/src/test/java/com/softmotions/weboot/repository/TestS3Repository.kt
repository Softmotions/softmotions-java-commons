package com.softmotions.weboot.repository

import com.google.inject.Guice
import com.google.inject.Stage
import io.findify.s3mock.S3Mock
import org.testng.Assert
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI

@Test
class TestS3Repository : BaseTest() {

    private val cfgLocation: String = "conf/aws3-test-configuration.xml"
    private lateinit var s3Mock: S3Mock

    @BeforeClass
    fun before() {
        s3Mock = S3Mock.Builder().withPort(8001)
                .withInMemoryBackend()
                .build()
        s3Mock.start()
        env = TestEnv(cfgLocation)
        injector = Guice.createInjector(Stage.PRODUCTION, env)
    }

    @AfterClass
    fun after() {
        s3Mock.stop()
    }

    @Test
    fun test1Upload() {
        val rep = getRepositoryByName("aws3")
        val data = "24a4c735b3f1"
        val key = "7bf4"

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
        val rep = getRepositoryByName("aws3")
        // c523fa90-7bf4-480f-a1d5-24a4c735b3f1
        // 5760e3d7-7d24-4030-a663-84955e06c7fd
        val data1 = "24a4c735b3f1"
        val data2 = "84955e06c7fd"
        val key = "480f"

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
        val rep = getRepositoryByName("aws3")
        val data = "24a4c735b3f1"
        val key = "a1d5"

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
            Assert.assertThrows(IOException::class.java) {
                rep.transferTo(uri, output)
            }
        }
    }
}