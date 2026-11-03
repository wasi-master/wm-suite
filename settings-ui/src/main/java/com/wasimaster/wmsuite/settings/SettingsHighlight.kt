package com.wasimaster.wmsuite.settings

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.wasimaster.wmkeyboard.core.ui.DisabledReason

/** The border a row an update brought wears; see [SettingsTables.isNewRow]. */
private val NewRowGold = Color(0xFFD4A62A)

/**
 * The setting the user picked out of search, remembered just long enough for
 * its destination screen to compose and flash it.
 *
 * A plain object rather than a nav argument because the row composables that
 * do the flashing sit far below the NavHost and are shared by every screen —
 * threading an argument down to all ~200 call sites would mean touching all
 * of them, while this needs only the six row helpers to read it.
 */
public object SettingsHighlight {
    /**
     * The string resource of the row to flash, or 0 when nothing is pending.
     *
     * A resource id rather than the drawn words: the index and the addon
     * screens both name a row before the row is composed, and the words differ
     * in every language while the id does not.
     */
    @get:StringRes
    var target: Int by mutableIntStateOf(0)
        private set

    /**
     * The list entries to flash, named by whatever id the screen holding them
     * uses — a theme or layout id, a pack id, a dictionary file path.
     *
     * A resource id cannot name one of these: they are the user's own things,
     * and no two installations have the same set. A row's title cannot either,
     * because two installed addons may well share a name. So an addon's Use
     * button hands over the local handle its install produced
     * ((an addon install's local reference, say)) and the
     * owning screen matches on it.
     *
     * A set, not one id: a snippet pack installs several snippets at once and
     * all of them are what the user just downloaded.
     */
    var targetItems: Set<String> by mutableStateOf(emptySet())
        private set

    /** A row named by its words, for an app whose rows have no string resources. */
    var targetTitle: String? by mutableStateOf(null)
        private set

    /**
     * Bumped on every request.
     *
     * A screen being torn down clears any highlight it is leaving behind, so
     * that one which found no matching row doesn't flash something unrelated
     * later. But a settings screen can also *arm* a highlight on its way out —
     * an addon's Use button does exactly that — and then it would wipe its own
     * request a frame after making it. Comparing this against the value the
     * screen saw when it opened tells the two apart.
     */
    var serial: Int by mutableIntStateOf(0)
        private set

    /**
     * False until one of the flashing rows has scrolled itself into view.
     *
     * Several rows can match one request, and every one of them asking to be
     * brought into view would leave whichever was placed last on screen. The
     * first to claim it wins, which in a top-to-bottom composition is the
     * topmost — where the eye already is.
     */
    private var scrollClaimed = false

    fun requestTitle(title: String) {
        target = 0
        targetTitle = title
        targetItems = emptySet()
        scrollClaimed = false
        awaitingArrival = true
        serial++
    }

    fun request(@StringRes titleRes: Int) {
        targetTitle = null
        target = titleRes
        targetItems = emptySet()
        scrollClaimed = false
        awaitingArrival = true
        serial++
    }

    /**
     * Flashes the entries named by [keys], falling back to the row [titleRes]
     * names when the screen turns out not to list any of them.
     */
    fun requestItems(keys: Collection<String>, @StringRes titleRes: Int = 0) {
        target = titleRes
        targetItems = keys.filter { it.isNotBlank() }.toSet()
        scrollClaimed = false
        awaitingArrival = true
        serial++
    }

    /**
     * For a link to a whole screen: nothing to flash, but the screen opens with
     * every row the settings depth would hide; see [DepthReveal].
     */
    fun requestScreen() {
        awaitingArrival = true
        serial++
    }

    /** True while a row or entry is waiting to be flashed. */
    val pending: Boolean get() = target != 0 || targetItems.isNotEmpty()

    /**
     * Set by a request and taken by the screen it sent the user to, so one
     * arrival opens one screen's hidden rows and not those of the next screen
     * opened while the flash is still running.
     */
    private var awaitingArrival = false

