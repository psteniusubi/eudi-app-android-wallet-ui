/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package eu.europa.ec.uilogic.extension

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import eu.europa.ec.uilogic.container.EudiComponentActivity

fun Context.openDeepLink(deepLink: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = deepLink
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
}

fun Context.getPendingDeepLink(): Uri? {
    return (this as? EudiComponentActivity)?.pendingDeepLink?.let { deepLink ->
        clearPendingDeepLink()
        deepLink
    }
}

fun Context.finish() {
    (this as? EudiComponentActivity)?.finish()
}

fun Context.findActivity(): ComponentActivity {
    var context = this
    while (context is ContextWrapper) {
        if (context is  ComponentActivity) return context
        context = context.baseContext
    }
    throw IllegalStateException("No Activity found.")
}

private fun Context.clearPendingDeepLink() {
    (this as? EudiComponentActivity)?.pendingDeepLink = null
}

/**
 * Parses a string url and sends the Action View Intent.
 *
 * @param url the url to parse.
 */
fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
    }
}
