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
    Column(modifier = modifier) {
        working.forEachIndexed { index, item ->
            val rowKey = reorderKey(working, index)
            key(rowKey) {
                val liveIndex by rememberUpdatedState(index)
                ReorderRow(
                    dragging = index == dragIndex,
                    dragOffset = dragOffset,
                    settleTo = settling[rowKey] ?: 0f,
                    onSettled = { settling.remove(rowKey) },
                    rowHeight = rowHeight,
                    position = index + 1,
                    label = label(item),
                    onDelete = onDelete?.let { delete -> { delete(item) } },
                    deletable = !keepLast || working.size > 1,
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
                            label(item),
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

/**
 * One row of a [ReorderableColumn]: position, the caller's body, the
 * optional bin and the handle. While dragged it rides above its neighbours
 * on a raised card at [dragOffset]; otherwise it draws at [settleTo] and
 * springs to zero, so a row that just changed slots slides into it rather
 * than appearing there.
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
        modifier = Modifier
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

