package com.example.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class Player(
    val username: String,
    val xuid: String,
    val pfid: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        return (other as? Player)?.username == username
    }

    override fun hashCode(): Int = username.hashCode()
}

interface GameMonitor {
    val connectedPlayerFlow: StateFlow<List<Player>>
}

class GameMonitorImpl(
    val process: MinecraftProcess,
    private val scope: CoroutineScope = GlobalScope
) : GameMonitor {

    private var _connectedPlayer: MutableStateFlow<MutableList<Player>> = MutableStateFlow(mutableListOf())

    override val connectedPlayerFlow = _connectedPlayer.asStateFlow()

    init {
        scope.launch {
            process.eventFlow.collect { event ->
                when (event) {
                    is MinecraftProcess.Event.LogEvent -> {}
                    is MinecraftProcess.Event.OnPlayerConnected -> {
                        _connectedPlayer.value += event.player
                    }

                    is MinecraftProcess.Event.OnPlayerDisconnected -> {
                        _connectedPlayer.value -= event.player
                    }

                    is MinecraftProcess.Event.OnPlayerSpawned -> {

                    }

                    is MinecraftProcess.Event.ProcessStopped -> {}
                }
            }
        }
    }
}