    /** True once per request, for the screen the user landed on. */
    fun takeArrival(): Boolean {
        val arrived = awaitingArrival
        awaitingArrival = false
        return arrived
    }

    /** True for the one row that gets to scroll itself into view. */
    fun claimScroll(): Boolean {
        if (scrollClaimed) return false
        scrollClaimed = true
        return true
    }

    /** True once some row has scrolled itself into view for this request. */
    fun scrollTaken(): Boolean = scrollClaimed

    fun clear() {
        target = 0
        targetTitle = null
        targetItems = emptySet()
        awaitingArrival = false
    }

    /** Clears only if nothing new was requested since [serialAtEntry]. */
    fun clearIfUnchanged(serialAtEntry: Int) {
        if (serial == serialAtEntry) clear()
    }

    /**
     * Keeps a highlight standing instead of pulsing it once, untinted, and
     * names it [HIGHLIGHT_TAG] for a test to find.
     *
     * Only the docs screenshot run sets this (app/src/docShots): it marks the
     * setting a page is about, and a pulse that has already faded by the time
     * the frame is captured marks nothing. Never set in the app.
     */
    var hold: Boolean = false

    /** The test tag a held highlight carries; see [hold]. */
    const val HIGHLIGHT_TAG = "settingHighlight"
}

/**
 * Where a list screen should be standing the next time it is composed, armed by
 * the screen itself on its way out.
 *
 * Navigation keeps a route's saveable state while it sits on the back stack, so
 * coming back from an addon's page or a language's page *usually* restores the
 * scroll position on its own. It stops being reliable the moment the content
 * measures shorter on the first frame than it did on the last — a catalogue
 * whose cards grow as their screenshots decode is exactly that shape, and a
 * scroll offset past the end of a half-built page is clamped to what fits.
 * Naming the entry instead of an offset survives it.
 *
 * Not [SettingsHighlight]: coming back to where you were is not the same event
 * as being sent somewhere, and it must not flash. It is also keyed by route, so
 * an anchor left by one screen can never be consumed by another.
 *
 * A plain map rather than Compose state — the only reader is a `remember` at
 * screen entry, and nothing re-reads it as it changes.
 */
public object ReturnAnchor {
    private val pending = HashMap<String, String>()

    /** Called as the screen navigates away, naming the entry being opened. */
    fun arm(route: String, key: String) {
        if (key.isNotBlank()) pending[route] = key
    }

    /** Reads [route]'s anchor and forgets it, so it is used exactly once. */
    fun take(route: String): String? = pending.remove(route)
}

/**
 * Wraps a settings row so it scrolls itself into view and pulses once when it
 * is the setting the user searched for.
 *
 * Pass [highlightKey], the string resource of the row's own name, and the match
 * is on the resource: exact, and the same in every language. A row that only
 * has its drawn [title] is matched on the words instead, against the target id
 * resolved through the same resources, which is as unique as a title is within
 * one screen — the only scope where two rows are ever composed at the same
 * time.
 *
 * A row with neither is a row nothing can match on. It still gets the wrapper,
 * so that a group which names itself only once it has content ("Repositories")
 * keeps its children's state when the name appears. Branching on the title
 * around [content] instead would move the slot and discard everything inside.
 */
