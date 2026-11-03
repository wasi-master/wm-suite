package com.wasimaster.wmsuite.settings

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The lookups rows and headings draw from, filled once when the app starts.
 *
 * They are fixed tables in every app that uses this kit, read by nearly every
 * row, so a plain object beats a composition local: nothing subscribes, and a
 * row's default parameter stays a lookup. Unset, a row simply has no icon.
 */
public object SettingsTables {
    /** A destination's accent, or null for the theme's primary. */
    public var routeAccent: (String) -> Color? = { null }

    /** A destination's glyph, shared by its heading, its row and its crumb. */
    public var routeIcon: (String) -> ImageVector? = { null }

    /** A row's glyph, by its title resource. */
    public var rowIcon: (Int) -> ImageVector? = { null }

    /** The glyph for one option of a one-of-N setting. */
    public var choiceIcon: (Any?) -> ImageVector? = { null }

    /** Whether a row is one the last update brought, to wear the new-setting border. */
    public var isNewRow: (title: String?, key: Int) -> Boolean = { _, _ -> false }

    /** [routeIcon], for the kit's readers. */
    public operator fun get(@StringRes title: Int): ImageVector? = rowIcon(title)
}

/**
 * What an app adds around every screen, beyond the screen's own content: small
 * buttons above its add button, a bar along the bottom, an action in the bar.
 */
@Stable
public interface ScreenExtras {
    /** Small buttons to stack above the screen's own, top first. */
    @Composable
    public fun fabs(route: String?, hasDock: Boolean, hasOwnFab: Boolean): List<@Composable () -> Unit>

    /** A bar along the bottom of the window, or null to leave the screen's dock in place. */
    @Composable
    public fun bottomBar(route: String?, hasDock: Boolean): (@Composable () -> Unit)?

    /** An action ahead of the screen's own in the top bar. */
    @Composable
    public fun barAction(title: String) {}
}

/** See [ScreenExtras]. */
public val LocalScreenExtras = compositionLocalOf<ScreenExtras?> { null }

/**
 * Draws a crumb's glyph where the app has its own for [route] (an icon pack's,
 * say), returning true; false falls back to [SettingsTables.routeIcon].
 */
public val LocalCrumbGlyph = compositionLocalOf<(@Composable (route: String, tint: Color, modifier: Modifier) -> Boolean)?> { null }

/**
 * Opens a row's explanation the app's own way (a guide with more to say than
 * one paragraph, say), given the row's title and its `info` text. Null leaves
 * the "?" and the hold menu's "What is this?" on the kit's [InfoDialog].
 */
public val LocalInfoOpener = compositionLocalOf<((title: String, detail: String) -> Unit)?> { null }

/**
 * The glyph every [InfoButton] draws unless the call names its own, so an app
 * whose icons follow a style setting can have the "?" follow it too. Null is
 * the kit's outlined question mark.
 */
public val LocalInfoIcon = compositionLocalOf<ImageVector?> { null }

/**
 * False puts away every "?" on rows and headings, for someone who has asked
 * for none. A row's long press then offers "What is this?" instead, as it
 * does at the Simple depth, so the explanation is still one gesture away.
 */
public val LocalInfoButtons = compositionLocalOf { true }
