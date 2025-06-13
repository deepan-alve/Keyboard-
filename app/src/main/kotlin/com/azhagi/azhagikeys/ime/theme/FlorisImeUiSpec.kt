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

package com.azhagi.azhagikeys.ime.theme

/*
import org.azhagi.lib.snygg.Snygg
import org.azhagi.lib.snygg.SnyggLevel
import org.azhagi.lib.snygg.SnyggPropertySetSpecDeclBuilder
import org.azhagi.lib.snygg.SnyggSpecDecl
import org.azhagi.lib.snygg.value.SnyggCircleShapeValue
import org.azhagi.lib.snygg.value.SnyggCutCornerDpShapeValue
import org.azhagi.lib.snygg.value.SnyggCutCornerPercentShapeValue
import org.azhagi.lib.snygg.value.SnyggDpSizeValue
import org.azhagi.lib.snygg.value.SnyggDynamicColorDarkColorValue
import org.azhagi.lib.snygg.value.SnyggDynamicColorLightColorValue
import org.azhagi.lib.snygg.value.SnyggRectangleShapeValue
import org.azhagi.lib.snygg.value.SnyggRoundedCornerDpShapeValue
import org.azhagi.lib.snygg.value.SnyggRoundedCornerPercentShapeValue
import org.azhagi.lib.snygg.value.SnyggStaticColorValue
import org.azhagi.lib.snygg.value.SnyggSpSizeValue

fun SnyggPropertySetSpecDeclBuilder.background() {
    property(
        name = Snygg.Background,
        level = SnyggLevel.BASIC,
        supportedValues(SnyggStaticColorValue, SnyggDynamicColorLightColorValue, SnyggDynamicColorDarkColorValue),
    )
}
fun SnyggPropertySetSpecDeclBuilder.foreground() {
    property(
        name = Snygg.Foreground,
        level = SnyggLevel.BASIC,
        supportedValues(SnyggStaticColorValue, SnyggDynamicColorLightColorValue, SnyggDynamicColorDarkColorValue),
    )
}
fun SnyggPropertySetSpecDeclBuilder.border() {
    property(
        name = Snygg.BorderColor,
        level = SnyggLevel.ADVANCED,
        supportedValues(SnyggStaticColorValue, SnyggDynamicColorLightColorValue, SnyggDynamicColorDarkColorValue),
    )
    property(
        name = Snygg.BorderWidth,
        level = SnyggLevel.ADVANCED,
        supportedValues(SnyggDpSizeValue),
    )
}
fun SnyggPropertySetSpecDeclBuilder.font() {
    property(
        name = Snygg.FontSize,
        level = SnyggLevel.ADVANCED,
        supportedValues(SnyggSpSizeValue),
    )
}
fun SnyggPropertySetSpecDeclBuilder.shadow() {
    property(
        name = Snygg.ShadowElevation,
        level = SnyggLevel.ADVANCED,
        supportedValues(SnyggDpSizeValue),
    )
}
fun SnyggPropertySetSpecDeclBuilder.shape() {
    property(
        name = Snygg.Shape,
        level = SnyggLevel.ADVANCED,
        supportedValues(
            SnyggRectangleShapeValue,
            SnyggCircleShapeValue,
            SnyggRoundedCornerDpShapeValue,
            SnyggRoundedCornerPercentShapeValue,
            SnyggCutCornerDpShapeValue,
            SnyggCutCornerPercentShapeValue,
        ),
    )
}

object AzhagiImeUiSpec : SnyggSpecDecl({
    element(AzhagiImeUi.Keyboard) {
        background()
    }
    element(AzhagiImeUi.Key) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }
    element(AzhagiImeUi.KeyHint) {
        background()
        foreground()
        font()
        shape()
    }
    element(AzhagiImeUi.KeyPopup) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }

    element(AzhagiImeUi.Smartbar) {
        background()
    }
    element(AzhagiImeUi.SmartbarSharedActionsRow) {
        background()
    }
    element(AzhagiImeUi.SmartbarSharedActionsToggle) {
        background()
        foreground()
        shape()
        shadow()
        border()
    }
    element(AzhagiImeUi.SmartbarExtendedActionsRow) {
        background()
    }
    element(AzhagiImeUi.SmartbarExtendedActionsToggle) {
        background()
        foreground()
        shape()
        shadow()
        border()
    }
    element(AzhagiImeUi.SmartbarActionKey) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }
    element(AzhagiImeUi.SmartbarActionTile) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }
    element(AzhagiImeUi.SmartbarActionsOverflowCustomizeButton) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }
    element(AzhagiImeUi.SmartbarActionsOverflow) {
        background()
    }
    element(AzhagiImeUi.SmartbarActionsEditor) {
        background()
        shape()
    }
    element(AzhagiImeUi.SmartbarActionsEditorHeader) {
        background()
        foreground()
        font()
    }
    element(AzhagiImeUi.SmartbarActionsEditorSubheader) {
        foreground()
        font()
    }
    element(AzhagiImeUi.SmartbarCandidatesRow) {
        background()
    }
    element(AzhagiImeUi.SmartbarCandidateWord) {
        background()
        foreground()
        font()
        shape()
    }
    element(AzhagiImeUi.SmartbarCandidateClip) {
        background()
        foreground()
        font()
        shape()
    }
    element(AzhagiImeUi.SmartbarCandidateSpacer) {
        foreground()
    }

    element(AzhagiImeUi.ClipboardHeader) {
        background()
        foreground()
        font()
    }
    element(AzhagiImeUi.ClipboardItem) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }
    element(AzhagiImeUi.ClipboardItemPopup) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }
    element(AzhagiImeUi.ClipboardEnableHistoryButton) {
        background()
        foreground()
        shape()
    }

    element(AzhagiImeUi.EmojiKey) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }
    element(AzhagiImeUi.EmojiKeyPopup) {
        background()
        foreground()
        font()
        shape()
        shadow()
        border()
    }
    element(AzhagiImeUi.EmojiTab) {
        foreground()
    }

    element(AzhagiImeUi.ExtractedLandscapeInputLayout) {
        background()
    }
    element(AzhagiImeUi.ExtractedLandscapeInputField) {
        background()
        foreground()
        font()
        shape()
        border()
    }
    element(AzhagiImeUi.ExtractedLandscapeInputAction) {
        background()
        foreground()
        shape()
    }

    element(AzhagiImeUi.GlideTrail) {
        foreground()
    }

    element(AzhagiImeUi.IncognitoModeIndicator) {
        foreground()
    }

    element(AzhagiImeUi.OneHandedPanel) {
        background()
        foreground()
    }

    element(AzhagiImeUi.SystemNavBar) {
        background()
    }
})

Snygg.init(
            stylesheetSpec = AzhagiImeUiSpec,
            rulePreferredElementSorting = listOf(
                AzhagiImeUi.Keyboard,
                AzhagiImeUi.Key,
                AzhagiImeUi.KeyHint,
                AzhagiImeUi.KeyPopup,
                AzhagiImeUi.Smartbar,
                AzhagiImeUi.SmartbarSharedActionsRow,
                AzhagiImeUi.SmartbarSharedActionsToggle,
                AzhagiImeUi.SmartbarExtendedActionsRow,
                AzhagiImeUi.SmartbarExtendedActionsToggle,
                AzhagiImeUi.SmartbarActionKey,
                AzhagiImeUi.SmartbarActionTile,
                AzhagiImeUi.SmartbarActionsOverflow,
                AzhagiImeUi.SmartbarActionsOverflowCustomizeButton,
                AzhagiImeUi.SmartbarActionsEditor,
                AzhagiImeUi.SmartbarActionsEditorHeader,
                AzhagiImeUi.SmartbarActionsEditorSubheader,
                AzhagiImeUi.SmartbarCandidatesRow,
                AzhagiImeUi.SmartbarCandidateWord,
                AzhagiImeUi.SmartbarCandidateClip,
                AzhagiImeUi.SmartbarCandidateSpacer,
            ),
            rulePlaceholders = mapOf(
                "c:delete" to KeyCode.DELETE,
                "c:enter" to KeyCode.ENTER,
                "c:shift" to KeyCode.SHIFT,
                "c:space" to KeyCode.SPACE,
                "sh:unshifted" to InputShiftState.UNSHIFTED.value,
                "sh:shifted_manual" to InputShiftState.SHIFTED_MANUAL.value,
                "sh:shifted_automatic" to InputShiftState.SHIFTED_AUTOMATIC.value,
                "sh:caps_lock" to InputShiftState.CAPS_LOCK.value,
            ),
        )
*/

