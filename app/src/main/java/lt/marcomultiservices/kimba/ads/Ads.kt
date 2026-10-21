package lt.marcomultiservices.kimba.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import lt.marcomultiservices.kimba.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean

/** GDPR consent through Google's UMP (IAB TCF v2.2) and AdMob start-up after consent. */
object AdsConsent {
    private const val TAG = "AdsConsent"
    private val started = AtomicBoolean(false)
    private val requested = AtomicBoolean(false)
    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds
    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired

    private fun info(context: Context): ConsentInformation = UserMessagingPlatform.getConsentInformation(context)

    fun gather(activity: Activity, onReady: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed || requested.getAndSet(true)) return
        val info = info(activity)
        info.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                if (activity.isFinishing || activity.isDestroyed) {
                    update(activity, onReady)
                    return@requestConsentInfoUpdate
                }
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    if (error != null) Log.w(TAG, "Consent form: ${error.message}")
                    update(activity, onReady)
                }
            },
            { error ->
                Log.w(TAG, "Consent update failed: ${error.message}")
                requested.set(false)
                update(activity, onReady)
            },
        )
        // Consent from a previous session is valid right away.
        update(activity, onReady)
    }

    private fun update(activity: Activity, onReady: () -> Unit) {
        val info = info(activity)
        _privacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (info.canRequestAds() && !started.getAndSet(true)) {
            val app = activity.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                MobileAds.initialize(app) {
                    _canRequestAds.value = true
                    onReady()
                }
            }
        }
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { }
    }
}

/**
 * Rewarded videos for extra casts and double coins, an occasional interstitial between results.
 * Nothing here ever blocks the game: without an ad the reward is simply granted.
 */
class AdManager(private val context: Context) {
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var loadingInterstitial = false
    private var loadingRewarded = false

    fun preload() {
        if (!AdsConsent.canRequestAds.value) return
        if (interstitial == null && !loadingInterstitial) {
            loadingInterstitial = true
            InterstitialAd.load(context, BuildConfig.ADMOB_INTERSTITIAL_ID, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad; loadingInterstitial = false }
                override fun onAdFailedToLoad(error: LoadAdError) { loadingInterstitial = false; Log.w(TAG, "Interstitial: ${error.message}") }
            })
        }
        if (rewarded == null && !loadingRewarded) {
            loadingRewarded = true
            RewardedAd.load(context, BuildConfig.ADMOB_REWARDED_ID, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewarded = ad; loadingRewarded = false }
                override fun onAdFailedToLoad(error: LoadAdError) { loadingRewarded = false; Log.w(TAG, "Rewarded: ${error.message}") }
            })
        }
    }

    private var lastInterstitial = 0L

    /** Shows an interstitial if one is ready and the last one was long enough ago; [then] runs afterwards either way. */
    fun showInterstitial(activity: Activity, then: () -> Unit) {
        val ad = interstitial
        val now = System.currentTimeMillis()
        if (ad == null || activity.isFinishing || now - lastInterstitial < INTERSTITIAL_GAP_MS) { preload(); then(); return }
        interstitial = null
        lastInterstitial = now
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { preload(); then() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { preload(); then() }
        }
        ad.show(activity)
    }

    /** Plays a rewarded video; [onReward] runs when it was watched, or right away when no video is available. */
    fun showRewarded(activity: Activity, onReward: () -> Unit) {
        val ad = rewarded
        if (ad == null || activity.isFinishing) { preload(); onReward(); return }
        rewarded = null
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { preload(); if (earned) onReward() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { preload(); onReward() }
        }
        ad.show(activity) { earned = true }
    }

    companion object {
        private const val TAG = "Ads"
        /** At most one interstitial every four minutes. */
        const val INTERSTITIAL_GAP_MS = 4 * 60_000L
    }
}

