package im.bigs.pg.application.exception.enums

enum class PaymentError(val httpStatusCode: Int, val message: String) {
    NOT_FOUND_FEE_POLICY(404, "해당 제휴사의 수수료 정책의 데이터가 존재하지 않습니다"),
    NOT_FOUND_PARTNER_ID(404, "해당 Partner Id로 조회된 파트너가 없습니다."),
    NOT_ACTIVE_PARTNER(400, "해당 파트너는 비활성 파트너 입니다."),
    NEGATIVE_NET_AMOUNT(400, "정산 금액이 0보다 작을 수 없습니다."),
    NO_PG_CLIENT_FOR_PARTNER(404, "해당 Partner ID에 맞는 PG Client 찾을 수 없습니다");
}
