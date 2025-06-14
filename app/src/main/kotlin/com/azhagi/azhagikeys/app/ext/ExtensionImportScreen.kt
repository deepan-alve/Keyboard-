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

import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.azhagi.azhagikeys.R
import com.azhagi.azhagikeys.app.LocalNavController
import com.azhagi.azhagikeys.cacheManager
import com.azhagi.azhagikeys.extensionManager
import com.azhagi.azhagikeys.ime.keyboard.KeyboardExtension
import com.azhagi.azhagikeys.ime.nlp.LanguagePackExtension
import com.azhagi.azhagikeys.ime.theme.ThemeExtension
import com.azhagi.azhagikeys.lib.NATIVE_NULLPTR
import org.florisboard.lib.android.showLongToast
import com.azhagi.azhagikeys.lib.cache.CacheManager
import com.azhagi.azhagikeys.lib.compose.AzhagiBulletSpacer
import com.azhagi.azhagikeys.lib.compose.AzhagiButtonBar
import com.azhagi.azhagikeys.lib.compose.AzhagiOutlinedBox
import com.azhagi.azhagikeys.lib.compose.AzhagiOutlinedButton
import com.azhagi.azhagikeys.lib.compose.AzhagiScreen
import com.azhagi.azhagikeys.lib.compose.defaultAzhagiOutlinedBox
import com.azhagi.azhagikeys.lib.compose.AzhagiHorizontalScroll
import com.azhagi.azhagikeys.lib.compose.stringRes
import com.azhagi.azhagikeys.lib.io.FileRegistry
import org.florisboard.lib.kotlin.resultOk

enum class ExtensionImportScreenType(
    val id: String,
    @StringRes val titleResId: Int,
    val supportedFiles: List<FileRegistry.Entry>,
) {
    EXT_ANY(
        id = "ext-any",
        titleResId = R.string.ext__import__ext_any,
        supportedFiles = listOf(FileRegistry.FlexExtension),
    ),
    EXT_KEYBOARD(
        id = "ext-keyboard",
        titleResId = R.string.ext__import__ext_keyboard,
        supportedFiles = listOf(FileRegistry.FlexExtension),
    ),
    EXT_THEME(
        id = "ext-theme",
        titleResId = R.string.ext__import__ext_theme,
        supportedFiles = listOf(FileRegistry.FlexExtension),
    ),
    EXT_LANGUAGEPACK(
        id = "ext-languagepack",
        titleResId = R.string.ext__import__ext_languagepack,
        supportedFiles = listOf(FileRegistry.FlexExtension),
    );
}

