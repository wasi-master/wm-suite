package com.wasimaster.wmsuite.settings

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wasimaster.wmkeyboard.core.icons.symbols.automirrored.outlined.HelpOutline
import com.wasimaster.wmkeyboard.core.icons.symbols.automirrored.outlined.KeyboardArrowRight
import com.wasimaster.wmkeyboard.core.icons.symbols.outlined.Add
import com.wasimaster.wmkeyboard.core.icons.symbols.outlined.ArrowDropDown
import com.wasimaster.wmkeyboard.core.icons.symbols.outlined.Check
import com.wasimaster.wmkeyboard.core.icons.symbols.outlined.Info
import com.wasimaster.wmkeyboard.core.icons.symbols.outlined.KeyboardArrowDown
import com.wasimaster.wmkeyboard.core.icons.symbols.outlined.Remove
import com.wasimaster.wmkeyboard.core.icons.symbols.outlined.Warning
import com.wasimaster.wmkeyboard.core.ui.DisabledReason
import com.wasimaster.wmkeyboard.core.ui.ScrollRail
import com.wasimaster.wmkeyboard.core.ui.WmSlider
import com.wasimaster.wmkeyboard.core.ui.rememberLiveSlider
import com.wasimaster.wmkeyboard.core.ui.rememberScrollRailState
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import com.wasimaster.wmsuite.theme.*

/**
 * Collects the rows of one visually grouped card stack — the modern
 * Material settings look: each row sits on its own surface with tiny
 * gaps between rows, large rounded corners at the group's ends and
 * small ones inside.
 */
public class SettingsGroupScope {
    /**
     * One row of the group.
     *
     * [conditional] is what separates a row that is simply there from one the
     * caller hands a [visible] to. Only the second kind is wrapped in a
     * reveal, so the ordinary group — which is nearly all of them — pays
     * nothing for the machinery.
     */
    class Entry(
        val visible: Boolean,
        val conditional: Boolean,
        /** Who the row is, for a row in a list the user edits; see [item]. */
        val key: Any?,
        /** How far down the settings depth the row sits; see [SettingLevel]. */
        val level: SettingLevel = SettingLevel.BASIC,
        val content: @Composable () -> Unit,
    )

    val items = mutableListOf<Entry>()

    /**
     * A row that is always part of the group.
     *
     * Give a row of a list the user adds to and deletes from — a word, a
     * snippet, a pack — a [key] that names what it shows (its id, its
     * spelling). The row then keeps its own state when the rows above it
     * change, opens into its place when it is added and closes out of it when
     * it goes, instead of the rows under it jumping. See [GroupRows]. A keyed
     * row is still composed for the moment it takes to close, so its body
     * must not depend on the item still being in the list.
     *
     * [level] is how far down the settings depth the row sits. At a depth
     * above it the row is not drawn in its place but behind the group's
     * "more settings" row; see [SettingsDepth.kt]. Untagged rows are basic.
     */
    fun item(key: Any? = null, level: SettingLevel = SettingLevel.BASIC, content: @Composable () -> Unit) {
        items += Entry(visible = true, conditional = false, key = key, level = level, content = content)
    }

    /**
     * A row that belongs to the group but is only showing while [visible].
     *
     * Prefer this over wrapping the `item` call in an `if`. Both draw the same
     * thing, but a row that is never added simply is not there, so the row
     * a switch reveals appears and disappears in a single frame and the rows
     * under it jump. Declared this way it expands into place and pushes them
     * down instead, and the card corners either side of it follow.
     *
     * The body is not composed while the row is hidden, exactly as it is not
     * inside an `if`. It *is* composed while the row is on its way out, so a
     * body that would crash once [visible] has gone false — one that reads
     * through a null check the condition is doing, say — wants the `if` after
     * all.
     */
    fun item(
        visible: Boolean,
        key: Any? = null,
        level: SettingLevel = SettingLevel.BASIC,
        content: @Composable () -> Unit,
    ) {
        items += Entry(visible = visible, conditional = true, key = key, level = level, content = content)
    }
}

// Only for reading the shared-transition local as the reduced-motion switch.
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
public fun SettingsGroup(
    title: String? = null,
    @StringRes highlightKey: Int = 0,
    /**
     * Makes the group a fold: closed by default, opened by a press on its
     * heading, and remembered per screen so a power user who opened it once
     * finds it open next time. The key is joined to the screen's route, so
     * "advanced" on Typing and "advanced" on Key press are two folds.
     *
     * A search result or an addon's Use button that lands inside a closed
     * fold would flash nothing, so while a highlight is pending the fold is
     * drawn open regardless of what is remembered.
     */
    foldKey: String? = null,
    /** The section's explanation, behind the heading's "?" — see [SectionHeader]. */
    info: String? = null,
    /**
     * What a closed fold holds, named rather than counted: the count alone
     * says a group has thirteen things in it and nothing about whether the
     * one being looked for is among them. Read only while the fold is closed,
     * so a group that is open pays nothing for it — which matters where the
     * names cost a resource lookup each.
     *
     * Ignored without [foldKey]: an unfolded group already shows its rows.
     */
    foldSummary: (@Composable () -> String)? = null,
    /** A control on the heading's right — the pencil that puts a list into reorder mode. */
    action: (@Composable () -> Unit)? = null,
    /**
     * How far down the settings depth the whole group sits. Above its depth
     * a titled group draws closed under its heading, like a fold; one with no
     * heading tucks every row behind its "more settings" row. Rows keep their
     * own [SettingsGroupScope.item] level inside a group that is shown.
     */
    level: SettingLevel = SettingLevel.BASIC,
    builder: SettingsGroupScope.() -> Unit,
) {
    // The builder runs during composition, so rows may be added
    // conditionally on snapshot state (e.g. sliders that appear only
    // while their feature's toggle is on).
    val scope = SettingsGroupScope().apply(builder)
    val route = LocalScreenRoute.current.orEmpty()
    val depthControl = LocalSettingsDepth.current
    val depth = depthControl.depth
    val groupAbove = !depth.shows(level)
    val folds = LocalAdvancedFolds.current
    // Held by the screen, not the flash: the flash ends after two seconds, and
    // a row hidden by the depth went with it, out from under the finger of
    // whoever had just followed a link to it (#1030).
    val depthReveal = LocalDepthReveal.current
    val highlighted = depthReveal?.arrived ?: SettingsHighlight.pending
    // A row above the depth is not drawn at all; that is what the depth is
    // for. Two things bring it back: arriving from a link, a search result or
    // an addon's Use button (the row they name has to be there, so the page is
    // drawn as if the depth allowed it, and says so; see [DepthRevealNotice]),
    // and About › App › "Show where settings are hidden", which puts a "N more
    // settings" row at the end of the group and folds a whole titled group
    // closed under its heading instead.
    val reveal = highlighted || depthControl.hiddenRows
    if (groupAbove && !reveal) return
    val foldsWhole = groupAbove && title != null && depthControl.hiddenRows
    val tucked: (SettingsGroupScope.Entry) -> Boolean =
        { e -> !foldsWhole && (groupAbove || !depth.shows(e.level)) }
    val tuckedCount = scope.items.count { it.visible && tucked(it) }
    val entries = when {
        tuckedCount == 0 -> scope.items
        // Not pointed at: the hidden rows are simply not there.
        !depthControl.hiddenRows && !highlighted -> scope.items.filterNot { tucked(it) }
        // Arrived here and nothing to point with: every row in its place, each
        // knowing the depth it would otherwise need.
        !depthControl.hiddenRows -> {
            val needed = scope.items.filter { it.visible && tucked(it) }.maxOf { maxOf(it.level, level) }
            SideEffect { depthReveal?.note(needed) }
            scope.items.map { entry ->
                if (!tucked(entry)) return@map entry
                val revealedAt = maxOf(entry.level, level)
                SettingsGroupScope.Entry(entry.visible, entry.conditional, entry.key, entry.level) {
                    CompositionLocalProvider(LocalRevealedLevel provides revealedAt, entry.content)
                }
            }
        }
        else -> {
            // Remembered per group like a fold, and drawn open on arrival so a
            // search result can land inside.
            val moreId = "$route/${foldKey ?: title ?: "group"}#more"
            val moreOpen = (depthReveal?.holdsOpen(moreId) ?: highlighted) || (folds?.isOpen(moreId) ?: false)
            buildList<SettingsGroupScope.Entry> {
                scope.items.filterNotTo(this) { tucked(it) }
                add(
                    SettingsGroupScope.Entry(visible = true, conditional = false, key = null) {
                        MoreSettingsRow(tuckedCount, moreOpen) {
                            depthReveal?.letGo(moreId)
                            folds?.toggle(moreId, !moreOpen)
                        }
                    },
                )
                // Conditional on the fold, so they grow in under the row
                // rather than appear. Unkeyed on purpose: a keyed row's own
                // arrive-and-leave motion is for a list the user edits.
                for (entry in scope.items) {
                    if (!tucked(entry)) continue
                    add(
                        SettingsGroupScope.Entry(
                            visible = entry.visible && moreOpen,
                            conditional = true,
                            key = null,
                            level = entry.level,
                            content = entry.content,
                        ),
                    )
                }
            }
        }
    }
    // What the group is showing right now, which is what every count below is
    // about: a hidden conditional row is not a row the reader can see, so it
    // is not a slab in the skeleton and not one of the rows a closed fold
    // says it is holding.
    val shown = entries.count { it.visible }
    if (shown == 0) return
    // Below the fold while the screen is still animating in: come back for
    // the rows once the entrance can spare them — see [rememberGroupRevealed].
    // Until then the group holds its place as slabs the size of its rows, so
    // the screen opens at its full height instead of growing under the reader.
    if (!rememberGroupRevealed(shown)) {
        GroupSkeleton(shown, hasTitle = title != null)
        return
    }
    // A group folded by its level is named by its title: two such groups on
    // one screen must not share a remembered state.
    val foldId = (foldKey ?: if (foldsWhole) "level:$title" else null)?.let { "$route/$it" }
    val open = foldId == null || (depthReveal?.holdsOpen(foldId) ?: highlighted) || (folds?.isOpen(foldId) ?: false)
    // The heading of a group above the depth is itself a row the depth hid.
    val groupRevealedAt = level.takeIf { groupAbove && !depthControl.hiddenRows }
    // A named group is a scroll target in its own right. Some things the user
    // arrives at from search — or from an addon's Use button — are a whole
    // section rather than one row: "Icon pack", "Your packs", "Installed
    // fonts". Unnamed groups have nothing to match on and stay plain. Coarse,
    // because anything inside it that answers to the same request is a better
    // answer than the section around them.
    HighlightableRow(title, highlightKey, coarse = true, revealedAt = groupRevealedAt) {
        if (foldId != null && title != null) {
            FoldHeader(
                title,
                count = shown,
                open = open,
                info = info,
                summary = if (open) null else foldSummary?.invoke(),
            ) {
                depthReveal?.letGo(foldId)
                folds?.toggle(foldId, !open)
            }
        } else if (title != null) {
            SectionHeader(title, info = info, action = action, modifier = GroupHeadingPadding, linkable = true)
        }
        val cards: @Composable () -> Unit = {
            // The 3 dp between two cards is drawn by the card below rather
            // than by the column's arrangement (#136): an arrangement puts a
            // gap between every pair of children, including a conditional row
            // that has collapsed to nothing, and the group would keep a sliver
            // of air where a row used to be. Carried by the row, the gap goes
            // away with it.
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // Which rows are drawn, and how keyed ones arrive and leave,
                // is GroupRows' business (SettingsGroupMotion.kt); the card
                // each one sits on is this file's.
                GroupRows(entries) { modifier, gapAbove, first, last, content ->
                    Column(modifier = modifier) {
                        if (gapAbove) Spacer(Modifier.height(3.dp))
                        GroupCard(
                            top = if (first) GroupEndCorner else GroupInnerCorner,
                            bottom = if (last) GroupEndCorner else GroupInnerCorner,
                            content = content,
                        )
                    }
                }
            }
        }
        when {
            // Not a fold at all: the rows are simply there, and wrapping them
            // in a visibility that is always true costs a layer on every
            // group on every screen.
            foldId == null -> cards()
            // Reduced motion has no still version of a reveal, so the body
            // is or is not there — the switch [ExpandableCard] already reads,
            // from the same place.
            LocalSharedTransition.current == null -> if (open) cards()
            else -> AnimatedVisibility(
                visible = open,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                cards()
            }
        }
        Spacer(Modifier.height(GroupTail))
    }
}

