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

package com.azhagi.azhagikeys.app

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.azhagi.azhagikeys.R
import com.azhagi.azhagikeys.app.apptheme.AzhagiAppTheme
import com.azhagi.azhagikeys.app.ext.ExtensionImportScreenType
import com.azhagi.azhagikeys.app.setup.NotificationPermissionState
import com.azhagi.azhagikeys.cacheManager
import com.azhagi.azhagikeys.lib.AzhagiLocale
import com.azhagi.azhagikeys.lib.compose.LocalPreviewFieldController
import com.azhagi.azhagikeys.lib.compose.PreviewKeyboardField
import com.azhagi.azhagikeys.lib.compose.ProvideLocalizedResources
import com.azhagi.azhagikeys.lib.compose.conditional
import com.azhagi.azhagikeys.lib.compose.rememberPreviewFieldController
import com.azhagi.azhagikeys.lib.compose.stringRes
import com.azhagi.azhagikeys.lib.util.AppVersionUtils
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.datastore.ui.ProvideDefaultDialogPrefStrings
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.android.hideAppIcon
import org.florisboard.lib.android.showAppIcon

enum class AppTheme(val id: String) {
    AUTO("auto"),
    AUTO_AMOLED("auto_amoled"),
    LIGHT("light"),
    DARK("dark"),
    AMOLED_DARK("amoled_dark");
}

val LocalNavController = staticCompositionLocalOf<NavController> {
    error("LocalNavController not initialized")
}

class AzhagiAppActivity : ComponentActivity() {
    private val prefs by AzhagiPreferenceModel()
    private val cacheManager by cacheManager()
    private var appTheme by mutableStateOf(AppTheme.AUTO)
    private var showAppIcon = true
    private var resourcesContext by mutableStateOf(this as Context)
    private var intentToBeHandled by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen should be installed before calling super.onCreate()
        installSplashScreen().apply {
            setKeepOnScreenCondition { !prefs.datastoreReadyStatus.get() }
        }
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        prefs.other.settingsTheme.observe(this) {
            appTheme = it
        }
        prefs.other.settingsLanguage.observe(this) {
            val config = Configuration(resources.configuration)
            val locale = if (it == "auto") AzhagiLocale.default() else AzhagiLocale.fromTag(it)
            config.setLocale(locale.base)
            resourcesContext = createConfigurationContext(config)
        }
        if (AndroidVersion.ATMOST_API28_P) {
            prefs.other.showAppIcon.observe(this) {
                showAppIcon = it
            }
        }

        //Check if android 13+ is running and the NotificationPermission is not set
        if (AndroidVersion.ATLEAST_API33_T &&
            prefs.internal.notificationPermissionState.get() == NotificationPermissionState.NOT_SET
        ) {
            // update pref value to show the setup screen again again
            prefs.internal.isImeSetUp.set(false)
        }

        // We defer the setContent call until the datastore model is loaded, until then the splash screen stays drawn
        prefs.datastoreReadyStatus.observe(this) { isModelLoaded ->
            if (!isModelLoaded) return@observe
            AppVersionUtils.updateVersionOnInstallAndLastUse(this, prefs)
            setContent {
                ProvideLocalizedResources(resourcesContext) {
                    AzhagiAppTheme(theme = appTheme) {
                        Surface(color = MaterialTheme.colorScheme.background) {
                            AppContent()
                        }
                    }
                }
            }
            onNewIntent(intent)
        }
    }

    override fun onPause() {
        super.onPause()

        // App icon visibility control was restricted in Android 10.
        // See https://developer.android.com/reference/android/content/pm/LauncherApps#getActivityList(java.lang.String,%20android.os.UserHandle)
        if (AndroidVersion.ATMOST_API28_P) {
            if (showAppIcon) {
                this.showAppIcon()
            } else {
                this.hideAppIcon()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (intent.action == Intent.ACTION_VIEW && intent.categories?.contains(Intent.CATEGORY_BROWSABLE) == true) {
            intentToBeHandled = intent
            return
        }
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            intentToBeHandled = intent
            return
        }
        if (intent.action == Intent.ACTION_SEND && intent.clipData != null) {
            intentToBeHandled = intent
            return
        }
        intentToBeHandled = null
    }

    @Composable
    private fun AppContent() {
        val navController = rememberNavController()
        val previewFieldController = rememberPreviewFieldController()

        val isImeSetUp by prefs.internal.isImeSetUp.observeAsState()

        CompositionLocalProvider(
            LocalNavController provides navController,
            LocalPreviewFieldController provides previewFieldController,
        ) {
            ProvideDefaultDialogPrefStrings(
                confirmLabel = stringRes(R.string.action__ok),
                dismissLabel = stringRes(R.string.action__cancel),
                neutralLabel = stringRes(R.string.action__default),
            ) {
                Column(
                    modifier = Modifier
                        //.statusBarsPadding()
                        .navigationBarsPadding()
                        .conditional(LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            displayCutoutPadding()
                        }
                        .imePadding(),
                ) {
                    Routes.AppNavHost(
                        modifier = Modifier.weight(1.0f),
                        navController = navController,
                        startDestination = if (isImeSetUp) Routes.Settings.Home else Routes.Setup.Screen,
                    )
                    PreviewKeyboardField(previewFieldController)
                }
            }
        }

        LaunchedEffect(intentToBeHandled) {
            val intent = intentToBeHandled
            if (intent != null) {
                if (intent.action == Intent.ACTION_VIEW && intent.categories?.contains(Intent.CATEGORY_BROWSABLE) == true) {
                    navController.handleDeepLink(intent)
                } else {
                    val data = if (intent.action == Intent.ACTION_VIEW) {
                        intent.data!!
                    } else {
                        intent.clipData!!.getItemAt(0).uri
                    }
                    val workspace = runCatching { cacheManager.readFromUriIntoCache(data) }.getOrNull()
                    navController.navigate(Routes.Ext.Import(ExtensionImportScreenType.EXT_ANY, workspace?.uuid))
                }
            }
            intentToBeHandled = null
        }
    }
}

