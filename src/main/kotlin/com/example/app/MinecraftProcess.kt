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

        data class RawCommand(val message: String) : Command{
            override fun executableCommand() = message
        }

        data class Say(val message: String) : Command {
            override fun executableCommand(): String = "say $message"
        }
    }

    sealed interface Event {
        data object ProcessStopped : Event
        data class LogEvent(val entry : LogEntry) : Event
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
        _eventFlow.emit(Event.ProcessStopped)
        job?.cancelAndJoin()
    }

    private suspend fun listenToProcessOutput() {
        coroutineScope {
            launch {
                process?.inputStream?.bufferedReader()?.use { reader ->
                    reader.forEachLine { line ->
                        parseLogEntry(line)?.let {
                            _eventFlow.tryEmit(Event.LogEvent(it))
                        }
                    }
                }
            }
        }
    }
}

fun parseLogEntry(message: String): LogEntry? {
    // [2024-01-01 18:59:22:123 INFO] the message
    val pattern = ".*\\[(\\d{4}-\\d{2}-\\d{2})\\s(\\d{2}:\\d{2}:\\d{2}):\\d{3}\\s(\\w+)]\\s(.+)".toRegex()
    val matchResult = pattern.matchEntire(message)
    return if (matchResult != null) {
        val (dateString, timeString, type, logMessage) = matchResult.destructured
        val dateTime =
            LocalDateTime.parse("$dateString $timeString", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        LogEntry(dateTime = dateTime, message = logMessage, type = type)
    } else {
        System.err.println("Unparsable data: $message")
        null
    }
}

data class LogEntry(
    val dateTime: LocalDateTime,
    val message: String,
    val type: String,
)