@Composable
public fun HighlightableRow(
    title: String?,
    @StringRes highlightKey: Int = 0,
    /**
     * True for a whole section rather than one row. A group and a row inside it
     * can both answer to the same name, and then the row is the better answer —
     * see [HighlightFrame].
     */
    coarse: Boolean = false,
    /**
     * Why the row is greyed out, or null while it is usable: the long press
     * then opens with it (see [settingLinkHold]). The row's own `enabled` is
     * what decides, so callers pass this only while it is off.
     */
    disabledReason: DisabledReason? = null,
    /**
     * The row's explanation, the one its "?" opens. At the Simple depth the
     * "?" is not drawn ([RowExtras]), and the long press offers it as "What is
     * this?" instead; at any other depth the button is on the row and this
     * is not read.
     */
    info: String? = null,
    /** The level the depth would need to show this row, when it is here only for a [DepthReveal]. */
    revealedAt: SettingLevel? = LocalRevealedLevel.current,
    /** For the row's frame, around everything [content] draws. */
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val target = SettingsHighlight.target
    val named = SettingsHighlight.targetTitle
    val requested = when {
        named != null -> title == named
        target == 0 -> false
        highlightKey != 0 -> highlightKey == target
        title == null -> false
        else -> title == stringResource(target)
    }
    // Sent here, to a row the depth would have hidden: the notice on the screen
    // names the depth that would keep it.
    if (requested && revealedAt != null) {
        val depthReveal = LocalDepthReveal.current
        SideEffect { depthReveal?.landedOn = revealedAt }
    }
    // The same two names are what a row's link is made of (#646), so the row
    // publishes them for its link button, and a plain row takes the long
    // press here: its link and its place in favourites (#898). A group's
    // heading takes its own, in [SettingsGroup]: the whole group would
    // otherwise answer a hold on any row inside it.
    CompositionLocalProvider(LocalRowLinkKey provides RowLinkKey(title, highlightKey)) {
        // Also with the app's "?" put away: the hold is then the way to the explanation.
        val bare = LocalSettingsDepth.current.depth == SettingsDepth.SIMPLE || !LocalInfoButtons.current
        val whatIsThis = if (info != null && bare) remember(title, info) { RowInfo(title.orEmpty(), info) } else null
        val hold = if (coarse) null else rememberRowHold(disabledReason, whatIsThis)
        // A row an update brought wears a gold border until the user puts the
        // new settings bar away (#970).
        val isNew = !coarse && SettingsTables.isNewRow(title, highlightKey)
        HighlightFrame(
            requested = requested,
            rank = if (coarse) HighlightRank.SECTION else HighlightRank.ROW,
            modifier = modifier
                .settingLinkHold(hold, disabledReason, whatIsThis)
                .then(if (isNew) Modifier.border(1.5.dp, NewRowGold, RoundedCornerShape(12.dp)) else Modifier),
        ) {
            content()
            RowHoldMenu(hold)
        }
    }
}

/**
 * How precisely a wrapper names what the user asked for. The narrowest match on
 * screen is the one that scrolls and pulses; see [HighlightFrame].
 */
private enum class HighlightRank(val settleMillis: Long) {
    /** One entry of a list, matched on the screen's own id for it. */
    ITEM(80),

    /** One row, matched by name. */
    ROW(200),

    /** A whole named section. */
    SECTION(320),
}

/**
 * [HighlightableRow] for one entry of a list the user owns — an installed theme,
 * layout, pack or word list — matched on the screen's own id for it rather than
 * on a string resource.
 *
 * This is the other half of an addon's Use button: the section anchor lands the
 * user on the right screen, and this puts them in front of the thing they just
 * installed, which on a screen listing thirty fonts is the whole of the answer.
 */
@Composable
public fun HighlightableItem(key: String, content: @Composable () -> Unit) {
    HighlightFrame(
        requested = key.isNotEmpty() && key in SettingsHighlight.targetItems,
        rank = HighlightRank.ITEM,
        content = content,
    )
}

/**
 * [HighlightableItem] for a card that stands for several ids at once — a theme
 * family's card answers for every variant in it, so a deep link naming a
 * variant still lands somewhere visible.
 */
@Composable
public fun HighlightableItem(keys: List<String>, content: @Composable () -> Unit) {
    HighlightFrame(
        requested = keys.any { it.isNotEmpty() && it in SettingsHighlight.targetItems },
        rank = HighlightRank.ITEM,
        content = content,
    )
}

/**
 * Scrolls its content into view once, without flashing it.
 *
 * For "you were already here": the current selection in a picker, the entry a
 * screen was opened from. A pulse says *this is the thing you asked for*, and
 * spending it every time a dialog opens on its own default would leave it
 * meaning nothing when a search result or an addon actually needs it.
 */
