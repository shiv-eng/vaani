package com.quietstack.vaani.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietstack.vaani.ui.components.WordmarkLockup
import com.quietstack.vaani.ui.components.mrise
import com.quietstack.vaani.ui.components.mslide
import com.quietstack.vaani.ui.components.rememberAnimateOnce
import com.quietstack.vaani.ui.theme.VaaniTheme

private data class AboutPoint(val title: String, val body: String)

private val points = listOf(
    AboutPoint(
        "Nothing ever leaves your phone",
        "Recording, transcription, storage — all of it runs right here on your device. Vaani never sends your voice, your recordings, or your transcripts anywhere.",
    ),
    AboutPoint(
        "Works with no internet at all",
        "The speech model downloads once, the first time you set up the app. After that, recording and transcribing both work completely offline — on a flight, underground, anywhere.",
    ),
    AboutPoint(
        "No account, no tracking",
        "There's nothing to sign in to and nothing being measured. Vaani doesn't know who you are and isn't trying to find out.",
    ),
    AboutPoint(
        "Transcription takes real time",
        "Running speech recognition on your phone, not a server, means it's not instant — a recording can take a few minutes to transcribe. It keeps working in the background, and you'll get a notification the moment it's ready, even if you've closed the app.",
    ),
)

/** Reassures users of the app's core privacy promise — explicit, not just implied by an absence of a login screen. */
@Composable
fun AboutScreen() {
    val colors = VaaniTheme.colors
    val animatedRows = remember { mutableSetOf<Any>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.bg).padding(horizontal = 20.dp).mrise(),
        contentPadding = PaddingValues(top = 28.dp, bottom = 128.dp),
    ) {
        item {
            WordmarkLockup(barWidth = 4.dp, barMaxHeight = 22.dp, fontSize = 18.sp, letterSpacing = 2.8.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "Private by design, not by promise",
                color = colors.ink2,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(28.dp))
        }

        itemsIndexed(points, key = { index, _ -> index }) { index, point ->
            val animate = rememberAnimateOnce(index, animatedRows)
            AboutCard(point, modifier = Modifier.mslide(index * 60, animate = animate))
            Spacer(Modifier.height(12.dp))
        }

        item {
            Spacer(Modifier.height(16.dp))
            Text(
                "Vaani",
                color = colors.ink3,
                fontSize = 11.sp,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                "version 0.1.0",
                color = colors.ink3,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun AboutCard(point: AboutPoint, modifier: Modifier = Modifier) {
    val colors = VaaniTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surf2, RoundedCornerShape(18.dp))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.size(9.dp).background(colors.ok, CircleShape))
            Text(point.title, color = colors.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(point.body, color = colors.ink2, fontSize = 12.5.sp, lineHeight = 18.sp)
    }
}
