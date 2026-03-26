package com.op.aod.enhance.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiuixTheme {
                MainScreen(
                    onOpenBrightness = { startActivity(Intent(this, BrightnessActivity::class.java)) },
                    onOpenFeatures = { startActivity(Intent(this, FeaturesActivity::class.java)) }
                )
            }
        }
    }
}

@Composable
private fun MainScreen(
    onOpenBrightness: () -> Unit,
    onOpenFeatures: () -> Unit,
) {
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "ColorOS AOD 增强",
                color = MiuixTheme.colorScheme.secondaryContainer,
            )
        },
        containerColor = MiuixTheme.colorScheme.secondaryContainer,
    ) { paddingValues: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.background,
                )
            ) {
                SuperArrow(
                    title = "AOD亮度设置",
                    summary = "调整初始亮度与运行时倍率",
                    onClick = onOpenBrightness,
                )
                SuperArrow(
                    title = "AOD功能设置",
                    summary = "系统界面、息屏设置与唤醒行为",
                    onClick = onOpenFeatures,
                )
            }
        }
    }
}
