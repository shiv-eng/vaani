package com.quietstack.vaani.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.quietstack.vaani.ui.theme.VaaniTheme

private val PressSpring = spring<Float>(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)
// Real, measured clearance between the FAB's bottom edge and the pill's
// bottom edge (uiautomator bounds on the test device, collapsed state).
private val FAB_BOTTOM_CLEARANCE = (-70).dp
private val FAB_END_INSET = (-6).dp

enum class VaaniTab(val label: String) {
    Home("Home"), Library("Library"), Search("Search"), About("About"),
}

/**
 * The floating pill nav + record FAB (README "Navigation shell"): surf
 * container, 26dp radius, active tab is a priDim pill, FAB enters with mpop
 * (scale .7→1.06→1) and sits 80px above the bar.
 *
 * The FAB expands into two labeled actions (record / import an existing
 * file) rather than jumping straight to recording — real users wanted an
 * obvious way to bring in audio they already have, not just capture new
 * audio, without hunting for it inside Library.
 */
@Composable
fun VaaniBottomBar(
    current: VaaniTab,
    onTabSelected: (VaaniTab) -> Unit,
    onRecordClick: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VaaniTheme.colors
    var expanded by remember { mutableStateOf(false) }
    val fabInteraction = remember { MutableInteractionSource() }
    val fabPressed by fabInteraction.collectIsPressedAsState()
    val fabPressScale by animateFloatAsState(if (fabPressed) 0.9f else 1f, PressSpring, label = "fabPress")
    val fabRotation by animateFloatAsState(if (expanded) 45f else 0f, tween(200), label = "fabRotate")

    val fabEntrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        fabEntrance.animateTo(
            1f,
            animationSpec = keyframes {
                durationMillis = 450
                0.7f at 0
                1.06f at 270
                1f at 450
            },
        )
    }

    Box(modifier = modifier.fillMaxWidth().padding(14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surf, RoundedCornerShape(26.dp))
                .border(1.dp, colors.line, RoundedCornerShape(26.dp))
                .padding(vertical = 9.dp, horizontal = 6.dp),
        ) {
            VaaniTab.entries.forEach { tab ->
                NavTabItem(
                    tab = tab,
                    selected = tab == current,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Column(
            // Reports zero size to the outer Box no matter how tall the
            // real content is (see the layout{} below) — the actual bug
            // behind "opening the FAB ruins the layout": this Box sizes
            // itself to its tallest child, and this column grows a lot
            // when the chips appear. That growth pushed the Box's own top
            // edge — where the pill sits — upward, dragging the whole nav
            // pill up into the FAB's floating space. Since only the pill
            // now determines the Box's height, the pill never moves, and
            // the FAB/chips (still measured and placed at full size, just
            // not counted for sizing) float above it exactly like before,
            // at any screen size — this isn't tuned to one device's pixels.
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(0, 0) {
                        placeable.placeRelative(
                            x = -placeable.width + FAB_END_INSET.roundToPx(),
                            y = -placeable.height + FAB_BOTTOM_CLEARANCE.roundToPx(),
                        )
                    }
                }
                .zIndex(1f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.7f),
                exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.7f),
            ) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FabAction(icon = Icons.Outlined.FileUpload, label = "Import", onClick = { expanded = false; onImportClick() })
                    FabAction(icon = Icons.Outlined.Mic, label = "Record", onClick = { expanded = false; onRecordClick() })
                }
            }

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(fabPressScale)
                    .background(colors.acc, RoundedCornerShape(23.dp))
                    .clickable(interactionSource = fabInteraction, indication = null, onClick = { expanded = !expanded }),
                contentAlignment = Alignment.Center,
            ) {
                if (expanded) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.rotate(fabRotation))
                } else {
                    Box(Modifier.size(20.dp).background(Color.White, CircleShape))
                }
            }
        }
    }
}

@Composable
private fun FabAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val colors = VaaniTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        modifier = Modifier
            .background(colors.surf, RoundedCornerShape(999.dp))
            .border(1.dp, colors.line, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = colors.ink, modifier = Modifier.size(18.dp))
        Text(label, color = colors.ink, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun NavTabItem(tab: VaaniTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = VaaniTheme.colors
    val fg by animateColorAsState(if (selected) colors.pri else colors.ink3, tween(250), label = "tabFg")
    val pillBg by animateColorAsState(if (selected) colors.priDim else Color.Transparent, tween(250), label = "tabBg")
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.94f else 1f, PressSpring, label = "tabPress")

    Column(
        modifier = modifier
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .scale(pressScale)
                .background(pillBg, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (tab) {
                VaaniTab.Home -> Icon(Icons.Outlined.Home, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
                VaaniTab.Library -> LibraryBarsIcon(fg)
                VaaniTab.Search -> Icon(Icons.Outlined.Search, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
                VaaniTab.About -> Icon(Icons.Outlined.Info, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
            }
        }
        Text(tab.label, color = fg, style = MaterialTheme.typography.labelSmall)
    }
}
