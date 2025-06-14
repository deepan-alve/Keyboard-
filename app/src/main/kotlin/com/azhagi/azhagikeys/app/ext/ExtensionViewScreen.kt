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

package com.azhagi.azhagikeys.app.ext

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.azhagi.azhagikeys.R
import com.azhagi.azhagikeys.app.LocalNavController
import com.azhagi.azhagikeys.app.Routes
import com.azhagi.azhagikeys.extensionManager
import com.azhagi.azhagikeys.ime.nlp.LanguagePackExtension
import com.azhagi.azhagikeys.ime.theme.ThemeExtension
import com.azhagi.azhagikeys.ime.theme.ThemeExtensionComponentImpl
import org.florisboard.lib.android.showLongToast
import com.azhagi.azhagikeys.lib.compose.AzhagiConfirmDeleteDialog
import com.azhagi.azhagikeys.lib.compose.AzhagiHyperlinkText
import com.azhagi.azhagikeys.lib.compose.AzhagiOutlinedButton
import com.azhagi.azhagikeys.lib.compose.AzhagiScreen
import com.azhagi.azhagikeys.lib.compose.defaultAzhagiOutlinedBox
import com.azhagi.azhagikeys.lib.compose.stringRes
import com.azhagi.azhagikeys.lib.ext.Extension
import com.azhagi.azhagikeys.lib.ext.ExtensionMaintainer
import com.azhagi.azhagikeys.lib.ext.ExtensionMeta
import com.azhagi.azhagikeys.lib.io.AzhagiRef

@Composable
fun ExtensionViewScreen(id: String) {
    val context = LocalContext.current
    val extensionManager by context.extensionManager()

    val ext = extensionManager.getExtensionById(id)
    if (ext != null) {
        ViewScreen(ext)
    } else {
        ExtensionNotFoundScreen(id)
    }
}

@Composable
private fun ViewScreen(ext: Extension) = AzhagiScreen {
    title = ext.meta.title

    val navController = LocalNavController.current
    val context = LocalContext.current
    val extensionManager by context.extensionManager()

    var extToDelete by remember { mutableStateOf<Extension?>(null) }

    content {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            ext.meta.description?.let { Text(it) }
            Spacer(modifier = Modifier.height(16.dp))
            ExtensionMetaRowScrollableChips(
                label = stringRes(R.string.ext__meta__maintainers),
                showDividerAbove = false,
            ) {
                for ((n, maintainer) in ext.meta.maintainers.withIndex()) {
                    if (n > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    ExtensionMaintainerChip(maintainer)
                }
            }
            ExtensionMetaRowSimpleText(label = stringRes(R.string.ext__meta__id)) {
                Text(text = ext.meta.id)
            }
            ExtensionMetaRowSimpleText(label = stringRes(R.string.ext__meta__version)) {
                Text(text = ext.meta.version)
            }
            if (ext.meta.keywords != null && ext.meta.keywords!!.isNotEmpty()) {
                ExtensionMetaRowScrollableChips(label = stringRes(R.string.ext__meta__keywords)) {
                    for ((n, keyword) in ext.meta.keywords!!.withIndex()) {
                        if (n > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        ExtensionKeywordChip(keyword)
                    }
                }
            }
            if (!ext.meta.homepage.isNullOrBlank()) {
                ExtensionMetaRowSimpleText(label = stringRes(R.string.ext__meta__homepage)) {
                    AzhagiHyperlinkText(
                        text = AzhagiRef.fromUrl(ext.meta.homepage!!).authority,
                        url = ext.meta.homepage!!,
                    )
                }
            }
            if (!ext.meta.issueTracker.isNullOrBlank()) {
                ExtensionMetaRowSimpleText(label = stringRes(R.string.ext__meta__issue_tracker)) {
                    AzhagiHyperlinkText(
                        text = AzhagiRef.fromUrl(ext.meta.issueTracker!!).authority,
                        url = ext.meta.issueTracker!!,
                    )
                }
            }
            ExtensionMetaRowSimpleText(label = stringRes(R.string.ext__meta__license)) {
                // TODO: display human-readable License name instead of
                //  SPDX identifier
                Text(text = ext.meta.license)
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                if (extensionManager.canDelete(ext)) {
                    AzhagiOutlinedButton(
                        onClick = {
                            extToDelete = ext
                        },
                        icon = Icons.Default.Delete,
                        text = stringRes(R.string.action__delete),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                AzhagiOutlinedButton(
                    onClick = {
                        navController.navigate(Routes.Ext.Export(ext.meta.id))
                    },
                    icon = Icons.Default.Share,
                    text = stringRes(R.string.action__export),
                )
            }
        }

        when (ext) {
            is ThemeExtension -> {
                ExtensionComponentListView(
                    title = stringRes(R.string.ext__meta__components_theme),
                    components = ext.themes,
                ) { component ->
                    ExtensionComponentView(
                        modifier = Modifier.defaultAzhagiOutlinedBox(),
                        meta = ext.meta,
                        component = component,
                    )
                }
            }
            is LanguagePackExtension -> {
                ExtensionComponentListView(
                    title = stringRes(R.string.ext__meta__components_language_pack),
                    components = ext.items,
                ) { component ->
                    ExtensionComponentView(
                        modifier = Modifier.defaultAzhagiOutlinedBox(),
                        meta = ext.meta,
                        component = component,
                    )
                }
            }
            else -> {
                // Render nothing
            }
        }

        if (extToDelete != null) {
            AzhagiConfirmDeleteDialog(
                onConfirm = {
                    runCatching {
                        extensionManager.delete(extToDelete!!)
                    }.onSuccess {
                        navController.popBackStack()
                    }.onFailure { error ->
                        context.showLongToast(
                            R.string.error__snackbar_message,
                            "error_message" to error.localizedMessage,
                        )
                    }
                    extToDelete = null
                },
                onDismiss = { extToDelete = null },
                what = extToDelete!!.meta.title,
            )
        }
    }
}

@Composable
private fun ExtensionMetaRowSimpleText(
    label: String,
    modifier: Modifier = Modifier,
    showDividerAbove: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    if (showDividerAbove) {
        HorizontalDivider()
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(modifier = Modifier.padding(end = 24.dp), text = label)
        content()
    }
}

@Composable
private fun ExtensionMetaRowScrollableChips(
    label: String,
    modifier: Modifier = Modifier,
    showDividerAbove: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    if (showDividerAbove) {
        HorizontalDivider()
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(modifier = Modifier.padding(end = 24.dp), text = label)
        Row(
            modifier = Modifier
                .weight(1.0f, fill = false)
                .horizontalScroll(rememberScrollState()),
        ) {
            content()
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PreviewExtensionViewerScreen() {
    val testExtension = ThemeExtension(
        meta = ExtensionMeta(
            id = "com.example.theme.test",
            version = "2.4.3",
            title = "Test theme",
            description = "This is a test theme to preview the extension viewer screen UI.",
            keywords = listOf("Beach", "Sea", "Sun"),
            homepage = "https://example.com",
            issueTracker = "https://git.example.com/issues",
            maintainers = listOf(
                "Max Mustermann <max.mustermann@example.com> (maxmustermann.example.com)",
            ).map { ExtensionMaintainer.fromOrTakeRaw(it) },
            license = "apache-2.0",
        ),
        dependencies = null,
        themes = listOf(
            ThemeExtensionComponentImpl(id = "test", label = "Test", authors = listOf(), stylesheetPath = "test.json"),
        ),
    )
    ViewScreen(ext = testExtension)
}

