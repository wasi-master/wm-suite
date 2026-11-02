package com.wasimaster.wmsuite.settings

import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.annotation.StringRes
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.wasimaster.wmkeyboard.core.icons.symbols.automirrored.outlined.ArrowForward
import com.wasimaster.wmkeyboard.core.icons.symbols.automirrored.outlined.HelpOutline
import com.wasimaster.wmkeyboard.core.icons.symbols.filled.Star
import com.wasimaster.wmkeyboard.core.icons.symbols.outlined.Link
import com.wasimaster.wmkeyboard.core.icons.symbols.outlined.StarBorder
import com.wasimaster.wmkeyboard.core.ui.DisabledReason
import java.net.URLEncoder

/**
 * Where a row's link points: the screen, when it has one a link can name, and
 * the row's resource name.
 */
@Immutable
public data class SettingLinkTarget(val route: String?, val name: String)

/** How rows offer their link: not at all, on a long press, or with a small button. */
public enum class SettingLinkMode { OFF, HOLD, BUTTON }

/**
 * An app's answer to "what is this row's link". The kit draws the button and
 * the long press; the scheme is the app's. No source means rows have no links.
 */
@Stable
public interface SettingLinkSource {
    public val mode: SettingLinkMode

    /** The link target of a row on [route], or null for one nothing can name. */
    public fun target(route: String?, title: String?, @StringRes key: Int): SettingLinkTarget?

    /** The screen the row [name] lives on, or null when that is not known. */
    public fun routeOf(name: String): String?

    /** The link that opens the way out of [reason], or null when it names none. */
    public fun reasonLink(reason: DisabledReason): String?

    /** Copies [target]'s link, under the settings [path] that leads to it. */
    public fun copy(context: Context, target: SettingLinkTarget, path: List<String>)
}

/** See [SettingLinkSource]. Null where the app offers no links. */
public val LocalSettingLinks = compositionLocalOf<SettingLinkSource?> { null }

/** Opens a link from [SettingLinkSource.reasonLink]. */
public val LocalOpenSettingLink = compositionLocalOf<((String) -> Unit)?> { null }

/** Favourites as a row's long press sees them. Null where the app has none. */
@Stable
public interface RowFavourites {
    public fun contains(target: SettingLinkTarget): Boolean

    /** Adds or removes [target]; [path] is the way there, the row's own name last. */
    public fun toggle(target: SettingLinkTarget, path: List<String>)
}

/** See [RowFavourites]. */
public val LocalRowFavourites = compositionLocalOf<RowFavourites?> { null }

/**
 * The route a back stack entry is showing, with its arguments filled in:
 * `tool/CLIPBOARD` for `tool/{tool}`. Null when an argument is missing, since
 * a route with a hole in it names no screen.
 */
public fun concreteRoute(pattern: String?, argument: (String) -> String?): String? {
    if (pattern.isNullOrEmpty()) return null
    var missing = false
    val route = RouteArgument.replace(pattern) { match ->
        val value = argument(match.groupValues[1])
        if (value == null) {
            missing = true
            ""
        } else {
            URLEncoder.encode(value, "UTF-8").replace("+", "%20")
        }
    }
    return route.takeUnless { missing }
}

private val RouteArgument = Regex("\\{([^}]+)\\}")

/**
 * The settings path down to one row, as the breadcrumb strip names it: every
 * screen on [trail] up to and including [entryId]'s, then [row] unless the
 * screen is already called that.
 */
public fun settingPath(trail: List<SettingsCrumb>, entryId: String?, row: String?): List<String> {
    val at = trail.indexOfFirst { it.entryId == entryId }
    val screens = (if (at < 0) trail else trail.subList(0, at + 1)).map { it.title }.filter { it.isNotBlank() }
    return if (row.isNullOrBlank() || screens.lastOrNull() == row) screens else screens + row
}

/**
 * The row a link button or a long press below this point belongs to, as
 * [HighlightableRow] knows it: the drawn title and its resource. Null where
 * no row has claimed the space.
 */
@Immutable
public data class RowLinkKey(val title: String?, @StringRes val key: Int)

public val LocalRowLinkKey = compositionLocalOf<RowLinkKey?> { null }

