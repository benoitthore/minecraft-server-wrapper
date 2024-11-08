package com.example.controllers

import com.example.app.MinecraftProcess
import com.example.app.MinecraftProcessImpl
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private val process: MinecraftProcess by lazy {
    MinecraftProcessImpl("")
}

fun Application.configureProcessController() {
    routing {
        get("/start") {
            if (process.isRunning) {
                call.respondText("Already Started")
            } else {
                process.run()
                call.respondText("Process Started")
            }
        }

        get("/stop") {
            if (!process.isRunning) {
                call.respondText("Already Stopped")
            } else {
                process.stop()
                call.respondText("Process Stopped")
            }
        }

        get("/messages") {
            call.respondText(process.eventFlow.replayCache.toList().toString())
        }
    }
}