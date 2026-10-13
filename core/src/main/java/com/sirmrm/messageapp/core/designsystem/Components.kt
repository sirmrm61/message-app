package com.sirmrm.messageapp.core.designsystem

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Semantic colors not covered by the Material scheme. */
val appColors: AppExtendedColors
    @Composable get() = LocalAppColors.current

/* ---------------------------------- layout ---------------------------------- */

/** Thin separator, iOS hairline style. */
@Composable
fun Hairline(modifier: Modifier = Modifier, indent: Dp = 16.dp) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(start = indent)
            .height(0.5.dp)
            .background(appColors.separator),
    )
}

/** Small gray section label above a grouped list, iOS settings style. */
@Composable
fun AppSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = appColors.secondaryLabel,
        modifier = modifier.padding(start = 32.dp, top = 18.dp, bottom = 6.dp, end = 16.dp),
    )
}

/**
 * Inset grouped list container: white rounded card on gray background with
 * hairline separators between rows.
 */
@Composable
fun AppListGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(appColors.card),
        content = content,
    )
}

/** One row inside an [AppListGroup] — icon tile, label(s), accessory. */
@Composable
fun AppListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val base = modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 48.dp)
    Row(
        if (onClick != null) base.pressable(onClick = onClick) else base,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Spacer(Modifier.width(12.dp))
            leading()
        }
        Spacer(Modifier.width(if (leading != null) 10.dp else 16.dp))
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = appColors.secondaryLabel,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = trailing,
            )
            Spacer(Modifier.width(12.dp))
        } else {
            Spacer(Modifier.width(16.dp))
        }
    }
}

/** iOS-style chevron accessory. */
@Composable
fun AppChevron() {
    Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = appColors.tertiaryLabel,
        modifier = Modifier.size(18.dp),
    )
}

/** Rounded-square icon tile like iOS settings icons. */
@Composable
fun AppIconTile(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(tint),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * 0.62f))
    }
}

/* ------------------------------- segmented -------------------------------- */

/**
 * iOS segmented control: gray pill track, white floating indicator.
 */
@Composable
fun <T> AppSegmentedControl(
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val index = items.indexOf(selected).coerceAtLeast(0)
    BoxWithSegmentedTrack(modifier, items.size, index) {
        items.forEachIndexed { i, item ->
            val isSel = i == index
            val color by animateFloatAsState(if (isSel) 1f else 0f, AppMotion.Fast, label = "seg")
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pressable { onSelect(item) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(item),
                    style = MaterialTheme.typography.labelLarge,
                    color = androidx.compose.ui.graphics.lerp(
                        appColors.secondaryLabel,
                        MaterialTheme.colorScheme.onSurface,
                        color,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun BoxWithSegmentedTrack(
    modifier: Modifier,
    count: Int,
    index: Int,
    content: @Composable RowScope.() -> Unit,
) {
    val density = LocalDensity.current
    var widthPx by remember { mutableStateOf(0) }
    val segWidth = if (count > 0 && widthPx > 0) widthPx / count else 0
    val offsetX by animateFloatAsState((segWidth * index).toFloat(), AppMotion.SpringSoft, label = "segX")
    Box(
        modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(appColors.fill)
            .onSizeChanged { widthPx = it.width },
    ) {
        if (segWidth > 0) {
            Box(
                Modifier
                    .offset { IntOffset(offsetX.toInt() + with(density) { 2.dp.roundToPx() }, with(density) { 2.dp.roundToPx() }) }
                    .width(with(density) { (segWidth - with(density) { 4.dp.roundToPx() }).toDp() })
                    .height(30.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(appColors.card),
            )
        }
        Row(Modifier.fillMaxSize().padding(2.dp), content = content)
    }
}

/* --------------------------------- inputs --------------------------------- */

/** Flat rounded search field — no border, gray fill. */
@Composable
fun AppSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(appColors.fill)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = appColors.secondaryLabel,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(hint, style = MaterialTheme.typography.bodyMedium, color = appColors.secondaryLabel)
                    }
                    inner()
                }
            },
        )
        trailing?.let { Row(verticalAlignment = Alignment.CenterVertically, content = it) }
    }
}

/* --------------------------------- buttons -------------------------------- */

@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
        ),
        modifier = modifier.height(50.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

/** iOS-style capsule switch colors — green when on. */
@Composable
fun appSwitchColors() = SwitchDefaults.colors(
    checkedTrackColor = appColors.deposit,
    checkedThumbColor = Color.White,
    uncheckedTrackColor = appColors.fill,
    uncheckedThumbColor = Color.White,
    uncheckedBorderColor = Color.Transparent,
    checkedBorderColor = Color.Transparent,
)

@Composable
fun AppSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Switch(checked = checked, onCheckedChange = onCheckedChange, colors = appSwitchColors(), modifier = modifier)
}

/* --------------------------------- misc ----------------------------------- */

/** Small count pill (unread / totals). */
@Composable
fun AppBadge(text: String, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    Box(
        modifier
            .defaultMinSize(minWidth = 20.dp)
            .height(20.dp)
            .clip(CircleShape)
            .background(tint)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 11.sp)
    }
}

/** Unread blue dot. */
@Composable
fun UnreadDot(modifier: Modifier = Modifier) {
    Box(modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
}

/** Neutral-tinted initials avatar; tint is derived from the name hash. */
@Composable
fun AppAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    photo: (@Composable () -> Unit)? = null,
) {
    val tints = listOf(
        Color(0xFF8E8E93), Color(0xFF5E5CE6), Color(0xFF34C759),
        Color(0xFFFF9F0A), Color(0xFF64D2FF), Color(0xFFBF5AF2),
        Color(0xFFFF375F), Color(0xFF30B0C7),
    )
    val tint = tints[(name.hashCode().and(Int.MAX_VALUE)) % tints.size]
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) photo() else Text(
            name.firstOrNull()?.toString() ?: "#",
            style = MaterialTheme.typography.titleMedium,
            color = tint,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Empty state: line icon, title, description, optional action. */
@Composable
fun AppEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = appColors.tertiaryLabel, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = appColors.secondaryLabel,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            AppPrimaryButton(actionLabel, onAction)
        }
    }
}

/** Subtle skeleton shimmer row. */
@Composable
fun AppShimmerRow(modifier: Modifier = Modifier) {
    val alpha by animateFloatAsState(0.5f, AppMotion.Medium, label = "shim")
    Row(modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(appColors.fill.copy(alpha = alpha)))
        Spacer(Modifier.width(12.dp))
        Column {
            Box(Modifier.width(140.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).background(appColors.fill.copy(alpha = alpha)))
            Spacer(Modifier.height(6.dp))
            Box(Modifier.width(220.dp).height(11.dp).clip(RoundedCornerShape(4.dp)).background(appColors.fill.copy(alpha = alpha * 0.7f)))
        }
    }
}

/** Bottom sheet with iOS look: grabber + rounded top corners. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = appColors.card,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 8.dp)
                    .size(width = 36.dp, height = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(appColors.fill),
            )
        },
        content = content,
    )
}
