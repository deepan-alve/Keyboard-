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

package com.azhagi.azhagikeys.app.settings.about

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.m3.libraryColors
import com.azhagi.azhagikeys.R
import com.azhagi.azhagikeys.lib.compose.AzhagiScreen
import com.azhagi.azhagikeys.lib.compose.AzhagiScrollbar
import com.azhagi.azhagikeys.lib.compose.stringRes

@Composable
fun ThirdPartyLicensesScreen() = AzhagiScreen {
    title = stringRes(R.string.about__third_party_licenses__title)
    scrollable = false
    iconSpaceReserved = false

    val lazyListState = rememberLazyListState()

    content {
        LibrariesContainer(
            modifier = Modifier
                .fillMaxSize()
                .AzhagiScrollbar(lazyListState, isVertical = true),
            colors = LibraryDefaults.libraryColors(
                backgroundColor = MaterialTheme.colorScheme.background,
                badgeBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
                badgeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                contentColor = MaterialTheme.colorScheme.onBackground,
                dialogConfirmButtonColor = MaterialTheme.colorScheme.primary,
            ),
            lazyListState = lazyListState,
        )
    }
}

