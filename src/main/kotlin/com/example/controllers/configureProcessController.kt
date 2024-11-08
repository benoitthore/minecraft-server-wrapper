package com.example.controllers

import com.example.app.MinecraftProcess
import com.example.app.MinecraftProcessImpl
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

val MinecraftProcessInstance: MinecraftProcess by lazy {
    MinecraftProcessImpl("C:\\Users\\Bebeuz\\Desktop\\Bedrock Server\\bedrock_server.exe")
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
            call.respondText(MinecraftProcessInstance.eventFlow.replayCache.toList().joinToString(separator = "\n") {
                when(it){
                    is MinecraftProcess.Event.LogEvent -> with(it.entry){"$dateTime - $message"}
                    MinecraftProcess.Event.ProcessStopped -> "Process Stopped"
                }

            })
        }

        get("/command") {
            val cmd = call.request.queryParameters["cmd"]
            if (cmd.isNullOrBlank()) {
                call.respondText("Command not provided", status = HttpStatusCode.BadRequest)
            } else {
                MinecraftProcessInstance.sendCommand(MinecraftProcess.Command.RawCommand(cmd))
                call.respondText("Command sent: $cmd")
            }
        }

    }
}