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

package com.azhagi.azhagikeys.app.settings.advanced

import android.content.ContentUris
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import com.azhagi.azhagikeys.BuildConfig
import com.azhagi.azhagikeys.R
import com.azhagi.azhagikeys.app.LocalNavController
import com.azhagi.azhagikeys.cacheManager
import com.azhagi.azhagikeys.clipboardManager
import com.azhagi.azhagikeys.ime.clipboard.provider.ClipboardFileStorage
import com.azhagi.azhagikeys.ime.clipboard.provider.ItemType
import com.azhagi.azhagikeys.lib.cache.CacheManager
import com.azhagi.azhagikeys.lib.compose.AzhagiButtonBar
import com.azhagi.azhagikeys.lib.compose.AzhagiOutlinedBox
import com.azhagi.azhagikeys.lib.compose.AzhagiScreen
import com.azhagi.azhagikeys.lib.compose.defaultAzhagiOutlinedBox
import com.azhagi.azhagikeys.lib.compose.rippleClickable
import com.azhagi.azhagikeys.lib.compose.stringRes
import com.azhagi.azhagikeys.lib.devtools.flogError
import com.azhagi.azhagikeys.lib.ext.ExtensionManager
import com.azhagi.azhagikeys.lib.io.FileRegistry
import com.azhagi.azhagikeys.lib.io.ZipUtils
import dev.patrickgold.jetpref.datastore.jetprefDatastoreDir
import dev.patrickgold.jetpref.material.ui.JetPrefListItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.florisboard.lib.android.showLongToast
import org.florisboard.lib.android.writeFromFile
import org.florisboard.lib.kotlin.io.subDir
import org.florisboard.lib.kotlin.io.subFile
import org.florisboard.lib.kotlin.io.writeJson

object Backup {
    const val FILE_PROVIDER_AUTHORITY = "${BuildConfig.APPLICATION_ID}.provider.file"
    const val METADATA_JSON_NAME = "backup_metadata.json"
    const val CLIPBOARD_TEXT_ITEMS_JSON_NAME = "clipboard_text_items.json"
    const val CLIPBOARD_IMAGES_JSON_NAME = "clipboard_images.json"
    const val CLIPBOARD_VIDEO_JSON_NAME = "clipboard_video.json"

    fun defaultFileName(metadata: Metadata): String {
        return "backup_${metadata.packageName}_${metadata.versionCode}_${metadata.timestamp}.zip"
    }

    enum class Destination {
        FILE_SYS,
        SHARE_INTENT;
    }

    class FilesSelector {
        var jetprefDatastore by mutableStateOf(true)
        var imeKeyboard by mutableStateOf(true)
        var imeTheme by mutableStateOf(true)
        var clipboardTextItems by mutableStateOf(false)
        var clipboardImageItems by mutableStateOf(false)
        var clipboardVideoItems by mutableStateOf(false)

        private var _clipboardData: MutableState<ToggleableState> = mutableStateOf(ToggleableState.Off)
        val clipboardData: State<ToggleableState> = _clipboardData

        fun updateCheckboxState() {
            val newValue = if (
                !clipboardVideoItems && !clipboardImageItems && !clipboardTextItems
            ) {
                ToggleableState.Off
            } else if (
                clipboardVideoItems && clipboardImageItems && clipboardTextItems
            ) {
                ToggleableState.On
            } else {
                ToggleableState.Indeterminate
            }
            _clipboardData.value = newValue
        }

        fun provideClipboardItems(): Boolean {
            return clipboardTextItems || clipboardImageItems || clipboardVideoItems
        }

        fun atLeastOneSelected(): Boolean {
            return jetprefDatastore || imeKeyboard || imeTheme || clipboardTextItems || clipboardImageItems || clipboardVideoItems
        }
    }

    @Serializable
    data class Metadata(
        @SerialName("package")
        val packageName: String,
        val versionCode: Int,
        val versionName: String,
        val timestamp: Long,
    )
}

