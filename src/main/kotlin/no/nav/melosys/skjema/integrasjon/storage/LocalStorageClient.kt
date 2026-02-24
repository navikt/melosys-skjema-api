package no.nav.melosys.skjema.integrasjon.storage

import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Service
@Profile("local-q1")
class LocalStorageClient : VedleggStorageClient {

    private val baseDir: Path = Path.of("/tmp/melosys-vedlegg")

    init {
        Files.createDirectories(baseDir)
    }

    override fun lastOpp(storageReferanse: String, data: ByteArray, contentType: String) {
        val filePath = baseDir.resolve(storageReferanse)
        Files.createDirectories(filePath.parent)
        Files.write(filePath, data)
        log.info { "Lokal lagring: Vedlegg lagret til $filePath" }
    }

    override fun slett(storageReferanse: String) {
        val filePath = baseDir.resolve(storageReferanse)
        Files.deleteIfExists(filePath)
        log.info { "Lokal lagring: Vedlegg slettet fra $filePath" }
    }

    override fun hent(storageReferanse: String): ByteArray {
        val filePath = baseDir.resolve(storageReferanse)
        log.info { "Lokal lagring: Henter vedlegg fra $filePath" }
        return Files.readAllBytes(filePath)
    }
}
