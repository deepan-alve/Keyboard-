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

package com.azhagi.azhagikeys.ime.dictionary

import android.content.Context
import androidx.room.Room
import com.azhagi.azhagikeys.app.AzhagiPreferenceModel
import com.azhagi.azhagikeys.ime.nlp.SuggestionCandidate
import com.azhagi.azhagikeys.ime.nlp.WordSuggestionCandidate
import com.azhagi.azhagikeys.lib.AzhagiLocale
import java.lang.ref.WeakReference

/**
 * TODO: document
 */
class DictionaryManager private constructor(context: Context) {
    private val applicationContext: WeakReference<Context> = WeakReference(context.applicationContext ?: context)
    private val prefs by AzhagiPreferenceModel()

    private var AzhagiUserDictionaryDatabase: AzhagiUserDictionaryDatabase? = null
    private var systemUserDictionaryDatabase: SystemUserDictionaryDatabase? = null

    companion object {
        private var defaultInstance: DictionaryManager? = null

        fun init(applicationContext: Context): DictionaryManager {
            val instance = DictionaryManager(applicationContext)
            defaultInstance = instance
            return instance
        }

        fun default(): DictionaryManager {
            val instance = defaultInstance
            if (instance != null) {
                return instance
            } else {
                throw UninitializedPropertyAccessException(
                    "${DictionaryManager::class.simpleName} has not been initialized previously. Make sure to call init(applicationContext) before using default()."
                )
            }
        }
    }

    fun queryUserDictionary(word: String, locale: AzhagiLocale): List<SuggestionCandidate> {
        val AzhagiDao = AzhagiUserDictionaryDao()
        val systemDao = systemUserDictionaryDao()
        if (AzhagiDao == null && systemDao == null) {
            return emptyList()
        }
        return buildList {
            if (prefs.dictionary.enableAzhagiUserDictionary.get()) {
                AzhagiDao?.query(word, locale)?.let {
                    for (entry in it) {
                        add(WordSuggestionCandidate(entry.word, confidence = entry.freq / 255.0))
                    }
                }
                AzhagiDao?.queryShortcut(word, locale)?.let {
                    for (entry in it) {
                        add(WordSuggestionCandidate(entry.word, confidence = entry.freq / 255.0))
                    }
                }
            }
            if (prefs.dictionary.enableSystemUserDictionary.get()) {
                systemDao?.query(word, locale)?.let {
                    for (entry in it) {
                        add(WordSuggestionCandidate(entry.word, confidence = entry.freq / 255.0))
                    }
                }
                systemDao?.queryShortcut(word, locale)?.let {
                    for (entry in it) {
                        add(WordSuggestionCandidate(entry.word, confidence = entry.freq / 255.0))
                    }
                }
            }
        }

    }

    fun spell(word: String, locale: AzhagiLocale): Boolean {
        val AzhagiDao = AzhagiUserDictionaryDao()
        val systemDao = systemUserDictionaryDao()
        if (AzhagiDao == null && systemDao == null) {
            return false
        }
        var ret = false
        if (prefs.dictionary.enableAzhagiUserDictionary.get()) {
            ret = ret || AzhagiDao?.queryExactFuzzyLocale(word, locale)?.isNotEmpty() ?: false
            ret = ret || AzhagiDao?.queryShortcut(word, locale)?.isNotEmpty() ?: false
        }
        if (prefs.dictionary.enableSystemUserDictionary.get()) {
            ret = ret || systemDao?.queryExactFuzzyLocale(word, locale)?.isNotEmpty() ?: false
            ret = ret || systemDao?.queryShortcut(word, locale)?.isNotEmpty() ?: false
        }
        return ret
    }

    @Synchronized
    fun AzhagiUserDictionaryDao(): UserDictionaryDao? {
        return if (prefs.dictionary.enableAzhagiUserDictionary.get()) {
            AzhagiUserDictionaryDatabase?.userDictionaryDao()
        } else {
            null
        }
    }

    @Synchronized
    fun AzhagiUserDictionaryDatabase(): AzhagiUserDictionaryDatabase? {
        return if (prefs.dictionary.enableAzhagiUserDictionary.get()) {
            AzhagiUserDictionaryDatabase
        } else {
            null
        }
    }

    @Synchronized
    fun systemUserDictionaryDao(): UserDictionaryDao? {
        return if (prefs.dictionary.enableSystemUserDictionary.get()) {
            systemUserDictionaryDatabase?.userDictionaryDao()
        } else {
            null
        }
    }

    @Synchronized
    fun systemUserDictionaryDatabase(): SystemUserDictionaryDatabase? {
        return if (prefs.dictionary.enableSystemUserDictionary.get()) {
            systemUserDictionaryDatabase
        } else {
            null
        }
    }    @Synchronized
    fun loadUserDictionariesIfNecessary() {
        val context = applicationContext.get() ?: return
        
        if (AzhagiUserDictionaryDatabase == null && prefs.dictionary.enableAzhagiUserDictionary.get()) {
            AzhagiUserDictionaryDatabase = Room.databaseBuilder(
                context,
                com.azhagi.azhagikeys.ime.dictionary.AzhagiUserDictionaryDatabase::class.java,
                com.azhagi.azhagikeys.ime.dictionary.AzhagiUserDictionaryDatabase.DB_FILE_NAME
            ).allowMainThreadQueries().build()
        }
        if (systemUserDictionaryDatabase == null && prefs.dictionary.enableSystemUserDictionary.get()) {
            systemUserDictionaryDatabase = SystemUserDictionaryDatabase(context)
        }
    }

    @Synchronized
    fun unloadUserDictionariesIfNecessary() {
        if (AzhagiUserDictionaryDatabase != null) {
            AzhagiUserDictionaryDatabase?.close()
            AzhagiUserDictionaryDatabase = null
        }
        if (systemUserDictionaryDatabase != null) {
            systemUserDictionaryDatabase = null
        }
    }
}

