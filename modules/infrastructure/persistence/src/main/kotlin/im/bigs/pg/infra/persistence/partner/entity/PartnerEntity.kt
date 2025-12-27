package im.bigs.pg.infra.persistence.partner.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * DB용 Partner 엔티티.
 * - 도메인 모델과 1:1이 아니어도 되며, 저장 기술스택에 맞춘 컬럼/제약을 가질 수 있습니다.
 */
@Entity
@Table(name = "partner")
class PartnerEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
    @Column(nullable = false, unique = true)
    lateinit var code: String
    @Column(nullable = false)
    lateinit var name: String
    @Column(nullable = false)
    var active: Boolean = true

    constructor(id: Long, code: String, name: String, active: Boolean) {
        this.id = id
        this.code = code
        this.name = name
        this.active = active
    }
    constructor(code: String, name: String, active: Boolean) {
        this.code = code
        this.name = name
        this.active = active
    }
    constructor()
}
