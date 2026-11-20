package com.licount

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.licount.ui.LiCountApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 不能用无参 enableEdgeToEdge()（= SystemBarStyle.auto）：它会把
        // isNavigationBarContrastEnforced 置为 true，系统在导航栏上加一层半透明灰，
        // 底部就会出现一条割裂的横条、不沉浸。FreeleeWatch 也踩过同一个坑。
        // dark(TRANSPARENT) 走的是不强制对比度的分支；图标明暗由 App.kt 的
        // WindowInsetsController.isAppearanceLightNavigationBars 单独控制。
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            LiCountApp()
        }
    }
}
