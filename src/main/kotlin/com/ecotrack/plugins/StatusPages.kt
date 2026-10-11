package com.ecotrack.plugins

import com.ecotrack.common.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory

fun Application.configureStatusPages() {
    val logger = LoggerFactory.getLogger("com.ecotrack.StatusPages")

    install(StatusPages) {
        exception<ValidationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError(
                    error = ErrorBody(
                        code = cause.code,
                        message = cause.message,
                        details = cause.errors.ifEmpty { null }
                    )
                )
            )
        }

        exception<UnauthorizedException> { call, cause ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiError(
                    error = ErrorBody(
                        code = cause.code,
                        message = cause.message
                    )
                )
            )
        }

        exception<ForbiddenException> { call, cause ->
            call.respond(
                HttpStatusCode.Forbidden,
                ApiError(
                    error = ErrorBody(
                        code = cause.code,
                        message = cause.message
                    )
                )
            )
        }

        exception<NotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ApiError(
                    error = ErrorBody(
                        code = cause.code,
                        message = cause.message
                    )
                )
            )
        }

        exception<ConflictException> { call, cause ->
            call.respond(
                HttpStatusCode.Conflict,
                ApiError(
                    error = ErrorBody(
                        code = cause.code,
                        message = cause.message
                    )
                )
            )
        }

        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiError(
                    error = ErrorBody(
                        code = "INTERNAL_ERROR",
                        message = "An unexpected error occurred"
                    )
                )
            )
        }
    }
}
