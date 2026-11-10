plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ktlint) apply false
}


// OWASP dependency-check is applied from the dedicated security-scan CI job
// via a standalone init script — NOT the root plugins block. Applying it at
// root pollutes the build classloader with its BouncyCastle jars, which
// conflicts with AGP's UTP test runner (used by connectedDebugAndroidTest)
// and causes NoClassDefFoundError: org/bouncycastle/asn1/edec/EdECObjectIdentifiers.
