package com.wasimaster.wmsuite.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.wasimaster.wmkeyboard.core.icons.symbols.outlined.Delete
import com.wasimaster.wmkeyboard.core.icons.symbols.outlined.DragHandle
import kotlin.math.roundToInt

/** Row height inside [ReorderableColumn] — fixed, so drags map to index shifts. */
private val ReorderRowHeight = 52.dp

/** How far a row settles into its slot after a swap or a drop — a short spring. */
private val ReorderSettleSpring = spring<Float>(stiffness = Spring.StiffnessMediumLow)

/**
 * A list the user drags into order, in place. Rows carry a handle on the
 * right; dragging one past half the next row's height moves it a slot, and
 * every row it passes settles into the slot it gave up. The caller hears the
 * whole new order once, through [onReorder], when the row is dropped.
 *
 * The order on screen is this column's own copy for the length of a drag,
 * moved synchronously under the finger. Routing every swap through the caller
 * was the jitter: the store echoed the new list a frame or more later, and
 * until it did the slot the dragged row had moved into still held its
 * neighbour — which drew translated under the finger for that frame, then
 * snapped back. A fast drag computed its next swap from a list the store had
 * not caught up with either, so swaps were dropped or applied twice. Outside a
 * drag the copy follows [items].
 *
 * Deliberately not a LazyColumn: every row has to stay composed for a drag
 * to pass it, and these lists are short enough that laying them all out is
 * free. Rows are keyed by item, so a row keeps its node — and the gesture
 * that is driving it — as it moves.
 *
 * [onDelete] adds a bin to each row's left, for the lists where the same
 * pencil that opens the reorder is also the way out of the list — removing a
 * language, say. Null (every other caller) draws no bin, and the last item is
 * never removable unless [keepLast] says otherwise: a bar's rows have to keep
 * one, while a list of snippet folders is perfectly good empty.
 *
 * [content] replaces the one-line label with the caller's own row body (the
 * bar order draws a preview of each row); [label] still names the row for
 * the handle's description. A taller body passes its [rowHeight].
 *
 * Rows arrive and leave by moving, not in one frame. A row new to [items]
 * opens into its slot, growing from nothing to its height as it fades in; a
 * row gone from [items] closes out of the slot it had, fading as it shrinks
 * and taking no more taps or drags on the way; the rows under either slide
 * by however much height it has at the time. It is the motion a keyed
 * [SettingsGroup] row has (`GroupRows`, same timing and the same limit): the
 * first pass, a different list altogether (more than a handful of rows at
 * once) and everything under [LocalReduceMotion] land in one frame instead.
 * A closing row keeps the name [label] gave it while it was listed, so
 * [label] is only ever asked about items in [items]; [content], though, is
 * still drawn for it for those few frames, and must not assume its item is
 * still in [items].
 */
