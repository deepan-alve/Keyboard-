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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.azhagi.azhagikeys.R
import com.azhagi.azhagikeys.app.LocalNavController
import com.azhagi.azhagikeys.app.Routes
import com.azhagi.azhagikeys.app.AzhagiPreferenceModel
import com.azhagi.azhagikeys.ime.core.DisplayLanguageNamesIn
import com.azhagi.azhagikeys.ime.core.Subtype
import com.azhagi.azhagikeys.ime.core.SubtypeJsonConfig
import com.azhagi.azhagikeys.ime.core.SubtypeLayoutMap
import com.azhagi.azhagikeys.ime.core.SubtypeNlpProviderMap
import com.azhagi.azhagikeys.ime.core.SubtypePreset
import com.azhagi.azhagikeys.ime.keyboard.LayoutArrangementComponent
import com.azhagi.azhagikeys.ime.keyboard.LayoutType
import com.azhagi.azhagikeys.ime.keyboard.extCorePopupMapping
import com.azhagi.azhagikeys.ime.nlp.han.HanShapeBasedLanguageProvider
import com.azhagi.azhagikeys.ime.nlp.latin.LatinLanguageProvider
import com.azhagi.azhagikeys.keyboardManager
import com.azhagi.azhagikeys.lib.AzhagiLocale
import com.azhagi.azhagikeys.lib.compose.AzhagiButtonBar
import com.azhagi.azhagikeys.lib.compose.AzhagiDropdownLikeButton
import com.azhagi.azhagikeys.lib.compose.AzhagiDropdownMenu
import com.azhagi.azhagikeys.lib.compose.AzhagiScreen
import com.azhagi.azhagikeys.lib.compose.AzhagiScrollbar
import com.azhagi.azhagikeys.lib.compose.stringRes
import com.azhagi.azhagikeys.lib.ext.ExtensionComponentName
import com.azhagi.azhagikeys.lib.observeAsNonNullState
import com.azhagi.azhagikeys.subtypeManager
import dev.patrickgold.jetpref.datastore.model.observeAsState
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import dev.patrickgold.jetpref.material.ui.JetPrefListItem
import kotlinx.serialization.encodeToString

private val SelectComponentName = ExtensionComponentName("00", "00")
private val SelectNlpProviderId = SelectComponentName.toString()
private val SelectNlpProviders = SubtypeNlpProviderMap(
    spelling = SelectNlpProviderId,
)
private val SelectLayoutMap = SubtypeLayoutMap(
    characters = SelectComponentName,
    symbols = SelectComponentName,
    symbols2 = SelectComponentName,
    numeric = SelectComponentName,
    numericAdvanced = SelectComponentName,
    numericRow = SelectComponentName,
    phone = SelectComponentName,
    phone2 = SelectComponentName,
)
private val SelectLocale = AzhagiLocale.from("00", "00")
private val SelectListKeys = listOf(SelectComponentName)

private class SubtypeEditorState(init: Subtype?) {
    companion object {
        val Saver = Saver<SubtypeEditorState, String>(
            save = { editor ->
                val subtype = Subtype(
                    id = editor.id.value,
                    primaryLocale = editor.primaryLocale.value,
                    secondaryLocales = editor.secondaryLocales.value,
                    nlpProviders = editor.nlpProviders.value,
                    composer = editor.composer.value,
                    currencySet = editor.currencySet.value,
                    punctuationRule = editor.punctuationRule.value,
                    popupMapping = editor.popupMapping.value,
                    layoutMap = editor.layoutMap.value,
                )
                SubtypeJsonConfig.encodeToString(subtype)
            },
            restore = { str ->
                val subtype = SubtypeJsonConfig.decodeFromString<Subtype>(str)
                SubtypeEditorState(subtype)
            },
        )
    }

    val id: MutableState<Long> = mutableLongStateOf(init?.id ?: -1)
    val primaryLocale: MutableState<AzhagiLocale> = mutableStateOf(init?.primaryLocale ?: SelectLocale)
    val secondaryLocales: MutableState<List<AzhagiLocale>> = mutableStateOf(init?.secondaryLocales ?: listOf())
    val nlpProviders: MutableState<SubtypeNlpProviderMap> = mutableStateOf(init?.nlpProviders ?: Subtype.DEFAULT.nlpProviders)
    val composer: MutableState<ExtensionComponentName> = mutableStateOf(init?.composer ?: SelectComponentName)
    val currencySet: MutableState<ExtensionComponentName> = mutableStateOf(init?.currencySet ?: SelectComponentName)
    val punctuationRule: MutableState<ExtensionComponentName> = mutableStateOf(init?.punctuationRule ?: Subtype.DEFAULT.punctuationRule)
    val popupMapping: MutableState<ExtensionComponentName> = mutableStateOf(init?.popupMapping ?: SelectComponentName)
    val layoutMap: MutableState<SubtypeLayoutMap> = mutableStateOf(init?.layoutMap ?: SelectLayoutMap)

