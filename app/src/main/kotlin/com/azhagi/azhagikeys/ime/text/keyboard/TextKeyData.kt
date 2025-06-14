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

package com.azhagi.azhagikeys.ime.text.keyboard

import android.icu.lang.UCharacter
import com.azhagi.azhagikeys.ime.keyboard.AbstractKeyData
import com.azhagi.azhagikeys.ime.keyboard.ComputingEvaluator
import com.azhagi.azhagikeys.ime.keyboard.KeyData
import com.azhagi.azhagikeys.ime.popup.PopupSet
import com.azhagi.azhagikeys.ime.text.key.KeyCode
import com.azhagi.azhagikeys.ime.text.key.KeyType
import com.azhagi.azhagikeys.lib.AzhagiLocale
import com.azhagi.azhagikeys.lib.Unicode
import com.azhagi.azhagikeys.lib.lowercase
import com.azhagi.azhagikeys.lib.uppercase
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Data class which describes a single key and its attributes.
 *
 * @property type The type of the key. Some actions require both [code] and [type] to match in order
 *  to be successfully executed. Defaults to [KeyType.CHARACTER].
 * @property code The UTF-8 encoded code of the character. The code defined here is used as the
 *  data passed to the system. Defaults to 0.
 * @property label The string used to display the key in the UI. Is not used for the actual data
 *  passed to the system. Should normally be the exact same as the [code]. Defaults to an empty
 *  string.
 */
