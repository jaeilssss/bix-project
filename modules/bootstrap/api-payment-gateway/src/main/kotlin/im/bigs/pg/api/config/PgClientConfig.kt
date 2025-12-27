package im.bigs.pg.api.config

import im.bigs.pg.external.pg.MockPgClient
import im.bigs.pg.external.pg.TestPgClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class PgClientConfig(
    @Value("\${pg.test-pg.url}")
    private val testPgBaseUrl: String,
    @Value("\${pg.test-pg.api-key}")
    private val testPgApiKey: String,
    private val webClient: WebClient
) {

    @Bean
    fun testPgClient() = TestPgClient(testPgBaseUrl, webClient, testPgApiKey)

}