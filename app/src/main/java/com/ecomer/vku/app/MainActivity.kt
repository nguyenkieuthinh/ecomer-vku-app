package com.ecomer.vku.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ecomer.vku.app.data.FirebaseExt
import com.ecomer.vku.app.ui.nav.EcomerNavHost
import com.ecomer.vku.app.ui.nav.Screen
import com.ecomer.vku.app.ui.theme.EcomerTheme

/**
 * Activity chinh cua app ECOMER.
 *
 * Firebase giu phien dang nhap, nen mo app lai da vao thang man hinh chinh
 * neu nguoi dung da dang nhap truoc do.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startDestination = if (FirebaseExt.auth?.currentUser != null) {
            Screen.HOME
        } else {
            Screen.SIGN_IN
        }

        setContent {
            EcomerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EcomerNavHost(startDestination = startDestination)
                }
            }
        }
    }
}