@Composable
public fun ScrollAnchor(active: Boolean, content: @Composable () -> Unit) {
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        // One frame's grace so the row has been placed before we scroll to it.
        delay(80)
        requester.bringIntoView()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(requester),
    ) {
        content()
    }
}

/**
 * The scroll-and-pulse itself, shared by the highlight wrappers.
 *
 * One request routinely matches at more than one size: an addon names both the
 * section that owns its kind and the entry it installed, and a group can carry
 * the same name as a row inside it. The narrowest match is the right answer —
 * landing on the font you installed beats landing on the words "Installed
 * fonts" — so each rank waits a little longer than the one below it and stands
 * down once something narrower has taken the screen. Flashing all of them would
 * pulse twice and scroll to whichever happened to be placed last.
 */
@Composable
private fun HighlightFrame(
    requested: Boolean,
    rank: HighlightRank,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var flashing by remember { mutableStateOf(false) }
    val requester = remember { BringIntoViewRequester() }
    val scroll = LocalScreenScroll.current
    // Where the row sits, tracked only while it is being held in view. Its
    // own effect, since the flash's ends when the request is consumed.
    var holds by remember { mutableIntStateOf(0) }
    var holding by remember { mutableStateOf(false) }
    var top by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(holds) {
        if (holds > 0) holdInView(requester, scroll, { holding = it }) { top }
    }
    val color by animateColorAsState(
        targetValue = if (flashing && !SettingsHighlight.hold) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else Color.Transparent,
        animationSpec = tween(durationMillis = 320),
        label = "settingHighlight",
    )
    LaunchedEffect(requested) {
        // Also the path out of a flash that the *first* matching row ended by
        // clearing the request: without it this one would keep its tint for as
        // long as the screen lives.
        if (!requested) {
            flashing = false
            return@LaunchedEffect
        }
        // A frame's grace so the row has been placed before we scroll to it,
        // plus however long this rank owes the narrower ones.
        delay(rank.settleMillis)
        if (rank != HighlightRank.ITEM && SettingsHighlight.scrollTaken()) return@LaunchedEffect
        if (SettingsHighlight.claimScroll()) {
            requester.bringIntoView()
            holds++
        }
        flashing = true
        if (SettingsHighlight.hold) return@LaunchedEffect
        delay(1400)
        flashing = false
        // Consumed: a later visit to the same screen must not flash again.
        SettingsHighlight.clear()
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(requester)
            .then(if (holding) Modifier.onGloballyPositioned { top = it.positionInRoot().y } else Modifier)
            .then(if (flashing && SettingsHighlight.hold) Modifier.testTag(SettingsHighlight.HIGHLIGHT_TAG) else Modifier)
            .background(color),
    ) {
        content()
    }
}

/**
 * Keeps a row the user was sent to on screen while the page settles under it
 * (#904). The first scroll lands before the page is finished: groups above
 * still waiting their turn to compose stand in as skeletons of a guessed
 * height, and rows that appear once a setting has loaded push everything
 * down, so a row scrolled to at once was carried off screen a moment later.
 * For [HighlightHoldMs], every move of the row brings it back, until the user
 * drags the page themselves. [setHolding] switches the position tracking on
 * for exactly that long; [top] reads where it last saw the row.
 */
private suspend fun holdInView(
    requester: BringIntoViewRequester,
    scroll: ScrollState?,
    setHolding: (Boolean) -> Unit,
    top: () -> Float,
) {
    setHolding(true)
    try {
        withTimeoutOrNull(HighlightHoldMs) {
            coroutineScope {
                val follow = launch {
                    snapshotFlow(top).drop(1).collect { requester.bringIntoView() }
                }
                // The user's own drag is where they want to be.
                scroll?.interactionSource?.interactions?.first { it is DragInteraction.Start }
                    ?: awaitCancellation()
                follow.cancel()
            }
        }
    } finally {
        setHolding(false)
    }
}

/** How long a row the user was sent to is held in view; see [holdInView]. */
private const val HighlightHoldMs = 3_000L
