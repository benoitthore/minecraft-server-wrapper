package com.example.plugins

import com.example.app.MinecraftProcess
import com.example.app.MinecraftProcessImpl
import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

private val minecraftProcessModule = module {
    single<MinecraftProcess> { MinecraftProcessImpl("C:\\Users\\Bebeuz\\Desktop\\Bedrock Server\\bedrock_server.exe") }
}

fun Application.configureDependencies() {
    // Install Koin
    install(Koin) {
        modules(minecraftProcessModule)
    }

}