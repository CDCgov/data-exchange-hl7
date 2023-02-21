package gov.cdc.ncezid.cloud

/**
 * Intended to provide access to any provider details (which provider, region, account, etc.)
 */
interface ProviderMeta {
    fun provider(): Providers
}

enum class Providers {
    AWS,
    Azure
}