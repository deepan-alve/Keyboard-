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

package com.azhagi.azhagikeys.ime.clipboard.provider

import android.content.Context
import android.net.Uri
import com.azhagi.azhagikeys.lib.devtools.LogTopic
import com.azhagi.azhagikeys.lib.devtools.flogDebug
import org.florisboard.lib.android.readToFile
import org.florisboard.lib.kotlin.io.FsFile
import org.florisboard.lib.kotlin.io.subFile

/**
 * Backend helper object which is used by [ClipboardMediaProvider] to serve content.
 */
object ClipboardFileStorage {
    const val CLIPBOARD_FILES_PATH = "clipboard_files"

    private val Context.clipboardFilesDir: FsFile
        get() = FsFile(this.noBackupFilesDir, "clipboard_files").also { it.mkdirs() }

    /**
     * Clones a content URI to internal storage.
     *
     * @param uri The URI
     *
     * @return The file's name which is a unique long
     */
    @Synchronized
    fun cloneUri(context: Context, uri: Uri): Long {
        val id = System.nanoTime()
        val file = context.clipboardFilesDir.subFile(id.toString())
        context.contentResolver.readToFile(uri, file)
        return id
    }

    /**
     * Deletes the file corresponding to an id.
     */
    fun deleteById(context: Context, id: Long) {
        flogDebug(LogTopic.CLIPBOARD) { "Cleaning up $id" }
        val file = context.clipboardFilesDir.subFile(id.toString())
        file.delete()
    }

    fun getFileForId(context: Context, id: Long): FsFile {
        return context.clipboardFilesDir.subFile(id.toString())
    }


    /**
     * Insert file from backup if not existing
     *
     * @param context the application context
     * @param file the file to be inserted
     */
    fun insertFileFromBackupIfNotExisting(context: Context, file: FsFile) {
        if (!context.clipboardFilesDir.subFile(file.name).isFile) {
            file.copyTo(context.clipboardFilesDir.subFile(file.name), overwrite = false)
        }
    }

    /**
     * Deletes all files from the clipboard subdirectory
     *
     * @param context the application context
     */
    fun resetClipboardFileStorage(context: Context) {
        context.clipboardFilesDir.listFiles()?.forEach {
            it.delete()
        }
    }

}

