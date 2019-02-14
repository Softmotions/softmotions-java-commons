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
import java.util.*

@Test
class TestS3Repository {

    private val cfgLocation: String = "conf/repository-test-configuration.xml"
    private lateinit var injector: Injector
    private lateinit var s3Mock: S3Mock

    @BeforeClass
    fun before() {
        s3Mock = S3Mock.Builder().withPort(8001).withInMemoryBackend().build()
        s3Mock.start()
        val env = TestEnv(cfgLocation)
        injector = Guice.createInjector(Stage.PRODUCTION, env)
    }

    @Test
    fun test1() {
        val rep = injector.getInstance(WBRepository::class.java)

        val data = "TEST TEXT"

        val uri = data.byteInputStream().use { input ->
            rep.persist(input, "key")
        }

        ByteArrayOutputStream().use { output ->
            rep.transferTo(uri, output)
            Assert.assertEquals(data.toByteArray(), output.toByteArray())
        }
    }

    @AfterClass
    fun after() {
        s3Mock.stop()
    }
}