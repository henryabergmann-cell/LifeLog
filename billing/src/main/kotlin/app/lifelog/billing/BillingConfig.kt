package app.lifelog.billing

/**
 * Play Console identifiers. Supplied by :app from BuildConfig (populated from
 * local.properties), never hard-coded in source.
 */
data class BillingConfig(
    val productId: String,
    val monthlyBasePlanId: String,
    val yearlyBasePlanId: String,
    val yearlyTrialOfferId: String,
    /** Base64 RSA public key used by LocalPurchaseVerifier; may be blank in debug builds. */
    val licenseKey: String,
)