@Composable
fun BackupScreen() = AzhagiScreen {
    title = stringRes(R.string.backup_and_restore__back_up__title)
    previewFieldVisible = false

    val navController = LocalNavController.current
    val context = LocalContext.current
    val cacheManager by context.cacheManager()

    var backupDestination by remember { mutableStateOf(Backup.Destination.FILE_SYS) }
    val backupFilesSelector = remember { Backup.FilesSelector() }
    var backupWorkspace: CacheManager.BackupAndRestoreWorkspace? = null

    val backUpToFileSystemLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            if (uri == null) {
                // User can modify checkboxes between cancellation and second
                // trigger, so we make sure to clear out the previous workspace
                backupWorkspace?.close()
                backupWorkspace = null
                return@rememberLauncherForActivityResult
            }
            runCatching {
                context.contentResolver.writeFromFile(uri, backupWorkspace!!.zipFile)
                backupWorkspace!!.close()
            }.onSuccess {
                context.showLongToast(R.string.backup_and_restore__back_up__success)
                navController.popBackStack()
            }.onFailure { error ->
                flogError { error.stackTraceToString() }
                context.showLongToast(R.string.backup_and_restore__back_up__failure, "error_message" to error.message)
                backupWorkspace = null
            }
        },
    )

    fun prepareBackupWorkspace() {
        val workspace = cacheManager.backupAndRestore.new()
        if (backupFilesSelector.jetprefDatastore) {
            context.jetprefDatastoreDir.let { dir ->
                dir.copyRecursively(workspace.inputDir.subDir(dir.name))
            }
        }
        val workspaceFilesDir = workspace.inputDir.subDir("files")
        if (backupFilesSelector.imeKeyboard) {
            context.filesDir.subDir(ExtensionManager.IME_KEYBOARD_PATH).let { dir ->
                dir.copyRecursively(workspaceFilesDir.subDir(ExtensionManager.IME_KEYBOARD_PATH))
            }
        }
        if (backupFilesSelector.imeTheme) {
            context.filesDir.subDir(ExtensionManager.IME_THEME_PATH).let { dir ->
                dir.copyRecursively(workspaceFilesDir.subDir(ExtensionManager.IME_THEME_PATH))
            }
        }

        if (backupFilesSelector.provideClipboardItems()) {
            val clipboardHistory = context.clipboardManager().value.history().all
            val clipboardFilesDir = workspace.inputDir.subDir("clipboard")
            clipboardFilesDir.mkdir()
            if (backupFilesSelector.clipboardTextItems) {
                clipboardFilesDir.subFile(Backup.CLIPBOARD_TEXT_ITEMS_JSON_NAME)
                    .writeJson(clipboardHistory.filter { it.type == ItemType.TEXT })
            }
            if (backupFilesSelector.clipboardImageItems) {
                clipboardFilesDir.subFile(Backup.CLIPBOARD_IMAGES_JSON_NAME)
                    .writeJson(clipboardHistory.filter { it.type == ItemType.IMAGE })
                for (item in clipboardHistory.filter { it.type == ItemType.IMAGE }) {
                    val id = ContentUris.parseId(item.uri!!)
                    ClipboardFileStorage.getFileForId(context, id).copyTo(
                        clipboardFilesDir.subFile("${ClipboardFileStorage.CLIPBOARD_FILES_PATH}/$id")
                    )
                }
            }
            if (backupFilesSelector.clipboardVideoItems) {
                clipboardFilesDir.subFile(Backup.CLIPBOARD_VIDEO_JSON_NAME)
                    .writeJson(clipboardHistory.filter { it.type == ItemType.VIDEO })
                for (item in clipboardHistory.filter { it.type == ItemType.VIDEO }) {
                    val id = ContentUris.parseId(item.uri!!)
                    ClipboardFileStorage.getFileForId(context, id).copyTo(
                        clipboardFilesDir.subFile("${ClipboardFileStorage.CLIPBOARD_FILES_PATH}/$id")
                    )
                }
            }
        }
        workspace.metadata = Backup.Metadata(
            packageName = BuildConfig.APPLICATION_ID,
            versionCode = BuildConfig.VERSION_CODE,
            versionName = BuildConfig.VERSION_NAME,
            timestamp = System.currentTimeMillis(),
        )
        workspace.inputDir.subFile(Backup.METADATA_JSON_NAME).writeJson(workspace.metadata)
        workspace.zipFile = workspace.outputDir.subFile(Backup.defaultFileName(workspace.metadata))
        ZipUtils.zip(workspace.inputDir, workspace.zipFile)
        backupWorkspace = workspace
    }

    fun prepareAndPerformBackup() {
        runCatching {
            if (backupWorkspace == null || backupWorkspace!!.isClosed()) {
                prepareBackupWorkspace()
            }
            when (backupDestination) {
                Backup.Destination.FILE_SYS -> {
                    backUpToFileSystemLauncher.launch(backupWorkspace!!.zipFile.name)
                }

                Backup.Destination.SHARE_INTENT -> {
                    val uri =
                        FileProvider.getUriForFile(context, Backup.FILE_PROVIDER_AUTHORITY, backupWorkspace!!.zipFile)
                    val shareIntent = ShareCompat.IntentBuilder(context)
                        .setStream(uri)
                        .setType(FileRegistry.BackupArchive.mediaType)
                        .createChooserIntent()
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(shareIntent)
                }
            }
        }.onFailure { error ->
            flogError { error.stackTraceToString() }
            context.showLongToast(R.string.backup_and_restore__back_up__failure, "error_message" to error.message)
            backupWorkspace = null
        }
    }

    bottomBar {
        AzhagiButtonBar {
            ButtonBarSpacer()
            ButtonBarTextButton(
                onClick = {
                    backupWorkspace?.close()
                    navController.popBackStack()
                },
                text = stringRes(R.string.action__cancel),
            )
            ButtonBarButton(
                onClick = {
                    prepareAndPerformBackup()
                },
                text = stringRes(R.string.action__back_up),
                enabled = backupFilesSelector.atLeastOneSelected(),
            )
        }
    }

    content {
        AzhagiOutlinedBox(
            modifier = Modifier.defaultAzhagiOutlinedBox(),
            title = stringRes(R.string.backup_and_restore__back_up__destination),
        ) {
            RadioListItem(
                onClick = {
                    backupDestination = Backup.Destination.FILE_SYS
                },
                selected = backupDestination == Backup.Destination.FILE_SYS,
                text = stringRes(R.string.backup_and_restore__back_up__destination_file_sys),
            )
            RadioListItem(
                onClick = {
                    backupDestination = Backup.Destination.SHARE_INTENT
                },
                selected = backupDestination == Backup.Destination.SHARE_INTENT,
                text = stringRes(R.string.backup_and_restore__back_up__destination_share_intent),
            )
        }
        BackupFilesSelector(
            filesSelector = backupFilesSelector,
            title = stringRes(R.string.backup_and_restore__back_up__files),
        )
    }
}

