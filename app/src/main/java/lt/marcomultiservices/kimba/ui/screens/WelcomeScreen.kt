package lt.marcomultiservices.kimba.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lt.marcomultiservices.kimba.core.Conditions
import lt.marcomultiservices.kimba.data.Place
import lt.marcomultiservices.kimba.data.WeatherRepo
import lt.marcomultiservices.kimba.game.GameViewModel
import lt.marcomultiservices.kimba.game.LakeState
import lt.marcomultiservices.kimba.ui.Glass
import lt.marcomultiservices.kimba.ui.K
import lt.marcomultiservices.kimba.ui.S
import lt.marcomultiservices.kimba.ui.scene.LakeScene
import lt.marcomultiservices.kimba.ui.t
import java.time.ZoneId

@Composable
fun WelcomeScreen(onPick: (Place) -> Unit) {
    val place = GameViewModel.defaultPlace
    val conditions = remember { Conditions(WeatherRepo.seasonal(), System.currentTimeMillis(), place.lat, place.lon, ZoneId.systemDefault()) }
    val lake = remember { mutableStateOf(LakeState()) }
    Box(Modifier.fillMaxSize().background(K.Night)) {
        LakeScene(conditions, lake, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x55000000), Color(0x22000000), Color(0xAA000000)))))
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(18.dp)) {
            Spacer(Modifier.height(24.dp))
            Text(t(S.appName), style = MaterialTheme.typography.displaySmall.copy(fontSize = 52.sp), color = K.Text)
            Text(t(S.tagline), color = K.Amber, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(160.dp))
            Glass(Modifier.fillMaxWidth(), radius = 24.dp, padding = 18.dp) {
                Column {
                    Text(t(S.welcomeTitle), style = MaterialTheme.typography.headlineSmall, color = K.Text)
                    Spacer(Modifier.height(8.dp))
                    Text(t(S.welcomeBody), color = K.Muted, fontSize = 14.sp, lineHeight = 20.sp)
                    Spacer(Modifier.height(16.dp))
                    PlacePicker(selected = null, onPick = onPick)
                    Spacer(Modifier.height(10.dp))
                    Text(t(S.placeNote), color = Color(0x99B4BCC6), fontSize = 11.sp)
                }
            }
        }
    }
}
