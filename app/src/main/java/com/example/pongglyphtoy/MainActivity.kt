package com.example.pongglyphtoy

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pongglyphtoy.ui.theme.PongGlyphToyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PongGlyphToyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SetupScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun SetupScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var managerUnavailable by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.setup_title))
        Text(
            text = stringResource(R.string.setup_description),
            modifier = Modifier
                .widthIn(max = 480.dp)
                .padding(vertical = 24.dp)
        )
        Button(
            onClick = {
                val intent = Intent().setComponent(
                    ComponentName(
                        GLYPH_TOYS_PACKAGE,
                        GLYPH_TOYS_MANAGER_ACTIVITY
                    )
                )
                if (intent.resolveActivity(context.packageManager) != null) {
                    managerUnavailable = false
                    context.startActivity(intent)
                } else {
                    managerUnavailable = true
                }
            }
        ) {
            Text(text = stringResource(R.string.activate_toy))
        }
        if (managerUnavailable) {
            Text(
                text = stringResource(R.string.manager_unavailable),
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SetupScreenPreview() {
    PongGlyphToyTheme {
        SetupScreen()
    }
}

private const val GLYPH_TOYS_PACKAGE = "com.nothing.thirdparty"
private const val GLYPH_TOYS_MANAGER_ACTIVITY =
    "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity"