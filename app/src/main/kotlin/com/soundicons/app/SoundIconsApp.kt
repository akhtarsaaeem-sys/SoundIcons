package com.soundicons.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class required by Hilt for dependency injection.
 */
@HiltAndroidApp
class SoundIconsApp : Application()
