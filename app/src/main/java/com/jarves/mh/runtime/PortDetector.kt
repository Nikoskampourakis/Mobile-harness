package com.jarves.mh.runtime

import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Represents a local web service or server detected on the device.
 */
data class DetectedService(
    val port: Int,
    val name: String,
    val url: String,
    val isVsCode: Boolean = false,
    val detectedAtMillis: Long = System.currentTimeMillis(),
)

/**
 * Automatic port detector for development servers running inside Mobile Harness
 * (e.g., VS Code Server, Vite, Next.js, Node, Flask, Python HTTP servers).
 * Scans /proc/net/tcp and tests common developer ports.
 */
object PortDetector {

    val COMMON_DEV_PORTS = listOf(
        3000, // React / Next.js / Node
        3001,
        5000, // Flask / Express
        5173, // Vite / Vue / Svelte
        5174,
        8000, // Python HTTP / Django / FastAPI
        8080, // VS Code / Web IDE / Spring
        8081, // Metro / React Native
        8443, // HTTPS dev / VS Code
        8501, // Streamlit
        8888, // Jupyter
        9000, // PHP / Web
        4200, // Angular
    )

    suspend fun detectListeningServices(): List<DetectedService> = withContext(Dispatchers.IO) {
        val detectedPorts = mutableSetOf<Int>()

        // 1. Try kernel procfs tcp inspection for instant zero-overhead detection
        parseProcNetTcp(File("/proc/net/tcp"), detectedPorts)
        parseProcNetTcp(File("/proc/net/tcp6"), detectedPorts)

        // 2. Fall back to socket probing for common developer ports if procfs is restricted
        if (detectedPorts.isEmpty()) {
            COMMON_DEV_PORTS.forEach { port ->
                if (isPortListening(port)) {
                    detectedPorts.add(port)
                }
            }
        }

        // Filter out low system ports (<1024)
        val validPorts = detectedPorts.filter { it in 1024..65535 }.sorted()

        validPorts.map { port ->
            val name = resolveServiceName(port)
            DetectedService(
                port = port,
                name = name,
                url = "http://localhost:$port",
                isVsCode = port == 8080 || port == 8443 || name.contains("VS Code", ignoreCase = true),
            )
        }
    }

    private fun parseProcNetTcp(file: File, results: MutableSet<Int>) {
        if (!file.exists() || !file.canRead()) return
        runCatching {
            file.forEachLine { line ->
                val parts = line.trim().split(Regex("\\s+"))
                // sl local_address rem_address st
                if (parts.size >= 4) {
                    val state = parts[3]
                    // 0A is TCP_LISTEN in Linux kernel
                    if (state.equals("0A", ignoreCase = true)) {
                        val localAddress = parts[1]
                        val colon = localAddress.indexOf(':')
                        if (colon != -1 && colon + 1 < localAddress.length) {
                            val hexPort = localAddress.substring(colon + 1)
                            val port = hexPort.toIntOrNull(16)
                            if (port != null && port in 1024..65535) {
                                results.add(port)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isPortListening(port: Int): Boolean = runCatching {
        Socket().use { socket ->
            socket.reuseAddress = true
            socket.connect(InetSocketAddress("127.0.0.1", port), 70)
            true
        }
    }.getOrDefault(false)

    fun resolveServiceName(port: Int): String = when (port) {
        8080 -> "VS Code Server (:8080)"
        8443 -> "VS Code Secure (:8443)"
        3000 -> "Web App / Next.js (:3000)"
        3001 -> "Node Server (:3001)"
        5173 -> "Vite Dev Server (:5173)"
        5174 -> "Vite Server (:5174)"
        5000 -> "Flask / Express (:5000)"
        8000 -> "Python HTTP / FastAPI (:8000)"
        8501 -> "Streamlit App (:8501)"
        8081 -> "Metro Bundler (:8081)"
        4200 -> "Angular App (:4200)"
        8888 -> "Jupyter / Web (:8888)"
        9000 -> "Web Service (:9000)"
        else -> "Web App (:$port)"
    }
}