    fun applySubtype(subtype: Subtype) {
        id.value = subtype.id
        primaryLocale.value = subtype.primaryLocale
        secondaryLocales.value = subtype.secondaryLocales
        composer.value = subtype.composer
        nlpProviders.value = subtype.nlpProviders
        currencySet.value = subtype.currencySet
        punctuationRule.value = subtype.punctuationRule
        popupMapping.value = subtype.popupMapping
        layoutMap.value = subtype.layoutMap
    }

    fun toSubtype() = runCatching<Subtype> {
        check(primaryLocale.value != SelectLocale)
        check(nlpProviders.value.spelling != SelectNlpProviderId)
        check(nlpProviders.value.suggestion != SelectNlpProviderId)
        check(composer.value != SelectComponentName)
        check(currencySet.value != SelectComponentName)
        check(punctuationRule.value != SelectComponentName)
        check(popupMapping.value != SelectComponentName)
        check(layoutMap.value.characters != SelectComponentName)
        check(layoutMap.value.symbols != SelectComponentName)
        check(layoutMap.value.symbols2 != SelectComponentName)
        check(layoutMap.value.numeric != SelectComponentName)
        check(layoutMap.value.numericAdvanced != SelectComponentName)
        check(layoutMap.value.numericRow != SelectComponentName)
        check(layoutMap.value.phone != SelectComponentName)
        check(layoutMap.value.phone2 != SelectComponentName)
        Subtype(
            id.value, primaryLocale.value, secondaryLocales.value, nlpProviders.value, composer.value,
            currencySet.value, punctuationRule.value, popupMapping.value, layoutMap.value,
        )
    }
}

