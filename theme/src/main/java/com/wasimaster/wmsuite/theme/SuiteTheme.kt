package com.wasimaster.wmsuite.theme

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/** Whether this device is a television, where the D-pad is the only pointer. */
public fun Context.isTelevision(): Boolean {
    val uiMode = (getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)?.currentModeType
    if (uiMode == Configuration.UI_MODE_TYPE_TELEVISION) return true
    @Suppress("DEPRECATION")
    return packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
        packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
}

/** How an app in the suite picks its light or dark look. */
public enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

/**
 * The suite's Material theme. Each app passes its own [lightColors] and [darkColors], and
 * shares the type scale, shapes and the television focus treatment.
 */
@Composable
public fun SuiteTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    lightColors: ColorScheme = WmLightColors,
    darkColors: ColorScheme = WmDarkColors,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val supportsDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    // Remembered, and not only to save the forty resource reads a dynamic
    // scheme costs. MaterialTheme hands the scheme down through a *static*
    // composition local, and ColorScheme compares by instance, so a freshly
    // built one, identical to the last, still counted as a change and
    // recomposed every screen under this theme.
    val configuration = LocalConfiguration.current
    val scheme = remember(dark, supportsDynamic, themeMode, context, configuration, lightColors, darkColors) {
        val base = when {
            supportsDynamic && dark -> dynamicDarkColorScheme(context)
            supportsDynamic -> dynamicLightColorScheme(context)
            dark -> darkColors
            else -> lightColors
        }
        if (themeMode == ThemeMode.AMOLED) base.amoled() else base
    }
    // On a television the D-pad is the only pointer there is, so the app says
    // where it is pointing. Nothing is provided anywhere else, so a phone keeps
    // the ripple and pays one boolean.
    val television = remember(context) { context.isTelevision() }
    val indication = remember(television, scheme.primary) {
        if (television) arrayOf(LocalIndication provides TvFocusIndication(scheme.primary))
        else emptyArray()
    }
    MaterialTheme(colorScheme = scheme, typography = WmTypography, shapes = WmShapes) {
        CompositionLocalProvider(*indication, content = content)
    }
}
