package lt.marcomultiservices.skaitaupats.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lt.marcomultiservices.skaitaupats.BuildConfig
import lt.marcomultiservices.skaitaupats.R
import lt.marcomultiservices.skaitaupats.billing.BillingManager
import lt.marcomultiservices.skaitaupats.core.Course
import lt.marcomultiservices.skaitaupats.data.Progress
import kotlin.random.Random

/** A small sum a young child cannot answer, so purchases and links stay with adults. */
@Composable
fun ParentGate(onPassed: () -> Unit, onBack: () -> Unit) {
    val a = rememberSaveable { Random.nextInt(4, 10) }
    val b = rememberSaveable { Random.nextInt(4, 10) }
    var answer by rememberSaveable { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(Palette.Cream).statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
    ) {
        Text("🔒", fontSize = 56.sp)
        Text(stringResource(R.string.gate_title), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
        Text(stringResource(R.string.gate_question, a, b), fontSize = 22.sp, color = Palette.Ink)
        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it.filter(Char::isDigit).take(3); wrong = false },
            label = { Text(stringResource(R.string.gate_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        if (wrong) Text(stringResource(R.string.gate_wrong), color = Palette.Red)
        Button(onClick = { if (answer.toIntOrNull() == a * b) onPassed() else wrong = true }) {
            Text(stringResource(R.string.gate_ok), fontSize = 18.sp)
        }
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
    }
}

@Composable
fun ParentsScreen(
    course: Course,
    progress: Progress,
    billing: BillingManager,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val product by billing.product.collectAsState()
    var confirmReset by remember { mutableStateOf(false) }
    var unavailable by remember { mutableStateOf(false) }
    val lessons = course.lessons
    val finished = lessons.filter { progress.finished(it.id) }
    val letters = lessons.filter { it.letter != null && progress.finished(it.id) }.joinToString(" ") { it.letter!!.letter.uppercase() + it.letter!!.letter }
    val hard = progress.letterMistakes.entries.sortedByDescending { it.value }.filter { it.value >= 2 }.take(3)
        .joinToString(", ") { it.key.uppercase() + it.key }

    Column(
        Modifier.fillMaxSize().background(Palette.Cream).statusBarsPadding().verticalScroll(rememberScrollState())
            .navigationBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill("←", label = stringResource(R.string.back), onClick = onBack)
            Spacer(Modifier.size(14.dp))
            Text(stringResource(R.string.parents_title), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
        }

        Section(stringResource(R.string.progress_title)) {
            if (finished.isEmpty()) Body(stringResource(R.string.progress_none)) else {
                Body(stringResource(R.string.progress_lessons, finished.size, lessons.size))
                if (letters.isNotEmpty()) Body(stringResource(R.string.progress_letters, letters))
                if (hard.isNotEmpty()) Body(stringResource(R.string.progress_hard, hard))
            }
        }

        Section(stringResource(R.string.buy_title)) {
            when {
                progress.owned -> Body(stringResource(R.string.buy_owned))
                else -> {
                    Body(stringResource(R.string.buy_text, lessons.size))
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { unavailable = !billing.launch(context as Activity) },
                        colors = ButtonDefaults.buttonColors(containerColor = Palette.Green),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Text(stringResource(R.string.buy_button, product?.oneTimePurchaseOfferDetails?.formattedPrice ?: "9,99 €"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = { billing.connect() }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.buy_restore))
                    }
                    if (unavailable) Body(stringResource(R.string.buy_unavailable))
                }
            }
            if (BuildConfig.TEST_UNLOCK) Body(stringResource(R.string.test_mode))
        }

        Section(stringResource(R.string.tips_title)) {
            listOf(R.string.tip_1, R.string.tip_2, R.string.tip_3, R.string.tip_4, R.string.tip_5).forEach {
                Body("• " + stringResource(it))
            }
        }

        Section(stringResource(R.string.privacy)) {
            Body(stringResource(R.string.privacy_text))
            TextButton(onClick = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_URL))) }
            }) { Text(stringResource(R.string.privacy)) }
        }

        OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.reset), color = Palette.Red)
        }
        Text(stringResource(R.string.version, BuildConfig.VERSION_NAME), fontSize = 13.sp, color = Palette.Soft)
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            text = { Text(stringResource(R.string.reset_confirm)) },
            confirmButton = { TextButton(onClick = { confirmReset = false; onReset() }) { Text(stringResource(R.string.reset_yes), color = Palette.Red) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Palette.Paper),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Ink)
            content()
        }
    }
}

@Composable
private fun Body(text: String) {
    Text(text, fontSize = 16.sp, color = Palette.Ink, lineHeight = 22.sp)
}
