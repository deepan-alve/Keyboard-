/*
 * Copyright (C) 2024-2025 The AzhagiKeys Contributors
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

package com.azhagi.azhagikeys.ime.smartbar

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.azhagi.azhagikeys.app.AzhagiPreferenceModel
import com.azhagi.azhagikeys.ime.nlp.ClipboardSuggestionCandidate
import com.azhagi.azhagikeys.ime.nlp.SuggestionCandidate
import com.azhagi.azhagikeys.ime.theme.AzhagiImeUi
import com.azhagi.azhagikeys.keyboardManager
import com.azhagi.azhagikeys.lib.compose.conditional
import com.azhagi.azhagikeys.lib.compose.AzhagiHorizontalScroll
import com.azhagi.azhagikeys.nlpManager
import com.azhagi.azhagikeys.subtypeManager
import dev.patrickgold.jetpref.datastore.model.observeAsState
import org.azhagi.lib.snygg.SnyggSelector
import org.azhagi.lib.snygg.ui.SnyggBox
import org.azhagi.lib.snygg.ui.SnyggColumn
import org.azhagi.lib.snygg.ui.SnyggIcon
import org.azhagi.lib.snygg.ui.SnyggRow
import org.azhagi.lib.snygg.ui.SnyggSpacer
import org.azhagi.lib.snygg.ui.SnyggText

val CandidatesRowScrollbarHeight = 2.dp

@Composable
fun CandidatesRow(modifier: Modifier = Modifier) {
    val prefs by AzhagiPreferenceModel()
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val nlpManager by context.nlpManager()
    val subtypeManager by context.subtypeManager()

    val displayMode by prefs.suggestion.displayMode.observeAsState()
    val candidates by nlpManager.activeCandidatesFlow.collectAsState()

    SnyggRow(
        elementName = AzhagiImeUi.SmartbarCandidatesRow.elementName,
        modifier = modifier
            .fillMaxSize()
            .conditional(displayMode == CandidatesDisplayMode.DYNAMIC_SCROLLABLE && candidates.size > 1) {
                AzhagiHorizontalScroll(scrollbarHeight = CandidatesRowScrollbarHeight)
            },
        horizontalArrangement = if (candidates.size > 1) {
            Arrangement.Start
        } else {
            Arrangement.Center
        },
    ) {
        if (candidates.isNotEmpty()) {
            val candidateModifier = if (candidates.size == 1) {
                Modifier
                    .fillMaxHeight()
                    .weight(1f, fill = false)
            } else {
                Modifier
                    .fillMaxHeight()
                    .conditional(displayMode == CandidatesDisplayMode.CLASSIC) {
                        weight(1f)
                    }
                    .conditional(displayMode != CandidatesDisplayMode.CLASSIC) {
                        wrapContentWidth().widthIn(max = 160.dp)
                    }
            }
            val list = when (displayMode) {
                CandidatesDisplayMode.CLASSIC -> candidates.subList(0, 3.coerceAtMost(candidates.size))
                else -> candidates
            }
            for ((n, candidate) in list.withIndex()) {
                if (n > 0) {
                    SnyggSpacer(
                        elementName = AzhagiImeUi.SmartbarCandidateSpacer.elementName,
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight(0.6f)
                            .align(Alignment.CenterVertically),
                    )
                }
                CandidateItem(
                    modifier = candidateModifier,
                    candidate = candidate,
                    displayMode = displayMode,
                    onClick = {
                        // Can't use candidate directly
                        keyboardManager.commitCandidate(candidates[n])
                    },
                    onLongPress = {
                        // Can't use candidate directly
                        val candidateItem = candidates[n]
                        if (candidateItem.isEligibleForUserRemoval) {
                            nlpManager.removeSuggestion(subtypeManager.activeSubtype, candidateItem)
                        } else {
                            false
                        }
                    },
                    longPressDelay = prefs.keyboard.longPressDelay.get().toLong(),
                )
            }
        }
    }
}

@Composable
private fun CandidateItem(
    candidate: SuggestionCandidate,
    displayMode: CandidatesDisplayMode,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = { },
    onLongPress: () -> Boolean = { false },
    longPressDelay: Long,
) = with(LocalDensity.current) {
    var isPressed by remember { mutableStateOf(false) }

    val elementName = if (candidate is ClipboardSuggestionCandidate) {
        AzhagiImeUi.SmartbarCandidateClip
    } else {
        AzhagiImeUi.SmartbarCandidateWord
    }.elementName
    val attributes = mapOf("auto-commit" to if (candidate.isEligibleForAutoCommit) 1 else 0)
    val selector = if (isPressed) SnyggSelector.PRESSED else SnyggSelector.NONE

    SnyggRow(
        elementName = elementName,
        attributes = attributes,
        selector = selector,
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    isPressed = true
                    if (down.pressed != down.previousPressed) down.consume()
                    var upOrCancel: PointerInputChange? = null
                    try {
                        upOrCancel = withTimeout(longPressDelay) {
                            waitForUpOrCancellation()
                        }
                        upOrCancel?.let { if (it.pressed != it.previousPressed) it.consume() }
                    } catch (_: PointerEventTimeoutCancellationException) {
                        if (onLongPress()) {
                            upOrCancel = null
                            isPressed = false
                        }
                        waitForUpOrCancellation()?.let { if (it.pressed != it.previousPressed) it.consume() }
                    }
                    if (upOrCancel != null) {
                        onClick()
                    }
                    isPressed = false
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (candidate.icon != null) {
            SnyggBox(
                elementName = "$elementName-icon",
                attributes = attributes,
                selector = selector,
            ) {
                SnyggIcon(imageVector = candidate.icon!!)
            }
        }
        SnyggColumn(
            modifier = if (displayMode == CandidatesDisplayMode.CLASSIC) Modifier.weight(1f) else Modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SnyggText(
                elementName = "$elementName-text",
                attributes = attributes,
                selector = selector,
                text = candidate.text.toString(),
            )
            if (candidate.secondaryText != null) {
                SnyggText(
                    elementName = "$elementName-secondary-text",
                    attributes = attributes,
                    selector = selector,
                    text = candidate.secondaryText!!.toString(),
                )
            }
        }
    }
}