/** The radius at the two ends of a group's stack of cards. */
private val GroupEndCorner = 24.dp

/** The radius where one card meets the next. */
private val GroupInnerCorner = 6.dp

/**
 * One row's card.
 *
 * The corners animate, because which card is the group's first and which its
 * last changes when a conditional row appears above or below them, and a
 * corner that snapped from 6 dp to 24 dp would undo the reveal that moved it.
 */
@Composable
private fun GroupCard(top: Dp, bottom: Dp, content: @Composable () -> Unit) {
    val topCorner by animateDpAsState(top, label = "groupCardTop")
    val bottomCorner by animateDpAsState(bottom, label = "groupCardBottom")
    Surface(
        shape = RoundedCornerShape(
            topStart = topCorner, topEnd = topCorner,
            bottomStart = bottomCorner, bottomEnd = bottomCorner,
        ),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // No padding of its own: a row brings its own (ListItem's 8 dp, a
        // slider row's 8 dp), and 4 dp on top of that made every row 80 dp
        // where 72 is the height the list is designed around.
        Column { content() }
    }
}

/**
 * What a group leaves under its last card. Most of the gap between two groups
 * lives here rather than above the next heading, so the first group on a screen
 * sits close under the bar's own air — see `ScreenTopGap` — instead of
 * doubling it.
 */
private val GroupTail = 16.dp

/** A group heading's inset: level with the text in the rows under it. */
private val GroupHeadingPadding = Modifier.padding(start = 32.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)

/**
 * A fold's heading: the title, how many rows it holds, and a chevron. With a
 * [summary] it also names what is inside, which is what makes a fold that
 * starts closed browsable — see `foldSummary` on [SettingsGroup].
 */
@Composable
private fun FoldHeader(
    title: String,
    count: Int,
    open: Boolean,
    info: String? = null,
    summary: String? = null,
    onToggle: () -> Unit,
) {
    val numberFormat = stringResource(R.string.values_number)
    val hold = rememberRowHold()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .settingLinkHold(hold)
            .clickable(onClick = onToggle)
            .then(GroupHeadingPadding),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            // Two lines and then an ellipsis: this is a hint at what is in the
            // fold, not the list itself. The list is one press away.
            if (summary != null) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp, end = 8.dp),
                )
            }
        }
        Text(
            numberFormat.format(count),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp),
        )
        HeadingControls {
            if (info != null) InfoButton(title = title, detail = info)
            SettingLinkButton(title)
            ExpandChevron(open)
        }
        RowHoldMenu(hold)
    }
}

/**
 * The lane a heading's controls sit in: as tall as the heading's own line, so
 * a "?" or a chevron does not make the heading a button's height. The touch
 * targets keep their full size and simply overhang the line, which is what
 * lets a heading with a "?" on it be as tall as one without.
 */
@Composable
private fun HeadingControls(content: @Composable RowScope.() -> Unit) {
    Box(modifier = Modifier.height(HeadingControlLane), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier.requiredHeight(HeadingControlHitHeight),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/** A heading's line: what `titleSmall` comes to at the default font scale. */
private val HeadingControlLane = 20.dp

/** The tap height a heading's controls keep while overhanging the lane. */
private val HeadingControlHitHeight = 48.dp

/** How a [StateBanner] reads: a fact, or something the user should fix. */
public enum class BannerTone { INFO, WARNING }

/**
 * A line of live state that changes what the controls near it do — "auto
 * theme is on, so picking a theme here does nothing" — drawn as a card with
 * the button that resolves it, not as a sentence telling the user where to
 * go. Explanation belongs in a row's "?" or a heading's; this is for the
 * case a subtitle cannot carry, because it is about the screen, not a row.
 */
@Composable
public fun StateBanner(
    text: String,
    action: String? = null,
    tone: BannerTone = BannerTone.INFO,
    onAction: (() -> Unit)? = null,
) {
    val container = when (tone) {
        BannerTone.INFO -> MaterialTheme.colorScheme.secondaryContainer
        BannerTone.WARNING -> MaterialTheme.colorScheme.errorContainer
    }
    val content = when (tone) {
        BannerTone.INFO -> MaterialTheme.colorScheme.onSecondaryContainer
        BannerTone.WARNING -> MaterialTheme.colorScheme.onErrorContainer
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Icon(
                if (tone == BannerTone.WARNING) Icons.Outlined.Warning else Icons.Outlined.Info,
                contentDescription = null,
                tint = content,
                modifier = Modifier.padding(end = 12.dp),
            )
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = content,
                modifier = Modifier.weight(1f),
            )
            if (action != null && onAction != null) {
                TextButton(onClick = onAction) { Text(action) }
            }
        }
    }
}

