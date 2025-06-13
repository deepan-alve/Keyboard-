/*
 * Copyright (C) 2021-2025 The AzhagiKeys Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.azhagi.azhagikeys

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import androidx.core.os.UserManagerCompat
import com.azhagi.azhagikeys.app.AzhagiPreferenceModel
import com.azhagi.azhagikeys.ime.clipboard.ClipboardManager
import com.azhagi.azhagikeys.ime.core.SubtypeManager
import com.azhagi.azhagikeys.ime.dictionary.DictionaryManager
import com.azhagi.azhagikeys.ime.editor.EditorInstance
import com.azhagi.azhagikeys.ime.keyboard.KeyboardManager
import com.azhagi.azhagikeys.ime.media.emoji.AzhagiEmojiCompat
import com.azhagi.azhagikeys.ime.nlp.NlpManager
import com.azhagi.azhagikeys.ime.text.gestures.GlideTypingManager
import com.azhagi.azhagikeys.ime.theme.ThemeManager
import com.azhagi.azhagikeys.lib.cache.CacheManager
import com.azhagi.azhagikeys.lib.crashutility.CrashUtility
import com.azhagi.azhagikeys.lib.devtools.Flog
import com.azhagi.azhagikeys.lib.devtools.LogTopic
import com.azhagi.azhagikeys.lib.devtools.flogError
import com.azhagi.azhagikeys.lib.ext.ExtensionManager
import dev.patrickgold.jetpref.datastore.JetPref
import org.florisboard.lib.kotlin.io.deleteContentsRecursively
import org.florisboard.lib.kotlin.tryOrNull
import org.florisboard.libnative.dummyAdd
import java.lang.ref.WeakReference

/**
 * Global weak reference for the [AzhagiApplication] class. This is needed as in certain scenarios an application
 * reference is needed, but the Android framework hasn't finished setting up
 */
private var AzhagiApplicationReference = WeakReference<AzhagiApplication?>(null)

@Suppress("unused")
class AzhagiApplication : Application() {
    companion object {
        init {
            try {
                System.loadLibrary("fl_native")
            } catch (_: Exception) {
            }
        }
    }

    private val prefs by AzhagiPreferenceModel()
    private val mainHandler by lazy { Handler(mainLooper) }

    val cacheManager = lazy { CacheManager(this) }
    val clipboardManager = lazy { ClipboardManager(this) }
    val editorInstance = lazy { EditorInstance(this) }
    val extensionManager = lazy { ExtensionManager(this) }
    val glideTypingManager = lazy { GlideTypingManager(this) }
    val keyboardManager = lazy { KeyboardManager(this) }
    val nlpManager = lazy { NlpManager(this) }
    val subtypeManager = lazy { SubtypeManager(this) }
    val themeManager = lazy { ThemeManager(this) }

    override fun onCreate() {
        super.onCreate()
        AzhagiApplicationReference = WeakReference(this)
        try {
            JetPref.configure(saveIntervalMs = 500)
            Flog.install(
                context = this,
                isFloggingEnabled = BuildConfig.DEBUG,
                flogTopics = LogTopic.ALL,
                flogLevels = Flog.LEVEL_ALL,
                flogOutputs = Flog.OUTPUT_CONSOLE,
            )
            CrashUtility.install(this)
            AzhagiEmojiCompat.init(this)
            flogError { "dummy result: ${dummyAdd(3,4)}" }

            if (!UserManagerCompat.isUserUnlocked(this)) {
                cacheDir?.deleteContentsRecursively()
                extensionManager.value.init()
                registerReceiver(BootComplete(), IntentFilter(Intent.ACTION_USER_UNLOCKED))
                return
            }

            init()
        } catch (e: Exception) {
            CrashUtility.stageException(e)
            return
        }
    }

    fun init() {
        cacheDir?.deleteContentsRecursively()
        prefs.initializeBlocking(this)
        extensionManager.value.init()
        clipboardManager.value.initializeForContext(this)
        DictionaryManager.init(this)
    }

    private inner class BootComplete : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            if (intent.action == Intent.ACTION_USER_UNLOCKED) {
                try {
                    unregisterReceiver(this)
                } catch (e: Exception) {
                    flogError { e.toString() }
                }
                mainHandler.post { init() }
            }
        }
    }
}

private tailrec fun Context.AzhagiApplication(): AzhagiApplication {
    return when (this) {
        is AzhagiApplication -> this
        is ContextWrapper -> when {
            this.baseContext != null -> this.baseContext.AzhagiApplication()
            else -> AzhagiApplicationReference.get()!!
        }
        else -> tryOrNull { this.applicationContext as AzhagiApplication } ?: AzhagiApplicationReference.get()!!
    }
}

fun Context.appContext() = lazyOf(this.AzhagiApplication())

fun Context.cacheManager() = this.AzhagiApplication().cacheManager

fun Context.clipboardManager() = this.AzhagiApplication().clipboardManager

fun Context.editorInstance() = this.AzhagiApplication().editorInstance

fun Context.extensionManager() = this.AzhagiApplication().extensionManager

fun Context.glideTypingManager() = this.AzhagiApplication().glideTypingManager

fun Context.keyboardManager() = this.AzhagiApplication().keyboardManager

fun Context.nlpManager() = this.AzhagiApplication().nlpManager

fun Context.subtypeManager() = this.AzhagiApplication().subtypeManager

fun Context.themeManager() = this.AzhagiApplication().themeManager

