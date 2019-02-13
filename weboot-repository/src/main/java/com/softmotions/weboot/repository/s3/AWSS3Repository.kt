package com.softmotions.weboot.repository.s3

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.google.inject.Inject
import com.softmotions.commons.ServicesConfiguration
import com.softmotions.kotlin.loggerFor
import com.softmotions.weboot.repository.WBRepository
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.util.concurrent.atomic.AtomicLong

class AWSS3Repository
@Inject
constructor(env: ServicesConfiguration) : WBRepository {

    companion object {
        private val log = loggerFor()
    }

    private var s3: AmazonS3

    private var bucketName: String

    private val seq = AtomicLong(0)

    init {
        val xcfg = env.xcfg()
        bucketName = xcfg["repository.s3.bucket"]!!
        val region = xcfg["repository.s3.region"]!!

        s3 = AmazonS3ClientBuilder.standard().apply {
            if ((xcfg["repository.s3.test.endpoint"] ?: "").isBlank()) {
                withEndpointConfiguration(
                        AwsClientBuilder.EndpointConfiguration(xcfg["repository.s3.test.endpoint"], region)
                )
                withPathStyleAccessEnabled(true)
                withCredentials(AWSStaticCredentialsProvider(AnonymousAWSCredentials()))
            } else {
                withRegion(region)
            }
        }.build()

        log.info("Connected to S3 cloud, region: {}", region)
        if (!s3.doesBucketExistV2(bucketName)) {
            log.warn("Bucket $bucketName doesn't exists trying to create it")
            try {
                s3.createBucket(bucketName)
            } catch (e: Exception) {
                log.error("Failed to create bucket: '$bucketName'", e)
            }
        }

        if (!s3.doesObjectExist(bucketName, "0")) {
            s3.putObject(bucketName, "0", seq.get().toString())
        } else {
            seq.set(s3.getObject(bucketName, "0").objectContent.readAllBytes().joinToString().toLong())
        }
    }

    override fun acceptUri(uri: URI): Boolean {
        return uri.scheme == "s3"
    }

    override fun persist(input: InputStream, fname: String?): URI {
        val next = seq.incrementAndGet().let {
            if (fname != null) {
                "$it/$fname"
            } else {
                "$it"
            }
        }

        s3.putObject(bucketName, next, input, null)
        return URI("s3", next, null)
    }

    override fun remove(uri: URI): Boolean {
        return if (s3.doesObjectExist(bucketName, uri.schemeSpecificPart)) {
            s3.deleteObject(bucketName, uri.schemeSpecificPart)
            true
        } else {
            false
        }
    }

    override fun transferTo(uri: URI, output: OutputStream) {
        s3.getObject(bucketName, uri.schemeSpecificPart).use { s3o ->
            s3o.objectContent.transferTo(output)
        }
    }
}