/**
 * Reference material that most visits never need — a table of template
 * variables, a syntax note — behind one row that opens it. Collapsed unless
 * asked: a page is for the settings on it, and a block of prose that is
 * always open is a block of prose everyone scrolls past.
 *
 * Drawn as one group card so it sits in the list like everything else; the
 * body is whatever the caller puts in [content], on the card's own surface.
 */
// Only for reading the shared-transition local as the reduced-motion switch.
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
public fun ExpandableCard(
    title: String,
    subtitle: String? = null,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            WmRow(
                title = title,
                subtitle = subtitle,
                trailing = { ExpandChevron(expanded) },
                onClick = { expanded = !expanded },
            )
            // Reduced motion has no still version of a reveal, so the body
            // simply is or is not there — the same switch the shared elements
            // use, read from the same place.
            if (LocalSharedTransition.current == null) {
                if (expanded) Column(modifier = Modifier.padding(bottom = 8.dp), content = content)
            } else {
                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(bottom = 8.dp), content = content)
                }
            }
        }
    }
}

/**
 * The chevron that says "this opens", pointing down closed and up open, and
 * turning between the two while the body it belongs to opens or closes.
 */
// Only for reading the shared-transition local as the reduced-motion switch.
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
public fun ExpandChevron(expanded: Boolean) {
    val target = if (expanded) 180f else 0f
    val angle = if (LocalSharedTransition.current == null) {
        target
    } else {
        animateFloatAsState(target, label = "expandChevron").value
    }
    Icon(
        Icons.Outlined.KeyboardArrowDown,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.rotate(angle),
    )
}

/** ListItem colors that let the group card's surface show through. */
@Composable
public fun transparentListColors(): ListItemColors =
    ListItemDefaults.colors(containerColor = Color.Transparent)

/**
 * "?" affordance that opens a dialog with the full explanation of a setting.
 * Draws nothing inside a [RowExtras] at the Simple depth, where the row's
 * long press offers the same explanation as "What is this?" (see
 * [RowHoldMenu]); a heading's "?" is outside any RowExtras and stays.
 */
@Composable
public fun InfoButton(title: String, detail: String) {
    if (LocalRowExtrasBare.current) return
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }, modifier = Modifier.size(RowExtraSize)) {
        Icon(
            Icons.AutoMirrored.Outlined.HelpOutline,
            contentDescription = stringResource(R.string.home_info_desc, title),
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
    if (open) InfoDialog(title, detail) { open = false }
}

/** The explanation [InfoButton] opens: the setting's name over its [detail], and OK. */
@Composable
public fun InfoDialog(title: String, detail: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(detail) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_ok))
            }
        },
    )
}

/**
 * A permission's grant state, re-read every time the settings screen comes
 * back to the foreground. Both the runtime permissions and the special ones
 * (Usage Access) are granted on a system screen we leave the app for, so a
 * plain read during composition stays stale until something unrelated
 * recomposes — which is what made the "permission required" rows outlive the
 * grant.
 */
@Composable
public fun rememberGrantState(check: (Context) -> Boolean): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(check(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = check(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return granted
}

/**
 * Whether the user granted Usage Access — a special permission, so it is an
 * app-op rather than a runtime grant. Mirrors the IME's own check.
 */
public fun hasUsageAccess(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
        ?: return false
    val mode = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName,
            )
        }
    }.getOrDefault(AppOpsManager.MODE_ERRORED)
    return mode == AppOpsManager.MODE_ALLOWED
}

/**
 * A group's heading. [info] is the section's explanation, drawn as the "?"
 * that rows already use rather than as a paragraph under the heading: the
 * explanation is there for whoever wants it and takes no room from anyone
 * who does not.
 *
 * [trailing] is a figure that belongs to the whole group — the total size of
 * the models or files listed under it — drawn muted on the heading's right,
 * level with the right edge of the rows' content. It answers "which of these
 * is the big one" at the group before the rows are read.
 */
@Composable
public fun SectionHeader(
    text: String,
    info: String? = null,
    // Aligns with the text inside group rows: 16dp group margin plus the
    // rows' own 16dp content inset.
    modifier: Modifier = Modifier.padding(start = 32.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
    action: (@Composable () -> Unit)? = null,
    trailing: String? = null,
    /**
     * True for a [SettingsGroup]'s own heading, which can hand out a link to
     * its group (#646). A free-standing heading is not the row the link would
     * flash, so it never offers one.
     */
    linkable: Boolean = false,
) {
    val link = linkable && settingLinkButtonShown() && currentRowLink() != null
    val hold = if (linkable) rememberRowHold() else null
    val outer = Modifier.settingLinkHold(hold).then(modifier)
    if (trailing != null) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = outer.fillMaxWidth()) {
            Text(
                text,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            if (info != null || action != null || link) {
                HeadingControls {
                    if (info != null) InfoButton(title = text, detail = info)
                    if (link) SettingLinkButton(text)
                    action?.invoke()
                }
            }
            Text(
                trailing,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                // The heading ends 16dp short of the card; the rows' own
                // content ends 16dp further in, and so does this.
                modifier = Modifier.padding(start = 8.dp, end = 16.dp),
            )
            RowHoldMenu(hold)
        }
        return
    }
    if (info == null && action == null && !link) {
        Box(outer) {
            Text(
                text,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            RowHoldMenu(hold)
        }
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = outer) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        HeadingControls {
            if (info != null) InfoButton(title = text, detail = info)
            if (link) SettingLinkButton(text)
            action?.invoke()
        }
        RowHoldMenu(hold)
    }
}

/** Free-standing explanatory text aligned with group content. */
@Composable
public fun CaptionText(text: String, modifier: Modifier = Modifier, error: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = if (error) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 32.dp, vertical = 8.dp),
    )
}

/*
 * The row helpers come in pairs. The `@StringRes` form is the one to use: it
 * knows the row's own resource, so it can look the row's glyph up in
 * [SettingsRowIcons] and match a search highlight on the resource rather than
 * on the drawn words. The `String` form is for the handful of rows whose name
 * is not a fixed resource at all — a language, a layout, an installed pack —
 * and those get no icon, because there is no row to hang one on.
 */

/** [NavRow] for a row named by a string resource; see the note above. */
@Composable
public fun NavRow(
    @StringRes title: Int,
    subtitle: String? = null,
    value: String? = null,
    route: String? = null,
    icon: ImageVector? = SettingsTables[title],
    onClick: () -> Unit,
) = NavRow(
    title = stringResource(title),
    subtitle = subtitle,
    value = value,
    route = route,
    icon = icon,
    highlightKey = title,
    onClick = onClick,
)

/**
 * A navigation row: title, optional subtitle, optional current value, chevron.
 *
 * [route] names the destination the row opens, which flies the row's name up
 * into that screen's heading. Only the name travels; the heading's icon fades
 * in with its screen rather than flying from the row's own tile.
 */
@Composable
public fun NavRow(
    title: String,
    subtitle: String? = null,
    value: String? = null,
    route: String? = null,
    icon: ImageVector? = null,
    @StringRes highlightKey: Int = 0,
    swipeActions: List<SwipeAction> = emptyList(),
    onClick: () -> Unit,
) {
    // A value only sits beside the title while it fits there; a longer one
    // goes under the title, where it has the width to be read. The chevron and
    // the gap before it are what the row's end costs whatever the value is.
    val beside = value != null && fitsBesideTitle(
        value,
        MaterialTheme.typography.labelLarge,
        furniture = TRAILING_ICON_WIDTH + 4.dp + (if (rowLinkButtonShown()) SettingLinkTargetSize else 0.dp),
        hasIcon = rowHasTile(icon),
    )
    val below = value.takeIf { it != null && !beside }
    HighlightableRow(title, highlightKey) {
        WmRow(
            title = title,
            subtitle = subtitle.takeIf { below == null },
            supporting = supportingWith(subtitle, below?.let { { RowValueText(it) } }),
            icon = icon,
            flightTo = route,
            swipeActions = swipeActions,
            // A value beside the title sits on the title's own line. In the
            // trailing slot it took a column the whole height of the row, and
            // a long subtitle was squeezed to three words a line beside it.
            trailingOnTitleLine = beside,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RowExtras { SettingLinkButton(title) }
                    if (beside && value != null) {
                        RowValueText(value, textAlign = TextAlign.End, maxLines = 1)
                        Spacer(Modifier.width(4.dp))
                    }
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            onClick = onClick,
        )
    }
}

