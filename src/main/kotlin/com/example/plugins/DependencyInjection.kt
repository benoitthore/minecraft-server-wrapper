package com.example.plugins

import com.example.app.GameMonitor
import com.example.app.GameMonitorImpl
import com.example.app.MinecraftProcess
import com.example.app.MinecraftProcessImpl
import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

interface ServerScope : CoroutineScope

private val ServerScopeImpl = object : ServerScope, CoroutineScope by GlobalScope {}

private val minecraftProcessModule = module {
    single<MinecraftProcess> { MinecraftProcessImpl("C:\\Users\\Bebeuz\\Desktop\\Bedrock Server\\bedrock_server.exe") }
    single<ServerScope> { ServerScopeImpl }
    single<GameMonitor> { GameMonitorImpl(get()) }
}

fun Application.configureDependencies() {
    // Install Koin
    install(Koin) {
        modules(minecraftProcessModule)
    }

}