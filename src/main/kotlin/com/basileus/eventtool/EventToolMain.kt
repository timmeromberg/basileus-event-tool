package com.basileus.eventtool

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.koin.core.context.startKoin
import com.basileus.eventtool.di.eventToolModule

fun main() = application {
    val koinApp = startKoin {
        modules(eventToolModule)
    }

    Window(
        title = "Basileus Event Tool",
        onCloseRequest = ::exitApplication
    ) {
        MaterialTheme {
            Surface {
                EventToolUI(koinApp.koin)
            }
        }
    }
}
