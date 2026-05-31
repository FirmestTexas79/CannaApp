package cz.cannaclub.cannaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import cz.cannaclub.cannaapp.navigation.CannaNavGraph
import cz.cannaclub.cannaapp.ui.theme.Background
import cz.cannaclub.cannaapp.ui.theme.CannaAppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CannaAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = Background
                ) {
                    CannaNavGraph()
                }
            }
        }
    }
}