package no.nav.melosys.skjema.integrasjon.storage

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Service
@Profile("!local-q1")
class GcpStorageClient(
    @param:Value("\${vedlegg.bucket-name}") private val bucketName: String
) : VedleggStorageClient {

    private val storage: Storage = StorageOptions.getDefaultInstance().service

    override fun lastOpp(storageReferanse: String, data: ByteArray, contentType: String) {
        log.info { "Laster opp vedlegg til GCP Storage: $storageReferanse" }
        val blobId = BlobId.of(bucketName, storageReferanse)
        val blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType(contentType)
            .build()
        storage.create(blobInfo, data)
    }

    override fun slett(storageReferanse: String) {
        log.info { "Sletter vedlegg fra GCP Storage: $storageReferanse" }
        val blobId = BlobId.of(bucketName, storageReferanse)
        storage.delete(blobId)
    }

    override fun hent(storageReferanse: String): ByteArray {
        log.info { "Henter vedlegg fra GCP Storage: $storageReferanse" }
        val blobId = BlobId.of(bucketName, storageReferanse)
        return storage.readAllBytes(blobId)
    }
}
