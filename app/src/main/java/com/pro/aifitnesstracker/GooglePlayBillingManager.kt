package com.pro.aifitnesstracker

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class GooglePlayBillingManager(
    private val context: Context,
    private val prefs: SharedPrefsManager,
    private val onPremiumActivated: (plan: String) -> Unit = {}
) {
    companion object {
        private const val TAG = "BillingManager"
        const val PRODUCT_MONTHLY = "premium_monthly"
        const val PRODUCT_YEARLY = "premium_yearly"
        const val PRODUCT_LIFETIME = "premium_lifetime"
    }

    private var billingClient: BillingClient? = null
    private var isConnected = false

    val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else {
            Log.w(TAG, "Purchase failed: ${billingResult.debugMessage}")
        }
    }

    fun initialize() {
        val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .build()

        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(pendingPurchasesParams)
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isConnected = true
                    Log.d(TAG, "Billing client connected")
                    checkExistingPurchases()
                }
            }
            override fun onBillingServiceDisconnected() {
                isConnected = false
                Log.w(TAG, "Billing service disconnected")
            }
        })
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val plan = when {
                purchase.products.contains(PRODUCT_LIFETIME) -> PremiumManager.PLAN_LIFETIME
                purchase.products.contains(PRODUCT_YEARLY) -> PremiumManager.PLAN_YEARLY
                purchase.products.contains(PRODUCT_MONTHLY) -> PremiumManager.PLAN_MONTHLY
                else -> return
            }
            PremiumManager.activate(prefs, plan)
            onPremiumActivated(plan)

            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient?.acknowledgePurchase(params) { result ->
                    Log.d(TAG, "Purchase acknowledged: ${result.responseCode}")
                }
            }
        }
    }

    private fun checkExistingPurchases() {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { _, purchases ->
            purchases.forEach { handlePurchase(it) }
        }
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { _, purchases ->
            purchases.forEach { handlePurchase(it) }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productId: String, isSubscription: Boolean = true) {
        if (!isConnected) { initialize(); return }
        val productType = if (isSubscription) BillingClient.ProductType.SUBS else BillingClient.ProductType.INAPP
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(productType)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        billingClient?.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            val productDetailsList = queryProductDetailsResult.productDetailsList
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !productDetailsList.isNullOrEmpty()) {
                val productDetails = productDetailsList[0]
                val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                val productDetailsParamsList = if (offerToken != null) {
                    listOf(BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build())
                } else {
                    listOf(BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build())
                }
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()
                billingClient?.launchBillingFlow(activity, billingFlowParams)
            } else {
                Log.w(TAG, "Product not found: $productId (${billingResult.debugMessage})")
            }
        }
    }

    fun destroy() {
        billingClient?.endConnection()
    }
}