/** A row's current value, wherever the row ended up putting it. */
@Composable
private fun RowValueText(
    value: String,
    textAlign: TextAlign? = null,
    maxLines: Int = 2,
) {
    Text(
        value,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = if (textAlign == null) Modifier else Modifier.widthIn(max = RowTrailingTextMaxWidth),
    )
}

/** An outlined button's content padding, which its label does not get to use. */
private val BUTTON_FURNITURE = 48.dp

/**
 * The slider a short-named row draws on its title line: long enough to drag
 * by thumb, short enough to leave the name its room (SETTINGS_REDESIGN.md,
 * section 5).
 */
private val SliderOnTitleLineWidth = 140.dp

/** The gap either side of that slider. */
private val SliderPillGap = 8.dp

/** The readout pill's own horizontal padding, both sides. */
private val SliderPillPadding = 16.dp

/**
 * The narrowest a row's title may be left. Below this it stops reading as
 * words and becomes a letter or two a line, which is the failure this whole
 * measurement exists to prevent.
 */
private val ROW_TITLE_FLOOR = 120.dp

/** `ListItem`'s own padding: 16 dp at each end of the row. */
private val ROW_PADDING = 32.dp

/** The leading slot of a row that has one: the tile, and the gap after it. */
private val ROW_ICON_LANE = WmIconTileSize + 16.dp

/** A trailing chevron or drop-down arrow, at the default `Icon` size. */
private val TRAILING_ICON_WIDTH = 24.dp

/** The inset a group card keeps from each edge of the screen. */
private val GROUP_CARD_INSET = 32.dp

/**
 * How wide a settings row is: the screen, less the inset its group card keeps
 * from the edges. A row is otherwise as wide as the list it sits in, so this
 * is the number the rows that budget their own width start from.
 */
@Composable
private fun settingsRowWidth(): Dp =
    LocalConfiguration.current.screenWidthDp.dp - GROUP_CARD_INSET

/**
 * Whether [text], drawn in [style], fits beside a row's title once the row has
 * paid for everything else it draws: [furniture] is what the trailing controls
 * take, [hasIcon] whether there is a leading tile, and [rowWidth] how much
 * there was to divide.
 *
 * `ListItem` hands its trailing slot whatever width it asks for and gives the
 * headline whatever is left. So a long value or a long button label does not
 * shrink itself — it shrinks the title, down to one letter a line, and the row
 * becomes unreadable while the thing that squeezed it looks fine. A row that
 * knows its own trailing text measures it here and puts it under the title
 * instead of beside it when it will not fit.
 *
 * What fits is what is left of [rowWidth] once the title keeps [floor]
 * ([ROW_TITLE_FLOOR]), capped at [cap] ([RowTrailingTextMaxWidth]). Measuring
 * against that cap alone — which is what this did — passes a value that fits
 * the cap and still leaves the title nothing, because the cap knows nothing
 * about the "?", the reset, the glyph and the arrow sharing the row's end
 * with it. A row measuring its *title* against the rest of the line (the
 * slider row) passes a floor of zero and no cap: the title is the text.
 */
@Composable
private fun fitsBesideTitle(
    text: String,
    style: TextStyle,
    furniture: Dp,
    hasIcon: Boolean,
    rowWidth: Dp = settingsRowWidth(),
    floor: Dp = ROW_TITLE_FLOOR,
    cap: Dp = RowTrailingTextMaxWidth,
): Boolean {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val lane = if (hasIcon) ROW_ICON_LANE else 0.dp
    val max = (rowWidth - ROW_PADDING - lane - furniture - floor).coerceAtMost(cap)
    return remember(text, style, max, density) {
        max > 0.dp &&
            measurer.measure(text, style, maxLines = 1).size.width <=
            with(density) { max.toPx() }
    }
}

/** The widest of [texts] drawn in [style] on one line, for a row budgeting a slot whose text changes. */
@Composable
private fun widestText(texts: List<String>, style: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(texts, style, density) {
        with(density) { texts.maxOf { measurer.measure(it, style, maxLines = 1).size.width }.toDp() }
    }
}

/**
 * A row's supporting line, plus [extra] under it when the trailing slot could
 * not hold it. Null when there is nothing to draw, which leaves the row's own
 * [subtitle] to it.
 */
private fun supportingWith(
    subtitle: String?,
    extra: (@Composable () -> Unit)?,
): (@Composable () -> Unit)? {
    if (extra == null) return null
    return {
        Column {
            if (subtitle != null) Text(subtitle)
            extra()
        }
    }
}

/** [ToggleSetting] for a row named by a string resource. */
@Composable
public fun ToggleSetting(
    @StringRes title: Int,
    subtitle: String?,
    checked: Boolean,
    info: String? = null,
    switchKey: String? = null,
    icon: ImageVector? = SettingsTables[title],
    enabled: Boolean = true,
    disabledReason: DisabledReason? = null,
    default: Boolean? = null,
    onChange: (Boolean) -> Unit,
) = ToggleSetting(
    title = stringResource(title),
    subtitle = subtitle,
    checked = checked,
    info = info,
    switchKey = switchKey,
    icon = icon,
    highlightKey = title,
    enabled = enabled,
    disabledReason = disabledReason,
    default = default,
    onChange = onChange,
)

/**
 * [default] is the value the setting shipped with, from [SettingsDefaults].
 * Passing it gives the row a reset control while it is switched the other way;
 * see [ResetSetting]. Leave it null on the rows that have no one default — a
 * toggle over a list the user built, or one whose default depends on the
 * device rather than on the app.
 */
@Composable
public fun ToggleSetting(
    title: String,
    subtitle: String?,
    checked: Boolean,
    info: String? = null,
    switchKey: String? = null,
    icon: ImageVector? = null,
    @StringRes highlightKey: Int = 0,
    // Off for a setting that cannot be turned on yet — the row still reads and
    // still opens, it just has nothing to switch. The subtitle is where the
    // caller says why, and [disabledReason] what a long press on the row says,
    // with the way to the setting that unblocks it.
    enabled: Boolean = true,
    disabledReason: DisabledReason? = null,
    default: Boolean? = null,
    onChange: (Boolean) -> Unit,
) {
    // Every switch in the app latches through here, so this is the one place
    // the latch has to be given a feel — see [SettingsHaptics].
    val haptics = rememberSettingsHaptics()
    val change: (Boolean) -> Unit = { on ->
        haptics.toggle(on)
        onChange(on)
    }
    HighlightableRow(title, highlightKey, disabledReason = disabledReason.takeUnless { enabled }, info = info) {
        WmRow(
            title = title,
            subtitle = subtitle,
            icon = icon,
            // The switch, the "?" and the reset are the title's furniture, not
            // a second column beside the whole row: see [trailingOnTitleLine].
            trailingOnTitleLine = true,
            // Only a row that can grow a reset control has a title line that
            // changes width, so only that row pays for the word-by-word name.
            titleReflows = default != null,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RowExtras {
                        if (info != null) InfoButton(title, info)
                        SettingLinkButton(title)
                        ResetSetting(title, default != null && checked != default, possible = default != null) {
                            change(default == true)
                        }
                    }
                    // A little air before the switch (#970): with the "?" right
                    // against it, a tap meant for one landed on the other.
                    Spacer(Modifier.width(6.dp))
                    Switch(
                        checked = checked,
                        onCheckedChange = change,
                        enabled = enabled,
                        // The same switch the row that opened this screen was
                        // showing, when the caller says so.
                        modifier = if (switchKey == null) Modifier
                        else Modifier.wmSharedElement(switchKey),
                    )
                }
            },
        )
    }
}

/**
 * A row that is both a switch and a door (#136): the switch turns the feature
 * on or off in place, and the rest of the row opens [route], the page holding
 * everything else the feature has. The tools list is the same shape, and like
 * it the switch flies into the destination's own enable row when that row
 * takes `switchKey = landingKey("switch")`.
 *
 * [default] is what [ToggleSetting] takes it for: the per-setting reset.
 */