@Composable
fun SubtypeEditorScreen(id: Long?) = AzhagiScreen {
    title = stringRes(if (id == null) {
        R.string.settings__localization__subtype_add_title
    } else {
        R.string.settings__localization__subtype_edit_title
    })

    val selectValue = stringRes(R.string.settings__localization__subtype_select_placeholder)
    val selectListValues = remember(selectValue) { listOf(selectValue) }

    val prefs by AzhagiPreferenceModel()
    val navController = LocalNavController.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val keyboardManager by context.keyboardManager()
    val subtypeManager by context.subtypeManager()

    val displayLanguageNamesIn by prefs.localization.displayLanguageNamesIn.observeAsState()
    val composers by keyboardManager.resources.composers.observeAsNonNullState()
    val currencySets by keyboardManager.resources.currencySets.observeAsNonNullState()
    val layoutExtensions by keyboardManager.resources.layouts.observeAsNonNullState()
    val popupMappings by keyboardManager.resources.popupMappings.observeAsNonNullState()
    val subtypePresets by keyboardManager.resources.subtypePresets.observeAsNonNullState()

    val subtypeEditor = rememberSaveable(saver = SubtypeEditorState.Saver) {
        val subtype = id?.let { subtypeManager.getSubtypeById(id) }
        SubtypeEditorState(subtype)
    }
    var primaryLocale by subtypeEditor.primaryLocale
    //var secondaryLocales by subtypeEditor.secondaryLocales
    var composer by subtypeEditor.composer
    var currencySet by subtypeEditor.currencySet
    var popupMapping by subtypeEditor.popupMapping
    var layoutMap by subtypeEditor.layoutMap
    var nlpProviders by subtypeEditor.nlpProviders

    var showSubtypePresetsDialog by rememberSaveable { mutableStateOf(id == null) }
    var showSelectAsError by rememberSaveable { mutableStateOf(false) }
    var errorDialogStrId by rememberSaveable { mutableStateOf<Int?>(null) }

    val selectLocaleScreenResult = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<String>(SelectLocaleScreenResultLanguageTag)
    DisposableEffect(selectLocaleScreenResult, lifecycleOwner) {
        val observer = Observer<String> { languageTag ->
            val locale = AzhagiLocale.fromTag(languageTag)
            primaryLocale = locale
            val preset = subtypeManager.getSubtypePresetForLocale(locale)
            popupMapping = preset?.popupMapping ?: extCorePopupMapping("default")
        }
        selectLocaleScreenResult?.observe(lifecycleOwner, observer)
        onDispose { selectLocaleScreenResult?.removeObserver(observer) }
    }


    @Composable
    fun SubtypePropertyDropdown(
        title: String,
        layoutType: LayoutType,
    ) {
        SubtypeProperty(title) {
            SubtypeLayoutDropdown(
                layoutType = layoutType,
                layouts = layoutExtensions[layoutType] ?: mapOf(),
                showSelectAsError = showSelectAsError,
                layoutMap = layoutMap,
                onLayoutMapChanged = { layoutMap = it },
                selectListValues = selectListValues,
            )
        }
    }

    actions {
        if (id != null) {
            IconButton(onClick = {
                val subtype = subtypeManager.getSubtypeById(id)
                if (subtype != null) {
                    subtypeManager.removeSubtype(subtype)
                    navController.popBackStack()
                }
            }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                )
            }
        }
    }

    bottomBar {
        AzhagiButtonBar {
            ButtonBarSpacer()
            ButtonBarTextButton(text = stringRes(R.string.action__cancel)) {
                navController.popBackStack()
            }
            ButtonBarButton(text = stringRes(R.string.action__save)) {
                subtypeEditor.toSubtype().onSuccess { subtype ->
                    if (id == null) {
                        if (!subtypeManager.addSubtype(subtype)) {
                            errorDialogStrId = R.string.settings__localization__subtype_error_already_exists
                            return@ButtonBarButton
                        }
                    } else {
                        subtypeManager.modifySubtypeWithSameId(subtype)
                    }
                    navController.popBackStack()
                }.onFailure {
                    showSelectAsError = true
                    errorDialogStrId = R.string.settings__localization__subtype_error_fields_no_value
                }
            }
        }
    }

    content {
        Column(modifier = Modifier.padding(8.dp)) {
            if (id == null) {
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                            text = stringRes(R.string.settings__localization__suggested_subtype_presets),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val systemLocales = remember {
                            val list = mutableListOf<AzhagiLocale>()
                            val localeList = configuration.locales
                            for (n in 0 until localeList.size()) {
                                list.add(AzhagiLocale.from(localeList.get(n)))
                            }
                            list
                        }
                        val suggestedPresets = remember(subtypePresets) {
                            val presets = mutableListOf<SubtypePreset>()
                            for (systemLocale in systemLocales) {
                                subtypePresets.find { it.locale == systemLocale }?.let { presets.add(it) }
                            }
                            presets
                        }
                        if (suggestedPresets.isNotEmpty()) {
                            for (suggestedPreset in suggestedPresets) {
                                JetPrefListItem(
                                    modifier = Modifier.clickable {
                                        subtypeEditor.applySubtype(suggestedPreset.toSubtype())
                                    },
                                    text = when (displayLanguageNamesIn) {
                                        DisplayLanguageNamesIn.SYSTEM_LOCALE -> suggestedPreset.locale.displayName()
                                        DisplayLanguageNamesIn.NATIVE_LOCALE -> suggestedPreset.locale.displayName(suggestedPreset.locale)
                                    },
                                    secondaryText = suggestedPreset.preferred.characters.componentId,
                                    colors = ListItemDefaults.colors(containerColor = CardDefaults.cardColors().containerColor),
                                )
                            }
                        } else {
                            Text(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                                text = stringRes(R.string.settings__localization__suggested_subtype_presets_none_found),
                            )
                        }
                        Button(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .align(Alignment.End),
                            onClick = { showSubtypePresetsDialog = true },
                        ) {
                            Text(
                                text = stringRes(R.string.settings__localization__subtype_presets_view_all)
                            )
                        }
                    }
                }
            }

            SubtypeProperty(stringRes(R.string.settings__localization__subtype_locale)) {
                AzhagiDropdownLikeButton(
                    item = if (primaryLocale == SelectLocale) selectValue else when (displayLanguageNamesIn) {
                        DisplayLanguageNamesIn.SYSTEM_LOCALE -> primaryLocale.displayName()
                        DisplayLanguageNamesIn.NATIVE_LOCALE -> primaryLocale.displayName(primaryLocale)
                    },
                    isError = showSelectAsError && primaryLocale == SelectLocale,
                    onClick = {
                        navController.navigate(Routes.Settings.SelectLocale)
                    },
                )
            }
            SubtypeProperty(stringRes(R.string.settings__localization__subtype_popup_mapping)) {
                val popupMappingIds = remember(popupMappings) {
                    SelectListKeys + popupMappings.keys
                }
                val popupMappingLabels = remember(popupMappings) {
                    selectListValues + popupMappings.values.map { it.label }
                }
                var expanded by remember { mutableStateOf(false) }
                val selectedIndex = popupMappingIds.indexOf(popupMapping).coerceAtLeast(0)
                AzhagiDropdownMenu(
                    items = popupMappingLabels,
                    expanded = expanded,
                    selectedIndex = selectedIndex,
                    isError = showSelectAsError && selectedIndex == 0,
                    onSelectItem = { popupMapping = popupMappingIds[it] },
                    onExpandRequest = { expanded = true },
                    onDismissRequest = { expanded = false },
                )
            }
            SubtypePropertyDropdown(stringRes(R.string.settings__localization__subtype_characters_layout), LayoutType.CHARACTERS)

            SubtypeGroupSpacer()

            SubtypeProperty(stringRes(R.string.settings__localization__subtype_suggestion_provider)) {
                // TODO: Put this map somewhere more formal (another KeyboardExtension field?)
                //  optionally use a string resource below
                val nlpProviderMappings = mapOf(
                    LatinLanguageProvider.ProviderId to "Latin",
                    HanShapeBasedLanguageProvider.ProviderId to "Chinese shape-based"
                )

                val nlpProviderMappingIds = remember(nlpProviderMappings) {
                    listOf(SelectNlpProviderId) + nlpProviderMappings.keys
                }
                val nlpProviderMappingLabels = remember(nlpProviderMappings) {
                    selectListValues + nlpProviderMappings.values.map { it }
                }
                var expanded by remember { mutableStateOf(false) }
                val selectedIndex = nlpProviderMappingIds.indexOf(nlpProviders.suggestion).coerceAtLeast(0)
                AzhagiDropdownMenu(
                    items = nlpProviderMappingLabels,
                    expanded = expanded,
                    selectedIndex = selectedIndex,
                    isError = showSelectAsError && selectedIndex == 0,
                    onSelectItem = { nlpProviders = SubtypeNlpProviderMap(
                        suggestion = nlpProviderMappingIds[it],
                        spelling = nlpProviderMappingIds[it]
                    ) },
                    onExpandRequest = { expanded = true },
                    onDismissRequest = { expanded = false },
                )
            }

            SubtypeGroupSpacer()

            SubtypePropertyDropdown(stringRes(R.string.settings__localization__subtype_symbols_layout), LayoutType.SYMBOLS)
            SubtypePropertyDropdown(stringRes(R.string.settings__localization__subtype_symbols2_layout), LayoutType.SYMBOLS2)

            SubtypeProperty(stringRes(R.string.settings__localization__subtype_composer)) {
                val composerIds = remember(composers) {
                    SelectListKeys + composers.keys
                }
                val composerNames = remember(composers) {
                    selectListValues + composers.values.map { it.label }
                }
                var expanded by remember { mutableStateOf(false) }
                AzhagiDropdownMenu(
                    items = composerNames,
                    expanded = expanded,
                    selectedIndex = composerIds.indexOf(composer).coerceAtLeast(0),
                    isError = showSelectAsError && composer == SelectComponentName,
                    onSelectItem = { composer = composerIds[it] },
                    onExpandRequest = { expanded = true },
                    onDismissRequest = { expanded = false },
                )
            }
            SubtypeProperty(stringRes(R.string.settings__localization__subtype_currency_set)) {
                val currencySetIds = remember(currencySets) {
                    SelectListKeys + currencySets.keys
                }
                val currencySetNames = remember(currencySets) {
                    selectListValues + currencySets.values.map { it.label }
                }
                var expanded by remember { mutableStateOf(false) }
                AzhagiDropdownMenu(
                    items = currencySetNames,
                    expanded = expanded,
                    selectedIndex = currencySetIds.indexOf(currencySet).coerceAtLeast(0),
                    isError = showSelectAsError && currencySet == SelectComponentName,
                    onSelectItem = { currencySet = currencySetIds[it] },
                    onExpandRequest = { expanded = true },
                    onDismissRequest = { expanded = false },
                )
            }

            SubtypeGroupSpacer()

            SubtypePropertyDropdown(stringRes(R.string.settings__localization__subtype_numeric_layout), LayoutType.NUMERIC)

            SubtypePropertyDropdown(stringRes(R.string.settings__localization__subtype_numeric_advanced_layout), LayoutType.NUMERIC_ADVANCED)

            SubtypePropertyDropdown(stringRes(R.string.settings__localization__subtype_numeric_row_layout), LayoutType.NUMERIC_ROW)

            SubtypeGroupSpacer()

            SubtypePropertyDropdown(stringRes(R.string.settings__localization__subtype_phone_layout), LayoutType.PHONE)

            SubtypePropertyDropdown(stringRes(R.string.settings__localization__subtype_phone2_layout), LayoutType.PHONE2)
        }

        if (showSubtypePresetsDialog) {
            JetPrefAlertDialog(
                title = stringRes(R.string.settings__localization__subtype_presets),
                dismissLabel = stringRes(android.R.string.cancel),
                scrollModifier = Modifier,
                contentPadding = PaddingValues(horizontal = 8.dp),
                onDismiss = {
                    showSubtypePresetsDialog = false
                },
            ) {
                Column {
                    HorizontalDivider()
                    val lazyListState = rememberLazyListState()
                    LazyColumn(
                        modifier = Modifier
                            .AzhagiScrollbar(lazyListState, isVertical = true).weight(1f),
                        state = lazyListState,
                    ) {
                        items(subtypePresets) { subtypePreset ->
                            JetPrefListItem(
                                modifier = Modifier.clickable {
                                    subtypeEditor.applySubtype(subtypePreset.toSubtype())
                                    showSubtypePresetsDialog = false
                                },
                                text = when (displayLanguageNamesIn) {
                                    DisplayLanguageNamesIn.SYSTEM_LOCALE -> subtypePreset.locale.displayName()
                                    DisplayLanguageNamesIn.NATIVE_LOCALE -> subtypePreset.locale.displayName(subtypePreset.locale)
                                },
                                secondaryText = subtypePreset.preferred.characters.componentId,
                                colors = ListItemDefaults.colors(containerColor = AlertDialogDefaults.containerColor),
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }

        errorDialogStrId?.let { strId ->
            JetPrefAlertDialog(
                title = stringRes(R.string.error__title),
                confirmLabel = stringRes(android.R.string.ok),
                onConfirm = {
                    errorDialogStrId = null
                },
                onDismiss = {
                    errorDialogStrId = null
                },
            ) {
                Text(text = stringRes(strId))
            }
        }
    }
}

@Composable
private fun SubtypeProperty(text: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)) {
        Text(
            modifier = Modifier.padding(bottom = 8.dp),
            text = text,
            style = MaterialTheme.typography.titleSmall,
        )
        content()
    }
}

@Composable
private fun SubtypeLayoutDropdown(
    layoutType: LayoutType,
    layouts: Map<ExtensionComponentName, LayoutArrangementComponent>,
    showSelectAsError: Boolean,
    layoutMap: SubtypeLayoutMap,
    onLayoutMapChanged: (SubtypeLayoutMap) -> Unit,
    selectListValues: List<String>,
) {
    val layoutIds = remember(layouts) { SelectListKeys + layouts.keys.toList() }
    val layoutLabels = remember(layouts) { selectListValues + layouts.values.map { it.label } }
    val layoutId = remember(layoutMap) { layoutMap[layoutType] }
    var expanded by remember { mutableStateOf(false) }
    val selectedIndex = layoutIds.indexOf(layoutId).coerceAtLeast(0)
    AzhagiDropdownMenu(
        items = layoutLabels,
        expanded = expanded,
        selectedIndex = selectedIndex,
        isError = showSelectAsError && selectedIndex == 0,
        onSelectItem = { onLayoutMapChanged(layoutMap.copy(layoutType, layoutIds[it])!!) },
        onExpandRequest = { expanded = true },
        onDismissRequest = { expanded = false },
    )
}

@Composable
private fun SubtypeGroupSpacer() {
    Spacer(modifier = Modifier
        .fillMaxWidth()
        .height(32.dp))
}

