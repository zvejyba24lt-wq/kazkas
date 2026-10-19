package lt.marcomultiservices.skaitaupats.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lt.marcomultiservices.skaitaupats.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean

/**
 * One interstitial after a finished lesson in the free version. Every request is marked as meant
 * for children (Google Play Families): no personalised ads, content rated G, no advertising id.
 */
class AdManager(private val context: Context) {
    private val started = AtomicBoolean(false)
    @Volatile private var ready = false
    private var interstitial: InterstitialAd? = null
    private var loading = false
    private var lastShown = 0L

    /** Settles consent (no form is shown for children) and starts AdMob. Call once from the activity. */
    fun start(activity: Activity) {
        // Google Play's automatic pre-launch testing must not click real ads.
        if (Settings.System.getString(activity.contentResolver, "firebase.test.lab") == "true") return
        if (started.getAndSet(true)) return
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
                .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build(),
        )
        val info = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(true).build()
        info.requestConsentInfoUpdate(activity, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                if (error != null) Log.w(TAG, "Consent form: ${error.message}")
                if (info.canRequestAds()) initialize()
            }
        }, { error ->
            Log.w(TAG, "Consent update failed: ${error.message}")
            if (info.canRequestAds()) initialize()
        })
    }

    private fun initialize() {
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(context) {
                ready = true
                CoroutineScope(Dispatchers.Main).launch { preload() }
            }
        }
    }

    fun preload() {
        if (!ready || interstitial != null || loading) return
        loading = true
        InterstitialAd.load(context, BuildConfig.ADMOB_INTERSTITIAL_ID, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad; loading = false }
            override fun onAdFailedToLoad(error: LoadAdError) { loading = false; Log.w(TAG, "Interstitial: ${error.message}") }
        })
    }

    /** Shows an ad if one is ready and the last one was a while ago; [then] runs afterwards either way. */
    fun showAfterLesson(activity: Activity, then: () -> Unit) {
        val ad = interstitial
        val now = SystemClock.elapsedRealtime()
        if (ad == null || activity.isFinishing || activity.isDestroyed || (lastShown != 0L && now - lastShown < MIN_GAP_MS)) { preload(); then(); return }
        interstitial = null
        lastShown = now
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { preload(); then() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { preload(); then() }
        }
        ad.show(activity)
    }

    private companion object {
        const val TAG = "Ads"
        /** Quick replays of a short lesson do not bring a new ad each time. */
        const val MIN_GAP_MS = 2 * 60_000L
    }
}
