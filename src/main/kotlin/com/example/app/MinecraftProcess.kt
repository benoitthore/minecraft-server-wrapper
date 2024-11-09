package com.example.app

import com.example.app.MinecraftProcess.Command
import com.example.app.MinecraftProcess.Event
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

interface MinecraftProcess {
    val isRunning: Boolean
    val eventFlow: SharedFlow<Event>

    fun run()
    suspend fun stop()

    suspend fun sendCommand(command: Command)

    sealed interface Command {
        fun executableCommand(): String

        data class RawCommand(val message: String) : Command {
            override fun executableCommand() = message
        }

        data class Say(val message: String) : Command {
            override fun executableCommand(): String = "say $message"
        }

        data class Kill(val player: String) : Command {
            override fun executableCommand(): String = "kill $player"
        }
    }

    sealed interface Event {
        val time: LocalDateTime

        data class ProcessStopped(override val time: LocalDateTime = LocalDateTime.now()) : Event

        sealed interface PlayerEvent : Event {
            val player: Player
        }

        data class OnPlayerConnected(
            override val player: Player,
            override val time: LocalDateTime = LocalDateTime.now()
        ) : PlayerEvent

        data class OnPlayerDisconnected(
            override val player: Player,
            override val time: LocalDateTime = LocalDateTime.now()
        ) : PlayerEvent

        data class OnPlayerSpawned(
            override val player: Player,
            override val time: LocalDateTime = LocalDateTime.now()
        ) : PlayerEvent

        data class LogEvent(
            override val time: LocalDateTime,
            val message: String,
            val type: String,
        ) : Event
    }
}


class MinecraftProcessImpl(
    processToRun: String,
    memorySize: Int = 300,
    private val scope: CoroutineScope = GlobalScope
) : MinecraftProcess {

    private var job: Job? = null
    private val fileToRun = File(processToRun)

    init {
        require(fileToRun.isFile) { "${fileToRun.absolutePath} isn't a file" }
        require(fileToRun.canExecute()) { "${fileToRun.absolutePath} isn't executable" }
    }

    private val _eventFlow = MutableSharedFlow<Event>(replay = memorySize)
    override val eventFlow: SharedFlow<Event> get() = _eventFlow.asSharedFlow()

    private var process: Process? = null

    override val isRunning: Boolean get() = job?.isActive ?: false

    override fun run() {
        if (isRunning) throw IllegalStateException("Process is already running")

        job = scope.launch {
            withContext(Dispatchers.IO) {
                process = ProcessBuilder(fileToRun.absolutePath)
                    .redirectErrorStream(true)
                    .start()
                listenToProcessOutput()
            }
        }
    }

    override suspend fun sendCommand(command: Command): Unit = withContext(Dispatchers.IO) {
        val executableCommand = command.executableCommand()
        process?.outputStream?.bufferedWriter()?.use { writer ->
            writer.write(executableCommand)
            writer.newLine()
            writer.flush()
        }
    }

    override suspend fun stop(): Unit = withContext(Dispatchers.IO) {
        process?.destroy()
        _eventFlow.emit(Event.ProcessStopped())
        job?.cancelAndJoin()
    }

    private suspend fun listenToProcessOutput() {
        coroutineScope {
            launch {
                process?.inputStream?.bufferedReader()?.use { reader ->
                    reader.forEachLine { line ->
                        parseLogEntry(line)?.let(_eventFlow::tryEmit)

                        listOf(
                            ::parsePlayerConnectedMessage,
                            ::parsePlayerDisconnectedMessage,
                            ::parsePlayerSpawnedMessage,
                        )
                            .map { it(line) }
                            .forEach {
                                it?.let(_eventFlow::tryEmit)
                            }
                    }
                }
            }
        }
    }
}

fun parsePlayerConnectedMessage(message: String): Event.OnPlayerConnected? {
    val regex = "Player connected: ([^,]+), xuid: (\\d+)".toRegex()
    val matchResult = regex.find(message) ?: return null
    val (username, xuid) = matchResult.destructured
    return Event.OnPlayerConnected(
        player = Player(username = username, xuid = xuid)
    )
}

fun parsePlayerDisconnectedMessage(message: String): Event.OnPlayerDisconnected? {
    val regex = "Player disconnected: ([^,]+), xuid: (\\d+), pfid: ([\\da-f]+)".toRegex()
    val matchResult =
        regex.find(message) ?: return null
    val (username, xuid, pfid) = matchResult.destructured
    return Event.OnPlayerDisconnected(
        player = Player(username = username, xuid = xuid, pfid = pfid)
    )
}

fun parsePlayerSpawnedMessage(message: String): Event.OnPlayerSpawned? {
    val regex = "Player Spawned: ([^ ]+) xuid: (\\d+), pfid: ([\\da-f]+)".toRegex()
    val matchResult = regex.find(message) ?: return null
    val (username, xuid, pfid) = matchResult.destructured
    return Event.OnPlayerSpawned(
        player = Player(username = username, xuid = xuid, pfid = pfid)
    )
}

fun parseLogEntry(message: String): Event.LogEvent? {
    val pattern = ".*\\[(\\d{4}-\\d{2}-\\d{2})\\s(\\d{2}:\\d{2}:\\d{2}):\\d{3}\\s(\\w+)]\\s(.+)".toRegex()
    val matchResult = pattern.matchEntire(message)
    return if (matchResult != null) {
        val (dateString, timeString, type, logMessage) = matchResult.destructured
        val dateTime =
            LocalDateTime.parse("$dateString $timeString", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        Event.LogEvent(time = dateTime, message = logMessage, type = type)
    } else {
        System.err.println("Unparsable data: $message")
        null
    }
}
