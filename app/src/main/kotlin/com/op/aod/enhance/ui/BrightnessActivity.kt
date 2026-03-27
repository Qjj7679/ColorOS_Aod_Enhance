package com.op.aod.enhance.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.op.aod.enhance.data.AodConfigStore
import com.op.aod.enhance.data.AodUiConfig
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

class BrightnessActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiuixTheme {
                BrightnessScreen(
                    initial = AodConfigStore.read(contentResolver),
                    onSave = { cfg -> AodConfigStore.write(contentResolver, cfg) },
                    context = this
                )
            }
        }
    }

}

@OptIn(FlowPreview::class)
@Composable
private fun BrightnessScreen(
    initial: AodUiConfig,
    onSave: (AodUiConfig) -> Unit,
    context: android.content.Context
) {
    var initDark by remember { mutableFloatStateOf(initial.initDark.toFloat()) }
    var initBright by remember { mutableFloatStateOf(initial.initBright.toFloat()) }
    var runningMultiplier by remember { mutableFloatStateOf(initial.runningMultiplier) }
    val currentOnSave by rememberUpdatedState(onSave)

    LaunchedEffect(Unit) {
        snapshotFlow { Triple(initDark, initBright, runningMultiplier) }
            .debounce(300)
            .distinctUntilChanged()
            .collect { (dark, bright, multi) ->
                val base = AodConfigStore.read(context.contentResolver)
                currentOnSave(
                    base.copy(
                        initDark = dark.toInt(),
                        initBright = bright.toInt(),
                        runningMultiplier = multi,
                    )
                )
            }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "AOD亮度设置"
            )
        }
    ) { paddingValues: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("熄屏前暗光环境AOD亮度：${initDark.toInt()}")
            Slider(
                value = initDark,
                onValueChange = { initDark = it.toInt().coerceIn(0, 255).toFloat() },
                valueRange = 0f..255f,
                steps = 254,
                modifier = Modifier.fillMaxWidth()
            )

            Text("熄屏前亮光环境AOD亮度：${initBright.toInt()}")
            Slider(
                value = initBright,
                onValueChange = { initBright = it.toInt().coerceIn(0, 255).toFloat() },
                valueRange = 0f..255f,
                steps = 254,
                modifier = Modifier.fillMaxWidth()
            )

            Text("熄屏时AOD自动亮度倍率：$runningMultiplier")
            Slider(
                value = runningMultiplier,
                onValueChange = { runningMultiplier = ((it * 10).toInt().coerceIn(10, 20) / 10f) },
                valueRange = 1.0f..2.0f,
                steps = 9,
                modifier = Modifier.fillMaxWidth()
            )

            TextField(
                value = initDark.toInt().toString(),
                onValueChange = { it.toIntOrNull()?.let { v -> initDark = v.coerceIn(0, 255).toFloat() } },
                label = "输入熄屏前暗光环境AOD亮度",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            TextField(
                value = initBright.toInt().toString(),
                onValueChange = { it.toIntOrNull()?.let { v -> initBright = v.coerceIn(0, 255).toFloat() } },
                label = "输入熄屏前亮光环境AOD亮度",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            TextField(
                value = runningMultiplier.toString(),
                onValueChange = { it.toFloatOrNull()?.let { v -> runningMultiplier = v.coerceIn(1.0f, 2.0f) } },
                label = "输入熄屏时AOD自动亮度倍率",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

        }
    }
}
