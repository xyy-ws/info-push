package com.infopush.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.infopush.app.ui.navigation.AppNav

@Composable
fun InfoPushApp() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AppNav()
        }
    }
}