@Composable
public fun ToggleNavRow(
    @StringRes title: Int,
    subtitle: String?,
    checked: Boolean,
    route: String,
    info: String? = null,
    default: Boolean? = null,
    onChange: (Boolean) -> Unit,
    onOpen: () -> Unit,
) {
    val name = stringResource(title)
    HighlightableRow(name, title, info = info) {
        WmRow(
            title = name,
            subtitle = subtitle,
            icon = SettingsTables[title],
            flightTo = route,
            trailingOnTitleLine = true,
            titleReflows = default != null,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RowExtras {
                        if (info != null) InfoButton(name, info)
                        SettingLinkButton(name)
                        ResetSetting(name, default != null && checked != default, possible = default != null) {
                            onChange(default == true)
                        }
                    }
                    Switch(
                        checked = checked,
                        onCheckedChange = onChange,
                        modifier = Modifier.wmSharedElement(takeOffKey("switch", route)),
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            onClick = onOpen,
        )
    }
}

/** The gap between a row's icon tile and the words beside it. */
private val RowIconGap = 16.dp

/** How far the tile pushes a row's text in, so a subtitle lines up with the title. */
private val RowIconLane = WmIconTileSize + RowIconGap

/**
 * The frame a row that is not a [WmRow] sits in — a slider, a segmented choice.
 *
 * Those draw their own control under their own name, so they cannot use
 * `ListItem`. The tile goes where `ListItem` would put it and the [header] and
 * [subtitle] indent past it, so a group holding both kinds of row lines its
 * words up down one edge.
 *
 * [content] — the slider, the segmented row — deliberately does *not* indent.
 * It is a control, not a line of text, and 56 dp is a sixth of a phone's width:
 * taking that off a row of segmented buttons is the difference between "After
 * the shortcut key" and "After the".
 *
 * The tile and the subtitle's indent leave together when "Icons in settings"
 * is turned off, the lane closing as the tile shrinks — see [IconReveal].
 */
@Composable
private fun IconedRow(
    icon: ImageVector?,
    subtitle: String? = null,
    header: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val reveal = LocalIconReveal.current.takeIf { icon != null && it.present }
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null && reveal != null) {
                Row(Modifier.iconReveal(reveal)) {
                    WmIconTile(icon, currentRouteAccent())
                    Spacer(Modifier.width(RowIconGap))
                }
            }
            header()
        }
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (reveal == null) Modifier else Modifier.revealInset(reveal),
            )
        }
        content()
    }
}

/** A subtitle's indent past the tile, closing as the tile leaves. */
private fun Modifier.revealInset(reveal: IconReveal): Modifier = layout { measurable, constraints ->
    val inset = (RowIconLane.toPx() * reveal.progress.value).roundToInt()
    val placeable = measurable.measure(constraints.offset(horizontal = -inset))
    layout(placeable.width + inset, placeable.height) { placeable.placeRelative(inset, 0) }
}

/** [ControlSetting] for a row named by a string resource. */
@Composable
public fun ControlSetting(
    @StringRes title: Int,
    subtitle: String? = null,
    info: String? = null,
    icon: ImageVector? = SettingsTables[title],
    content: @Composable ColumnScope.() -> Unit,
) = ControlSetting(
    title = stringResource(title),
    subtitle = subtitle,
    info = info,
    icon = icon,
    highlightKey = title,
    content = content,
)

/**
 * A titled row whose control sits under the title and spans the row: a text
 * field, a run of chips that each act as they are tapped, a slider with presets
 * beside it — anything the other `*Setting` helpers have no shape for.
 *
 * The name, "?", subtitle and tile lane are [SliderSetting]'s and
 * [ChoiceSetting]'s, so a bespoke control still reads as one of the page's rows
 * rather than a label someone set above a widget. [content] starts right under
 * the subtitle; it brings its own top gap.
 */
@Composable
public fun ControlSetting(
    title: String,
    subtitle: String? = null,
    info: String? = null,
    icon: ImageVector? = null,
    @StringRes highlightKey: Int = 0,
    content: @Composable ColumnScope.() -> Unit,
) {
    HighlightableRow(title, highlightKey, info = info) {
        IconedRow(
            icon = icon,
            subtitle = subtitle,
            header = {
                ReflowingText(title, style = MaterialTheme.typography.bodyLarge)
                RowExtras(stack = false) {
                    if (info != null) InfoButton(title, info)
                    SettingLinkButton(title)
                }
                Spacer(Modifier.weight(1f))
            },
            content = content,
        )
    }
}

/** [SliderSetting] for a row named by a string resource. */
@Composable
public fun SliderSetting(
    @StringRes title: Int,
    subtitle: String? = null,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: (Float) -> String,
    info: String? = null,
    icon: ImageVector? = SettingsTables[title],
    enabled: Boolean = true,
    disabledReason: DisabledReason? = null,
    default: Float? = null,
    onReset: (() -> Unit)? = null,
    preview: ((Float) -> Unit)? = null,
    typed: Boolean = true,
    onChange: (Float) -> Unit,
) = SliderSetting(
    title = stringResource(title),
    subtitle = subtitle,
    value = value,
    range = range,
    display = display,
    info = info,
    icon = icon,
    highlightKey = title,
    enabled = enabled,
    disabledReason = disabledReason,
    default = default,
    onReset = onReset,
    preview = preview,
    typed = typed,
    onChange = onChange,
)

/**
 * A labelled slider row. [display] formats the *live* value rather than taking a
 * pre-rendered string, so the readout tracks the thumb instead of the stored
 * setting; see [rememberLiveSlider] for the rest.
 *
 * [default] is the value the slider shipped at, from [SettingsDefaults]. The
 * reset control compares against the *stored* value rather than the live one,
 * so it neither flickers under a drag nor disappears the instant the thumb
 * passes over the default on its way somewhere else.
 */
