package com.basileus.eventtool.di

import com.basileus.eventtool.EventToolViewModel
import com.basileus.eventtool.storage.EventLoader
import com.basileus.eventtool.storage.EventToolStorageConfig
import com.basileus.eventtool.storage.GraphBuilder
import com.basileus.eventtool.storage.OutcomeLoader
import org.koin.dsl.module

val eventToolModule = module {
    single { EventToolStorageConfig() }
    single { EventLoader(get()) }
    single { OutcomeLoader(get()) }
    single { GraphBuilder() }
    single { EventToolViewModel(get(), get(), get()) }
}