/** The route the screen being drawn is showing, arguments filled in. */
@Composable
public fun currentLinkRoute(): String? {
    val entry = currentCrumbEntry() ?: return null
    return remember(entry) {
        val args: Bundle? = entry.arguments
        concreteRoute(entry.destination.route) { name ->
            @Suppress("DEPRECATION")
            args?.get(name)?.toString()
        }
    }
}

/**
 * The link target of the row around the caller, or null when it has none or
 * links are off. Worked out while composing, so only for the link button:
 * a row without its own resource builds the whole search index to answer.
 */
@Composable
public fun currentRowLink(): SettingLinkTarget? {
    val links = LocalSettingLinks.current?.takeIf { it.mode != SettingLinkMode.OFF } ?: return null
    val row = LocalRowLinkKey.current ?: return null
    val route = currentLinkRoute()
    return remember(links, row, route) { links.target(route, row.title, row.key) }
}

/**
 * The settings path to the row around the caller, read when it is asked for
 * rather than while composing: the trail is still settling on a screen's first
 * frames, and a copy is only ever made long after them.
 */
@Composable
public fun rememberSettingPath(): () -> List<String> {
    val trail = LocalSettingsCrumbTrail.current
    val entryId = currentCrumbEntry()?.id
    val row = LocalRowLinkKey.current
    val resources = LocalContext.current.resources
    return remember(trail, entryId, row, resources) {
        {
            val title = row?.title ?: row?.key?.takeIf { it != 0 }?.let { runCatching { resources.getString(it) }.getOrNull() }
            settingPath(trail?.path.orEmpty(), entryId, title)
        }
    }
}

/** The link button's tap target: the reset control's size, for the same reason. */
public val SettingLinkTargetSize = RowExtraSize

/**
 * The small link button a row shows beside its reset and its "?" while
 * **Setting links** is on Button. Draws nothing in any other mode, or on a row
 * that has no link, or inside a [RowExtras] at the Simple depth, where a row
 * is its name and its control and the long press copies the link instead
 * (see [holdCopiesLink]). [name] is the row's own name, for the content
 * description.
 */