@Serializable
@SerialName("text_key")
data class TextKeyData(
    override val type: KeyType = KeyType.CHARACTER,
    override val code: Int = KeyCode.UNSPECIFIED,
    override val label: String = "",
    override val groupId: Int = KeyData.GROUP_DEFAULT,
    override val popup: PopupSet<AbstractKeyData>? = null
) : KeyData {
    override fun compute(evaluator: ComputingEvaluator): KeyData? {
        return if (evaluator.isSlot(this)) {
            evaluator.slotData(this)?.let { data ->
                TextKeyData(type, data.code, data.label, groupId, popup)
            }
        } else {
            this
        }
    }

    override fun asString(isForDisplay: Boolean): String {
        return asString(this, isForDisplay)
    }

    override fun toString(): String {
        return "${TextKeyData::class.simpleName} { type=$type code=$code label=\"$label\" groupId=$groupId }"
    }

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        fun getCodeInfoAsTextKeyData(code: Int): TextKeyData? {
            return if (code <= 0) {
                InternalKeys.find { it.code == code }
            } else {
                TextKeyData(
                    type = KeyType.CHARACTER,
                    code = code,
                    label = buildString {
                        try {
                            appendCodePoint(code)
                        } catch (_: Throwable) {
                        }
                    },
                )
            }
        }

        // TODO: find better solution than to hand define array of below keys...
        private val InternalKeys by lazy {
            listOf(
                UNSPECIFIED,
                SPACE,
                CTRL,
                CTRL_LOCK,
                ALT,
                ALT_LOCK,
                FN,
                FN_LOCK,
                DELETE,
                DELETE_WORD,
                FORWARD_DELETE,
                FORWARD_DELETE_WORD,
                SHIFT,
                CAPS_LOCK,
                ARROW_LEFT,
                ARROW_RIGHT,
                ARROW_UP,
                ARROW_DOWN,
                MOVE_START_OF_PAGE,
                MOVE_END_OF_PAGE,
                MOVE_START_OF_LINE,
                MOVE_END_OF_LINE,
                CLIPBOARD_COPY,
                CLIPBOARD_CUT,
                CLIPBOARD_PASTE,
                CLIPBOARD_SELECT,
                CLIPBOARD_SELECT_ALL,
                CLIPBOARD_CLEAR_HISTORY,
                CLIPBOARD_CLEAR_FULL_HISTORY,
                CLIPBOARD_CLEAR_PRIMARY_CLIP,
                TOGGLE_COMPACT_LAYOUT,
                COMPACT_LAYOUT_TO_LEFT,
                COMPACT_LAYOUT_TO_RIGHT,
                UNDO,
                REDO,
                VIEW_CHARACTERS,
                VIEW_SYMBOLS,
                VIEW_SYMBOLS2,
                VIEW_NUMERIC_ADVANCED,
                IME_UI_MODE_TEXT,
                IME_UI_MODE_MEDIA,
                IME_UI_MODE_CLIPBOARD,
                SYSTEM_INPUT_METHOD_PICKER,
                SYSTEM_PREV_INPUT_METHOD,
                SYSTEM_NEXT_INPUT_METHOD,
                IME_SUBTYPE_PICKER,
                IME_PREV_SUBTYPE,
                IME_NEXT_SUBTYPE,
                LANGUAGE_SWITCH,
                IME_SHOW_UI,
                IME_HIDE_UI,
                SETTINGS,
                VOICE_INPUT,
                TOGGLE_SMARTBAR_VISIBILITY,
                TOGGLE_ACTIONS_OVERFLOW,
                TOGGLE_ACTIONS_EDITOR,
                TOGGLE_INCOGNITO_MODE,
                TOGGLE_AUTOCORRECT,
            )
        }

        /** Predefined key data for [KeyCode.UNSPECIFIED] */
        val UNSPECIFIED = TextKeyData(
            type = KeyType.UNSPECIFIED,
            code = KeyCode.UNSPECIFIED,
            label = "unspecified",
        )

        /** Predefined key data for [KeyCode.SPACE] */
        val SPACE = TextKeyData(
            type = KeyType.CHARACTER,
            code = KeyCode.SPACE,
            label = "space",
        )

        /** Predefined key data for [KeyCode.CTRL] */
        val CTRL = TextKeyData(
            type = KeyType.MODIFIER,
            code = KeyCode.CTRL,
            label = "ctrl",
        )
        /** Predefined key data for [KeyCode.CTRL_LOCK] */
        val CTRL_LOCK = TextKeyData(
            type = KeyType.MODIFIER,
            code = KeyCode.CTRL_LOCK,
            label = "ctrl_lock",
        )
        /** Predefined key data for [KeyCode.ALT] */
        val ALT = TextKeyData(
            type = KeyType.MODIFIER,
            code = KeyCode.ALT,
            label = "alt",
        )
        /** Predefined key data for [KeyCode.ALT_LOCK] */
        val ALT_LOCK = TextKeyData(
            type = KeyType.MODIFIER,
            code = KeyCode.ALT_LOCK,
            label = "alt_lock",
        )
        /** Predefined key data for [KeyCode.FN] */
        val FN = TextKeyData(
            type = KeyType.MODIFIER,
            code = KeyCode.FN,
            label = "fn",
        )
        /** Predefined key data for [KeyCode.FN_LOCK] */
        val FN_LOCK = TextKeyData(
            type = KeyType.MODIFIER,
            code = KeyCode.FN_LOCK,
            label = "fn_lock",
        )
        /** Predefined key data for [KeyCode.DELETE] */
        val DELETE = TextKeyData(
            type = KeyType.ENTER_EDITING,
            code = KeyCode.DELETE,
            label = "delete",
        )
        /** Predefined key data for [KeyCode.DELETE_WORD] */
        val DELETE_WORD = TextKeyData(
            type = KeyType.ENTER_EDITING,
            code = KeyCode.DELETE_WORD,
            label = "delete_word",
        )
        /** Predefined key data for [KeyCode.FORWARD_DELETE] */
        val FORWARD_DELETE = TextKeyData(
            type = KeyType.ENTER_EDITING,
            code = KeyCode.FORWARD_DELETE,
            label = "forward_delete",
        )
        /** Predefined key data for [KeyCode.FORWARD_DELETE_WORD] */
        val FORWARD_DELETE_WORD = TextKeyData(
            type = KeyType.ENTER_EDITING,
            code = KeyCode.FORWARD_DELETE_WORD,
            label = "forward_delete_word",
        )
        /** Predefined key data for [KeyCode.SHIFT] */
        val SHIFT = TextKeyData(
            type = KeyType.MODIFIER,
            code = KeyCode.SHIFT,
            label = "shift",
        )
        /** Predefined key data for [KeyCode.CAPS_LOCK] */
        val CAPS_LOCK = TextKeyData(
            type = KeyType.MODIFIER,
            code = KeyCode.CAPS_LOCK,
            label = "caps_lock",
        )

        /** Predefined key data for [KeyCode.ARROW_LEFT] */
        val ARROW_LEFT = TextKeyData(
            type = KeyType.NAVIGATION,
            code = KeyCode.ARROW_LEFT,
            label = "arrow_left",
        )
        /** Predefined key data for [KeyCode.ARROW_RIGHT] */
        val ARROW_RIGHT = TextKeyData(
            type = KeyType.NAVIGATION,
            code = KeyCode.ARROW_RIGHT,
            label = "arrow_right",
        )
        /** Predefined key data for [KeyCode.ARROW_UP] */
        val ARROW_UP = TextKeyData(
            type = KeyType.NAVIGATION,
            code = KeyCode.ARROW_UP,
            label = "arrow_up",
        )
        /** Predefined key data for [KeyCode.ARROW_DOWN] */
        val ARROW_DOWN = TextKeyData(
            type = KeyType.NAVIGATION,
            code = KeyCode.ARROW_DOWN,
            label = "arrow_down",
        )
        /** Predefined key data for [KeyCode.MOVE_START_OF_PAGE] */
        val MOVE_START_OF_PAGE = TextKeyData(
            type = KeyType.NAVIGATION,
            code = KeyCode.MOVE_START_OF_PAGE,
            label = "move_start_of_page",
        )
        /** Predefined key data for [KeyCode.MOVE_END_OF_PAGE] */
        val MOVE_END_OF_PAGE = TextKeyData(
            type = KeyType.NAVIGATION,
            code = KeyCode.MOVE_END_OF_PAGE,
            label = "move_end_of_page",
        )
        /** Predefined key data for [KeyCode.MOVE_START_OF_LINE] */
        val MOVE_START_OF_LINE = TextKeyData(
            type = KeyType.NAVIGATION,
            code = KeyCode.MOVE_START_OF_LINE,
            label = "move_start_of_line",
        )
        /** Predefined key data for [KeyCode.MOVE_END_OF_LINE] */
        val MOVE_END_OF_LINE = TextKeyData(
            type = KeyType.NAVIGATION,
            code = KeyCode.MOVE_END_OF_LINE,
            label = "move_end_of_line",
        )

        /** Predefined key data for [KeyCode.CLIPBOARD_COPY] */
        val CLIPBOARD_COPY = TextKeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.CLIPBOARD_COPY,
            label = "clipboard_copy",
        )
        /** Predefined key data for [KeyCode.CLIPBOARD_CUT] */
        val CLIPBOARD_CUT = TextKeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.CLIPBOARD_CUT,
            label = "clipboard_cut",
        )
        /** Predefined key data for [KeyCode.CLIPBOARD_PASTE] */
        val CLIPBOARD_PASTE = TextKeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.CLIPBOARD_PASTE,
            label = "clipboard_paste",
        )
        /** Predefined key data for [KeyCode.CLIPBOARD_SELECT] */
        val CLIPBOARD_SELECT = TextKeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.CLIPBOARD_SELECT,
            label = "clipboard_select",
        )
        /** Predefined key data for [KeyCode.CLIPBOARD_SELECT_ALL] */
        val CLIPBOARD_SELECT_ALL = TextKeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.CLIPBOARD_SELECT_ALL,
            label = "clipboard_select_all",
        )
        /** Predefined key data for [KeyCode.CLIPBOARD_CLEAR_HISTORY] */
        val CLIPBOARD_CLEAR_HISTORY = TextKeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.CLIPBOARD_CLEAR_HISTORY,
            label = "clipboard_clear_history",
        )
        /** Predefined key data for [KeyCode.CLIPBOARD_CLEAR_FULL_HISTORY] */
        val CLIPBOARD_CLEAR_FULL_HISTORY = TextKeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.CLIPBOARD_CLEAR_FULL_HISTORY,
            label = "clipboard_clear_full_history",
        )
        /** Predefined key data for [KeyCode.CLIPBOARD_CLEAR_PRIMARY_CLIP] */
        val CLIPBOARD_CLEAR_PRIMARY_CLIP = TextKeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.CLIPBOARD_CLEAR_PRIMARY_CLIP,
            label = "clipboard_clear_primary_clip",
        )

        /** Predefined key data for [KeyCode.TOGGLE_COMPACT_LAYOUT] */
        val TOGGLE_COMPACT_LAYOUT = TextKeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.TOGGLE_COMPACT_LAYOUT,
            label = "toggle_compact_layout",
        )
        /** Predefined key data for [KeyCode.COMPACT_LAYOUT_TO_LEFT] */
        val COMPACT_LAYOUT_TO_LEFT = TextKeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.COMPACT_LAYOUT_TO_LEFT,
            label = "compact_layout_to_left",
        )
        /** Predefined key data for [KeyCode.COMPACT_LAYOUT_TO_RIGHT] */
        val COMPACT_LAYOUT_TO_RIGHT = TextKeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.COMPACT_LAYOUT_TO_RIGHT,
            label = "compact_layout_to_right",
        )

        /** Predefined key data for [KeyCode.UNDO] */
        val UNDO = TextKeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.UNDO,
            label = "undo",
        )
        /** Predefined key data for [KeyCode.REDO] */
        val REDO = TextKeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.REDO,
            label = "redo",
        )

        /** Predefined key data for [KeyCode.VIEW_CHARACTERS] */
        val VIEW_CHARACTERS = TextKeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.VIEW_CHARACTERS,
            label = "view_characters",
        )
        /** Predefined key data for [KeyCode.VIEW_SYMBOLS] */
        val VIEW_SYMBOLS = TextKeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.VIEW_SYMBOLS,
            label = "view_symbols",
        )
        /** Predefined key data for [KeyCode.VIEW_SYMBOLS2] */
        val VIEW_SYMBOLS2 = TextKeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.VIEW_SYMBOLS2,
            label = "view_symbols2",
        )
        /** Predefined key data for [KeyCode.VIEW_NUMERIC_ADVANCED] */
        val VIEW_NUMERIC_ADVANCED = TextKeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.VIEW_NUMERIC_ADVANCED,
            label = "view_numeric_advanced",
        )

        /** Predefined key data for [KeyCode.IME_UI_MODE_TEXT] */
        val IME_UI_MODE_TEXT = TextKeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.IME_UI_MODE_TEXT,
            label = "ime_ui_mode_text",
        )
        /** Predefined key data for [KeyCode.IME_UI_MODE_MEDIA] */
        val IME_UI_MODE_MEDIA = TextKeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.IME_UI_MODE_MEDIA,
            label = "ime_ui_mode_media",
        )
        /** Predefined key data for [KeyCode.IME_UI_MODE_CLIPBOARD] */
        val IME_UI_MODE_CLIPBOARD = TextKeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.IME_UI_MODE_CLIPBOARD,
            label = "ime_ui_mode_clipboard",
        )

        /** Predefined key data for [KeyCode.SYSTEM_INPUT_METHOD_PICKER] */
        val SYSTEM_INPUT_METHOD_PICKER = TextKeyData(
            type = KeyType.FUNCTION,
            code = KeyCode.SYSTEM_INPUT_METHOD_PICKER,
            label = "system_input_method_picker",
        )
        /** Predefined key data for [KeyCode.SHOW_SUBTYPE_PICKER] */
        val SHOW_SUBTYPE_PICKER = TextKeyData(
            type = KeyType.FUNCTION,
            code = KeyCode.SHOW_SUBTYPE_PICKER,
            label = "subtype_picker",
        )
        /** Predefined key data for [KeyCode.SYSTEM_PREV_INPUT_METHOD] */
        val SYSTEM_PREV_INPUT_METHOD = TextKeyData(
            type = KeyType.FUNCTION,
            code = KeyCode.SYSTEM_PREV_INPUT_METHOD,
            label = "system_prev_input_method",
        )
        /** Predefined key data for [KeyCode.SYSTEM_NEXT_INPUT_METHOD] */
        val SYSTEM_NEXT_INPUT_METHOD = TextKeyData(
            type = KeyType.FUNCTION,
            code = KeyCode.SYSTEM_NEXT_INPUT_METHOD,
            label = "system_next_input_method",
        )
        /** Predefined key data for [KeyCode.IME_SUBTYPE_PICKER] */
        val IME_SUBTYPE_PICKER = TextKeyData(
            type = KeyType.FUNCTION,
            code = KeyCode.IME_SUBTYPE_PICKER,
            label = "ime_subtype_picker",
        )
        /** Predefined key data for [KeyCode.IME_PREV_SUBTYPE] */
        val IME_PREV_SUBTYPE = TextKeyData(
            type = KeyType.FUNCTION,
            code = KeyCode.IME_PREV_SUBTYPE,
            label = "ime_prev_subtype",
        )
        /** Predefined key data for [KeyCode.IME_NEXT_SUBTYPE] */
        val IME_NEXT_SUBTYPE = TextKeyData(
            type = KeyType.FUNCTION,
            code = KeyCode.IME_NEXT_SUBTYPE,
            label = "ime_next_subtype",
        )
        /** Predefined key data for [KeyCode.LANGUAGE_SWITCH] */
        val LANGUAGE_SWITCH = TextKeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.LANGUAGE_SWITCH,
            label = "language_switch",
        )

        /** Predefined key data for [KeyCode.IME_SHOW_UI] */
        val IME_SHOW_UI = TextKeyData(
            type = KeyType.FUNCTION,
            code = KeyCode.IME_SHOW_UI,
            label = "ime_show_ui",
        )
        /** Predefined key data for [KeyCode.IME_HIDE_UI] */
        val IME_HIDE_UI = TextKeyData(
            type = KeyType.FUNCTION,
            code = KeyCode.IME_HIDE_UI,
            label = "ime_hide_ui",
        )

        /** Predefined key data for [KeyCode.SETTINGS] */
        val SETTINGS = TextKeyData(
            type = KeyType.CHARACTER,
            code = KeyCode.SETTINGS,
            label = "settings",
        )

        /** Predefined key data for [KeyCode.VOICE_INPUT] */
        val VOICE_INPUT = TextKeyData(
            type = KeyType.UNSPECIFIED,
            code = KeyCode.VOICE_INPUT,
            label = "voice_input",
        )

        /** Predefined key data for [KeyCode.TOGGLE_SMARTBAR_VISIBILITY] */
        val TOGGLE_SMARTBAR_VISIBILITY = TextKeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.TOGGLE_SMARTBAR_VISIBILITY,
            label = "toggle_smartbar_visibility",
        )
        /** Predefined key data for [KeyCode.TOGGLE_ACTIONS_OVERFLOW] */
        val TOGGLE_ACTIONS_OVERFLOW = TextKeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.TOGGLE_ACTIONS_OVERFLOW,
            label = "toggle_actions_overflow",
        )
        /** Predefined key data for [KeyCode.TOGGLE_ACTIONS_EDITOR] */
        val TOGGLE_ACTIONS_EDITOR = TextKeyData(
            type = KeyType.SYSTEM_GUI,
            code = KeyCode.TOGGLE_ACTIONS_EDITOR,
            label = "toggle_actions_editor",
        )
        /** Predefined key data for [KeyCode.TOGGLE_INCOGNITO_MODE] */
        val TOGGLE_INCOGNITO_MODE = TextKeyData(
            type = KeyType.FUNCTION,
            code = KeyCode.TOGGLE_INCOGNITO_MODE,
            label = "toggle_incognito_mode",
        )
        /** Predefined key data for [KeyCode.TOGGLE_AUTOCORRECT] */
        val TOGGLE_AUTOCORRECT = TextKeyData(
            type = KeyType.FUNCTION,
            code = KeyCode.TOGGLE_AUTOCORRECT,
            label = "toggle_autocorrect",
        )
    }
}

