package com.atritripathi.stallwart.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.atritripathi.stallwart.sample.ui.theme.StallwartSampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StallwartSampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    StallwartDemo(
                        modifier = Modifier.padding(innerPadding),
                        onTriggerJank = { simulateJank() },
                        onTriggerAnr = { simulateAnr() }
                    )
                }
            }
        }
    }

    /**
     * Simulates a jank (short freeze) by blocking the Main Thread for 700ms.
     */
    private fun simulateJank() {
        Thread.sleep(700)
    }

    /**
     * Simulates an ANR by blocking the Main Thread for 6 seconds.
     * WARNING: This will make the app unresponsive!
     */
    private fun simulateAnr() {
        Thread.sleep(6000)
    }
}

@Composable
fun StallwartDemo(
    modifier: Modifier = Modifier,
    onTriggerJank: () -> Unit = {},
    onTriggerAnr: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Stallwart Demo",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Test freeze detection by triggering simulated freezes. Watch Logcat for Stallwart events.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onTriggerJank,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text("Trigger Jank (700ms)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onTriggerAnr,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD32F2F)
            )
        ) {
            Text("Trigger ANR (6s) ⚠️")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Filter Logcat by 'Stallwart' to see detection events",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StallwartDemoPreview() {
    StallwartSampleTheme {
        StallwartDemo()
    }
}
