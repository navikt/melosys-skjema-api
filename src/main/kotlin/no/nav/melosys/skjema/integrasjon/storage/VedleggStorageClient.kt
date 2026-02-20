package no.nav.melosys.skjema.integrasjon.storage

interface VedleggStorageClient {
    fun lastOpp(storageReferanse: String, data: ByteArray, contentType: String)
    fun slett(storageReferanse: String)
    fun hent(storageReferanse: String): ByteArray
}
