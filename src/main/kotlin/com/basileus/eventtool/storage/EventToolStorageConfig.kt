package com.basileus.eventtool.storage

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

class EventToolStorageConfig {
    val basePath: Path = findContentPath()

    val eventsPath: Path get() = basePath.resolve("events")
    val outcomesPath: Path get() = basePath.resolve("outcomes")

    companion object {
        private fun findContentPath(): Path {
            val userDir = System.getProperty("user.dir")

            val candidates = listOf(
                Path(userDir, "basileus-content"),
                Path(userDir).parent?.resolve("basileus-content"),
                Path(userDir).parent?.parent?.resolve("basileus-content"),
                Path("/Users/timme/Code/basileus/basileus-content"),
            ).filterNotNull()

            for (candidate in candidates) {
                if (candidate.exists()) {
                    return candidate
                }
            }

            return Path(System.getProperty("user.home"), ".basileus", "content")
        }
    }
}