@Serializable
@SerialName("auto_text_key")
class AutoTextKeyData(
    override val type: KeyType = KeyType.CHARACTER,
    override val code: Int = KeyCode.UNSPECIFIED,
    override val label: String = "",
    override val groupId: Int = KeyData.GROUP_DEFAULT,
    override val popup: PopupSet<AbstractKeyData>? = null
) : KeyData {
    @Transient private val state = AutoLetterState()

    override fun compute(evaluator: ComputingEvaluator): KeyData? {
        return if (evaluator.isSlot(this)) {
            evaluator.slotData(this)?.let { data ->
                TextKeyData(type, data.code, data.label, groupId, popup)
            }
        } else {
            state.recomputeIfNecessary(evaluator.subtype.primaryLocale)
            if (evaluator.state.isUppercase) { state.upper } else { state.lower }
        }
    }

    override fun asString(isForDisplay: Boolean): String {
        return asString(this, isForDisplay)
    }

    override fun toString(): String {
        return "${AutoTextKeyData::class.simpleName} { type=$type code=$code label=\"$label\" groupId=$groupId }"
    }

    private inner class AutoLetterState {
        private var locale: AzhagiLocale = AzhagiLocale.ROOT
        var lower: TextKeyData = TextKeyData.UNSPECIFIED
            private set
        var upper: TextKeyData = TextKeyData.UNSPECIFIED
            private set

        fun recomputeIfNecessary(locale: AzhagiLocale) {
            if (this.locale == locale) return
            this.locale = locale
            lower = TextKeyData(
                type,
                UCharacter.toString(code).lowercase(locale).codePointAt(0),
                label.lowercase(locale),
                groupId,
                popup,
            )
            upper = TextKeyData(
                type,
                UCharacter.toString(code).uppercase(locale).codePointAt(0),
                label.uppercase(locale),
                groupId,
                popup,
            )
        }
    }
}

