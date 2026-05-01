package com.terxdesk.droid

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class TerminalActivity : AppCompatActivity() {

    private lateinit var outputView: TextView
    private lateinit var inputView: EditText
    private lateinit var scrollView: ScrollView
    private lateinit var statusText: TextView

    private var shellProcess: Process? = null
    private var shellInput: BufferedWriter? = null
    private var shellOutput: BufferedReader? = null
    private var readJob: Job? = null
    private val outputBuilder = SpannableStringBuilder()
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private val COLOR_DEFAULT = Color.WHITE
        private val COLOR_ERROR = Color.RED
        private val COLOR_PROMPT = 0xFF00E5FF.toInt()
        private val COLOR_RUNNING = Color.YELLOW
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terminal)

        outputView = findViewById(R.id.terminalOutput)
        inputView = findViewById(R.id.terminalInput)
        scrollView = findViewById(R.id.terminalScrollView)
        statusText = findViewById(R.id.terminalStatus)

        inputView.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                sendCommand()
                true
            } else false
        }

        findViewById<View>(R.id.btnSend).setOnClickListener { sendCommand() }

        startShell()
    }

    private fun startShell() {
        statusText.text = "Starting shell..."

        lifecycleScope.launch {
            try {
                val pb = ProcessBuilder("/system/bin/sh")
                pb.environment()["PATH"] = "/system/bin:/system/xbin"
                pb.environment()["HOME"] = "/data/data/${packageName}/files/home"
                pb.environment()["TERM"] = "xterm-256color"
                pb.directory(java.io.File("/data/data/${packageName}/files/home"))
                pb.redirectErrorStream(true)

                shellProcess = pb.start()
                shellInput = BufferedWriter(OutputStreamWriter(shellProcess!!.outputStream))
                shellOutput = BufferedReader(InputStreamReader(shellProcess!!.inputStream))

                statusText.text = "Shell ready"
                appendOutput("TERMINAL-X-DESKTOP\n", COLOR_PROMPT)
                appendOutput("Type commands below.\n\n", COLOR_RUNNING)
                appendOutput("$ ", COLOR_PROMPT)

                readJob = launch(Dispatchers.IO) { readShellOutput() }
            } catch (e: Exception) {
                statusText.text = "Shell failed: ${e.message}"
                appendOutput("Failed: ${e.message}\n", COLOR_ERROR)
            }
        }
    }

    private suspend fun readShellOutput() {
        try {
            val buffer = CharArray(4096)
            var bytesRead: Int
            while (shellOutput?.read(buffer).also { bytesRead = it ?: -1 } != -1) {
                if (bytesRead > 0) {
                    val chunk = String(buffer.copyOf(bytesRead))
                    withContext(Dispatchers.Main) {
                        outputBuilder.append(chunk)
                        outputView.text = outputBuilder
                        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                appendOutput("\n[Lost]\n", COLOR_ERROR)
            }
        }
    }

    private fun sendCommand() {
        val command = inputView.text.toString().trim()
        if (command.isEmpty()) return
        inputView.setText("")

        appendOutput("$command\n", COLOR_DEFAULT)

        lifecycleScope.launch {
            try {
                shellInput?.write("$command\n")
                shellInput?.flush()
                appendOutput("$ ", COLOR_PROMPT)
            } catch (e: Exception) {
                appendOutput("\n[Error: ${e.message}]\n", COLOR_ERROR)
                appendOutput("$ ", COLOR_PROMPT)
            }
        }
    }

    private fun appendOutput(text: String, color: Int) {
        val start = outputBuilder.length
        outputBuilder.append(text)
        if (text.isNotBlank()) {
            outputBuilder.setSpan(ForegroundColorSpan(color), start, outputBuilder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        handler.post {
            outputView.text = outputBuilder
            scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        readJob?.cancel()
        shellInput?.close()
        shellProcess?.destroy()
    }
}
