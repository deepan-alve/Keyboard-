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

package com.azhagi.azhagikeys.ime.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.azhagi.azhagikeys.app.AzhagiPreferenceModel
import com.azhagi.azhagikeys.appContext
import com.azhagi.azhagikeys.editorInstance
import com.azhagi.azhagikeys.ime.clipboard.provider.ClipboardHistoryDao
import com.azhagi.azhagikeys.ime.clipboard.provider.ClipboardHistoryDatabase
import com.azhagi.azhagikeys.ime.clipboard.provider.ClipboardItem
import com.azhagi.azhagikeys.ime.clipboard.provider.ItemType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.florisboard.lib.android.AndroidClipboardManager
import org.florisboard.lib.android.AndroidClipboardManager_OnPrimaryClipChangedListener
import org.florisboard.lib.android.setOrClearPrimaryClip
import org.florisboard.lib.android.showShortToast
import org.florisboard.lib.android.systemService
import org.florisboard.lib.kotlin.tryOrNull
import java.io.Closeable

/**
 * [ClipboardManager] manages the clipboard and clipboard history.
 *
 * Also just going to document how all the classes here work.
 *
 * [ClipboardManager] handles storage and retrieval of clipboard items. All manipulation of the
 * clipboard goes through here.
 */
class ClipboardManager(
    context: Context,
) : AndroidClipboardManager_OnPrimaryClipChangedListener, Closeable {
    companion object {
        // 1 minute
        private const val INTERVAL = 60 * 1000L
        private const val RECENT_TIMESPAN_MS = 300_000 // 300 sec = 5 min

        /**
         * Taken from ClipboardDescription.java from the AOSP
         *
         * Helper to compare two MIME types, where one may be a pattern.
         * @param concreteType A fully-specified MIME type.
         * @param desiredType A desired MIME type that may be a pattern such as * / *.
         * @return Returns true if the two MIME types match.
         */
        fun compareMimeTypes(concreteType: String, desiredType: String): Boolean {
            val typeLength = desiredType.length
            if (typeLength == 3 && desiredType == "*/*") {
                return true
            }
            val slashpos = desiredType.indexOf('/')
            if (slashpos > 0) {
                if (typeLength == slashpos + 2 && desiredType[slashpos + 1] == '*') {
                    if (desiredType.regionMatches(0, concreteType, 0, slashpos + 1)) {
                        return true
                    }
                } else if (desiredType == concreteType) {
                    return true
                }
            }
            return false
        }
    }

    private val prefs by AzhagiPreferenceModel()
    private val appContext by context.appContext()
    private val editorInstance by context.editorInstance()
    private val systemClipboardManager = context.systemService(AndroidClipboardManager::class)

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var cleanUpJob: Job
    private var clipHistoryDb: ClipboardHistoryDatabase? = null
    private val clipHistoryDao: ClipboardHistoryDao? get() = clipHistoryDb?.clipboardItemDao()

    private val _history = MutableLiveData(ClipboardHistory.Empty)
    val history: LiveData<ClipboardHistory> get() = _history

    private val primaryClipLastFromCallbackGuard = Mutex(locked = false)
    private var primaryClipLastFromCallback: ClipData? = null
    private val _primaryClipFlow = MutableStateFlow<ClipboardItem?>(null)
    val primaryClipFlow = _primaryClipFlow.asStateFlow()
    inline var primaryClip
        get() = primaryClipFlow.value
        private set(v) {
            _primaryClipFlow.value = v
        }

    init {
        systemClipboardManager.addPrimaryClipChangedListener(this)
        cleanUpJob = ioScope.launch {
            while (isActive) {
                delay(INTERVAL)
                enforceExpiryDate(history())
            }
        }
    }

    fun initializeForContext(context: Context) {
        ioScope.launch {
            if (clipHistoryDb == null) {
                clipHistoryDb = ClipboardHistoryDatabase.new(context.applicationContext)
                withContext(Dispatchers.Main) {
                    clipHistoryDao?.getAllLive()?.observeForever { items ->
                        updateHistory(items)
                    }
                }
            }
        }
    }

    private fun updateHistory(items: List<ClipboardItem>) {
        val itemsSorted = items.sortedByDescending { it.creationTimestampMs }
        val clipHistory = ClipboardHistory(itemsSorted)
        enforceHistoryLimit(clipHistory)
        _history.postValue(clipHistory)
    }

    fun history(): ClipboardHistory = history.value!!

    /**
     * Sets the current primary clip without updating the internal clipboard history.
     */
    fun updatePrimaryClip(item: ClipboardItem?) {
        primaryClip = item
        if (prefs.clipboard.useInternalClipboard.get()) {
            // Purposely do not sync to system if disabled in prefs
            if (prefs.clipboard.syncToSystem.get()) {
                systemClipboardManager.setOrClearPrimaryClip(item?.toClipData(appContext))
            }
        } else {
            systemClipboardManager.setOrClearPrimaryClip(item?.toClipData(appContext))
        }
    }

    /**
     * Called by system clipboard when the system primary clip has changed.
     */
    override fun onPrimaryClipChanged() {
        if (!prefs.clipboard.useInternalClipboard.get() || prefs.clipboard.syncToAzhagi.get()) {
            val systemPrimaryClip = systemClipboardManager.primaryClip
            ioScope.launch {
                val isDuplicate: Boolean
                primaryClipLastFromCallbackGuard.withLock {
                    val a = primaryClipLastFromCallback?.getItemAt(0)
                    val b = systemPrimaryClip?.getItemAt(0)
                    isDuplicate = when {
                        a === b || a == null && b == null -> true
                        a == null || b == null -> false
                        else -> a.text == b.text && a.uri == b.uri
                    }
                    primaryClipLastFromCallback = systemPrimaryClip
                }
                if (isDuplicate) return@launch

                val internalPrimaryClip = primaryClip

                if (systemPrimaryClip == null) {
                    primaryClip = null
                    return@launch
                }

                if (systemPrimaryClip.getItemAt(0).let { it.text == null && it.uri == null }) {
                    primaryClip = null
                    return@launch
                }

                val isEqual = internalPrimaryClip?.isEqualTo(systemPrimaryClip) == true
                if (!isEqual) {
                    val item = ClipboardItem.fromClipData(appContext, systemPrimaryClip, cloneUri = true)
                    primaryClip = item
                    insertOrMoveBeginning(item)
                }
            }
        }
    }

    /**
     * Change the current text on clipboard, update history (if enabled).
     */
    private fun addNewClip(item: ClipboardItem) {
        insertOrMoveBeginning(item)
        updatePrimaryClip(item)
    }

    /**
     * Wraps some plaintext in a ClipData and calls [addNewClip]
     */
    fun addNewPlaintext(newText: String) {
        val newData = ClipboardItem.text(newText)
        addNewClip(newData)
    }

    /**
     * Adds a new item to the clipboard history (if enabled).
     */
    private fun insertOrMoveBeginning(newItem: ClipboardItem) {
        if (prefs.clipboard.historyEnabled.get()) {
            val historyElement = history().all.firstOrNull { it.type == ItemType.TEXT && it.text == newItem.text }
            if (historyElement != null) {
                moveToTheBeginning(
                    oldItem = historyElement,
                    newItem = if (historyElement.isPinned) {
                        newItem.copy(isPinned = true)
                    } else {
                        newItem
                    }
                )
            } else {
                insertClip(newItem)
            }
        }
    }

    private fun enforceHistoryLimit(clipHistory: ClipboardHistory) {
        if (prefs.clipboard.limitHistorySize.get()) {
            val nonPinnedItems = clipHistory.recent + clipHistory.other
            val nToRemove = nonPinnedItems.size - prefs.clipboard.maxHistorySize.get()
            if (nToRemove > 0) {
                val itemsToRemove = nonPinnedItems.asReversed().filterIndexed { n, _ -> n < nToRemove }
                ioScope.launch {
                    clipHistoryDao?.delete(itemsToRemove)
                }
            }
        }
    }

    private fun enforceExpiryDate(clipHistory: ClipboardHistory) {
        val itemsToRemove = mutableSetOf<ClipboardItem>()
        if (prefs.clipboard.cleanUpOld.get()) {
            val nonPinnedItems = clipHistory.recent + clipHistory.other
            val expiryTime = System.currentTimeMillis() - (prefs.clipboard.cleanUpAfter.get() * 60 * 1000)
            itemsToRemove.addAll(nonPinnedItems.filter { it.creationTimestampMs < expiryTime })
        }
        if (prefs.clipboard.autoCleanSensitive.get()) {
            val sensitiveData = clipHistory.all.filter { it.isSensitive }
            val expiryTime = System.currentTimeMillis() - (prefs.clipboard.autoCleanSensitiveAfter.get() * 1000)
            itemsToRemove.addAll(sensitiveData.filter { it.creationTimestampMs < expiryTime })
        }
        if (itemsToRemove.isNotEmpty()) {
            ioScope.launch {
                clipHistoryDao?.delete(itemsToRemove.toList())
            }
        }
    }

    private fun moveToTheBeginning(oldItem: ClipboardItem, newItem: ClipboardItem) {
        ioScope.launch {
            clipHistoryDao?.delete(oldItem)
            clipHistoryDao?.insert(newItem)
        }
    }

    fun insertClip(item: ClipboardItem) {
        ioScope.launch {
            val id = clipHistoryDao?.insert(item)
            item.id = id ?: 0
        }
    }

    /**
     * Clears all unpinned items from the clipboard history
     */
    fun clearHistory() {
        ioScope.launch {
            for (item in history().all) {
                item.close(appContext)
            }
            clipHistoryDao?.deleteAllUnpinned()
        }
    }

    /**
     * Clears the full clipboard history
     */
    fun clearFullHistory() {
        ioScope.launch {
            for (item in history().all) {
                item.close(appContext)
            }
            clipHistoryDao?.deleteAll()
        }
    }


    /**
     * Restore the clipboard history from a [List]
     *
     * @param items the [ClipboardItem] list with the new items
     */
    fun restoreHistory(items: List<ClipboardItem>) {
        ioScope.launch {
            val currentHistory = this@ClipboardManager.history().all
            for (item in items) {
                if (!currentHistory.map { it.copy(id = 0) }.contains(item.copy(id = 0))) {
                    this@ClipboardManager.insertClip(item.copy(id = 0))
                }
            }
        }
    }

    fun deleteClip(item: ClipboardItem) {
        ioScope.launch {
            clipHistoryDao?.delete(item)
            tryOrNull {
                val uri = item.uri
                if (uri != null) {
                    appContext.contentResolver.delete(uri, null, null)
                }
            }
        }
    }

    fun pinClip(item: ClipboardItem) {
        ioScope.launch {
            clipHistoryDao?.update(item.copy(isPinned = true))
        }
    }

    fun unpinClip(item: ClipboardItem) {
        ioScope.launch {
            clipHistoryDao?.update(item.copy(isPinned = false))
        }
    }

    fun pasteItem(item: ClipboardItem) {
        val editorInstance by appContext.editorInstance()
        editorInstance.commitClipboardItem(item).also { result ->
            if (!result) {
                appContext.showShortToast("Failed to paste item.")
            }
        }
    }

    /**
     * Returns true if the editor can accept the clip item, else false.
     */
    fun canBePasted(clipItem: ClipboardItem?): Boolean {
        if (clipItem == null) return false

        return clipItem.mimeTypes.contains("text/plain") || editorInstance.activeInfo.contentMimeTypes.any { editorType ->
            clipItem.mimeTypes.any { clipType ->
                compareMimeTypes(clipType, editorType)
            }
        }
    }

    /**
     * Cleans up.
     *
     * Unregisters the system clipboard listener, cancels clipboard clean ups.
     */
    override fun close() {
        systemClipboardManager.removePrimaryClipChangedListener(this)
        cleanUpJob.cancel()
    }

    class ClipboardHistory(val all: List<ClipboardItem>) {
        companion object {
            val Empty = ClipboardHistory(emptyList())
        }

        private val now = System.currentTimeMillis()

        val pinned = all.filter { it.isPinned }
        val recent = all.filter { !it.isPinned && (now - it.creationTimestampMs < RECENT_TIMESPAN_MS) }
        val other = all.filter { !it.isPinned && (now - it.creationTimestampMs >= RECENT_TIMESPAN_MS) }
    }
}

