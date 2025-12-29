package im.bigs.pg.application.payment.service

import im.bigs.pg.application.exception.BaseException
import im.bigs.pg.application.exception.enums.PaymentError
import im.bigs.pg.application.partner.port.out.FeePolicyOutPort
import im.bigs.pg.application.partner.port.out.PartnerOutPort
import im.bigs.pg.application.payment.port.`in`.PaymentCommand
import im.bigs.pg.application.payment.port.`in`.PaymentUseCase
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.domain.calculation.FeeCalculator
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * 결제 생성 유스케이스 구현체.
 * - 입력(REST 등) → 도메인/외부PG/영속성 포트를 순차적으로 호출하는 흐름을 담당합니다.
 * - 수수료 정책 조회 및 적용(계산)은 도메인 유틸리티를 통해 수행합니다.
 */
@Service
class PaymentService(
    private val partnerRepository: PartnerOutPort,
    private val feePolicyRepository: FeePolicyOutPort,
    private val paymentRepository: PaymentOutPort,
    private val pgClients: List<PgClientOutPort>,
) : PaymentUseCase {
    /**
     * 결제 승인/수수료 계산/저장을 순차적으로 수행합니다.
     * - 현재 예시 구현은 하드코드된 수수료(3% + 100)로 계산합니다.
     * - 과제: 제휴사별 수수료 정책을 적용하도록 개선해 보세요.
     */
    override fun pay(command: PaymentCommand): Payment {
        val partner = partnerRepository.findById(command.partnerId)
            ?: throw BaseException(
                PaymentError.NOT_FOUND_PARTNER_ID.httpStatusCode,
                PaymentError.NOT_FOUND_PARTNER_ID.message
            )
        require(partner.active) {
            throw BaseException(
                PaymentError.NOT_ACTIVE_PARTNER.httpStatusCode,
                PaymentError.NOT_ACTIVE_PARTNER.message
            )
        }

        val pgClient = pgClients.firstOrNull { it.supports(partner.id) }
            ?: throw BaseException(
                PaymentError.NO_PG_CLIENT_FOR_PARTNER.httpStatusCode,
                PaymentError.NO_PG_CLIENT_FOR_PARTNER.message
            )

        val approve = pgClient.approve(
            PgApproveRequest(
                amount = command.amount,
                cardNumber = command.cardNumber,
                birthDate = command.birthDate,
                expiry = command.expiry,
                password = command.password
            ),
        )

        try {
            val paymentFeePolicy = feePolicyRepository.findEffectivePolicy(command.partnerId, LocalDateTime.now())
                ?: throw BaseException(
                    PaymentError.NOT_FOUND_FEE_POLICY.httpStatusCode,
                    PaymentError.NOT_FOUND_FEE_POLICY.message
                )

            val (fee, net) = FeeCalculator.calculateFee(command.amount, paymentFeePolicy.percentage, paymentFeePolicy.fixedFee)
            val approvedAtKst = approve.approvedAt
                .atZone(ZoneOffset.UTC)
                .withZoneSameInstant(ZoneId.of("Asia/Seoul"))
                .toLocalDateTime()
            val payment = Payment(
                partnerId = partner.id,
                amount = command.amount,
                appliedFeeRate = paymentFeePolicy.percentage,
                feeAmount = fee,
                netAmount = net,
                cardBin = command.cardNumber?.replace("-", "")?.substring(0, 6),
                cardLast4 = command.cardNumber?.takeLast(4),
                approvalCode = approve.approvalCode,
                approvedAt = approvedAtKst,
                status = PaymentStatus.APPROVED,
            )

            return paymentRepository.save(payment)
        } catch (e: Exception) {
            /***
             * 만약 제휴사별 수수료 정책 조회 및 payment 저장 중 에러 발생 시
             * 이미 결제 진행 된 PG 결제 내역 환불 진행 프로세스 진행
             */
            throw e
        }
    }
}
