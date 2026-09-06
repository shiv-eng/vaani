package com.murmur.transcriber.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.murmur.transcriber.shared.data.ModelCatalogue
import com.murmur.transcriber.ui.components.mrise
import com.murmur.transcriber.ui.components.mslide
import com.murmur.transcriber.ui.components.rememberRingProgress
import com.murmur.transcriber.ui.components.rememberWaveScale
import com.murmur.transcriber.ui.format.formatBytes
import com.murmur.transcriber.ui.theme.JetBrainsMonoFamily
import com.murmur.transcriber.ui.theme.ManropeFamily
import com.murmur.transcriber.ui.theme.MurmurTheme
import com.murmur.transcriber.ui.theme.SoraFamily
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val colors = MurmurTheme.colors

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onComplete()
    }
    if (state.isComplete) return

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        when (state.step) {
            OnboardingStep.WELCOME -> WelcomeStep(onSetUpClick = viewModel::onSetUpClicked)
            OnboardingStep.DOWNLOAD -> DownloadStep(
                state = state,
                onTogglePause = viewModel::togglePause,
                onSkipAhead = viewModel::skipAhead,
                onWaitForWifiChange = viewModel::setWaitForWifi,
                onRetry = viewModel::retry,
            )
        }
    }
}

@Composable
private fun WelcomeStep(onSetUpClick: () -> Unit) {
    val colors = MurmurTheme.colors

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(420.dp)
                .background(
                    Brush.radialGradient(listOf(colors.priDim.copy(alpha = 0.5f), Color.Transparent)),
                    CircleShape,
                ),
        )
        RingHalo(modifier = Modifier.align(Alignment.Center))

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 34.dp).mrise()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                WordmarkBars()
                Spacer(Modifier.width(4.dp))
                Text(
                    "MURMUR",
                    color = colors.ink,
                    fontFamily = SoraFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    letterSpacing = 2.1.sp,
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                "Two hours of audio.",
                color = colors.ink,
                fontFamily = SoraFamily,
                fontWeight = FontWeight.Light,
                fontSize = 34.sp,
                lineHeight = 38.sp,
            )
            Text(
                "Transcribed on this phone.",
                color = colors.ink,
                fontFamily = SoraFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 34.sp,
                lineHeight = 38.sp,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "No account, no upload, no minute limits. The recording and the transcript never leave the handset.",
                color = colors.ink2,
                fontFamily = ManropeFamily,
                fontSize = 14.5.sp,
                lineHeight = 20.sp,
                modifier = Modifier.widthIn(max = 300.dp),
            )

            Spacer(Modifier.height(22.dp))
            FeatureRow("2h", "Any length", "A full lecture or interview, in one file", delayMillis = 50)
            Spacer(Modifier.height(10.dp))
            FeatureRow("∞", "Free, without a meter", "There is no server to pay for", delayMillis = 130)
            Spacer(Modifier.height(10.dp))
            FeatureRow(null, "Hindi, English, and the mix", "Multilingual weights from the first release", delayMillis = 210)

            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(colors.pri, RoundedCornerShape(18.dp))
                    .clickable(onClick = onSetUpClick),
                contentAlignment = Alignment.Center,
            ) {
                Text("Set up on this phone", color = colors.priOn, fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "One ${formatBytes(ModelCatalogue.BASE_Q5_1.sizeBytes)} model download first. Offline after that.",
                color = colors.ink3,
                fontSize = 11.5.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RingHalo(modifier: Modifier = Modifier) {
    val colors = MurmurTheme.colors
    Box(modifier = modifier.size(200.dp), contentAlignment = Alignment.Center) {
        listOf(0, 800, 1600).forEach { delay ->
            val progress = rememberRingProgress(delayMillis = delay)
            Canvas(modifier = Modifier.size(180.dp * (0.4f + 0.6f * progress))) {
                drawCircle(
                    color = colors.pri.copy(alpha = (1f - progress) * 0.5f),
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun WordmarkBars() {
    val colors = MurmurTheme.colors
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        val heights = listOf(0.6f, 1f, 0.75f)
        val delays = listOf(0, 180, 360)
        heights.forEachIndexed { i, baseHeight ->
            val scale = rememberWaveScale(delayMillis = delays[i])
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp * baseHeight * (0.6f + 0.4f * scale))
                    .background(if (i == 2) colors.acc else colors.pri, RoundedCornerShape(1.5.dp)),
            )
        }
    }
}

@Composable
private fun FeatureRow(glyph: String?, title: String, sub: String, delayMillis: Int) {
    val colors = MurmurTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surf, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .mslide(delayMillis),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(34.dp).background(colors.priDim, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (glyph != null) {
                Text(glyph, color = colors.pri, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            } else {
                Icon(Icons.Outlined.Public, contentDescription = null, tint = colors.pri, modifier = Modifier.size(16.dp))
            }
        }
        Column {
            Text(title, color = colors.ink, fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
            Text(sub, color = colors.ink3, fontFamily = ManropeFamily, fontSize = 12.sp)
        }
    }
}

@Composable
private fun DownloadStep(
    state: OnboardingState,
    onTogglePause: () -> Unit,
    onSkipAhead: () -> Unit,
    onWaitForWifiChange: (Boolean) -> Unit,
    onRetry: () -> Unit,
) {
    val colors = MurmurTheme.colors
    val model = ModelCatalogue.BASE_Q5_1
    val totalSize = formatBytes(model.sizeBytes)
    val downloadedSize = formatBytes((model.sizeBytes * state.progress).toLong())

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 34.dp).mrise()) {
        Text(
            "STEP 2 OF 2",
            color = colors.ink3,
            fontWeight = FontWeight.Bold,
            fontSize = 11.5.sp,
            letterSpacing = 1.8.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text("The speech model, once", style = MaterialTheme.typography.headlineSmall, color = colors.ink, fontSize = 25.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "$totalSize, downloaded once. After that, everything happens right here on your phone — no internet needed.",
            color = colors.ink2,
            fontSize = 13.5.sp,
            lineHeight = 20.sp,
        )

        if (state.error != null) {
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth().background(colors.surf3, RoundedCornerShape(18.dp)).padding(16.dp),
            ) {
                Text("Download failed", color = colors.acc, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                Spacer(Modifier.height(4.dp))
                Text(state.error, color = colors.ink2, fontSize = 12.5.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .background(colors.pri, RoundedCornerShape(14.dp))
                        .clickable(onClick = onRetry)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text("Retry", color = colors.priOn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surf, RoundedCornerShape(26.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.size(148.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 11.dp.toPx()
                    drawCircle(color = colors.surf2, style = Stroke(width = strokeWidth))
                    drawArc(
                        color = colors.pri,
                        startAngle = -90f,
                        sweepAngle = 360f * state.progress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${(state.progress * 100).toInt()}",
                            color = colors.ink,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 29.sp,
                        )
                        Text("%", color = colors.ink3, fontFamily = JetBrainsMonoFamily, fontSize = 15.sp)
                    }
                    Text("$downloadedSize of $totalSize", color = colors.ink3, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Speech model · Hindi & English",
                color = colors.ink,
                fontFamily = ManropeFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text("Checked automatically once it's done", color = colors.ink3, fontSize = 11.5.sp)

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .background(colors.surf2, RoundedCornerShape(15.dp))
                        .clickable(onClick = onTogglePause),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (state.isPaused) "Resume" else "Pause",
                        color = colors.ink,
                        fontFamily = ManropeFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .background(colors.pri, RoundedCornerShape(15.dp))
                        .clickable(onClick = onSkipAhead),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Skip ahead", color = colors.priOn, fontFamily = ManropeFamily, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surf, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Wait for Wi-Fi", color = colors.ink, fontFamily = ManropeFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                Text(
                    "Resumes by itself, days later if need be",
                    color = colors.ink3,
                    fontSize = 11.5.sp,
                )
            }
            Switch(
                checked = state.waitForWifi,
                onCheckedChange = onWaitForWifiChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = colors.pri,
                    checkedThumbColor = colors.priOn,
                    uncheckedTrackColor = colors.surf3,
                    uncheckedThumbColor = colors.ink3,
                ),
            )
        }

        Spacer(Modifier.weight(1f))
        Text(
            "Until this finishes, the app is honest about it: nothing here claims to work offline yet.",
            color = colors.ink3,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
        )
    }
}
