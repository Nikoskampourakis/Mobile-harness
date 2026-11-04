package com.jarves.mh.runtime

import android.content.Context
import com.jarves.mh.data.AppPreferences
import java.io.File

/**
 * Detailed storage breakdown across Mobile Harness components.
 */
data class StorageBreakdown(
    val projectBytes: Long = 0L,
    val runtimeBytes: Long = 0L,
    val cacheBytes: Long = 0L,
    val workspaceCacheBytes: Long = 0L,
    val disposableOutputBytes: Long = 0L,
    val totalAppBytes: Long = 0L,
    val freeDeviceBytes: Long = 0L,
) {
    val totalReclaimableBytes: Long get() = cacheBytes + workspaceCacheBytes + disposableOutputBytes
}

/**
 * Storage cleaner and space manager for Mobile Harness.
 * Allows safely freeing package and build caches, pruning project build folders,
 * and performing a clean reinstall of the Linux runtime while keeping all project data,
 * workspaces, git repositories, chats, and API keys 100% intact.
 */
class StorageCleaner(private val context: Context) {
    private val filesDir = context.filesDir
    private val cacheDir = context.cacheDir
    private val runtimeDir = File(filesDir, "runtime")
    private val rootfs = File(runtimeDir, "ubuntu")
    private val workspacesDir = File(filesDir, "workspaces")
    private val chatsDir = File(filesDir, "chats")
    private val downloadsDir = File(cacheDir, "runtime-downloads")

    fun calculateBreakdown(): StorageBreakdown {
        val projects = folderSize(workspacesDir) + folderSize(chatsDir)
        val runtime = folderSize(runtimeDir)
        val caches = calculatePackageCaches()
        val wsCaches = calculateWorkspaceCaches()
        val outputBytes = cacheDir.listFiles().orEmpty()
            .asSequence()
            .filter { it.isFile && it.name.startsWith("runtime-output-") }
            .sumOf(File::length)
        val total = folderSize(filesDir) + folderSize(cacheDir)
        val free = runCatching { filesDir.freeSpace }.getOrDefault(0L)

        return StorageBreakdown(
            projectBytes = projects,
            runtimeBytes = runtime,
            cacheBytes = caches,
            workspaceCacheBytes = wsCaches,
            disposableOutputBytes = outputBytes,
            totalAppBytes = total,
            freeDeviceBytes = free,
        )
    }

