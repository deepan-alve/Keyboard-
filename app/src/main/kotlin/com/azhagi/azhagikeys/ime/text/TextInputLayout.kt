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

package com.azhagi.azhagikeys.ime.text

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import com.azhagi.azhagikeys.R
import com.azhagi.azhagikeys.app.AzhagiPreferenceModel
import com.azhagi.azhagikeys.ime.smartbar.IncognitoDisplayMode
import com.azhagi.azhagikeys.ime.smartbar.InlineSuggestionsStyleCache
import com.azhagi.azhagikeys.ime.smartbar.Smartbar
import com.azhagi.azhagikeys.ime.smartbar.quickaction.QuickActionsOverflowPanel
import com.azhagi.azhagikeys.ime.text.keyboard.TextKeyboardLayout
import com.azhagi.azhagikeys.ime.theme.AzhagiImeUi
import com.azhagi.azhagikeys.keyboardManager
import dev.patrickgold.jetpref.datastore.model.observeAsState
import org.azhagi.lib.snygg.ui.SnyggIcon

@Composable
fun TextInputLayout(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()

    val prefs by AzhagiPreferenceModel()

    val state by keyboardManager.activeState.collectAsState()
    val evaluator by keyboardManager.activeEvaluator.collectAsState()

    InlineSuggestionsStyleCache()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        ) {
            Smartbar()
            if (state.isActionsOverflowVisible) {
                QuickActionsOverflowPanel()
            } else {
                Box {
                    val incognitoDisplayMode by prefs.keyboard.incognitoDisplayMode.observeAsState()
                    val showIncognitoIcon = evaluator.state.isIncognitoMode &&
                        incognitoDisplayMode == IncognitoDisplayMode.DISPLAY_BEHIND_KEYBOARD
                    if (showIncognitoIcon) {
                        SnyggIcon(
                            AzhagiImeUi.IncognitoModeIndicator.elementName,
                            modifier = Modifier
                                .matchParentSize()
                                .align(Alignment.Center),
                            painter = painterResource(R.drawable.ic_incognito),
                        )
                    }
                    TextKeyboardLayout(evaluator = evaluator)
                }
            }
        }
    }
}

