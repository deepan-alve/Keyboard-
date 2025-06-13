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

package com.azhagi.azhagikeys.lib.compose

import android.app.Activity
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import com.azhagi.azhagikeys.app.AppPrefs
import com.azhagi.azhagikeys.app.LocalNavController
import com.azhagi.azhagikeys.app.AzhagiPreferenceModel
import dev.patrickgold.jetpref.datastore.ui.PreferenceLayout
import dev.patrickgold.jetpref.datastore.ui.PreferenceUiContent
import org.florisboard.lib.android.AndroidVersion

@Composable
fun AzhagiScreen(builder: @Composable AzhagiScreenScope.() -> Unit) {
    val scope = remember { AzhagiScreenScopeImpl() }
    builder(scope)
    scope.Render()
}

typealias AzhagiScreenActions = @Composable RowScope.() -> Unit
typealias AzhagiScreenBottomBar = @Composable () -> Unit
typealias AzhagiScreenContent = PreferenceUiContent<AppPrefs>
typealias AzhagiScreenFab = @Composable () -> Unit
typealias AzhagiScreenNavigationIcon = @Composable () -> Unit

interface AzhagiScreenScope {
    var title: String

    var navigationIconVisible: Boolean

    var previewFieldVisible: Boolean

    var scrollable: Boolean

    var iconSpaceReserved: Boolean

    fun actions(actions: AzhagiScreenActions)

    fun bottomBar(bottomBar: AzhagiScreenBottomBar)

    fun content(content: AzhagiScreenContent)

    fun floatingActionButton(fab: AzhagiScreenFab)

    fun navigationIcon(navigationIcon: AzhagiScreenNavigationIcon)
}

private class AzhagiScreenScopeImpl : AzhagiScreenScope {
    override var title: String by mutableStateOf("")
    override var navigationIconVisible: Boolean by mutableStateOf(true)
    override var previewFieldVisible: Boolean by mutableStateOf(false)
    override var scrollable: Boolean by mutableStateOf(true)
    override var iconSpaceReserved: Boolean by mutableStateOf(true)

    private var actions: AzhagiScreenActions = @Composable { }
    private var bottomBar: AzhagiScreenBottomBar = @Composable { }
    private var content: AzhagiScreenContent = @Composable { }
    private var fab: AzhagiScreenFab = @Composable { }
    private var navigationIcon: AzhagiScreenNavigationIcon = @Composable {
        val navController = LocalNavController.current
        AzhagiIconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.autoMirrorForRtl(),
            icon = Icons.AutoMirrored.Filled.ArrowBack,
        )
    }

    override fun actions(actions: AzhagiScreenActions) {
        this.actions = actions
    }

    override fun bottomBar(bottomBar: AzhagiScreenBottomBar) {
        this.bottomBar = bottomBar
    }

    override fun content(content: AzhagiScreenContent) {
        this.content = content
    }

    override fun floatingActionButton(fab: AzhagiScreenFab) {
        this.fab = fab
    }

    override fun navigationIcon(navigationIcon: AzhagiScreenNavigationIcon) {
        this.navigationIcon = navigationIcon
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Render() {
        val context = LocalContext.current
        val previewFieldController = LocalPreviewFieldController.current
        val colorScheme = MaterialTheme.colorScheme

        SideEffect {
            val window = (context as Activity).window
            previewFieldController?.isVisible = previewFieldVisible
            window.statusBarColor = Color.Transparent.toArgb()
            if (AndroidVersion.ATLEAST_API29_Q) {
                window.navigationBarColor = Color.Transparent.toArgb()
                window.isNavigationBarContrastEnforced = true
            } else {
                window.navigationBarColor = colorScheme.scrim.toArgb()
            }
        }

        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = { AzhagiAppBar(title, navigationIcon.takeIf { navigationIconVisible }, actions, scrollBehavior) },
            bottomBar = bottomBar,
            floatingActionButton = fab,
        ) { innerPadding ->
            val scrollModifier = if (scrollable) {
                Modifier.AzhagiVerticalScroll()
            } else {
                Modifier
            }
            PreferenceLayout(
                AzhagiPreferenceModel(),
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth()
                    .then(scrollModifier),
                iconSpaceReserved = iconSpaceReserved,
                content = content,
            )
        }
    }
}