@Composable
fun ExtensionImportScreen(type: ExtensionImportScreenType, initUuid: String?) = AzhagiScreen {
    title = stringRes(type.titleResId)

    val navController = LocalNavController.current
    val context = LocalContext.current
    val cacheManager by context.cacheManager()
    val extensionManager by context.extensionManager()

    fun getSkipReason(fileInfo: CacheManager.FileInfo): Int {
        return when {
            !FileRegistry.matchesFileFilter(fileInfo, type.supportedFiles) -> {
                R.string.ext__import__file_skip_unsupported
            }
            fileInfo.ext != null -> {
                val ext = fileInfo.ext
                if (extensionManager.getExtensionById(ext.meta.id)?.sourceRef?.isAssets == true) {
                    R.string.ext__import__file_skip_ext_core
                } else {
                    NATIVE_NULLPTR.toInt()
                }
            }
            else -> { // ext == null
                R.string.ext__import__file_skip_ext_corrupted
            }
        }
    }

    fun Result<CacheManager.ImporterWorkspace>.mapSkipReasons(): Result<CacheManager.ImporterWorkspace> {
        return this.map { workspace ->
            workspace.inputFileInfos.forEach { fileInfo ->
                fileInfo.skipReason = getSkipReason(fileInfo)
            }
            workspace
        }
    }

    var importResult by remember(initUuid) {
        val workspace = initUuid?.let { cacheManager.importer.getWorkspaceByUuid(it) }
            ?.let { resultOk(it) }
            ?.mapSkipReasons()
        mutableStateOf(workspace)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uriList ->
            // If uri is null it indicates that the selection activity
            //  was cancelled (mostly by pressing the back button), so
            //  we don't display an error message here.
            if (uriList.isEmpty()) return@rememberLauncherForActivityResult
            importResult?.getOrNull()?.close()
            importResult = runCatching { cacheManager.readFromUriIntoCache(uriList) }.mapSkipReasons()
        },
    )

    bottomBar {
        AzhagiButtonBar {
            ButtonBarSpacer()
            ButtonBarTextButton(
                text = stringRes(R.string.action__cancel),
            ) {
                importResult?.getOrNull()?.close()
                navController.popBackStack()
            }
            val enabled = remember(importResult) {
                importResult?.getOrNull()?.takeIf { workspace ->
                    workspace.inputFileInfos.any { it.skipReason == NATIVE_NULLPTR.toInt() }
                } != null
            }
            ButtonBarButton(
                text = stringRes(R.string.action__import),
                enabled = enabled,
            ) {
                val workspace = importResult!!.getOrThrow()
                runCatching {
                    for (fileInfo in workspace.inputFileInfos) {
                        if (fileInfo.skipReason != NATIVE_NULLPTR.toInt()) {
                            continue
                        }
                        val ext = fileInfo.ext
                        when (type) {
                            ExtensionImportScreenType.EXT_ANY -> {
                                ext?.let { extensionManager.import(it) }
                            }
                            ExtensionImportScreenType.EXT_KEYBOARD -> {
                                ext.takeIf { it is KeyboardExtension }?.let { extensionManager.import(it) }
                            }
                            ExtensionImportScreenType.EXT_THEME -> {
                                ext.takeIf { it is ThemeExtension }?.let { extensionManager.import(it) }
                            }
                            ExtensionImportScreenType.EXT_LANGUAGEPACK -> {
                                ext.takeIf { it is LanguagePackExtension }?.let { extensionManager.import(it) }
                            }
                        }
                    }
                }.onSuccess {
                    workspace.close()
                    context.showLongToast(R.string.ext__import__success)
                    navController.popBackStack()
                }.onFailure { error ->
                    context.showLongToast(R.string.ext__import__failure, "error_message" to error.localizedMessage)
                }
            }
        }
    }

    content {
        if (initUuid == null) {
            AzhagiOutlinedButton(
                onClick = {
                    importLauncher.launch("*/*")
                },
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .align(Alignment.CenterHorizontally),
                text = stringRes(R.string.action__select_files),
            )
        }

        val result = importResult
        when {
            result == null -> {
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 16.dp),
                    text = stringRes(R.string.state__no_files_selected),
                    fontStyle = FontStyle.Italic,
                )
            }
            result.isSuccess -> {
                val workspace = result.getOrThrow()
                for (fileInfo in workspace.inputFileInfos) {
                    FileInfoView(fileInfo)
                }
            }
            result.isFailure -> {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringRes(R.string.ext__import__error_unexpected_exception),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                SelectionContainer {
                    Text(
                        modifier = Modifier
                            .AzhagiHorizontalScroll()
                            .padding(horizontal = 16.dp),
                        text = result.exceptionOrNull()?.stackTraceToString() ?: "null",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontStyle = FontStyle.Italic,
                    )
                }
            }
        }
    }
}

@Composable
private fun FileInfoView(
    fileInfo: CacheManager.FileInfo,
) {
    AzhagiOutlinedBox(
        modifier = Modifier.defaultAzhagiOutlinedBox(),
        title = fileInfo.file.name,
        subtitle = fileInfo.mediaType ?: "application/unknown",
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            val grayColor = LocalContentColor.current.copy(alpha = 0.56f)
            val ext = fileInfo.ext
            Row {
                Text(
                    text = Formatter.formatShortFileSize(LocalContext.current, fileInfo.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = grayColor,
                )
                if (ext != null) {
                    AzhagiBulletSpacer()
                    Text(
                        text = ext.meta.id,
                        style = MaterialTheme.typography.bodyMedium,
                        color = grayColor,
                    )
                    AzhagiBulletSpacer()
                    Text(
                        text = ext.meta.version,
                        style = MaterialTheme.typography.bodyMedium,
                        color = grayColor,
                    )
                }
            }
            if (ext != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ext.meta.title,
                    style = MaterialTheme.typography.bodyMedium,
                )
                ext.meta.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                val maintainers = remember(ext) {
                    ext.meta.maintainers.joinToString { it.name }
                }
                Text(
                    text = stringRes(R.string.ext__meta__maintainers_by, "maintainers" to maintainers),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                for (component in ext.components()) {
                    Text(
                        text = component.id,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (fileInfo.skipReason != NATIVE_NULLPTR.toInt()) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(19.dp)
                    .padding(top = 10.dp, bottom = 8.dp)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.56f)))
                Text(
                    text = stringRes(R.string.ext__import__file_skip),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = stringRes(fileInfo.skipReason),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontStyle = FontStyle.Italic,
                )
            }
        }
    }
}

