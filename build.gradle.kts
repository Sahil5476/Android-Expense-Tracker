plugins {
    alias(libs.plugins.android.application) apply false

    // ADD THIS LINE BELOW:
    id("com.google.gms.google-services") version "4.4.2" apply false
}