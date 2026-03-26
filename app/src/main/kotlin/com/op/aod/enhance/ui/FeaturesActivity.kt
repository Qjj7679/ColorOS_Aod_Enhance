package com.op.aod.enhance.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.op.aod.enhance.data.AodConfigStore
import com.op.aod.enhance.data.AodUiConfig
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

class FeaturesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiuixTheme {
                FeaturesScreen(
                    initial = AodConfigStore.read(contentResolver),
                    onSave = { cfg -> AodConfigStore.write(contentResolver, cfg) }
                )
            }
        }
    }

}

@OptIn(FlowPreview::class)
@Composable
private fun FeaturesScreen(initial: AodUiConfig, onSave: (AodUiConfig) -> Unit) {
    var enablePanoramic by remember { mutableStateOf(initial.enablePanoramic) }
    var enableSettingsSupport by remember { mutableStateOf(initial.enableSettingsSupport) }
    var blockSingleClick by remember { mutableStateOf(initial.blockSingleClick) }
    val currentOnSave by rememberUpdatedState(onSave)

    LaunchedEffect(Unit) {
        snapshotFlow { Triple(enablePanoramic, enableSettingsSupport, blockSingleClick) }
            .debounce(300)
            .distinctUntilChanged()
            .collect { (panoramic, settingsSupport, singleClick) ->
                currentOnSave(
                    AodUiConfig(
                        enablePanoramic = panoramic,
                        enableSettingsSupport = settingsSupport,
                        blockSingleClick = singleClick,
                    )
                )
            }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "AOD功能设置",
                color = MiuixTheme.colorScheme.secondaryContainer,
            )
        },
        containerColor = MiuixTheme.colorScheme.secondaryContainer,
    ) { paddingValues: PaddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .scrollEndHaptic()
                .overScrollVertical()
                .padding(horizontal = 12.dp),
            contentPadding = paddingValues,
            overscrollEffect = null,
        ) {
            item {
                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.background,
                    ),
                ) {
                    SuperSwitch(
                        title = "系统界面-全天全景AOD支持",
                        summary = "让系统界面解锁全天全景 AOD 相关能力",
                        checked = enablePanoramic,
                        onCheckedChange = { enablePanoramic = it },
                    )
                    SuperSwitch(
                        title = "息屏-全天全景AOD开关",
                        summary = "在息屏设置中显示全天全景 AOD 开关",
                        checked = enableSettingsSupport,
                        onCheckedChange = { enableSettingsSupport = it },
                    )
                    SuperSwitch(
                        title = "AOD单击唤醒屏蔽",
                        summary = "避免 AOD 单击误触导致唤醒",
                        checked = blockSingleClick,
                        onCheckedChange = { blockSingleClick = it },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.padding(bottom = 16.dp))
            }
        }
    }
}
