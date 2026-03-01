plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("androidx.annotation:annotation:1.8.0")
    implementation("androidx.paging:paging-common-ktx:3.3.2")
    implementation("javax.inject:javax.inject:1")
}
