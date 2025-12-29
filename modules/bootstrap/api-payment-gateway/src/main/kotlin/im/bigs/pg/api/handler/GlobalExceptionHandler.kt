package im.bigs.pg.api.handler

import im.bigs.pg.application.exception.BaseException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    inner class ErrorResponse(val code: Int, val message: String)
    @ExceptionHandler(BaseException::class)
    fun handleBaseException(e: BaseException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.valueOf(e.httpStatusCode))
            .body(ErrorResponse(e.httpStatusCode, e.errorMessage))
    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun handleIllegalArgumentException(e: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .badRequest()
            .body(ErrorResponse(code = 400, message = e.message ?: "잘못된 요청입니다."))
}
