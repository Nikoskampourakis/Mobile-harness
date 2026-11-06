package com.jarves.mh.model

import java.util.UUID

data class TerminalOutputLine(
    val id: String = UUID.randomUUID().toString(),
    val command: String = "",
    val output: String = "",
    val exitCode: Int = 0,
)

data class TerminalSession(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Terminal",
    val cwd: String = "/workspace",
    val lines: List<TerminalOutputLine> = emptyList(),
    val liveOutput: String = "",
    val isRunning: Boolean = false,
    val command: String? = null,
    val commandDraft: String = "",
    val profileId: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
)

enum class TerminalSkinStyle(val label: String, val description: String) {
    MINIMAL("Minimal", "Clean flat borderless developer interface"),
    MATRIX("Matrix", "High-contrast phosphor green glow on obsidian"),
    CYBERPUNK("Cyberpunk", "Neon amber and magenta cyber accents"),
    GLASS("Glassmorphism", "Translucent frosted background with border tint"),
    CRT("Vintage CRT", "Scanline-inspired amber terminal emulator"),
}

enum class TerminalFontFamily(val label: String) {
    MONOSPACE("System Monospace"),
    CODE("Developer Code"),
    SANS("Clean Sans"),
}

data class TerminalSkinConfig(
    val style: TerminalSkinStyle = TerminalSkinStyle.MINIMAL,
    val fontFamily: TerminalFontFamily = TerminalFontFamily.MONOSPACE,
    val fontSizeSp: Float = 12.5f,
    val lineHeightSp: Float = 17f,
    val contentPaddingDp: Int = 12,
    val showAsciiBanner: Boolean = true,
    val customBannerText: String? = null,
    val bannerColor: Long? = null,
    val bannerGradient: List<Long>? = null,
    val autocorrectEnabled: Boolean = true,
)

data class TerminalProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String = "terminal",
    val customHomeDir: String? = null,
    val startupScript: String? = null,
    val environmentVariables: Map<String, String> = emptyMap(),
    val isIsolated: Boolean = false,
)

enum class BroadcastScope {
    ALL,
    OTHERS,
    CURRENT,
}

data class QuickCommand(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val command: String,
    val icon: String = "terminal",
)

data class PlanningGoal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val isInProgress: Boolean = false,
)

data class ActiveProcessInfo(
    val pid: String,
    val name: String,
    val cpuPercent: String = "0.0%",
    val memoryUsage: String = "12MB",
    val terminalSessionName: String = "Terminal",
    val status: String = "Running",
    val isKillable: Boolean = true,
)

object TerminalAutocorrect {
    private val commonCorrections = mapOf(
        "sl" to "ls",
        "dc" to "cd",
        "gerp" to "grep",
        "grpe" to "grep",
        "gti" to "git",
        "gut" to "git",
        "got" to "git",
        "stauts" to "status",
        "statys" to "status",
        "chekc" to "check",
        "csl" to "clear",
        "claer" to "clear",
        "clera" to "clear",
        "nod" to "node",
        "pyhton" to "python3",
        "pyton" to "python3",
        "cpm" to "npm",
        "mknir" to "mkdir",
        "mdkir" to "mkdir",
        "touhc" to "touch",
    )

    fun suggestCorrection(input: String): String? {
        val trimmed = input.trim()
        val parts = trimmed.split("\\s+".toRegex(), limit = 2)
        if (parts.isEmpty()) return null
        val cmd = parts[0]
        val correction = commonCorrections[cmd.lowercase()] ?: return null
        return if (parts.size > 1) "$correction ${parts[1]}" else correction
    }
}

