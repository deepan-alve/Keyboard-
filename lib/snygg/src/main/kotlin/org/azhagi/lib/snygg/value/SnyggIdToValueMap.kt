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

import org.florisboard.lib.kotlin.toStringWithoutDotZero

typealias SnyggIdToValueMap = MutableMap<String, String>

fun snyggIdToValueMapOf(vararg pairs: Pair<String, Any>): SnyggIdToValueMap {
    val map = mutableMapOf<String, String>()
    map.add(*pairs)
    return map
}

fun SnyggIdToValueMap.getInt(id: String): Int {
    return getValue(id).toInt()
}

fun SnyggIdToValueMap.getFloat(id: String): Float {
    return getValue(id).toFloat()
}

fun SnyggIdToValueMap.getString(id: String): String {
    return getValue(id)
}

fun SnyggIdToValueMap.add(vararg pairs: Pair<String, Any>) {
    pairs.forEach { (id, value) ->
        if (value is Number) {
            put(id, value.toStringWithoutDotZero())
        } else {
            put(id, value.toString())
        }
    }
}

