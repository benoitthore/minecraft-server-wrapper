package com.example.controllers

import com.example.app.MinecraftProcess
import com.example.app.MinecraftProcessImpl
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

val MinecraftProcessInstance: MinecraftProcess by lazy {
    MinecraftProcessImpl("")
}

fun Application.configureProcessController() {
    routing {
        get("/start") {
            if (MinecraftProcessInstance.isRunning) {
                call.respondText("Already Started")
            } else {
                MinecraftProcessInstance.run()
                call.respondText("Process Started")
            }
        }

        get("/stop") {
            if (!MinecraftProcessInstance.isRunning) {
                call.respondText("Already Stopped")
            } else {
                MinecraftProcessInstance.stop()
                call.respondText("Process Stopped")
            }
        }

        get("/messages") {
            call.respondText(MinecraftProcessInstance.eventFlow.replayCache.toList().toString())
        }
    }
}