@Composable
public fun SettingLinkButton(name: String) {
    if (LocalSettingLinks.current?.mode != SettingLinkMode.BUTTON || LocalRowExtrasBare.current) return
    val target = currentRowLink() ?: return
    val context = LocalContext.current
    val path = rememberSettingPath()
    val links = LocalSettingLinks.current
    IconButton(
        onClick = { links?.copy(context, target, path()) },
        modifier = Modifier.size(SettingLinkTargetSize),
    ) {
        Icon(
            Icons.Outlined.Link,
            contentDescription = stringResource(R.string.about_setting_link_desc, name),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Whether the link button is drawn on headings, for the headings that budget their own width. */
@Composable
public fun settingLinkButtonShown(): Boolean = LocalSettingLinks.current?.mode == SettingLinkMode.BUTTON

/**
 * Whether the link button is drawn on rows, for the rows that budget their own
 * width: on Button, and not at the Simple depth, where [RowExtras] leaves it
 * out along with the "?".
 */
@Composable
public fun rowLinkButtonShown(): Boolean =
    settingLinkButtonShown() && LocalSettingsDepth.current.depth != SettingsDepth.SIMPLE

/**
 * Whether a long press copies a row's link: **Setting links** on Hold, or on
 * Button at the Simple depth, where the button has left the row and the hold
 * is what is left to reach the link by.
 */
@Composable
private fun holdCopiesLink(): Boolean {
    val mode = LocalSettingLinks.current?.mode ?: return false
    return mode == SettingLinkMode.HOLD ||
        (mode == SettingLinkMode.BUTTON && LocalSettingsDepth.current.depth == SettingsDepth.SIMPLE)
}

/**
 * A row's explanation, the one its "?" opens, carried by its hold at the Simple
 * depth where the "?" is not drawn (see [RowExtras]).
 */
@Immutable
public class RowInfo(val title: String, val detail: String)

/**
 * A row's long press, open as a small menu: the row it was for, and where
 * under the finger the menu opens. One per row that can be held, made by
 * [rememberRowHold]; [settingLinkHold] fills it and [RowHoldMenu] draws it.
 */
@Stable
public class RowHold {
    /** The row the menu is open for, or null while it is closed or the row has no link. */
    var held by mutableStateOf<SettingLinkTarget?>(null)

    /** Why the row is greyed out, while the menu is open on a disabled row. */
    var reason by mutableStateOf<DisabledReason?>(null)

    /** The row's explanation, while the menu is open on a row whose "?" is not drawn. */
    var info by mutableStateOf<RowInfo?>(null)

    /** The explanation "What is this?" was chosen for, while its dialog is showing. */
    var explain by mutableStateOf<RowInfo?>(null)

    /** Whether the menu is showing. */
    val open: Boolean get() = held != null || reason != null || info != null

    fun close() {
        held = null
        reason = null
        info = null
    }

    /** The menu's offset from the row's bottom left: where the finger was. */
    var at by mutableStateOf(DpOffset.Zero)
}

/**
 * The [RowHold] for the row around the caller, or null when a long press has
 * nothing to offer: no favourites (#898), no link to copy ([holdCopiesLink]),
 * no [disabledReason] for a greyed-out row to give, and no [info] whose "?"
 * has left the row.
 */
@Composable
public fun rememberRowHold(disabledReason: DisabledReason? = null, info: RowInfo? = null): RowHold? {
    val copies = holdCopiesLink()
    if (!copies && LocalRowFavourites.current == null && disabledReason == null && info == null) return null
    return remember { RowHold() }
}

/**
 * Opens [hold]'s menu on a long press: add the row to favourites, and copy its
 * link while **Setting links** is on Hold. With favourites off there is
 * nothing to choose between, and the hold copies the link straight away, as
 * it always has.
 *
 * The row's own click stays its own. The press is watched in the main pass,
 * after the row's controls have seen it, and given up the moment one of them
 * takes it — a slider starting to drag, a field selecting text — or the
 * finger moves or lifts. Only once the hold has fired does this step in front
 * of the row, taking the rest of the gesture in the initial pass so the lift
 * that ends a hold is not also read as a tap.
 *
 * Which row it is gets worked out when the hold fires, not while composing:
 * a row named only by its drawn title is looked up in the search index, and
 * building that for every row of every screen to answer a press that almost
 * never comes would be most of the cost of a screen.
 *
 * [disabledReason] is a greyed-out row's answer to "why can't I use this"
 * (see [DisabledReason]). With one, the hold always opens the menu, reason
 * first, even on a row nothing can link to, and the greyed-out controls
 * inside take no part: a disabled switch or button does not consume the
 * press, so the hold sees it through them.
 *
 * [info] is the row's explanation while its "?" is not drawn (the Simple
 * depth, see [RowExtras]); like a reason, it makes the hold open the menu,
 * with "What is this?" first.
 */
@Composable
public fun Modifier.settingLinkHold(
    hold: RowHold?,
    disabledReason: DisabledReason? = null,
    info: RowInfo? = null,
): Modifier {
    if (hold == null) return this
    val links = LocalSettingLinks.current
    val row = LocalRowLinkKey.current
    if ((links == null || row == null) && disabledReason == null && info == null) return this
    val route = currentLinkRoute()
    val menu = LocalRowFavourites.current != null
    val context = LocalContext.current
    val view = LocalView.current
    val config = LocalViewConfiguration.current
    val path = rememberSettingPath()
    return pointerInput(hold, links, row, route, menu, path, disabledReason, info) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            // The scope's own timeout: the gesture scope restricts suspension,
            // so the coroutines one cannot be called from in here.
            val held = withTimeoutOrNull(config.longPressTimeoutMillis) {
                var givenUp = false
                while (!givenUp) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    givenUp = change == null || !change.pressed || change.isConsumed ||
                        (change.position - down.position).getDistance() > config.touchSlop
                }
                false
            } == null
            if (!held) return@awaitEachGesture
            // A language, a layout, anything the user made: nothing to name.
            val target = if (links != null && row != null) links.target(route, row.title, row.key) else null
            if (target == null && disabledReason == null && info == null) return@awaitEachGesture
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            if (menu || disabledReason != null || info != null) {
                hold.at = DpOffset(down.position.x.toDp(), (down.position.y - size.height).toDp())
                hold.reason = disabledReason
                hold.info = info
                hold.held = target
            } else if (target != null) {
                links?.copy(context, target, path())
            }
            // The rest of this gesture is the hold's, not the row's.
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.forEach { it.consume() }
                if (event.changes.none { it.pressed }) break
            }
        }
    }
}

