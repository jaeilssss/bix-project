package im.bigs.pg.application.exception


class BaseException(val httpStatusCode: Int, val errorMessage: String): RuntimeException() {

}