@Serializable
@SerialName("multi_text_key")
class MultiTextKeyData(
    override val type: KeyType = KeyType.CHARACTER,
    val codePoints: IntArray = intArrayOf(),
    override val label: String = "",
    override val groupId: Int = KeyData.GROUP_DEFAULT,
    override val popup: PopupSet<AbstractKeyData>? = null
) : KeyData {
    @Transient override val code: Int = KeyCode.MULTIPLE_CODE_POINTS

    override fun compute(evaluator: ComputingEvaluator): KeyData {
        return this
    }

    override fun asString(isForDisplay: Boolean): String {
        return buildString {
            if (isForDisplay) {
                append(label)
            } else {
                for (codePoint in codePoints) {
                    try { appendCodePoint(codePoint) } catch (_: Throwable) { }
                }
            }
        }
    }

    override fun toString(): String {
        return "${MultiTextKeyData::class.simpleName} { type=$type code=$code label=\"$label\" groupId=$groupId }"
    }
}

internal fun asString(data: KeyData, isForDisplay: Boolean) : String {
    return buildString {
        if (isForDisplay || data.code == KeyCode.URI_COMPONENT_TLD || data.code < KeyCode.SPACE) {
            if (Unicode.isNonSpacingMark(data.code) && !data.label.startsWith("◌")) {
                append("◌")
            }
            append(data.label)
        } else {
            try { appendCodePoint(data.code) } catch (_: Throwable) { }
        }
    }
}