@Composable
public fun SliderSetting(
    title: String,
    subtitle: String? = null,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: (Float) -> String,
    info: String? = null,
    icon: ImageVector? = null,
    @StringRes highlightKey: Int = 0,
    // Off for a slider whose value cannot take effect yet — the row still
    // reads, and the subtitle is where the caller says why. Matches
    // [ToggleSetting]'s own `enabled`, and so is [disabledReason].
    enabled: Boolean = true,
    disabledReason: DisabledReason? = null,
    default: Float? = null,
    /**
     * For a setting whose default is "unset", not a number: the reset shows
     * whenever this is non-null and runs it instead of writing [default], which
     * would pin the value the automatic one happens to be here. [default] is
     * then only where the slider sits.
     */
    onReset: (() -> Unit)? = null,
    /**
     * Acts on the value under the finger on every step, for a row whose whole
     * point is heard or felt rather than stored — the key sound's volume, a
     * haptic's strength. [onChange] still runs once, on release.
     */
    preview: ((Float) -> Unit)? = null,
    /**
     * Writes [onChange] on a throttle while the finger is down, for the theme
     * and sticker editors, whose screens draw the stored value they edit. See
     * `rememberLiveSlider`; everywhere else one write on release is the point.
     */
    live: Boolean = false,
    /**
     * Whether tapping the readout opens a field to type the value in. Off only
     * for a readout that changes unit along the track ("12 hours", then
     * "1 day"), where a bare number cannot say which one it means.
     */
    typed: Boolean = true,
    onChange: (Float) -> Unit,
) {
    val slider = rememberLiveSlider(value, onChange, live = live, preview = preview)
    var typing by rememberSaveable { mutableStateOf(false) }
    // The readout is the slider's detent: this row's values are continuous, so
    // the steps the user is actually aiming at are the ones the number they can
    // read changes on. Keyed on the string rather than the float, so a drag
    // across a range that formats to whole percent ticks a hundred times and
    // not once per touch event. Silent when the value moved without a finger on
    // it — a reset, or another screen writing the same setting.
    val haptics = rememberSettingsHaptics()
    val readout = display(slider.value)
    LaunchedEffect(readout) { if (slider.dragging) haptics.tick() }
    // A short name shares its line with the slider (SETTINGS_REDESIGN.md,
    // section 5): the row is then one line and its control, about 72 dp
    // rather than 110. What else the line has to hold is measured first — the
    // slider, the widest the pill gets, and the "?", link and reset the row
    // could grow — so a long name keeps the two-storey row rather than being
    // squeezed to a word a line beside a slider. The pill is measured at the
    // range's ends and middle rather than at the live readout, so the answer
    // holds still under a drag.
    val extras = listOf(
        info != null && rowInfoButtonShown(),
        rowLinkButtonShown(),
        onReset != null || default != null,
    ).count { it }
    val pillWidth = widestText(
        listOf(display(range.start), display((range.start + range.endInclusive) / 2f), display(range.endInclusive)),
        MaterialTheme.typography.labelLarge,
    ) + SliderPillPadding
    val onTitleLine = fitsBesideTitle(
        title,
        MaterialTheme.typography.bodyLarge,
        furniture = SliderOnTitleLineWidth + SliderPillGap * 2 + pillWidth + RowExtraSize * extras,
        hasIcon = rowHasTile(icon),
        floor = 0.dp,
        cap = Dp.Infinity,
    )
    HighlightableRow(title, highlightKey, disabledReason = disabledReason.takeUnless { enabled }, info = info) {
        IconedRow(
            icon = icon,
            subtitle = subtitle,
            header = {
                // The name is the weighted half and the readout is not, so a
                // Row measures the readout first and the name wraps around
                // whatever is left. The other way round a long name eats the
                // width and the number it is describing is the part that
                // disappears.
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Weighted so that the reset and the "?" are measured
                    // first and a long name wraps around what they leave. The
                    // other way round the name takes the whole line and the
                    // controls it is sharing it with are squeezed to nothing.
                    ReflowingText(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    RowExtras(stack = false) {
                        if (info != null) InfoButton(title, info)
                        SettingLinkButton(title)
                        if (onReset != null) {
                            ResetSetting(title, true, possible = true, onReset)
                        } else {
                            ResetSetting(title, default != null && value != default, possible = default != null) {
                                onChange(default ?: 0f)
                            }
                        }
                    }
                }
                if (onTitleLine) {
                    WmSlider(
                        value = slider.value,
                        onValueChange = slider::onDrag,
                        modifier = Modifier.width(SliderOnTitleLineWidth),
                        onValueChangeFinished = slider::onRelease,
                        valueRange = range,
                        enabled = enabled,
                    )
                    Spacer(Modifier.width(SliderPillGap))
                }
                // A slider cannot be landed on one exact millisecond by thumb,
                // so the number is a button that takes it typed (#415). The
                // pill is what says so; a bare number reads as a label.
                Text(
                    readout,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    modifier = if (typed && enabled) {
                        Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .clickable(
                                onClickLabel = stringResource(R.string.common_type_setting_value_desc, title),
                                role = Role.Button,
                            ) { typing = true }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    } else {
                        Modifier
                    },
                )
            },
        ) {
            if (!onTitleLine) {
                WmSlider(
                    value = slider.value,
                    onValueChange = slider::onDrag,
                    onValueChangeFinished = slider::onRelease,
                    valueRange = range,
                    enabled = enabled,
                )
            }
        }
    }
    if (typing) {
        SliderEntryDialog(
            title = title,
            readout = readout,
            range = range,
            display = display,
            onDismiss = { typing = false },
        ) {
            typing = false
            // Through the slider rather than to onChange directly, so the
            // thumb moves at once, a sound or haptic row plays the new value,
            // and a live editor writes it the same way a drag would.
            slider.onDrag(it)
            slider.onRelease()
        }
    }
}

/** [StepperSetting] for a row named by a string resource. */
@Composable
public fun StepperSetting(
    @StringRes title: Int,
    subtitle: String? = null,
    value: Int,
    range: List<Int>,
    display: (Int) -> String,
    info: String? = null,
    icon: ImageVector? = SettingsTables[title],
    default: Int? = null,
    onChange: (Int) -> Unit,
) = StepperSetting(
    title = stringResource(title),
    subtitle = subtitle,
    value = value,
    range = range,
    display = display,
    info = info,
    icon = icon,
    highlightKey = title,
    default = default,
    onChange = onChange,
)

/**
 * A whole-number row: the value between a minus and a plus, stepping through
 * [range].
 *
 * A stepper rather than a [SliderSetting] where the value is a small count and
 * every step of it matters. A slider spends a full row on a track that a count
 * of nine values cannot use — and it cannot be landed on exactly by thumb, which
 * is the whole of what someone picking a column count is trying to do.
 *
 * [range] is the steps in order rather than a numeric range, so a setting whose
 * values are not contiguous — an automatic 0 in front of 3..11, say — is still
 * one press per step in each direction.
 */
