package andryukha.api

import andryukha.integration.telegram.TelegramDisabledException
import andryukha.integration.telegram.TelegramUnavailableException

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(TelegramDisabledException::class)
    fun telegramDisabled(ex: TelegramDisabledException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.message!!)

    @ExceptionHandler(TelegramUnavailableException::class)
    fun telegramUnavailable(ex: TelegramUnavailableException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.message!!)

    @ExceptionHandler(IllegalArgumentException::class)
    fun invalid(ex: IllegalArgumentException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message ?: "Некорректные параметры.")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(ex: MethodArgumentNotValidException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Проверьте обязательные поля и допустимые значения.")
            .apply { setProperty("fields", ex.bindingResult.fieldErrors.map { it.field }.distinct()) }

    @ExceptionHandler(HttpMessageNotReadableException::class, MethodArgumentTypeMismatchException::class)
    fun malformed(): ProblemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
        "Некорректный JSON, дата или значение перечисления.")

    @ExceptionHandler(DataIntegrityViolationException::class, OptimisticLockingFailureException::class)
    fun conflict(): ProblemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
        "Конфликт данных. Повторите импорт после завершения параллельного запроса.")
}
