package com.ecotrack.config

import io.ktor.server.config.*

data class AppConfig(
    val port: Int,
    val jwt: JwtConfig,
    val database: DatabaseConfig,
    val cors: CorsConfig
) {
    companion object {
        fun load(config: ApplicationConfig): AppConfig {
            return AppConfig(
                port = config.propertyOrNull("ktor.deployment.port")?.getString()?.toIntOrNull() ?: 8080,
                jwt = JwtConfig(
                    secret = config.property("jwt.secret").getString(),
                    issuer = config.property("jwt.issuer").getString(),
                    audience = config.property("jwt.audience").getString(),
                    realm = config.propertyOrNull("jwt.realm")?.getString() ?: "EcoTrack API",
                    expirationSeconds = config.propertyOrNull("jwt.expirationSeconds")?.getString()?.toLongOrNull() ?: 3600L
                ),
                database = DatabaseConfig(
                    url = config.property("database.url").getString(),
                    user = config.property("database.user").getString(),
                    password = config.property("database.password").getString(),
                    maxPoolSize = config.propertyOrNull("database.maxPoolSize")?.getString()?.toIntOrNull() ?: 10
                ),
                cors = CorsConfig(
                    allowedOrigins = config.propertyOrNull("cors.allowedOrigins")?.getString()
                        ?.split(",")
                        ?.map { it.trim() }
                        ?: listOf("http://localhost:3000")
                )
            )
        }
    }
}

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val expirationSeconds: Long
)

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int
)

data class CorsConfig(
    val allowedOrigins: List<String>
)
