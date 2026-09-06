package com.murmur.transcriber.ui.components

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.murmur.transcriber.ui.theme.MurmurTheme

private val PressSpring = spring<Float>(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)

enum class MurmurTab(val label: String) {
    Home("Home"), Library("Library"), Search("Search"), Storage("Storage"),
}

/**
 * The floating pill nav + record FAB (README "Navigation shell"): surf
 * container, 26dp radius, active tab is a priDim pill, FAB enters with mpop
 * (scale .7→1.06→1) and sits 80px above the bar.
 */
@Composable
fun MurmurBottomBar(
    current: MurmurTab,
    onTabSelected: (MurmurTab) -> Unit,
    onRecordClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MurmurTheme.colors
    val fabInteraction = remember { MutableInteractionSource() }
    val fabPressed by fabInteraction.collectIsPressedAsState()
    val fabPressScale by animateFloatAsState(if (fabPressed) 0.9f else 1f, PressSpring, label = "fabPress")

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
            MurmurTab.entries.forEach { tab ->
                NavTabItem(
                    tab = tab,
                    selected = tab == current,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .absoluteOffset(x = (-6).dp, y = (-66).dp)
                .zIndex(1f)
                .size(64.dp)
                .scale(fabPressScale)
                .background(colors.acc, RoundedCornerShape(23.dp))
                .clickable(interactionSource = fabInteraction, indication = null, onClick = onRecordClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(20.dp).background(Color.White, CircleShape))
        }
    }
}

@Composable
private fun NavTabItem(tab: MurmurTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MurmurTheme.colors
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
                MurmurTab.Home -> Icon(Icons.Outlined.Home, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
                MurmurTab.Library -> LibraryBarsIcon(fg)
                MurmurTab.Search -> Icon(Icons.Outlined.Search, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
                MurmurTab.Storage -> Icon(Icons.Outlined.Storage, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
            }
        }
        Text(tab.label, color = fg, style = MaterialTheme.typography.labelSmall)
    }
}
