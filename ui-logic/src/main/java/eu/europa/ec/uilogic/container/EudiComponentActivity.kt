/*
 *
 *  * Copyright (c) 2023 European Commission
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package eu.europa.ec.uilogic.container

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import eu.europa.ec.resourceslogic.theme.ThemeManager
import eu.europa.ec.uilogic.navigation.RouterHost
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.KoinAndroidContext
import org.koin.core.annotation.KoinExperimentalAPI

open class EudiComponentActivity : ComponentActivity() {

    private val routerHost: RouterHost by inject()

    private var flowStarted: Boolean = false

    internal var pendingDeepLink: Uri? = null

    private fun cacheDeepLink(intent: Intent?) {
        pendingDeepLink = intent?.data
    }

    @OptIn(KoinExperimentalAPI::class)
    @Composable
    protected fun Content(
        builder: NavGraphBuilder.(NavController) -> Unit
    ) {
        ThemeManager.instance.Theme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                KoinAndroidContext {
                    routerHost.StartFlow {
                        builder(it)
                    }
                    flowStarted = true
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (flowStarted) {
            handleDeepLink(intent)
        } else {
            runPendingDeepLink(intent)
        }
    }

    private fun runPendingDeepLink(intent: Intent?) {
        lifecycleScope.launch {
            var count = 0
            while (!flowStarted && count <= 10) {
                count++
                delay(500)
            }
            if (count <= 10) {
                handleDeepLink(intent)
            }
        }
    }

    private fun handleDeepLink(intent: Intent?) {
        cacheDeepLink(intent)
        if (routerHost.currentFlowIsAfterOnBoarding()) {
            routerHost.popToLandingScreen()
        }
    }
}