package im.bigs.pg.api.payment.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.math.BigDecimal

@Schema(description = "결제 생성 요청")
data class CreatePaymentRequest(
    @field:Schema(description = "제휴사 ID 1은MockId, 2는 TestId", example = "2")
    val partnerId: Long,
    @field:Min(1)
    val amount: BigDecimal,
    @field:Schema(description = "결제 카드 번호", example = "1111-1111-1111-1111")
    val cardNumber: String?,
    @field:Schema(description = "결제 상품명", example = "샘플")
    val productName: String? = null,
    @field:Schema(description = "결제 카드 소유자 생년월일", example = "19900101")
    val birthDate: String? = null,
    @field:Schema(description = "결제 카드 유효기간", example = "1227")
    val expiry: String? = null,
    @field:Schema(description = "결제 카드 비밀번호 앞 2자리", example = "12")
    @field:Size(min = 2, max = 2)
    val password: String? = null
)
