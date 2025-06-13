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

package com.azhagi.azhagikeys.app.settings.localization

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Input
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.azhagi.azhagikeys.R
import com.azhagi.azhagikeys.app.LocalNavController
import com.azhagi.azhagikeys.app.Routes
import com.azhagi.azhagikeys.app.ext.ExtensionImportScreenType
import com.azhagi.azhagikeys.app.AzhagiPreferenceModel
import com.azhagi.azhagikeys.extensionManager
import com.azhagi.azhagikeys.ime.nlp.LanguagePackComponent
import org.florisboard.lib.android.showLongToast
import com.azhagi.azhagikeys.lib.compose.AzhagiConfirmDeleteDialog
import com.azhagi.azhagikeys.lib.compose.AzhagiOutlinedBox
import com.azhagi.azhagikeys.lib.compose.AzhagiScreen
import com.azhagi.azhagikeys.lib.compose.AzhagiTextButton
import com.azhagi.azhagikeys.lib.compose.defaultAzhagiOutlinedBox
import com.azhagi.azhagikeys.lib.compose.rippleClickable
import com.azhagi.azhagikeys.lib.compose.stringRes
import com.azhagi.azhagikeys.lib.ext.Extension
import com.azhagi.azhagikeys.lib.ext.ExtensionComponentName
import com.azhagi.azhagikeys.lib.observeAsNonNullState
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.material.ui.JetPrefListItem

enum class LanguagePackManagerScreenAction(val id: String) {
    MANAGE("manage-installed-language-packs");
}

// TODO: this file is based on ThemeManagerScreen.kt and can arguably be merged.
@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun LanguagePackManagerScreen(action: LanguagePackManagerScreenAction?) = AzhagiScreen {
    title = stringRes(when (action) {
        LanguagePackManagerScreenAction.MANAGE -> R.string.settings__localization__language_pack_title
        else -> error("LanguagePack manager screen action must not be null")
    })

    val prefs by AzhagiPreferenceModel()
    val navController = LocalNavController.current
    val context = LocalContext.current
    val extensionManager by context.extensionManager()

    val indexedLanguagePackExtensions by extensionManager.languagePacks.observeAsNonNullState()
    val selectedManagerLanguagePackId = remember { mutableStateOf<ExtensionComponentName?>(null) }
    val extGroupedLanguagePacks = remember(indexedLanguagePackExtensions) {
        buildMap<String, List<LanguagePackComponent>> {
            for (ext in indexedLanguagePackExtensions) {
                put(ext.meta.id, ext.items)
            }
        }.mapValues { (_, configs) -> configs.sortedBy { it.label } }
    }

    fun getLanguagePackIdPref(): Nothing = TODO("Not implemented yet")

    fun setLanguagePack(extId: String, componentId: String) {
        val extComponentName = ExtensionComponentName(extId, componentId)
        when (action) {
            LanguagePackManagerScreenAction.MANAGE -> {
                selectedManagerLanguagePackId.value = extComponentName
            }
        }
    }

    val activeLanguagePackId by when (action) {
        LanguagePackManagerScreenAction.MANAGE -> selectedManagerLanguagePackId
    }
    var languagePackExtToDelete by remember { mutableStateOf<Extension?>(null) }

    content {
        val grayColor = LocalContentColor.current.copy(alpha = 0.56f)
        if (action == LanguagePackManagerScreenAction.MANAGE) {
            AzhagiOutlinedBox(
                modifier = Modifier.defaultAzhagiOutlinedBox(),
            ) {
                Preference(
                    onClick = { navController.navigate(
                        Routes.Ext.Import(ExtensionImportScreenType.EXT_LANGUAGEPACK, null)
                    ) },
                    icon = Icons.Default.Input,
                    title = stringRes(R.string.action__import),
                )
            }
        }
        for ((extensionId, configs) in extGroupedLanguagePacks) key(extensionId) {
            val ext = extensionManager.getExtensionById(extensionId)!!
            AzhagiOutlinedBox(
                modifier = Modifier.defaultAzhagiOutlinedBox(),
                title = ext.meta.title,
                onTitleClick = { navController.navigate(Routes.Ext.View(extensionId)) },
                subtitle = extensionId,
                onSubtitleClick = { navController.navigate(Routes.Ext.View(extensionId)) },
            ) {
                Column(
                    // Allowing horizontal scroll to fit translations in descriptions.
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .width(intrinsicSize = IntrinsicSize.Max),
                ) {
                    for (config in configs) key(extensionId, config.id) {
                        JetPrefListItem(
                            modifier = Modifier.rippleClickable {
                                setLanguagePack(extensionId, config.id)
                            },
//                        icon = {
//                            RadioButton(
//                                selected = activeLanguagePackId?.extensionId == extensionId &&
//                                    activeLanguagePackId?.componentId == config.id,
//                                onClick = null,
//                            )
//                        },
                            text = config.label,
//                        trailing = {
//                            Icon(
//                                modifier = Modifier.size(ButtonDefaults.IconSize),
//                                painter = painterResource(if (config.isNightLanguagePack) {
//                                    R.drawable.ic_dark_mode
//                                } else {
//                                    R.drawable.ic_light_mode
//                                }),
//                                contentDescription = null,
//                                tint = grayColor,
//                            )
//                        },
                        )
                    }
                }
                if (action == LanguagePackManagerScreenAction.MANAGE && extensionManager.canDelete(ext)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                    ) {
                        AzhagiTextButton(
                            onClick = {
                                languagePackExtToDelete = ext
                            },
                            icon = Icons.Default.Delete,
                            text = stringRes(R.string.action__delete),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        )
                        Spacer(modifier = Modifier.weight(1f))
//                        AzhagiTextButton(
//                            onClick = {
//                                navController.navigate(Routes.Ext.Edit(ext.meta.id))
//                            },
//                            icon = painterResource(R.drawable.ic_edit),
//                            text = stringRes(R.string.action__edit),
//                        )
                    }
                }
            }
        }

        if (languagePackExtToDelete != null) {
            AzhagiConfirmDeleteDialog(
                onConfirm = {
                    runCatching {
                        extensionManager.delete(languagePackExtToDelete!!)
                    }.onFailure { error ->
                        context.showLongToast(
                            R.string.error__snackbar_message,
                            "error_message" to error.localizedMessage,
                        )
                    }
                    languagePackExtToDelete = null
                },
                onDismiss = { languagePackExtToDelete = null },
                what = languagePackExtToDelete!!.meta.title,
            )
        }
    }
}

