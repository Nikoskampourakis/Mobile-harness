package com.jarves.mh.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StorageCleanerTest {
    @get:Rule val temp = TemporaryFolder()

    @Test
    fun storageBreakdownCalculatesReclaimableBytes() {
        val breakdown = StorageBreakdown(
            projectBytes = 1000L,
            runtimeBytes = 5000L,
            cacheBytes = 800L,
            workspaceCacheBytes = 400L,
            disposableOutputBytes = 200L,
            totalAppBytes = 7400L,
            freeDeviceBytes = 50000L,
        )
        assertEquals(1400L, breakdown.totalReclaimableBytes)
    }

    @Test
    fun pruningPreservesProjectSourceFiles() {
        val root = temp.newFolder("workspaces")
        val proj = File(root, "my_project").apply { mkdirs() }
        val src = File(proj, "src/Main.kt").apply { parentFile?.mkdirs(); writeText("fun main() {}") }
        val git = File(proj, ".git/HEAD").apply { parentFile?.mkdirs(); writeText("ref: refs/heads/main") }
        val build = File(proj, "build/out.dex").apply { parentFile?.mkdirs(); writeText("compiled code") }
        val gradle = File(proj, ".gradle/cache.bin").apply { parentFile?.mkdirs(); writeText("cache") }

        assertTrue(src.exists())
        assertTrue(git.exists())
        assertTrue(build.exists())
        assertTrue(gradle.exists())

        // Pruning only removes disposable build/ and .gradle/
        build.parentFile?.deleteRecursively()
        gradle.parentFile?.deleteRecursively()

        assertTrue("Source files must remain intact", src.exists())
        assertTrue("Git history must remain intact", git.exists())
        assertFalse(build.exists())
        assertFalse(gradle.exists())
    }
}
