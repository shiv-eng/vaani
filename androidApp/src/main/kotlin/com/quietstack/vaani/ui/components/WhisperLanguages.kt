package com.quietstack.vaani.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietstack.vaani.ui.theme.VaaniTheme

/** One entry in the language picker. [code] null means "let whisper auto-detect". */
data class LanguageOption(val code: String?, val label: String)

/**
 * The exact set whisper.cpp supports (src/whisper.cpp's g_lang table) — not a
 * curated subset, plus Hindi routed to a dedicated model (see
 * RoutingTranscriptionEngine in the `:whisper` module) rather than Whisper's
 * own general multilingual decoding, which real on-device testing found
 * genuinely unreliable for Hindi (rendered in Urdu's script, Hindi/English
 * code-switching collapsing to English-only). English is pinned first since
 * it's the default (see AppSettings.transcriptionLanguage); Auto-detect and
 * Hindi are pinned right after since they're the two languages this app is
 * actually verified to work well for. The rest follow whisper's own list
 * order rather than being re-alphabetized, so it stays a direct mirror of
 * what the engine actually supports — most of them simply haven't been
 * tested on real speech the way English and Hindi have.
 */
val WHISPER_LANGUAGES: List<LanguageOption> = listOf(
    LanguageOption("en", "English"),
    LanguageOption(null, "Auto-detect"),
    LanguageOption("hi", "Hindi"),
    LanguageOption("zh", "Chinese"),
    LanguageOption("de", "German"),
    LanguageOption("es", "Spanish"),
    LanguageOption("ru", "Russian"),
    LanguageOption("ko", "Korean"),
    LanguageOption("fr", "French"),
    LanguageOption("ja", "Japanese"),
    LanguageOption("pt", "Portuguese"),
    LanguageOption("tr", "Turkish"),
    LanguageOption("pl", "Polish"),
    LanguageOption("ca", "Catalan"),
    LanguageOption("nl", "Dutch"),
    LanguageOption("ar", "Arabic"),
    LanguageOption("sv", "Swedish"),
    LanguageOption("it", "Italian"),
    LanguageOption("id", "Indonesian"),
    LanguageOption("fi", "Finnish"),
    LanguageOption("vi", "Vietnamese"),
    LanguageOption("he", "Hebrew"),
    LanguageOption("uk", "Ukrainian"),
    LanguageOption("el", "Greek"),
    LanguageOption("ms", "Malay"),
    LanguageOption("cs", "Czech"),
    LanguageOption("ro", "Romanian"),
    LanguageOption("da", "Danish"),
    LanguageOption("hu", "Hungarian"),
    LanguageOption("ta", "Tamil"),
    LanguageOption("no", "Norwegian"),
    LanguageOption("th", "Thai"),
    LanguageOption("ur", "Urdu"),
    LanguageOption("hr", "Croatian"),
    LanguageOption("bg", "Bulgarian"),
    LanguageOption("lt", "Lithuanian"),
    LanguageOption("la", "Latin"),
    LanguageOption("mi", "Maori"),
    LanguageOption("ml", "Malayalam"),
    LanguageOption("cy", "Welsh"),
    LanguageOption("sk", "Slovak"),
    LanguageOption("te", "Telugu"),
    LanguageOption("fa", "Persian"),
    LanguageOption("lv", "Latvian"),
    LanguageOption("bn", "Bengali"),
    LanguageOption("sr", "Serbian"),
    LanguageOption("az", "Azerbaijani"),
    LanguageOption("sl", "Slovenian"),
    LanguageOption("kn", "Kannada"),
    LanguageOption("et", "Estonian"),
    LanguageOption("mk", "Macedonian"),
    LanguageOption("br", "Breton"),
    LanguageOption("eu", "Basque"),
    LanguageOption("is", "Icelandic"),
    LanguageOption("hy", "Armenian"),
    LanguageOption("ne", "Nepali"),
    LanguageOption("mn", "Mongolian"),
    LanguageOption("bs", "Bosnian"),
    LanguageOption("kk", "Kazakh"),
    LanguageOption("sq", "Albanian"),
    LanguageOption("sw", "Swahili"),
    LanguageOption("gl", "Galician"),
    LanguageOption("mr", "Marathi"),
    LanguageOption("pa", "Punjabi"),
    LanguageOption("si", "Sinhala"),
    LanguageOption("km", "Khmer"),
    LanguageOption("sn", "Shona"),
    LanguageOption("yo", "Yoruba"),
    LanguageOption("so", "Somali"),
    LanguageOption("af", "Afrikaans"),
    LanguageOption("oc", "Occitan"),
    LanguageOption("ka", "Georgian"),
    LanguageOption("be", "Belarusian"),
    LanguageOption("tg", "Tajik"),
    LanguageOption("sd", "Sindhi"),
    LanguageOption("gu", "Gujarati"),
    LanguageOption("am", "Amharic"),
    LanguageOption("yi", "Yiddish"),
    LanguageOption("lo", "Lao"),
    LanguageOption("uz", "Uzbek"),
    LanguageOption("fo", "Faroese"),
    LanguageOption("ht", "Haitian Creole"),
    LanguageOption("ps", "Pashto"),
    LanguageOption("tk", "Turkmen"),
    LanguageOption("nn", "Nynorsk"),
    LanguageOption("mt", "Maltese"),
    LanguageOption("sa", "Sanskrit"),
    LanguageOption("lb", "Luxembourgish"),
    LanguageOption("my", "Myanmar"),
    LanguageOption("bo", "Tibetan"),
    LanguageOption("tl", "Tagalog"),
    LanguageOption("mg", "Malagasy"),
    LanguageOption("as", "Assamese"),
    LanguageOption("tt", "Tatar"),
    LanguageOption("haw", "Hawaiian"),
    LanguageOption("ln", "Lingala"),
    LanguageOption("ha", "Hausa"),
    LanguageOption("ba", "Bashkir"),
    LanguageOption("jw", "Javanese"),
    LanguageOption("su", "Sundanese"),
    LanguageOption("yue", "Cantonese"),
)

fun labelForLanguageCode(code: String?): String =
    WHISPER_LANGUAGES.firstOrNull { it.code == code }?.label ?: "Auto-detect"

/**
 * Sheet content for picking the transcription language. Auto-detect and the
 * two pinned entries (Hindi, English) sit above a scrollable list of every
 * other language whisper.cpp supports — see WHISPER_LANGUAGES' ordering note.
 * A search field filters that list by name, since ~100 entries is a lot to
 * scroll through to find one language.
 */
@Composable
fun LanguagePickerContent(selected: String?, onSelect: (String?) -> Unit) {
    val colors = VaaniTheme.colors
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) WHISPER_LANGUAGES else WHISPER_LANGUAGES.filter { it.label.contains(query, ignoreCase = true) }
    }
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
        item {
            Text(
                "Transcription language",
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
            )
            Text(
                "English and Hindi are both tuned for accuracy. Other languages are supported but not all tested the same way.",
                color = colors.ink3,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(colors.surf3, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("Search languages", color = colors.ink3, fontSize = 14.sp)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = colors.ink, fontSize = 14.sp),
                        cursorBrush = SolidColor(colors.pri),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        if (filtered.isEmpty()) {
            item {
                Text(
                    "No languages match “$query”.",
                    color = colors.ink3,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                )
            }
        }
        items(filtered) { option ->
            val isSelected = option.code == selected
            Text(
                option.label,
                color = if (isSelected) colors.pri else colors.ink,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 15.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSelected) colors.priDim else Color.Transparent, RoundedCornerShape(12.dp))
                    .clickable { onSelect(option.code) }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            )
        }
    }
}
