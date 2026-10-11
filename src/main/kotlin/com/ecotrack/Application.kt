package com.ecotrack

import com.ecotrack.config.AppConfig
import com.ecotrack.database.DatabaseFactory
import com.ecotrack.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val logger = LoggerFactory.getLogger("com.ecotrack.Application")
    logger.info("Starting EcoTrack API server...")

    val config = AppConfig.load(environment.config)
    DatabaseFactory.init(config.database)

    configureSerialization()
    configureMonitoring()
    configureSecurity(config.jwt)
    configureCors(config.cors)
    configureStatusPages()
    configureRouting(config.jwt)

    logger.info("EcoTrack API server started on port ${config.port}")
}
