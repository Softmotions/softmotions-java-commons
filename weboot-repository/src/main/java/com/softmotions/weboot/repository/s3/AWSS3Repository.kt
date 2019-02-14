package com.softmotions.weboot.repository.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.Bucket
import com.google.inject.Inject
import com.google.inject.Singleton
import com.softmotions.commons.ServicesConfiguration
import com.softmotions.kotlin.loggerFor
import com.softmotions.weboot.repository.WBRepository
import java.io.InputStream
import java.io.OutputStream
import java.net.URI

// 1 понял 2 тех план 3 минимальная реализация + мин тест 4. фантики?


//fun acceptUri(uri: URI): Boolean
//
//fun persist(input: InputStream, fname: String? = null): URI
//
//fun remove(uri: URI): Boolean
//
//fun transferTo(uri: URI, output: OutputStream)

@Singleton
class AWSS3Repository
@Inject
constructor(env: ServicesConfiguration) : WBRepository {
    /**
     * s3://<bucketname>/<uuid>
     */

    companion object {
        private val log = loggerFor()
    }

    private var s3: AmazonS3

    // todo Есть ли S3 embedded server
    // todo Mokito

    private var bucket: Bucket // todo

    init {
        val xcfg = env.xcfg()
        val bucketName = xcfg["repository.s3.bucket"]!!
        val region = xcfg["repository.s3.region"]!!

        s3 = AmazonS3ClientBuilder.standard().apply {
            if (xcfg.hasPattern("repository.s3.test")) {

            } else {
                withRegion(region)
            }
        }.build()

        bucket = if (s3.doesBucketExistV2(bucketName)) {
            Bucket(bucketName) // TODO
        } else {
            s3.createBucket(bucketName)
        }
    }

    override fun acceptUri(uri: URI): Boolean {
        return "s3" == uri.scheme && uri.host == bucket.name
    }

    override fun fetchFileName(uri: URI): String = uri.path

    override fun persist(input: InputStream, fname: String): URI {
        s3.putObject(bucket.name, fname, input, null)
        return URI("s3", bucket.name, fname, null)
    }

    override fun remove(uri: URI): Boolean {
        return if (s3.doesObjectExist(bucket.name, uri.schemeSpecificPart)) {
            s3.deleteObject(bucket.name, uri.schemeSpecificPart)
            true
        } else {
            false
        }
    }

    override fun transferTo(uri: URI, output: OutputStream) {
        s3.getObject(bucket.name, uri.schemeSpecificPart).use { s3o ->
            s3o.objectContent.transferTo(output)
        }
    }
}