@Composable
public fun <T> ReorderableColumn(
    items: List<T>,
    label: (T) -> String,
    onReorder: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    onDelete: ((T) -> Unit)? = null,
    keepLast: Boolean = true,
    rowHeight: Dp = ReorderRowHeight,
    content: (@Composable RowScope.(T) -> Unit)? = null,
) {
    var working by remember { mutableStateOf(items) }
    // -1 = nothing being dragged. The index is in [working].
    var dragIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    // Per row, how far it still has to travel to the slot it is drawn in: set
    // in the same snapshot as the move, so the frame that shows the new order
    // also shows every displaced row exactly where it was, and played out to
    // zero by the row's own [ReorderRow] spring.
    val settling = remember { mutableStateMapOf<Any, Float>() }
    // Adopt the caller's list whenever it changes while nothing is in flight;
    // mid-drag the finger's order wins and reaches the caller on the drop.
    LaunchedEffect(items) {
        if (dragIndex < 0) working = items
    }
    val rowPx = with(LocalDensity.current) { rowHeight.toPx() }
    // The drag gesture is keyed once per row and outlives every recomposition,
    // so it reads the caller's lambda through the latest snapshot rather than
    // closing over the one it was composed with.
    val currentItems by rememberUpdatedState(items)
    val currentOnReorder by rememberUpdatedState(onReorder)
    val finishDrag = {
        val from = dragIndex
        if (from in working.indices) {
            // At most half a slot from home: let the row settle rather than snap.
            if (dragOffset != 0f) {
                val key = reorderKey(working, from)
                settling[key] = (settling[key] ?: 0f) + dragOffset
            }
            if (working != currentItems) currentOnReorder(working)
        }
        dragIndex = -1
        dragOffset = 0f
    }
    val still = LocalReduceMotion.current
    val ledger = remember { ReorderLedger<T>() }
    // Read here so a row finishing its way out redraws the column without it.
    @Suppress("UNUSED_EXPRESSION")
    ledger.departures
    val pass = planReorderRows(working, label, ledger, motion = !still)
    SideEffect {
        ledger.settled = true
        ledger.drawn = pass.rows
        ledger.gone.retainAll { k -> pass.rows.any { it.leaving && it.key == k } }
    }
    Column(modifier = modifier) {
        pass.rows.forEach { row ->
            val rowKey = row.key
            val item = row.item
            val leaving = row.leaving
            key(rowKey) {
                // -1 once the row is on its way out: it has no place in the
                // order any more, so a drag cannot pick it up.
                val liveIndex by rememberUpdatedState(row.index)
                // Decided once, in the pass that brings the row in, so a row
                // that is going to open draws its first frame closed rather
                // than flashing in at full height first.
                val presence = remember { RowPresence(shown = !pass.animateArrivals) }
                val moving = leaving || presence.partial
                if (moving) {
                    LaunchedEffect(leaving) {
                        if (leaving) {
                            presence.amount.animateTo(0f, RowPresenceSpec)
                            ledger.depart(rowKey)
                        } else {
                            // Arriving, or put back (an undo) while still closing.
                            presence.amount.animateTo(1f, RowPresenceSpec)
                        }
                    }
                }
                ReorderRow(
                    dragging = !leaving && row.index == dragIndex,
                    dragOffset = dragOffset,
                    settleTo = settling[rowKey] ?: 0f,
                    onSettled = { settling.remove(rowKey) },
                    rowHeight = rowHeight,
                    position = row.number,
                    label = row.name,
                    onDelete = onDelete?.let { delete -> { delete(item) } },
                    // A closing row keeps the bin it was deleted with: flipping
                    // it would rebuild the row halfway out.
                    deletable = leaving || !keepLast || working.size > 1,
                    modifier = if (moving) Modifier.rowPresence(presence, leaving) else Modifier,
                    handle = Modifier.pointerInput(rowPx) {
                        detectDragGestures(
                            onDragStart = {
                                dragIndex = liveIndex
                                dragOffset = 0f
                            },
                            onDragEnd = finishDrag,
                            onDragCancel = finishDrag,
                        ) { change, drag ->
                            change.consume()
                            val list = working
                            val from = dragIndex
                            if (from !in list.indices) return@detectDragGestures
                            // Clamped to the list: past either end the row
                            // stops at the edge instead of leaving the column.
                            dragOffset = (dragOffset + drag.y)
                                .coerceIn(-from * rowPx, (list.lastIndex - from) * rowPx)
                            val to = (from + (dragOffset / rowPx).roundToInt()).coerceIn(list.indices)
                            if (to == from) return@detectDragGestures
                            // A fast drag can cross several rows in one event:
                            // each of them shifts one slot the other way and
                            // is told how far it has to settle.
                            val passed = if (to > from) (from + 1)..to else to until from
                            val back = if (to > from) rowPx else -rowPx
                            for (i in passed) {
                                val key = reorderKey(list, i)
                                settling[key] = (settling[key] ?: 0f) + back
                            }
                            working = list.toMutableList().apply { add(to, removeAt(from)) }
                            dragIndex = to
                            // Keep the offset relative to the row's new home,
                            // or the row would jump a full slot.
                            dragOffset -= (to - from) * rowPx
                        }
                    },
                ) {
                    if (content != null) {
                        content(item)
                    } else {
                        Text(
                            row.name,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * The key a row keeps as it moves: the item, plus which occurrence of it
 * this is, so a list with two equal entries still keys every row uniquely.
 */
private fun <T> reorderKey(list: List<T>, index: Int): Any {
    val item = list[index]
    var nth = 0
    for (i in 0 until index) if (list[i] == item) nth++
    return item to nth
}

/** [reorderKey] for every row of [list] in one walk. */
private fun <T> reorderKeys(list: List<T>): List<Any> {
    val seen = HashMap<T, Int>()
    return list.map { item ->
        val nth = seen[item] ?: 0
        seen[item] = nth + 1
        item to nth
    }
}

/**
 * One row as a [ReorderableColumn] draws it on this pass. [index] is its
 * place in the order, or -1 for a row that has left [ReorderableColumn]'s
 * items and is closing out of the slot it had; [number] and [name] are what
 * it shows, which a closing row keeps from its last pass in the list.
 */
private class ShownRow<T>(val key: Any, val item: T, val index: Int, val number: Int, val name: String) {
    val leaving: Boolean get() = index < 0
}

/** What a pass draws, and whether a row new to it opens or simply appears. */
private class ReorderPass<T>(val rows: List<ShownRow<T>>, val animateArrivals: Boolean)

/**
 * What the column remembers between passes, as a group's ledger does in
 * SettingsGroupMotion.kt: what it drew, and which of the closing rows have
 * finished.
 */
private class ReorderLedger<T> {
    /** False until the column has drawn once. Rows there from the first pass arrive in place. */
    var settled = false

    /** The last committed pass, closing rows included. Written in a SideEffect, never in composition. */
    var drawn: List<ShownRow<T>> = emptyList()

    /** Keys whose way out has finished, to be left out of the next pass. */
    val gone = HashSet<Any>()

    /** Bumped when a row finishes leaving, so the column draws once more without it. */
    var departures by mutableIntStateOf(0)

    fun depart(key: Any) {
        gone += key
        departures++
    }
}

/**
 * Lines this pass's rows up with the last one's: [working] as it stands, plus
 * each row drawn last time that is not in it now, kept after whichever of its
 * old neighbours is still here so it closes where it was.
 *
 * A drag only ever moves rows, so it never brings one in or sends one out;
 * these are the caller's own edits, adopted between drags.
 */
private fun <T> planReorderRows(
    working: List<T>,
    label: (T) -> String,
    ledger: ReorderLedger<T>,
    motion: Boolean,
): ReorderPass<T> {
    val keys = reorderKeys(working)
    val current = working.mapIndexed { i, item -> ShownRow(keys[i], item, i, i + 1, label(item)) }
    if (!motion || !ledger.settled) return ReorderPass(current, animateArrivals = false)
    val before = ledger.drawn
    val keysNow = keys.toHashSet()
    val keysBefore = before.mapNotNullTo(HashSet()) { row -> row.key.takeIf { !row.leaving } }
    val animateArrivals = keys.count { it !in keysBefore } <= RowMotionBulkLimit
    val departing = before.filter { it.key !in keysNow && it.key !in ledger.gone }
    // Rows already on their way out finish going; it is the fresh departures
    // that decide whether this pass is an edit or a different list.
    if (departing.isEmpty() || departing.count { !it.leaving } > RowMotionBulkLimit) {
        return ReorderPass(current, animateArrivals)
    }
    val out = ArrayList<ShownRow<T>>(current.size + departing.size)
    out.addAll(current)
    // Where in [out] the last row walked past landed.
    var anchor = -1
    for (row in before) {
        when (row.key) {
            in keysNow -> anchor = out.indexOfFirst { !it.leaving && it.key == row.key }
            in ledger.gone -> Unit
            else -> {
                out.add(anchor + 1, if (row.leaving) row else ShownRow(row.key, row.item, -1, row.number, row.name))
                anchor++
            }
        }
    }
    return ReorderPass(out, animateArrivals)
}

/**
 * One row of a [ReorderableColumn]: position, the caller's body, the
 * optional bin and the handle. While dragged it rides above its neighbours
 * on a raised card at [dragOffset]; otherwise it draws at [settleTo] and
 * springs to zero, so a row that just changed slots slides into it rather
 * than appearing there. [modifier] carries the row's opening or closing,
 * when it has one.
 */
@Composable
private fun ReorderRow(
    dragging: Boolean,
    dragOffset: Float,
    settleTo: Float,
    onSettled: () -> Unit,
    rowHeight: Dp,
    position: Int,
    label: String,
    onDelete: (() -> Unit)?,
    deletable: Boolean,
    handle: Modifier,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val settle = remember { Animatable(0f) }
    // The part of [settleTo] already folded into the spring. Until the spring
    // has started, the row draws the raw offset, so the frame that shows the
    // new order never shows a jump; once it runs, the spring's value is the
    // whole story, and a further shift mid-spring adds on from wherever it is.
    var applied by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(settleTo) {
        if (settleTo == 0f) {
            applied = 0f
            return@LaunchedEffect
        }
        settle.snapTo(settle.value + (settleTo - applied))
        applied = settleTo
        settle.animateTo(0f, ReorderSettleSpring)
        onSettled()
        applied = 0f
    }
    val rowShape = MaterialTheme.shapes.medium
    val lift = MaterialTheme.colorScheme.surfaceContainerHigh
    val removeDesc = stringResource(R.string.rows_reorder_remove_desc, label)
    // The bin, a swipe away as well as at the row's end. Enabled on what the
    // row is rather than on whether it is being dragged: flipping it mid-drag
    // would rebuild the row and drop the grip's gesture under the finger.
    SwipeActionsRow(
        actions = if (onDelete != null) listOf(deleteSwipeAction(removeDesc, onDelete)) else emptyList(),
        enabled = deletable,
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight)
            // The dragged row rides above its neighbours.
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
                translationY = if (dragging) dragOffset else settle.value + (settleTo - applied)
                shadowElevation = if (dragging) 6.dp.toPx() else 0f
                shape = rowShape
            }
            .background(if (dragging) lift else Color.Transparent, rowShape),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight),
        ) {
            Text(
                stringResource(R.string.rows_reorder_position_label, position),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp),
            )
            content()
            if (onDelete != null) {
                IconButton(onClick = onDelete, enabled = deletable) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.rows_reorder_remove_desc, label),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Icon(
                Icons.Outlined.DragHandle,
                contentDescription = stringResource(R.string.rows_reorder_handle_desc, label),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(28.dp)
                    .then(handle),
            )
        }
    }
}

