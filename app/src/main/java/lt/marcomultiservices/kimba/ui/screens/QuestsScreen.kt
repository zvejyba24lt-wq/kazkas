package lt.marcomultiservices.kimba.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lt.marcomultiservices.kimba.core.Progress
import lt.marcomultiservices.kimba.core.Quest
import lt.marcomultiservices.kimba.core.QuestKind
import lt.marcomultiservices.kimba.core.Quests
import lt.marcomultiservices.kimba.game.GameViewModel
import lt.marcomultiservices.kimba.ui.K
import lt.marcomultiservices.kimba.ui.LocalLang
import lt.marcomultiservices.kimba.ui.S
import lt.marcomultiservices.kimba.ui.SectionTitle
import lt.marcomultiservices.kimba.ui.t
import java.time.LocalDate

@Composable
fun QuestsScreen(vm: GameViewModel, onBack: () -> Unit) {
    val saved by vm.saved.collectAsStateWithLifecycle()
    val p = saved?.progress ?: Progress()
    val quests = Quests.today(p.quests, LocalDate.now().toEpochDay(), p.level)
    ScreenFrame(t(S.questsTitle), onBack) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp, 4.dp, 16.dp, 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinsLabel(p.coins, 20)
                Spacer(Modifier.weight(1f))
                Text(t(S.streakNow, p.streak), color = K.Amber, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            for (q in quests.list) {
                QuestCard(q)
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(t(S.questsNote), color = K.Muted, fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(22.dp))
            SectionTitle(t(S.streakNow, p.streak))
            Spacer(Modifier.height(10.dp))
            // The return gift grows for seven days.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (d in 1..7) {
                    val got = p.streak >= d || (p.streak > 7 && d == 7)
                    val today = minOf(p.streak, 7) == d
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(38.dp).clip(CircleShape)
                                .background(if (got) K.Amber else Color(0x1FFFFFFF))
                                .border(if (today) 2.dp else 1.dp, if (today) Color.White else K.Line, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (got) Icon(Icons.Rounded.Check, null, tint = Color(0xFF2A1A06), modifier = Modifier.size(20.dp))
                            else Text("$d", color = K.Muted, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(3.dp))
                        Text("${50 * d}", color = K.Gold, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestCard(q: Quest) {
    val shape = RoundedCornerShape(16.dp)
    val frac = when (q.kind) {
        QuestKind.BIG -> if (q.done) 1f else 0f
        else -> (q.progress.toFloat() / q.target).coerceIn(0f, 1f)
    }
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (q.done) Brush.horizontalGradient(listOf(Color(0x408DC26F), Color(0x148DC26F))) else Brush.horizontalGradient(listOf(Color(0x26FFFFFF), Color(0x0FFFFFFF))))
            .border(1.dp, if (q.done) Color(0x888DC26F) else K.Line, shape)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(q.text(LocalLang.current), color = K.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            CoinIcon(Modifier.size(16.dp))
            Text(" ${q.coins}", color = K.Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.weight(1f).height(8.dp)) {
                val r = CornerRadius(4.dp.toPx())
                drawRoundRect(Color(0x33FFFFFF), cornerRadius = r)
                drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF8DC26F), K.Amber)), size = Size(size.width * frac, size.height), cornerRadius = r)
            }
            Spacer(Modifier.width(10.dp))
            Text(q.progressText(), color = if (q.done) K.Reed else K.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
