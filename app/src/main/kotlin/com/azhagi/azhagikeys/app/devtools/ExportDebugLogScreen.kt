/*
 * Copyright (C) 2022-2025 The AzhagiKeys Contributors
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

package com.azhagi.azhagikeys.app.devtools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.azhagi.azhagikeys.R
import com.azhagi.azhagikeys.app.AzhagiPreferenceModel
import com.azhagi.azhagikeys.clipboardManager
import com.azhagi.azhagikeys.lib.compose.AzhagiButton
import com.azhagi.azhagikeys.lib.compose.AzhagiScreen
import com.azhagi.azhagikeys.lib.compose.AzhagiHorizontalScroll
import com.azhagi.azhagikeys.lib.compose.AzhagiScrollbar
import com.azhagi.azhagikeys.lib.compose.stringRes
import com.azhagi.azhagikeys.lib.devtools.Devtools
import org.florisboard.lib.android.showShortToast

// TODO: This screen is just a quick thrown-together thing and needs further enhancing in the UI
@Composable
fun ExportDebugLogScreen() = AzhagiScreen {
    title = stringRes(R.string.devtools__debuglog__title)
    scrollable = false

    val prefs by AzhagiPreferenceModel()
    val context = LocalContext.current
    val clipboardManager by context.clipboardManager()

    var debugLog by remember { mutableStateOf<List<String>?>(null) }
    var formattedDebugLog by remember { mutableStateOf<List<String>?>(null) }

    LaunchedEffect(Unit) {
        debugLog = Devtools.generateDebugLog(context, prefs, includeLogcat = true).lines()
        formattedDebugLog = Devtools.generateDebugLogForGithub(context, prefs, includeLogcat = true).lines()
    }

    bottomBar {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AzhagiButton(
                onClick = {
                    clipboardManager.addNewPlaintext(debugLog!!.joinToString("\n"))
                    context.showShortToast(context.getString(R.string.devtools__debuglog__copied_to_clipboard))
                },
                modifier = Modifier,
                text = stringRes(R.string.devtools__debuglog__copy_log),
                enabled = debugLog != null,
            )
            AzhagiButton(
                onClick = {
                    clipboardManager.addNewPlaintext(formattedDebugLog!!.joinToString("\n"))
                    context.showShortToast(context.getString(R.string.devtools__debuglog__copied_to_clipboard))
                },
                text = stringRes(R.string.devtools__debuglog__copy_for_github),
                enabled = debugLog != null,
            )
        }
    }

    content {
        // Forcing LTR because text displayed is a debug log
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            val lazyListState = rememberLazyListState()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .AzhagiScrollbar(lazyListState, isVertical = true)
                    .AzhagiHorizontalScroll(),
                state = lazyListState,
            ) {
                val log = debugLog
                if (log == null) {
                    item {
                        Text(stringRes(R.string.devtools__debuglog__loading))
                    }
                } else {
                    items(log) { logLine ->
                        Text(
                            text = logLine,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

