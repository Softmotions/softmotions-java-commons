package com.softmotions.weboot.repository.s3

import com.amazonaws.AmazonServiceException
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.Bucket
import com.amazonaws.services.s3.model.ObjectMetadata
import com.google.inject.Inject
import com.google.inject.Singleton
import com.softmotions.commons.ServicesConfiguration
import com.softmotions.kotlin.loggerFor
import com.softmotions.kotlin.toPath
import com.softmotions.weboot.repository.WBRepository
import java.io.*
import java.net.URI

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

    override fun fetchFileName(uri: URI): String {
        return uri.path?.toPath()?.fileName?.toString() ?: throw IOException("Invalid uri specified")
    }

    override fun persist(input: InputStream, fname: String): URI {
        var tmpFile: File? = null
        OverflowOutputStream(1024) {
            tmpFile = File.createTempFile("ovf-", null)
            tmpFile!!.outputStream()
        }.use { ovf ->
            input.transferTo(ovf)
            ovf.flush()
            if (tmpFile != null) {
                s3.putObject(bucket.name, fname, tmpFile)
            } else {
                s3.putObject(bucket.name, fname, ovf.memoryToByteArrayInputStream(), ObjectMetadata().apply {
                    contentLength = ovf.length
                })
            }
        }
        tmpFile?.delete()
        return URI("s3", bucket.name, "/$fname", null)
    }

    override fun remove(uri: URI): Boolean {
        val key = fetchFileName(uri)
        s3.deleteObject(bucket.name, key)
        return true
    }

    override fun transferTo(uri: URI, output: OutputStream) {
        val key = fetchFileName(uri)
        try {
            s3.getObject(bucket.name, key).use { s3o ->
                s3o.objectContent.transferTo(output)
            }
        } catch (e: AmazonServiceException) {
            throw IOException("Request error: ${e.errorCode}")
        }
    }
}