package im.bigs.pg.infra.persistence

import im.bigs.pg.infra.persistence.config.JpaConfig
import im.bigs.pg.infra.persistence.payment.entity.PaymentEntity
import im.bigs.pg.infra.persistence.payment.repository.PaymentJpaRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ContextConfiguration
import java.math.BigDecimal
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DataJpaTest
@ContextConfiguration(classes = [JpaConfig::class])
class PaymentRepositoryPagingTest @Autowired constructor(
    val paymentRepo: PaymentJpaRepository,
) {
    private fun payment(partnerId: Long, createdAt: Instant): PaymentEntity {
        return PaymentEntity(
            partnerId = partnerId,
            amount = BigDecimal("1000"),
            appliedFeeRate = BigDecimal("0.03"),
            feeAmount = BigDecimal("30"),
            netAmount = BigDecimal("970"),
            cardBin = "123456",
            cardLast4 = "4242",
            approvalCode = "approve123",
            approvedAt = createdAt,
            status = "APPROVED",
            createdAt = createdAt,
            updatedAt = createdAt
        )
    }

    @Test
    @DisplayName("커서 페이징과 통계가 일관되어야 한다")
    fun `커서 페이징과 통계가 일관되어야 한다`() {
        val baseTs = Instant.parse("2024-01-01T00:00:00Z")
        repeat(35) { i ->
            paymentRepo.save(
                PaymentEntity(
                    partnerId = 1L,
                    amount = BigDecimal("1000"),
                    appliedFeeRate = BigDecimal("0.0300"),
                    feeAmount = BigDecimal("30"),
                    netAmount = BigDecimal("970"),
                    cardBin = "123456",
                    cardLast4 = "%04d".format(i),
                    approvalCode = "A$i",
                    approvedAt = baseTs.plusSeconds(i.toLong()),
                    status = "APPROVED",
                    createdAt = baseTs.plusSeconds(i.toLong()),
                    updatedAt = baseTs.plusSeconds(i.toLong()),
                ),
            )
        }

        val first = paymentRepo.pageBy(1L, "APPROVED", null, null, null, null, PageRequest.of(0, 21))
        assertEquals(21, first.size)
        val lastOfFirst = first[20]
        val second = paymentRepo.pageBy(
            1L, "APPROVED", null, null,
            lastOfFirst.createdAt, lastOfFirst.id, PageRequest.of(0, 21),
        )
        assertTrue(second.isNotEmpty())

        val sumList = paymentRepo.summary(1L, "APPROVED", null, null)
        val row = sumList.first()
        assertEquals(35L, (row[0] as Number).toLong())
        assertEquals(BigDecimal("35000"), row[1] as BigDecimal)
        assertEquals(BigDecimal("33950"), row[2] as BigDecimal)
    }

    @Test
    fun `커서 기반으로 다음 페이지를 조회한다`() {
        // given
        val partnerId = 1L
        val baseTime = Instant.parse("2025-01-01T00:00:00Z")
        val fromTime = Instant.parse("2025-01-01T00:00:00Z")
        val toTime = Instant.parse("2028-12-31T00:00:00Z")
        val payments = listOf(
            payment(partnerId, baseTime.plusSeconds(3)), // 최신
            payment(partnerId, baseTime.plusSeconds(2)),
            payment(partnerId, baseTime.plusSeconds(1)),
            payment(partnerId, baseTime) // 가장 오래됨
        )

        paymentRepo.saveAll(payments)

        // when - 첫 페이지 조회
        val firstPage = paymentRepo.pageBy(
            partnerId = partnerId,
            cursorCreatedAt = null,
            status = null,
            cursorId = null,
            fromAt = fromTime,
            toAt = toTime,
            org = PageRequest.of(0, 2)
        )

        // then - 첫 페이지 검증
        assertEquals(2, firstPage.size)
        assertTrue(
            firstPage[0].createdAt.isAfter(firstPage[1].createdAt)
        )
        val lastOfFirstPage = firstPage.last()

        // when - 두 번째 페이지 조회 (커서 사용)
        val secondPage = paymentRepo.pageBy(
            partnerId = partnerId,
            cursorCreatedAt = lastOfFirstPage.createdAt,
            cursorId = lastOfFirstPage.id,
            fromAt = fromTime,
            toAt = toTime,
            status = null,
            org = PageRequest.of(0, 2)
        )

        // then - 두 번째 페이지 검증
        assertEquals(2, secondPage.size)

        // 중복 데이터 없음
        val firstIds = firstPage.map { it.id }.toSet()
        val secondIds = secondPage.map { it.id }.toSet()
        assertTrue(
            firstIds.intersect(secondIds).isEmpty(),
            "커서 기반 페이지네이션에서는 페이지 간 데이터가 겹치면 안 된다"
        )
        // 전체 순서 검증 (DESC)
        val all = firstPage + secondPage
        val ids = all.map { it.id!! }

        assertEquals(
            ids.size,
            ids.distinct().size,
            "결제 ID에 중복이 존재하면 안 됩니다"
        )
    }
}
