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

package com.azhagi.azhagikeys.ime.keyboard

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import com.azhagi.azhagikeys.R
import com.azhagi.azhagikeys.app.AzhagiPreferenceModel
import com.azhagi.azhagikeys.ime.onehanded.OneHandedMode
import com.azhagi.azhagikeys.ime.smartbar.ExtendedActionsPlacement
import com.azhagi.azhagikeys.ime.smartbar.SmartbarLayout
import com.azhagi.azhagikeys.ime.text.keyboard.TextKeyboard
import com.azhagi.azhagikeys.keyboardManager
import com.azhagi.azhagikeys.lib.observeAsTransformingState
import com.azhagi.azhagikeys.lib.util.ViewUtils
import dev.patrickgold.jetpref.datastore.model.observeAsState
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.android.isOrientationLandscape

private val LocalKeyboardRowBaseHeight = staticCompositionLocalOf { 65.dp }
private val LocalSmartbarHeight = staticCompositionLocalOf { 40.dp }

object AzhagiImeSizing {
    val keyboardRowBaseHeight: Dp
        @Composable
        @ReadOnlyComposable
        get() = LocalKeyboardRowBaseHeight.current

    val smartbarHeight: Dp
        @Composable
        @ReadOnlyComposable
        get() = LocalSmartbarHeight.current

    @Composable
    fun keyboardUiHeight(): Dp {
        val context = LocalContext.current
        val keyboardManager by context.keyboardManager()
        val evaluator by keyboardManager.activeEvaluator.collectAsState()
        val lastCharactersEvaluator by keyboardManager.lastCharactersEvaluator.collectAsState()
        val rowCount = when (evaluator.keyboard.mode) {
            KeyboardMode.CHARACTERS,
            KeyboardMode.NUMERIC_ADVANCED,
            KeyboardMode.SYMBOLS,
            KeyboardMode.SYMBOLS2 -> lastCharactersEvaluator.keyboard as TextKeyboard
            else -> evaluator.keyboard as TextKeyboard
        }.rowCount.coerceAtLeast(4)
        return (keyboardRowBaseHeight * rowCount)
    }

    @Composable
    fun smartbarUiHeight(): Dp {
        val prefs by AzhagiPreferenceModel()
        val smartbarEnabled by prefs.smartbar.enabled.observeAsState()
        val smartbarLayout by prefs.smartbar.layout.observeAsState()
        val extendedActionsExpanded by prefs.smartbar.extendedActionsExpanded.observeAsState()
        val extendedActionsPlacement by prefs.smartbar.extendedActionsPlacement.observeAsState()
        val height =
            if (smartbarEnabled) {
                if (smartbarLayout == SmartbarLayout.SUGGESTIONS_ACTIONS_EXTENDED && extendedActionsExpanded &&
                    extendedActionsPlacement != ExtendedActionsPlacement.OVERLAY_APP_UI) {
                    smartbarHeight * 2
                } else {
                    smartbarHeight
                }
            } else {
                0.dp
            }
        return height
    }

    @Composable
    fun imeUiHeight(): Dp {
        return keyboardUiHeight() + smartbarUiHeight()
    }

    object Static {
        var keyboardRowBaseHeightPx: Int = 0

        var smartbarHeightPx: Int = 0
    }
}

@Composable
fun ProvideKeyboardRowBaseHeight(content: @Composable () -> Unit) {
    val prefs by AzhagiPreferenceModel()
    val resources = LocalContext.current.resources
    val configuration = LocalConfiguration.current

    val heightFactorPortrait by prefs.keyboard.heightFactorPortrait.observeAsTransformingState { it.toFloat() / 100f }
    val heightFactorLandscape by prefs.keyboard.heightFactorLandscape.observeAsTransformingState { it.toFloat() / 100f }
    val oneHandedMode by prefs.keyboard.oneHandedModeEnabled.observeAsState()
    val oneHandedModeScaleFactor by prefs.keyboard.oneHandedModeScaleFactor.observeAsTransformingState { it.toFloat() / 100f }

    // Only set systemBarHeights on api 35 or later because https://developer.android.com/about/versions/15/behavior-changes-15#stable-configuration
    val systemBarHeights = if (AndroidVersion.ATLEAST_API35_V) {
        systemBarHeights()
    } else {
        0
    }
    val baseRowHeight = remember(
        configuration, resources, heightFactorPortrait, heightFactorLandscape,
        oneHandedMode, oneHandedModeScaleFactor, systemBarHeights,
    ) {
        calcInputViewHeight(resources, systemBarHeights) * when {
            configuration.isOrientationLandscape() -> heightFactorLandscape
            else -> heightFactorPortrait * (if (oneHandedMode) oneHandedModeScaleFactor else 1f)
        }
    }
    val smartbarHeight = baseRowHeight * 0.753f

    SideEffect {
        AzhagiImeSizing.Static.keyboardRowBaseHeightPx = baseRowHeight.toInt()
        AzhagiImeSizing.Static.smartbarHeightPx = smartbarHeight.toInt()
    }

    CompositionLocalProvider(
        LocalKeyboardRowBaseHeight provides ViewUtils.px2dp(baseRowHeight).dp,
        LocalSmartbarHeight provides ViewUtils.px2dp(smartbarHeight).dp,
    ) {
        content()
    }
}

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun systemBarHeights(): Int {
    val view = LocalView.current
    val context = LocalContext.current

    // Get the navigationBarHeight
    val insets = WindowInsetsCompat.toWindowInsetsCompat(view.rootWindowInsets)
    val navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

    // Use windowManager because the IME ui does not have statusBars insets
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val metrics = windowManager.currentWindowMetrics
    val statusBarHeight = metrics.windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top

    return navigationBarHeight + statusBarHeight
}

/**
 * Calculates the input view height based on the current screen dimensions and the auto
 * selected dimension values.
 *
 * This method and the fraction values have been inspired by [OpenBoard](https://github.com/dslul/openboard)
 * but are not 1:1 the same. This implementation differs from the
 * [original](https://github.com/dslul/openboard/blob/90ae4c8aec034a8935e1fd02b441be25c7dba6ce/app/src/main/java/org/dslul/openboard/inputmethod/latin/utils/ResourceUtils.java)
 * by calculating the average of the min and max height values, then taking at least the input
 * view base height and return this resulting value.
 */
private fun calcInputViewHeight(resources: Resources, systemBarHeights: Int): Float {
    val dm = resources.displayMetrics
    val height = dm.heightPixels - systemBarHeights
    val width = dm.widthPixels
    val minBaseSize = when {
        resources.configuration.isOrientationLandscape() -> resources.getFraction(
            R.fraction.inputView_minHeightFraction, height, height
        )
        else -> resources.getFraction(
            R.fraction.inputView_minHeightFraction, width, width
        )
    }
    val maxBaseSize = resources.getFraction(
        R.fraction.inputView_maxHeightFraction, height, height
    )
    return ((minBaseSize + maxBaseSize) / 2.0f).coerceAtLeast(
        resources.getDimension(R.dimen.inputView_baseHeight)
    ) * 0.21f
}

