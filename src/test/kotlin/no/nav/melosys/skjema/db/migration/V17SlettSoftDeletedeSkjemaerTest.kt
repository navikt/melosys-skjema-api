package no.nav.melosys.skjema.db.migration

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.UUID
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * Verifiserer engangs-oppryddingen V17 (ren SQL): hard delete av SLETTET-rader (med cascade paa
 * vedlegg/innsending/fullmakt) og innstramming av status-constraint.
 *
 * Schemaet migreres foerst til og med V16 (status-constraint inkluderer fortsatt SLETTET), slik at
 * vi kan seede soft-deletede data, og deretter kjoeres V17 for aa rydde dem.
 *
 * NB: V17 sletter ikke vedlegg-blobs i bucket - det er en separat manuell jobb (se SQL-kommentar).
 * Testen verifiserer derfor kun DB-tilstanden.
 */
class V17SlettSoftDeletedeSkjemaerTest {

    @Test
    fun `sletter soft-deletede skjemaer med cascade og strammer status-constraint`() {
        PostgreSQLContainer(DockerImageName.parse("postgres:17")).use { postgres ->
            postgres.start()

            // Migrer schema til og med V16 (foer innstrammingen i V17)
            Flyway.configure()
                .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("16"))
                .load()
                .migrate()

            val skjemaId = UUID.randomUUID()
            val vedleggId = UUID.randomUUID()
            val storageReferanse = "skjemaer/$skjemaId/vedlegg/$vedleggId/fil.pdf"

            connection(postgres).use { conn ->
                conn.createStatement().use { st ->
                    st.execute(
                        """
                        INSERT INTO skjema (id, status, type, fnr, orgnr, metadata, opprettet_av, endret_av)
                        VALUES ('$skjemaId', 'SLETTET', 'UTSENDT_ARBEIDSTAKER', '01816023404', '123456789',
                                '{"representasjonstype":"DEG_SELV"}'::jsonb, '01816023404', '01816023404')
                        """.trimIndent()
                    )
                    st.execute(
                        """
                        INSERT INTO vedlegg (id, skjema_id, filnavn, original_filnavn, filtype, filstorrelse, storage_referanse, opprettet_av)
                        VALUES ('$vedleggId', '$skjemaId', 'fil.pdf', 'fil.pdf', 'PDF', 123, '$storageReferanse', '01816023404')
                        """.trimIndent()
                    )
                }
            }

            // Kjoer V17
            Flyway.configure()
                .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
                .locations("classpath:db/migration")
                .load()
                .migrate()

            connection(postgres).use { conn ->
                conn.createStatement().use { st ->
                    st.executeQuery("SELECT COUNT(*) FROM skjema").use { rs ->
                        rs.next()
                        assertEquals(0, rs.getInt(1), "Soft-deletede skjema-rader skal vaere slettet")
                    }
                    st.executeQuery("SELECT COUNT(*) FROM vedlegg").use { rs ->
                        rs.next()
                        assertEquals(0, rs.getInt(1), "Vedlegg-rader skal cascade-slettes")
                    }
                }

                // Constraint skal avvise SLETTET etter innstramming
                assertThrows(SQLException::class.java) {
                    conn.createStatement().use { st ->
                        st.execute(
                            """
                            INSERT INTO skjema (id, status, type, fnr, orgnr, metadata, opprettet_av, endret_av)
                            VALUES ('${UUID.randomUUID()}', 'SLETTET', 'UTSENDT_ARBEIDSTAKER', '01816023404', '123456789',
                                    '{"representasjonstype":"DEG_SELV"}'::jsonb, '01816023404', '01816023404')
                            """.trimIndent()
                        )
                    }
                }
            }
        }
    }

    private fun connection(postgres: PostgreSQLContainer<*>): Connection =
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password)
}

