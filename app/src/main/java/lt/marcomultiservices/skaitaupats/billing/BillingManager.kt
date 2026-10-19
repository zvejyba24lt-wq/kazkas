package lt.marcomultiservices.skaitaupats.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** The one-time purchase through Google Play Billing that removes ads and the daily limit. */
class BillingManager(
    context: Context,
    private val scope: CoroutineScope,
    private val onOwnedChanged: suspend (Boolean) -> Unit,
) : PurchasesUpdatedListener {
    private val client = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private val _product = MutableStateFlow<ProductDetails?>(null)
    val product: StateFlow<ProductDetails?> = _product

    private val _checked = MutableStateFlow(false)
    /** True once Play has been asked about earlier purchases (or could not be reached). */
    val checked: StateFlow<Boolean> = _checked
    private var connecting = false

    fun connect() {
        if (client.isReady) {
            scope.launch { runCatching { queryProduct(); restore() } }
            return
        }
        if (connecting) return
        connecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode != BillingClient.BillingResponseCode.OK) { _checked.value = true; return }
                scope.launch {
                    runCatching { queryProduct(); restore() }.onFailure { Log.w(TAG, "Billing query failed", it) }
                    _checked.value = true
                }
            }

            override fun onBillingServiceDisconnected() { connecting = false }
        })
    }

    private suspend fun queryProduct() {
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_FULL)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build(),
            ),
        ).build()
        _product.value = suspendCancellableCoroutine { cont ->
            client.queryProductDetailsAsync(params) { _, result -> cont.resume(result.productDetailsList?.firstOrNull()) }
        }
    }

    private suspend fun restore() {
        val result = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        )
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) handle(result.purchasesList)
    }

    /** Returns false when the product is not available (e.g. app not installed from Play). */
    fun launch(activity: Activity): Boolean {
        if (!client.isReady) { connect(); return false }
        val details = _product.value ?: return false
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()),
        ).build()
        return client.launchBillingFlow(activity, params).responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when {
            result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null -> scope.launch { handle(purchases) }
            // Bought before (e.g. on another phone or before a reinstall): restore instead of failing.
            result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch { runCatching { restore() } }
        }
    }

    private suspend fun handle(purchases: List<Purchase>) {
        val owned = purchases.filter { PRODUCT_FULL in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
        owned.filterNot { it.isAcknowledged }.forEach {
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(it.purchaseToken).build())
        }
        onOwnedChanged(owned.isNotEmpty())
    }

    companion object {
        private const val TAG = "Billing"
        const val PRODUCT_FULL = "visas_kursas"
    }
}