    /**
     * Cleans APT, NPM, Pip, Gradle, Composer, and download caches.
     * Does not delete any user project data or installed tool binaries.
     * Returns total bytes freed.
     */
    fun cleanPackageCaches(): Long {
        var freed = 0L

        // 1. APT package archives
        val aptArchives = File(rootfs, "var/cache/apt/archives")
        if (aptArchives.isDirectory) {
            aptArchives.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".deb")) {
                    freed += file.length()
                    file.delete()
                }
            }
        }

        // 2. NPM cache
        val npmCache = File(rootfs, "root/.npm")
        if (npmCache.isDirectory) {
            freed += folderSize(npmCache)
            npmCache.deleteRecursively()
        }

        // 3. PIP cache
        val pipCache = File(rootfs, "root/.cache/pip")
        if (pipCache.isDirectory) {
            freed += folderSize(pipCache)
            pipCache.deleteRecursively()
        }

        // 4. Gradle caches in rootfs
        val gradleCaches = File(rootfs, "root/.gradle/caches")
        if (gradleCaches.isDirectory) {
            freed += folderSize(gradleCaches)
            gradleCaches.deleteRecursively()
        }
        val gradleDaemon = File(rootfs, "root/.gradle/daemon")
        if (gradleDaemon.isDirectory) {
            freed += folderSize(gradleDaemon)
            gradleDaemon.deleteRecursively()
        }

        // 5. Composer cache
        val composerCache = File(rootfs, "root/.cache/composer")
        if (composerCache.isDirectory) {
            freed += folderSize(composerCache)
            composerCache.deleteRecursively()
        }

        // 6. Runtime download archives in cacheDir
        if (downloadsDir.isDirectory) {
            freed += folderSize(downloadsDir)
            downloadsDir.deleteRecursively()
        }

        // 7. Disposable process output logs
        cacheDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith("runtime-output-")) {
                freed += file.length()
                file.delete()
            }
        }

        return freed
    }

    /**
     * Automatically cleans or compresses temporary files according to user preferences.
     * Compresses older logs to GZIP (saving ~90% storage) and removes stale temporary files.
     */
    fun autoCleanIfEnabled(): Long {
        val prefs = AppPreferences(context)
        if (!prefs.autoCleanStorageEnabled) return 0L
        return compressOldLogsAndCleanTempFiles(compress = prefs.compressLogsEnabled)
    }

    /**
     * Compresses uncompressed log files older than 3 hours into .log.gz files,
     * deletes logs older than 48 hours, and clears apt package archives.
     */
    fun compressOldLogsAndCleanTempFiles(compress: Boolean = true): Long {
        var freed = 0L
        val now = System.currentTimeMillis()
        val threeHoursAgo = now - 3 * 3600 * 1000L
        val twoDaysAgo = now - 48 * 3600 * 1000L

        // 1. Clean or compress output logs in cacheDir
        cacheDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith("runtime-output-")) {
                val lastMod = file.lastModified()
                if (lastMod < twoDaysAgo) {
                    freed += file.length()
                    file.delete()
                } else if (compress && file.name.endsWith(".log") && lastMod < threeHoursAgo && file.length() > 4096L) {
                    val originalSize = file.length()
                    val gzFile = File(file.parentFile, "${file.name}.gz")
                    runCatching {
                        java.util.zip.GZIPOutputStream(gzFile.outputStream().buffered()).use { gzOut ->
                            file.inputStream().buffered().use { inStream ->
                                inStream.copyTo(gzOut)
                            }
                        }
                        if (gzFile.exists() && gzFile.length() > 0) {
                            file.delete()
                            freed += (originalSize - gzFile.length()).coerceAtLeast(0L)
                        }
                    }
                }
            }
        }

        // 2. Clean apt deb archives
        val aptArchives = File(rootfs, "var/cache/apt/archives")
        if (aptArchives.isDirectory) {
            aptArchives.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".deb")) {
                    freed += file.length()
                    file.delete()
                }
            }
        }

        // 3. Clean temporary download files older than 24h
        if (downloadsDir.isDirectory) {
            downloadsDir.listFiles()?.forEach { file ->
                if (file.lastModified() < (now - 24 * 3600 * 1000L)) {
                    freed += file.length()
                    file.delete()
                }
            }
        }

        return freed
    }

    /**
     * Prunes disposable build outputs (build/, .gradle/, __pycache__) inside all
     * project workspaces. Keeps all source code, git history, and configs intact.
     * Returns total bytes freed.
     */
    fun pruneWorkspaceBuildCaches(): Long {
        var freed = 0L
        if (!workspacesDir.isDirectory) return 0L
        workspacesDir.listFiles()?.forEach { projectDir ->
            if (projectDir.isDirectory) {
                val buildDir = File(projectDir, "build")
                if (buildDir.isDirectory) {
                    freed += folderSize(buildDir)
                    buildDir.deleteRecursively()
                }
                val dotGradle = File(projectDir, ".gradle")
                if (dotGradle.isDirectory) {
                    freed += folderSize(dotGradle)
                    dotGradle.deleteRecursively()
                }
                val dotCache = File(projectDir, ".cache")
                if (dotCache.isDirectory) {
                    freed += folderSize(dotCache)
                    dotCache.deleteRecursively()
                }
                projectDir.walkTopDown().forEach { file ->
                    if (file.isDirectory && file.name == "__pycache__") {
                        freed += folderSize(file)
                        file.deleteRecursively()
                    }
                }
            }
        }
        return freed
    }

    /**
     * Completely removes the bloated Linux runtime rootfs while preserving 100% of project data,
     * workspaces, git repositories, chats, and API credentials.
     * Backs up user config files (like .gitconfig, .ssh) before deleting.
     */
    fun cleanReinstallRuntime(): Boolean {
        // 1. Preserve important user configs from /root
        val pendingHomeDir = File(filesDir, "pending-home").apply { mkdirs() }
        val preservedFiles = listOf(".gitconfig", ".claude.json", ".ssh")
        preservedFiles.forEach { name ->
            val src = File(rootfs, "root/$name")
            if (src.exists()) {
                val dst = File(pendingHomeDir, name)
                runCatching {
                    if (src.isDirectory) src.copyRecursively(dst, overwrite = true)
                    else src.copyTo(dst, overwrite = true)
                }
            }
        }

        // 2. Wipe runtime directory completely
        if (runtimeDir.exists()) {
            runtimeDir.deleteRecursively()
        }

        // 3. Wipe download caches
        if (downloadsDir.exists()) {
            downloadsDir.deleteRecursively()
        }

        // 4. Mark runtime as not installed
        AppPreferences(context).runtimeSetupComplete = false

        return true
    }

    private fun folderSize(dir: File): Long {
        if (!dir.exists()) return 0L
        if (dir.isFile) return dir.length()
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun calculatePackageCaches(): Long {
        var size = 0L
        val paths = listOf(
            File(rootfs, "var/cache/apt"),
            File(rootfs, "root/.npm"),
            File(rootfs, "root/.cache"),
            File(rootfs, "root/.gradle/caches"),
            downloadsDir,
        )
        paths.forEach { if (it.exists()) size += folderSize(it) }
        return size
    }

    private fun calculateWorkspaceCaches(): Long {
        var size = 0L
        if (!workspacesDir.isDirectory) return 0L
        workspacesDir.listFiles()?.forEach { projectDir ->
            if (projectDir.isDirectory) {
                listOf(
                    File(projectDir, "build"),
                    File(projectDir, ".gradle"),
                    File(projectDir, ".cache"),
                ).forEach { if (it.exists()) size += folderSize(it) }
                projectDir.walkTopDown().forEach { file ->
                    if (file.isDirectory && file.name == "__pycache__") {
                        size += folderSize(file)
                    }
                }
            }
        }
        return size
    }
}