/**
 * [hold]'s menu, drawn by the row that owns it once a long press has opened
 * it, and the explanation dialog after "What is this?" was chosen from it.
 * Composes nothing the rest of the time.
 */
@Composable
public fun RowHoldMenu(hold: RowHold?) {
    if (hold == null) return
    val explain = hold.explain
    if (explain != null) {
        // The same dialog the "?" opens where it is drawn.
        InfoDialog(explain.title, explain.detail) { hold.explain = null }
    }
    if (!hold.open) return
    val target = hold.held
    val reason = hold.reason
    val info = hold.info
    val favourites = LocalRowFavourites.current
    val copies = holdCopiesLink()
    val context = LocalContext.current
    val path = rememberSettingPath()
    val openLink = LocalOpenSettingLink.current
    val links = LocalSettingLinks.current
    val here = currentLinkRoute()
    val close = { hold.close() }
    DropdownMenu(expanded = true, onDismissRequest = close, offset = hold.at) {
        if (info != null) {
            // First, above a reason: what the row is comes before why it is off.
            DropdownMenuItem(
                text = { Text(stringResource(R.string.home_hold_what_is_this)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.HelpOutline, null) },
                onClick = {
                    close()
                    hold.explain = info
                },
            )
            if (reason != null || (target != null && (favourites != null || copies))) HorizontalDivider()
        }
        if (reason != null) {
            DisabledReasonMenuItems(reason, divider = target != null && (favourites != null || copies)) { link ->
                close()
                // A switch on this same screen is scrolled to and pulsed
                // rather than opened again on top of it, which would leave
                // the user a Back away from where they were.
                val name = reason.setting.orEmpty()
                val onScreen = name.isNotEmpty() && here != null &&
                    (reason.route ?: links?.routeOf(name)) == here
                @Suppress("DiscouragedApi")
                val row = if (onScreen) context.resources.getIdentifier(name, "string", context.packageName) else 0
                if (row != 0) SettingsHighlight.request(row) else openLink?.invoke(link)
            }
        }
        if (target == null) return@DropdownMenu
        if (favourites != null) {
            val pinned = favourites.contains(target)
            DropdownMenuItem(
                text = { Text(stringResource(if (pinned) R.string.favourites_remove else R.string.favourites_add)) },
                leadingIcon = { Icon(if (pinned) Icons.Filled.Star else Icons.Outlined.StarBorder, null) },
                onClick = {
                    close()
                    favourites.toggle(target, path())
                },
            )
        }
        if (copies) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.favourites_copy_link)) },
                leadingIcon = { Icon(Icons.Outlined.Link, null) },
                onClick = {
                    close()
                    links?.copy(context, target, path())
                },
            )
        }
    }
}

/**
 * The top of a greyed-out row's long-press menu: why it is greyed out, and the
 * way to the setting that lifts it when [reason] names one. [divider] sets it
 * apart from the favourite and link items under it.
 */
@Composable
private fun DisabledReasonMenuItems(reason: DisabledReason, divider: Boolean, onOpen: (String) -> Unit) {
    Text(
        reason.text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .widthIn(max = DisabledReasonMaxWidth)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
    val links = LocalSettingLinks.current
    val link = remember(reason, links) { links?.reasonLink(reason) }
    if (link != null) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.common_disabled_open_setting)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.ArrowForward, null) },
            onClick = { onOpen(link) },
        )
    }
    if (divider) HorizontalDivider()
}

/** How wide a reason may run before it wraps: a menu, not a dialog. */
private val DisabledReasonMaxWidth = 280.dp

/**
 * A [DisabledReason] for a settings row, its way out named by the title
 * resource of the switch that lifts it: [setting], on [route] when the caller
 * knows the screen, or wherever the settings index lists that row when not.
 * Pass no [setting] for a block nothing in settings can lift.
 */
@Composable
public fun rememberDisabledReason(text: String, @StringRes setting: Int = 0, route: String? = null): DisabledReason {
    val resources = LocalContext.current.resources
    return remember(text, setting, route, resources) {
        DisabledReason(
            text,
            route = route,
            setting = setting.takeIf { it != 0 }?.let { runCatching { resources.getResourceEntryName(it) }.getOrNull() },
        )
    }
}