@Composable
public fun StepperSetting(
    title: String,
    subtitle: String? = null,
    value: Int,
    range: List<Int>,
    display: (Int) -> String,
    info: String? = null,
    icon: ImageVector? = null,
    @StringRes highlightKey: Int = 0,
    default: Int? = null,
    onChange: (Int) -> Unit,
) {
    // A stored value off the ladder falls back to the first step. The repository
    // clamps every write onto it, so this is a guard against a hand-edited
    // preference rather than a path the settings screen can take.
    val index = range.indexOf(value).coerceAtLeast(0)
    // One step of the ladder, which is exactly what a tick means.
    val haptics = rememberSettingsHaptics()
    val step: (Int) -> Unit = { next ->
        haptics.tick()
        onChange(next)
    }
    HighlightableRow(title, highlightKey, info = info) {
        IconedRow(
            icon = icon,
            subtitle = subtitle,
            header = {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ReflowingText(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    RowExtras(stack = false) {
                        if (info != null) InfoButton(title, info)
                        SettingLinkButton(title)
                        ResetSetting(title, default != null && value != default, possible = default != null) {
                            onChange(default ?: 0)
                        }
                    }
                }
            },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { step(range[index - 1]) },
                    enabled = index > 0,
                ) {
                    Icon(
                        Icons.Outlined.Remove,
                        contentDescription = stringResource(
                            R.string.common_decrease_setting_desc,
                            title,
                        ),
                    )
                }
                Text(
                    display(range.getOrElse(index) { value }),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.widthIn(min = 88.dp),
                )
                IconButton(
                    onClick = { step(range[index + 1]) },
                    enabled = index < range.lastIndex,
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = stringResource(
                            R.string.common_increase_setting_desc,
                            title,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * A settings row whose control is a button rather than a value: clear this,
 * reset that, forget the other.
 *
 * [confirm] is the body of a confirmation dialog. Pass it for anything that
 * throws away something the user cannot get back (a word list, a history, a
 * set of pins) and leave it null for anything that only restores a default,
 * which the user can simply set again.
 *
 * [lock] names this button in the fingerprint lock's registry, so the user can
 * choose to put a check in front of it. The check comes *after* the confirm
 * dialog, deliberately: nobody should scan a finger for a dialog they are
 * about to cancel, and the dialog is what says what is about to go away. That
 * makes the fingerprint the last thing before the write, which is where it is
 * worth anything.
 */
@Composable
public fun ActionRow(
    @StringRes title: Int,
    subtitle: String?,
    action: String,
    confirm: String? = null,
    /** Runs the action behind the app's own check (a fingerprint, say), or null for none. */
    guard: ((() -> Unit) -> Unit)? = null,
    icon: ImageVector? = SettingsTables[title],
    enabled: Boolean = true,
    /** What a long press says while [enabled] is off; see [DisabledReason]. */
    disabledReason: DisabledReason? = null,
    onAction: () -> Unit,
) {
    var asking by remember { mutableStateOf(false) }
    val label = stringResource(title)
    val guarded: () -> Unit = { if (guard != null) guard(onAction) else onAction() }
    val press = { if (confirm == null) guarded() else asking = true }
    val button: @Composable () -> Unit = {
        OutlinedButton(enabled = enabled, onClick = press) { Text(action) }
    }
    // "Clear" sits at the end of the row. "Delete the learned words" cannot:
    // the button would take the row and leave the title a letter a line.
    val beside = fitsBesideTitle(
        action,
        MaterialTheme.typography.labelLarge,
        furniture = BUTTON_FURNITURE,
        hasIcon = rowHasTile(icon),
    )
    HighlightableRow(label, title, disabledReason = disabledReason.takeUnless { enabled }) {
        WmRow(
            title = label,
            subtitle = subtitle.takeIf { beside },
            supporting = supportingWith(subtitle, if (beside) null else button),
            icon = icon,
            trailing = if (beside) button else null,
        )
    }
    if (!asking) return
    AlertDialog(
        onDismissRequest = { asking = false },
        title = { Text(label) },
        text = { Text(confirm.orEmpty()) },
        confirmButton = {
            TextButton(onClick = {
                asking = false
                guarded()
            }) { Text(action) }
        },
        dismissButton = {
            TextButton(onClick = { asking = false }) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

/** [ChoiceSetting] for a row named by a string resource. */
@Composable
public fun <T> ChoiceSetting(
    @StringRes title: Int,
    subtitle: String? = null,
    info: String? = null,
    options: List<Pair<T, String>>,
    selected: T,
    icon: ImageVector? = SettingsTables[title],
    default: T? = null,
    detail: (@Composable (T) -> ChoiceDetail?)? = null,
    onChange: (T) -> Unit,
) = ChoiceSetting(
    title = stringResource(title),
    subtitle = subtitle,
    info = info,
    options = options,
    selected = selected,
    icon = icon,
    highlightKey = title,
    default = default,
    detail = detail,
    onChange = onChange,
)

/**
 * A titled single-choice row of segmented buttons over [options].
 *
 * [default] is the option the setting shipped on, from [SettingsDefaults]; it
 * gives the row a reset control while some other option is picked. Null on a
 * choice with no fixed default — one over a list the user assembled, or one
 * whose starting option is read off the device.
 */
@Composable
public fun <T> ChoiceSetting(
    title: String,
    subtitle: String? = null,
    info: String? = null,
    options: List<Pair<T, String>>,
    selected: T,
    icon: ImageVector? = null,
    @StringRes highlightKey: Int = 0,
    default: T? = null,
    detail: (@Composable (T) -> ChoiceDetail?)? = null,
    onChange: (T) -> Unit,
) {
    val measurer = rememberTextMeasurer()
    val tiers = segmentTypeTiers()
    val density = LocalDensity.current
    // Measured here rather than left to the control, because the answer picks
    // the whole row: the segmented form puts the control under the title, and
    // the sheet form is an ordinary value row. [IconedRow] insets its content
    // by 16dp on each side, so that is what the segments have to live in.
    // The control below re-measures the same options against the same width
    // and reaches the same answer; only "is there one" is needed here.
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val available = maxWidth - 32.dp
        val fits = remember(options, available, tiers, density) {
            segmentStyle(options, available, measurer, tiers, density) != null
        }
        if (fits) {
            HighlightableRow(title, highlightKey, info = info) {
                IconedRow(
                    icon = icon,
                    subtitle = subtitle,
                    header = {
                        ReflowingText(title, style = MaterialTheme.typography.bodyLarge)
                        // The same `default != null` that draws the control at all is what
                        // makes the let non-empty; there is no fallback option to reset to
                        // on a row that shipped without a default.
                        RowExtras(stack = false) {
                            if (info != null) InfoButton(title, info)
                            SettingLinkButton(title)
                            ResetSetting(title, default != null && selected != default, possible = default != null) {
                                default?.let(onChange)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                    },
                ) {
                    ChoiceControl(
                        options = options,
                        selected = selected,
                        modifier = Modifier.padding(top = 8.dp),
                        title = title,
                        detail = detail,
                        onChange = onChange,
                    )
                }
            }
            return@BoxWithConstraints
        }
        var open by rememberSaveable { mutableStateOf(false) }
        val current = options.firstOrNull { it.first == selected }?.second.orEmpty()
        val glyph = choiceIcon(selected, detail)
        // The row already spends its trailing width on the "?", the reset, the
        // option's glyph and the arrow, so what the option name may take is
        // what those four leave. The reset is counted whenever the row could
        // grow one, not only while it has one, or the value would jump under
        // the title the moment the setting stopped matching its default.
        val beside = fitsBesideTitle(
            current,
            MaterialTheme.typography.labelLarge,
            furniture = TRAILING_ICON_WIDTH +
                rowExtrasWidth(
                    listOf(info != null && rowInfoButtonShown(), default != null, rowLinkButtonShown()).count { it },
                ) +
                (if (glyph != null) ChoiceValueGlyphSize + ChoiceValueGlyphGap else 0.dp),
            hasIcon = rowHasTile(icon),
            rowWidth = maxWidth,
        )
        HighlightableRow(title, highlightKey, info = info) {
            WmRow(
                title = title,
                subtitle = subtitle.takeIf { beside },
                supporting = supportingWith(
                    subtitle,
                    if (beside) null else {
                        {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ChoiceValueGlyph(glyph)
                                RowValueText(current)
                            }
                        }
                    },
                ),
                icon = icon,
                titleReflows = default != null,
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RowExtras {
                            if (info != null) InfoButton(title, info)
                            SettingLinkButton(title)
                            ResetSetting(title, default != null && selected != default, possible = default != null) {
                                default?.let(onChange)
                            }
                        }
                        // The same glyph the sheet puts on this option, small
                        // enough to sit in a value's lane: the row and the
                        // sheet then agree at a glance about what is chosen.
                        if (beside) ChoiceValueGlyph(glyph)
                        if (beside) RowValueText(current, textAlign = TextAlign.End, maxLines = 1)
                        Icon(
                            Icons.Outlined.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                onClick = { open = true },
            )
        }
        if (open) {
            ChoiceSheet(
                title = title,
                subtitle = subtitle,
                options = options,
                selected = selected,
                detail = detail,
                onPick = onChange,
                onDismiss = { open = false },
            )
        }
    }
}

/**
 * Everything a segmented button spends on width that is not its label:
 * `TextButtonContentPadding` (12 dp each side) plus the check-mark lane
 * (`SegmentedButtonDefaults.IconSize` 18 dp + an 8 dp gap). Material reserves
 * that lane whether or not the item is selected — `SegmentedButtonContent`
 * measures `maxOf(IconSize, iconWidth) + IconSpacing` — so it is a fixed 50 dp,
 * and the 2 dp on top is slack for rounding.
 *
 * None of those three is public, hence the copy here. It matters because a
 * segmented row divides its width evenly and then ellipsises whatever does not
 * fit, silently: "After the shortcut key" becomes "After the" and the setting
 * stops saying what it does. [ChoiceControl] measures against this.
 */
private val SegmentFurniture = 52.dp

/**
 * What a picker says about one option beyond its name: a line explaining what
 * it does, and something to look at.
 *
 * [leading] is a slot for something drawn — a colour swatch, a preview — and
 * wins over everything else. It stays the last parameter on purpose: the theme
 * editor passes it as a trailing lambda, and anything declared after it would
 * take those lambdas instead, quietly.
 *
 * [icon] is the ordinary case, a glyph in the row's tile, and is only needed
 * where the option is not an enum constant: [ChoiceOptionIcons] already
 * answers for those, so most call sites give neither and still get an icon.
 */
public class ChoiceDetail(
    val description: String? = null,
    val icon: ImageVector? = null,
    val leading: (@Composable () -> Unit)? = null,
)

/**
 * The glyph for one option of a one-of-N setting: whatever the call site named
 * on its [ChoiceDetail], and otherwise whatever [ChoiceOptionIcons] holds for
 * the value itself.
 */
@Composable
private fun <T> choiceIcon(option: T, detail: (@Composable (T) -> ChoiceDetail?)?): ImageVector? =
    detail?.invoke(option)?.icon ?: SettingsTables.choiceIcon(option)

/** The size an option's glyph takes beside a value on the row behind a sheet. */
private val ChoiceValueGlyphSize = 18.dp

/** The air that glyph keeps between itself and the value it belongs to. */
private val ChoiceValueGlyphGap = 6.dp

/** The lane an option's glyph occupies on a sheet row, tile or no tile. */
private val ChoiceSheetGlyphLane = 40.dp

/** That glyph's own size inside the lane. */
private val ChoiceSheetGlyphSize = 24.dp

/**
 * One option's glyph on a sheet: the mark itself, in the text colour, with no
 * tile and no accent behind it. It keeps the width a [WmIconTile] would have
 * taken so the labels still line up with every other row in the app.
 */
@Composable
private fun ChoiceSheetGlyph(icon: ImageVector) {
    Box(
        modifier = Modifier.size(ChoiceSheetGlyphLane),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(ChoiceSheetGlyphSize),
        )
    }
}

/**
 * The chosen option's glyph, drawn small beside its name on the row that opens
 * the sheet. Draws nothing for an option that has no glyph, so a row whose
 * options are languages or layouts looks exactly as it did.
 */
@Composable
private fun ChoiceValueGlyph(icon: ImageVector?) {
    if (icon == null) return
    Icon(
        icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(end = ChoiceValueGlyphGap).size(ChoiceValueGlyphSize),
    )
}

/**
 * The options of a one-of-N setting on a sheet that slides up, one row each.
 *
 * This is what the wrapping chip cloud used to be. A cloud that runs onto two
 * or three lines costs a card of height on every visit, and it moves under the
 * finger as the selection changes, because the chips are different widths and
 * the check mark on the selected one makes it wider still. It also has nowhere
 * to say what any option means. A row of rows has room for a description and a
 * swatch, and it costs one line on the screen behind it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun <T> ChoiceSheet(
    title: String?,
    subtitle: String?,
    options: List<Pair<T, String>>,
    selected: T,
    detail: (@Composable (T) -> ChoiceDetail?)?,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    // Opened at its full height rather than half way. A sheet that stops after
    // three rows is a list whose fourth option the user has no reason to think
    // exists: the handle says "there is more here" only to someone who already
    // suspects it. The column below scrolls, so a long list still fits.
    val rail = rememberScrollRailState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        // The rail is the other half of opening at full height: the sheet says
        // the list is long, and the rail says where in it you are.
        ScrollRail(state = rail, fadeColor = BottomSheetDefaults.ContainerColor) {
            if (title != null) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            for ((option, label) in options) {
                val extra = detail?.invoke(option)
                // A drawn slot wins: a colour swatch says more about a colour
                // than any glyph could. Otherwise the option's own glyph,
                // which most sheets get without asking.
                val glyph = extra?.icon ?: SettingsTables.choiceIcon(option)
                WmRow(
                    title = label,
                    subtitle = extra?.description,
                    leading = extra?.leading ?: glyph?.let { vector ->
                        // Bare, not a WmIconTile: a sheet is a list of
                        // options rather than a list of destinations, and a
                        // column of accent tiles reads as a menu of places to
                        // go. The glyph is here to tell the options apart.
                        { ChoiceSheetGlyph(vector) }
                    },
                    trailing = { RadioButton(selected = option == selected, onClick = null) },
                    onClick = {
                        onPick(option)
                        onDismiss()
                    },
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

/**
 * A one-of-N control: segmented buttons when every option's name fits one, and
 * the current choice on a button that opens [ChoiceSheet] when they do not.
 *
 * Segmented buttons are the better control while the words survive: they read
 * as "one of these" and they are all reachable without a press. Two or three
 * short options ("On"/"Off", "Tabs"/"Mixed") fit anywhere; "After the shortcut
 * key" does not fit a third of a phone, and no amount of layout tuning makes
 * it.
 *
 * Every one-of-N row in settings goes through here rather than building its own
 * segmented row, because whether the words fit is not something the author can
 * know: it depends on the screen and on the language, and the failure is silent
 * (Material ellipsises and says nothing).
 *
 * [label] is the question the control answers, drawn above it. A control that
 * stands on its own needs one: a segmented row of three words, or worse a lone
 * button reading "System light or dark", says what the answers are and never
 * says what was asked. Leave it out only where the words directly above the
 * control already ask — a [ChoiceSetting] row, or a section heading with
 * nothing between it and the control.
 */
@Composable
public fun <T> ChoiceControl(
    options: List<Pair<T, String>>,
    selected: T,
    modifier: Modifier = Modifier,
    title: String? = null,
    label: String? = null,
    detail: (@Composable (T) -> ChoiceDetail?)? = null,
    onChange: (T) -> Unit,
) {
    val measurer = rememberTextMeasurer()
    val tiers = segmentTypeTiers()
    // A segment taking the selection is a step along a row of them, so it gets
    // the same tick a stepper's arrow does. Silent when the press lands on the
    // option that is already selected: nothing moved.
    val haptics = rememberSettingsHaptics()
    val pick: (T) -> Unit = { option ->
        if (option != selected) haptics.tick()
        onChange(option)
    }
    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            // Not the muted colour a subtitle gets: this is the name of the
            // control under it, not a footnote about it.
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val density = LocalDensity.current
            val width = maxWidth
            val style = remember(options, width, tiers, density) {
                segmentStyle(options, width, measurer, tiers, density)
            }
            if (style != null) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, (option, name) ->
                        SegmentedButton(
                            selected = selected == option,
                            onClick = { pick(option) },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        ) {
                            // The size the labels were measured at, which is
                            // not always the default one.
                            Text(name, style = style, maxLines = 1)
                        }
                    }
                }
                return@BoxWithConstraints
            }
            // The sheet gets the same question the control is drawn under, so
            // the compact form does not lose it on the way to the options.
            ChoiceValueButton(title ?: label, options, selected, detail, pick)
        }
    }
}

/**
 * The type size at which every option's name survives its share of [width] as a
 * segmented button, or null when none of [styles] fits and the sheet has to
 * take the choice instead. Shared by [ChoiceControl] and [ChoiceSetting], which
 * has to know before it draws anything: the answer decides whether the row
 * carries the control underneath it or opens a sheet instead.
 *
 * More than one size because the alternative to a slightly smaller label is not
 * a bigger one, it is losing the segments altogether and putting three short
 * words behind a press. Three seven-letter names have about 57 dp of label lane
 * each on a 360 dp screen (see [SegmentFurniture] for where the rest goes), and
 * that is under `labelLarge` and over `labelMedium`.
 */
private fun <T> segmentStyle(
    options: List<Pair<T, String>>,
    width: Dp,
    measurer: TextMeasurer,
    styles: List<TextStyle>,
    density: Density,
): TextStyle? {
    if (width <= 0.dp || options.isEmpty()) return null
    val perSegment = with(density) { (width / options.size).toPx() }
    val furniture = with(density) { SegmentFurniture.toPx() }
    return styles.firstOrNull { style ->
        options.all { (_, label) ->
            measurer.measure(label, style, maxLines = 1).size.width + furniture <= perSegment
        }
    }
}

/**
 * The sizes [segmentStyle] tries, largest first. Remembered on the typography
 * rather than rebuilt, because it is a `remember` key at both call sites.
 */
@Composable
private fun segmentTypeTiers(): List<TextStyle> {
    val typography = MaterialTheme.typography
    return remember(typography) { listOf(typography.labelLarge, typography.labelMedium) }
}

/**
 * The compact form for a control with no row of its own to put a value on: the
 * current choice on a full-width button that opens the sheet.
 */
@Composable
private fun <T> ChoiceValueButton(
    title: String?,
    options: List<Pair<T, String>>,
    selected: T,
    detail: (@Composable (T) -> ChoiceDetail?)?,
    onChange: (T) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val current = options.firstOrNull { it.first == selected }?.second.orEmpty()
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
        ChoiceValueGlyph(choiceIcon(selected, detail))
        Text(current, maxLines = 1, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
    }
    if (open) {
        ChoiceSheet(
            title = title,
            subtitle = null,
            options = options,
            selected = selected,
            detail = detail,
            onPick = onChange,
            onDismiss = { open = false },
        )
    }
}

/**
 * Several yes/no choices that are one decision, drawn as one row of filter
 * chips: the six hold shortcuts read as one setting rather than six. [default]
 * draws the restore icon while the set differs from it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun <T> MultiChoiceSetting(
    @StringRes title: Int,
    subtitle: String? = null,
    info: String? = null,
    options: List<Pair<T, String>>,
    selected: Set<T>,
    icon: ImageVector? = SettingsTables[title],
    default: Set<T>? = null,
    onChange: (Set<T>) -> Unit,
) {
    val name = stringResource(title)
    HighlightableRow(name, title, info = info) {
        IconedRow(
            icon = icon,
            subtitle = subtitle,
            header = {
                ReflowingText(name, style = MaterialTheme.typography.bodyLarge)
                RowExtras(stack = false) {
                    if (info != null) InfoButton(name, info)
                    SettingLinkButton(name)
                    ResetSetting(name, default != null && selected != default, possible = default != null) {
                        default?.let(onChange)
                    }
                }
                Spacer(Modifier.weight(1f))
            },
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                for ((option, label) in options) {
                    val on = option in selected
                    FilterChip(
                        selected = on,
                        onClick = { onChange(if (on) selected - option else selected + option) },
                        label = { Text(label) },
                        leadingIcon = if (on) {
                            {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}
