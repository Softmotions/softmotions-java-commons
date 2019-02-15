package com.softmotions.weboot.repository.s3

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.Bucket
import com.google.inject.Inject
import com.google.inject.Singleton
import com.softmotions.commons.ServicesConfiguration
import com.softmotions.kotlin.loggerFor
import com.softmotions.kotlin.toPath
import com.softmotions.weboot.repository.WBRepository
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import kotlin.NullPointerException

@Singleton
class AWSS3Repository
@Inject
constructor(env: ServicesConfiguration) : WBRepository {

    companion object {
        private val log = loggerFor()
    }

    private var s3: AmazonS3

    private var bucket: Bucket

    init {
        val xcfg = env.xcfg()
        val bucketName = xcfg["repository.s3.bucket"]!!
        val region = xcfg["repository.s3.region"]!!

        s3 = AmazonS3ClientBuilder.standard().apply {
            if (xcfg.hasPattern("repository.s3.test")) {
                val eCfg = AwsClientBuilder.EndpointConfiguration(xcfg["repository.s3.test.endpoint"], region)
                withPathStyleAccessEnabled(true)
                withEndpointConfiguration(eCfg)
                withCredentials(AWSStaticCredentialsProvider(AnonymousAWSCredentials()))
            } else {
                withRegion(region)
            }
        }.build()

        bucket = if (s3.doesBucketExistV2(bucketName)) {
            s3.listBuckets().last { bucket -> bucket.name == bucketName }
        } else {
            s3.createBucket(bucketName)
        }
    }

    override fun acceptUri(uri: URI): Boolean {
        return "s3" == uri.scheme && uri.host == bucket.name
    }

    override fun fetchFileName(uri: URI): String = uri.path.toPath().fileName.toString()

    override fun persist(input: InputStream, fname: String): URI {
        s3.putObject(bucket.name, fname, input, null)
        return URI("s3", bucket.name, "/$fname", null)
    }

    override fun remove(uri: URI): Boolean {
        return if (s3.doesObjectExist(bucket.name, fetchFileName(uri))) {
            s3.deleteObject(bucket.name, fetchFileName(uri))
            true
        } else {
            false
        }
    }

    override fun transferTo(uri: URI, output: OutputStream) {
        val key = fetchFileName(uri)
        if (s3.doesObjectExist(bucket.name, key)) {
            s3.getObject(bucket.name, key).use { s3o ->
                s3o.objectContent.transferTo(output)
            }
        } else {
            throw NullPointerException("Missing repository key: '$key'")
        }
    }
}