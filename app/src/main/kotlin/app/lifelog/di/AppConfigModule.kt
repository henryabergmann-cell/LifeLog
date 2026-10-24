package app.lifelog.di

import app.lifelog.BuildConfig
import app.lifelog.billing.BillingConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Hands build-time configuration (from local.properties via BuildConfig) to other modules. */
@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {

    @Provides
    fun provideBillingConfig(): BillingConfig = BillingConfig(
        productId = BuildConfig.BILLING_PRODUCT_ID,
        monthlyBasePlanId = BuildConfig.BILLING_BASE_PLAN_MONTHLY,
        yearlyBasePlanId = BuildConfig.BILLING_BASE_PLAN_YEARLY,
        yearlyTrialOfferId = BuildConfig.BILLING_OFFER_YEARLY_TRIAL,
        licenseKey = BuildConfig.BILLING_LICENSE_KEY,
    )
}
