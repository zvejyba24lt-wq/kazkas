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
import lt.marcomultiservices.kimba.core.Achievement
import lt.marcomultiservices.kimba.core.Catch
import lt.marcomultiservices.kimba.core.Fish
import lt.marcomultiservices.kimba.core.Plural
import lt.marcomultiservices.kimba.core.Progress
import lt.marcomultiservices.kimba.ui.fish.FishPicture
import lt.marcomultiservices.kimba.core.Quest
import lt.marcomultiservices.kimba.core.QuestKind
import lt.marcomultiservices.kimba.core.Quests
import lt.marcomultiservices.kimba.game.GameViewModel
import lt.marcomultiservices.kimba.ui.K
import lt.marcomultiservices.kimba.ui.LocalLang
import lt.marcomultiservices.kimba.ui.S
import lt.marcomultiservices.kimba.ui.SectionTitle
import lt.marcomultiservices.kimba.ui.t
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import lt.marcomultiservices.kimba.core.Invites
import lt.marcomultiservices.kimba.core.Challenge
import lt.marcomultiservices.kimba.share.ShareCard
import lt.marcomultiservices.kimba.ui.amberButtonColors
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
            ChallengeCard(p)
            Spacer(Modifier.height(10.dp))
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
            Spacer(Modifier.height(26.dp))
            SectionTitle(t(S.friendsTitle))
            Spacer(Modifier.height(10.dp))
            FriendsCard(p) { code, done -> vm.enterCode(code) { done(it != GameViewModel.CodeResult.BAD) } }
            Spacer(Modifier.height(26.dp))
            SectionTitle(t(S.trophies))
            Spacer(Modifier.height(10.dp))
            TrophyWall(saved?.catches ?: emptyList())
            Spacer(Modifier.height(26.dp))
            val catches = saved?.catches ?: emptyList()
            SectionTitle(t(S.achievements) + "  ·  " + t(S.achievementsDone, p.achieved.size, Achievement.entries.size))
            Spacer(Modifier.height(10.dp))
            // Earned first, then the closest ones.
            val sorted = Achievement.entries.sortedWith(compareByDescending<Achievement> { it.key in p.achieved }.thenByDescending { it.progress(p, catches).toFloat() / it.target })
            for (a in sorted) {
                AchievementRow(a, a.key in p.achieved, a.progress(p, catches))
                Spacer(Modifier.height(8.dp))
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

/** The biggest fish of each species, hung on wooden plaques. */
@Composable
private fun TrophyWall(catches: List<Catch>) {
    val lang = LocalLang.current
    val best = catches.groupBy { it.golden ?: it.species }.values.map { list -> list.maxBy { it.grams } }.sortedByDescending { it.grams }.take(6)
    val wall = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth().clip(wall)
            .background(Brush.verticalGradient(listOf(Color(0xFF5A3B22), Color(0xFF3E2716))))
            .border(1.dp, Color(0x66E0B070), wall)
            .padding(10.dp),
    ) {
        if (best.isEmpty()) {
            Text(t(S.trophiesEmpty), color = Color(0xFFE8D2B0), fontSize = 14.sp, modifier = Modifier.padding(12.dp))
        }
        for (row in best.chunked(2)) {
            Row(Modifier.fillMaxWidth()) {
                for (c in row) {
                    val plaque = RoundedCornerShape(14.dp)
                    Column(
                        Modifier.weight(1f).padding(4.dp).clip(plaque)
                            .background(Brush.verticalGradient(listOf(Color(0xFF8A5E36), Color(0xFF6B4527))))
                            .border(1.dp, if (Fish.gold(c.golden)) K.Gold else Color(0x55FFE0B0), plaque)
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        FishPicture(c.species, Fish.gold(c.golden), null, Modifier.fillMaxWidth().height(64.dp))
                        Spacer(Modifier.height(4.dp))
                        val name = (c.golden?.let { Fish.goldenById(it)?.name } ?: Fish.byId(c.species)?.name)?.get(lang) ?: c.species
                        Text(name, color = Color(0xFFFFF1DC), fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("${Plural.kg(c.grams, lang)} · ${c.cm.toInt()} cm", color = if (Fish.gold(c.golden)) K.Gold else Color(0xFFE8D2B0), fontSize = 12.sp)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AchievementRow(a: Achievement, earned: Boolean, value: Int) {
    val lang = LocalLang.current
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (earned) Brush.horizontalGradient(listOf(Color(0x40E0A030), Color(0x10E0A030))) else Brush.horizontalGradient(listOf(Color(0x1FFFFFFF), Color(0x0AFFFFFF))))
            .border(1.dp, if (earned) Color(0x88E0A030) else K.Line, shape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrophyIcon(Modifier.size(36.dp), earned)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(a.title[lang], color = K.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(a.text[lang], color = K.Muted, fontSize = 12.sp, lineHeight = 16.sp)
            if (!earned && a.target > 1) {
                Spacer(Modifier.height(6.dp))
                val shown = if (a == Achievement.KILO || a == Achievement.FIVE_KILO || a == Achievement.TEN_KILO) null else "$value/${a.target}"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.weight(1f).height(6.dp)) {
                        val r = CornerRadius(3.dp.toPx())
                        drawRoundRect(Color(0x33FFFFFF), cornerRadius = r)
                        drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF8DC26F), K.Amber)), size = Size(size.width * value / a.target.toFloat(), size.height), cornerRadius = r)
                    }
                    if (shown != null) { Spacer(Modifier.width(8.dp)); Text(shown, color = K.Muted, fontSize = 11.sp) }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        CoinIcon(Modifier.size(14.dp))
        Text(" ${a.coins}", color = if (earned) K.Gold else K.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/** The player's friend code to send, a field for a friend's code, and the thank-you code to give back. */
@Composable
private fun FriendsCard(p: Progress, onCode: (String, (Boolean) -> Unit) -> Unit) {
    val lang = LocalLang.current
    val context = LocalContext.current
    val shape = RoundedCornerShape(16.dp)
    var entry by remember { mutableStateOf("") }
    var bad by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0x332F6F8F), Color(0x14FFFFFF))))
            .border(1.dp, Color(0x5578B8D8), shape)
            .padding(14.dp),
    ) {
        Text(t(S.friendsBody), color = K.Muted, fontSize = 13.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(t(S.yourCode).uppercase(), color = K.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text(p.inviteCode, color = K.Text, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            }
            Button(
                {
                    runCatching { ShareCard.sendText(context, S.inviteText[lang].format(p.inviteCode, ShareCard.link(p.inviteCode)), S.inviteChooser[lang]) }
                },
                enabled = p.inviteCode.isNotEmpty(),
                colors = amberButtonColors(),
            ) {
                Icon(Icons.Rounded.Share, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(t(S.invite))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                entry, { entry = it.uppercase().take(12); bad = false },
                Modifier.weight(1f),
                singleLine = true,
                label = { Text(t(S.enterCode), fontSize = 12.sp) },
                isError = bad,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
            )
            Spacer(Modifier.width(8.dp))
            OutlinedButton({ onCode(entry) { ok -> if (ok) entry = "" else bad = true } }, enabled = entry.length >= 6) {
                Text(t(S.redeem), color = K.Text)
            }
        }
        if (bad) {
            Spacer(Modifier.height(4.dp))
            Text(t(S.codeBad), color = K.Danger, fontSize = 12.sp)
        }
        // A newcomer gives the friend who brought them a code back.
        val inviter = p.invitedBy
        if (inviter != null && p.inviteCode.isNotEmpty()) {
            val thanks = Invites.thanks(inviter, p.inviteCode)
            Spacer(Modifier.height(14.dp))
            Text(t(S.thanksTitle), color = K.Amber, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(t(S.thanksBody), color = K.Muted, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(thanks, color = K.Text, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                OutlinedButton({ runCatching { ShareCard.sendText(context, S.thanksText[lang].format(thanks), S.thanksTitle[lang]) } }) {
                    Text(t(S.send), color = K.Text)
                }
            }
        }
    }
}

/** The day's challenge, the same for every player, with today's best and a Wordle-style share. */
@Composable
private fun ChallengeCard(p: Progress) {
    val lang = LocalLang.current
    val context = LocalContext.current
    val today = LocalDate.now()
    val day = today.toEpochDay()
    val species = Challenge.species(day, today.monthValue)
    val log = p.challenge.takeIf { it.day == day }
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0x40E0A030), Color(0x14E0A030))))
            .border(1.dp, Color(0x88E0A030), shape)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(t(S.challengeTitle, Challenge.number(day)).uppercase(), color = K.Amber, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                Text(t(species.name), color = K.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            FishPicture(species.id, false, null, Modifier.width(92.dp).height(46.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(t(S.challengeGoal, t(species.name).lowercase()), color = K.Muted, fontSize = 12.sp, lineHeight = 17.sp)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (log == null) {
                Text(t(S.challengeNone), color = K.Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
            } else {
                val weight = Plural.kg(log.grams, lang)
                Column(Modifier.weight(1f)) {
                    Text(t(S.challengeBest, "$weight · ${log.cm.toInt()} cm"), color = K.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(Challenge.bar(Challenge.boxes(log, species)), fontSize = 16.sp)
                }
                OutlinedButton({
                    val text = S.challengeShareText[lang].format(
                        Challenge.number(day), species.name[lang], weight, Challenge.bar(Challenge.boxes(log, species)), p.inviteCode, ShareCard.link(p.inviteCode),
                    )
                    runCatching { ShareCard.sendText(context, text, S.challengeShare[lang]) }
                }) {
                    Icon(Icons.Rounded.Share, null, tint = K.Text, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(t(S.challengeShare), color = K.Text)
                }
            }
        }
    }
}
