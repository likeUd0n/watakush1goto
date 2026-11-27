package app.shinsatsunote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.shinsatsunote.ui.nav.AppNav
import app.shinsatsunote.ui.theme.ShinsatsuNoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShinsatsuNoteTheme {
                AppNav()
            }
        }
    }
}
