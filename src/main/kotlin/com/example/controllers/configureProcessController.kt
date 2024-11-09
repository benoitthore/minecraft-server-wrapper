package com.example.controllers

import com.example.app.GameMonitor
import com.example.app.MinecraftProcess
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject


fun Application.configureProcessController() {
    val minecraftProcessInstance: MinecraftProcess by inject()
    val gameMonitor: GameMonitor by inject()

    routing {
        get("/start") {
            if (minecraftProcessInstance.isRunning) {
                call.respondText("Already Started")
            } else {
                minecraftProcessInstance.run()
                call.respondText("Process Started")
            }
        }

        get("/stop") {
            if (!minecraftProcessInstance.isRunning) {
                call.respondText("Already Stopped")
            } else {
                minecraftProcessInstance.stop()
                call.respondText("Process Stopped")
            }
        }


        get("/stats") {
            if (!minecraftProcessInstance.isRunning) {
                call.respondText("Not running")
            } else {
                val list = gameMonitor.connectedPlayerFlow.value
                if (list.isEmpty()) {
                    call.respondText("yA PERSOnne COnnard")
                } else {
                    call.respondText(list.joinToString("\n") { it.username })

                }
            }
        }

        get("/kill") {
            if (!minecraftProcessInstance.isRunning) {
                call.respondText("Not running")
            } else {
                val playerName = call.request.queryParameters["player"]
                    ?: return@get call.respondText("player parameter required")

                minecraftProcessInstance.sendCommand(MinecraftProcess.Command.Kill(playerName))
                call.respondText(".")
            }
        }

        get("/messages") {
            call.respondText(minecraftProcessInstance.eventFlow.replayCache.toList().joinToString(separator = "\n") {
                when (it) {
                    is MinecraftProcess.Event.LogEvent -> with(it) { "$time - $message" }
                    is MinecraftProcess.Event.ProcessStopped -> "Process Stopped"
                    is MinecraftProcess.Event.PlayerEvent -> {
                        "${it.player.username} ${it::class}"
                    }
                }

            })
        }

        get("/command") {
            val cmd = call.request.queryParameters["cmd"]
            if (cmd.isNullOrBlank()) {
                call.respondText("Command not provided", status = HttpStatusCode.BadRequest)
            } else {
                minecraftProcessInstance.sendCommand(MinecraftProcess.Command.RawCommand(cmd))
                call.respondText("Command sent: $cmd")
            }
        }

    }
}