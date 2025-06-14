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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.azhagi.azhagikeys.app.LocalNavController
import com.azhagi.azhagikeys.app.Routes
import com.azhagi.azhagikeys.lib.ext.Extension
import dev.patrickgold.jetpref.material.ui.JetPrefListItem

@Composable
fun <T : Extension> ExtensionList(
    extList: List<T>,
    modifier: Modifier = Modifier,
    summaryProvider: (T) -> String? = { null },
) {
    val navController = LocalNavController.current

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        for (ext in extList) {
            JetPrefListItem(
                icon = { },
                modifier = Modifier
                    .clickable {
                        navController.navigate(Routes.Ext.View(ext.meta.id))
                    },
                text = ext.meta.title,
                secondaryText = summaryProvider(ext),
            )
        }
    }
}

