package im.bigs.pg.external.pg

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.external.PgKeyDeriver
import im.bigs.pg.external.encrypt.Aes256GcmEncryptor
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

class TestPgClient(
    val baseUrl: String,
    val webClient: WebClient,
    val apiKey: String

) : PgClientOutPort {

    companion object {
        private const val PAY_CREDIT_CARD_PATH = "/api/v1/pay/credit-card"
        private const val HEADER_NAME = "API-KEY"
    }

    override fun supports(partnerId: Long): Boolean = partnerId == 2L

    override fun approve(request: PgApproveRequest): PgApproveResult {
        val json = ObjectMapper().registerKotlinModule().writeValueAsString(request)
        val secretKey = PgKeyDeriver.deriveAes256Key(apiKey)
        return webClient.post()
            .uri(baseUrl.plus(PAY_CREDIT_CARD_PATH))
            .header(HEADER_NAME, apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("enc" to Aes256GcmEncryptor.encrypt(json, secretKey)))
            .retrieve()
            .onStatus({it.is4xxClientError}) {response ->
                response.bodyToMono(String::class.java)
                    .map { throw IllegalArgumentException("PG 승인 실패 4xx") }
            }
            .onStatus({it.is5xxServerError}) {response ->
                response.bodyToMono(String::class.java)
                    .map { throw IllegalArgumentException("PG 서버 오류 5xx") }
            }
            .bodyToMono(PgApproveResult::class.java)
            .block()
            ?: throw IllegalArgumentException("Payment request failed.")
    }
}
