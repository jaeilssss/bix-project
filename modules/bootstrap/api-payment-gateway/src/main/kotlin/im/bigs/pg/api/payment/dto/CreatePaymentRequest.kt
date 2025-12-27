package im.bigs.pg.api.payment.dto

import jakarta.validation.constraints.Min
import java.math.BigDecimal

data class CreatePaymentRequest(
    val partnerId: Long,
    @field:Min(1)
    val amount: BigDecimal,
    val cardNumber: String?,
    val productName: String? = null,
    val birthDate: String? = null,
    val expiry: String? = null,
    val password: String? = null
)

