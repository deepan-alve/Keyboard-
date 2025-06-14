/*
 * Copyright (C) 2025 The AzhagiKeys Contributors
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

package org.azhagi.lib.snygg.value

private const val VarKey = "varKey"

sealed interface SnyggVarValue : SnyggValue {
    companion object {
        val VariableNameRegex = """--[a-zA-Z0-9-]+""".toRegex()
    }
}

data class SnyggDefinedVarValue(val key: String) : SnyggVarValue {
    companion object : SnyggValueEncoder {
        override val spec = SnyggValueSpec {
            function(name = "var") { string(id = VarKey, regex = SnyggVarValue.VariableNameRegex) }
        }

        override fun defaultValue() = SnyggDefinedVarValue("")

        override fun serialize(v: SnyggValue) = runCatching<String> {
            require(v is SnyggDefinedVarValue)
            val map = snyggIdToValueMapOf(VarKey to v.key)
            return@runCatching spec.pack(map)
        }

        override fun deserialize(v: String) = runCatching<SnyggValue> {
            val map = snyggIdToValueMapOf()
            spec.parse(v, map)
            val key = map.getString(VarKey)
            return@runCatching SnyggDefinedVarValue(key)
        }
    }

    override fun encoder() = Companion
}

