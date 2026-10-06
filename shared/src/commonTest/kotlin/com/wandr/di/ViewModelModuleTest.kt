package com.wandr.di

import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(org.koin.core.annotation.KoinInternalApi::class) // only the module's registration keys are inspected
class ViewModelModuleTest {
    @Test
    fun recordingQualifierIsInitializedBeforeTheModuleThatUsesIt() {
        // Regression: when the qualifier was declared below the module it was still null while the module was built,
        // so the recording view model was registered without its name and could not be found.
        assertEquals("recording", RecordingScope.value)
        val definitions = viewModelModule.mappings.keys
        assertEquals(1, definitions.count { "recording" in it }, "the named recording view model is registered once")
    }
}