@Composable
internal fun BackupFilesSelector(
    modifier: Modifier = Modifier,
    filesSelector: Backup.FilesSelector,
    title: String,
) {
    AzhagiOutlinedBox(
        modifier = modifier.defaultAzhagiOutlinedBox(),
        title = title,
    ) {
        CheckboxListItem(
            onClick = { filesSelector.jetprefDatastore = !filesSelector.jetprefDatastore },
            checked = filesSelector.jetprefDatastore,
            text = stringRes(R.string.backup_and_restore__back_up__files_jetpref_datastore),
        )
        CheckboxListItem(
            onClick = { filesSelector.imeKeyboard = !filesSelector.imeKeyboard },
            checked = filesSelector.imeKeyboard,
            text = stringRes(R.string.backup_and_restore__back_up__files_ime_keyboard),
        )
        CheckboxListItem(
            onClick = { filesSelector.imeTheme = !filesSelector.imeTheme },
            checked = filesSelector.imeTheme,
            text = stringRes(R.string.backup_and_restore__back_up__files_ime_theme),
        )

        TriStateCheckboxListItem(
            onClick = {
                if (
                    filesSelector.clipboardData.value == ToggleableState.Off ||
                    filesSelector.clipboardData.value == ToggleableState.Indeterminate
                ) {
                    filesSelector.clipboardImageItems = true
                    filesSelector.clipboardVideoItems = true
                    filesSelector.clipboardTextItems = true
                } else {
                    filesSelector.clipboardImageItems = false
                    filesSelector.clipboardVideoItems = false
                    filesSelector.clipboardTextItems = false
                }
                filesSelector.updateCheckboxState()
            },
            state = filesSelector.clipboardData.value,
            text = stringRes(R.string.backup_and_restore__back_up__files_clipboard_history),
        )


        CheckboxListItem(
            onClick = {
                filesSelector.clipboardTextItems = !filesSelector.clipboardTextItems
                filesSelector.updateCheckboxState()
            },
            checked = filesSelector.clipboardTextItems,
            text = stringRes(R.string.backup_and_restore__back_up__files_clipboard_history__clipboard_text_items),
            isSecondaryListItem = true,
        )
        CheckboxListItem(
            onClick = {
                filesSelector.clipboardImageItems = !filesSelector.clipboardImageItems
                filesSelector.updateCheckboxState()
            },
            checked = filesSelector.clipboardImageItems,
            text = stringRes(R.string.backup_and_restore__back_up__files_clipboard_history__clipboard_image_items),
            isSecondaryListItem = true,
        )
        CheckboxListItem(
            onClick = {
                filesSelector.clipboardVideoItems = !filesSelector.clipboardVideoItems
                filesSelector.updateCheckboxState()
            },
            checked = filesSelector.clipboardVideoItems,
            text = stringRes(R.string.backup_and_restore__back_up__files_clipboard_history__clipboard_video_items),
            isSecondaryListItem = true,
        )

    }
}

@Composable
internal fun CheckboxListItem(
    onClick: () -> Unit,
    checked: Boolean,
    text: String,
    isSecondaryListItem: Boolean = false
) {
    JetPrefListItem(
        modifier = Modifier.rippleClickable(onClick = onClick),
        icon = {
            Row {
                if (isSecondaryListItem) {
                    Spacer(modifier = Modifier.width(40.dp))
                }
                Checkbox(
                    checked = checked,
                    onCheckedChange = null,
                )
            }
        },
        text = text,
    )
}

@Composable
internal fun TriStateCheckboxListItem(
    onClick: () -> Unit,
    state: ToggleableState,
    text: String,
    isSecondaryListItem: Boolean = false,
) {
    JetPrefListItem(
        modifier = Modifier.rippleClickable(onClick = onClick),
        icon = {
            Row {
                if (isSecondaryListItem) {
                    Spacer(modifier = Modifier.width(40.dp))
                }
                TriStateCheckbox(
                    state = state,
                    onClick = null,
                )
            }
        },
        text = text,
    )
}

@Composable
internal fun RadioListItem(
    onClick: () -> Unit,
    selected: Boolean,
    text: String,
    secondaryText: String? = null,
) {
    JetPrefListItem(
        modifier = Modifier.rippleClickable(onClick = onClick),
        icon = {
            RadioButton(
                selected = selected,
                onClick = null,
            )
        },
        text = text,
        secondaryText = secondaryText,
    )
}

