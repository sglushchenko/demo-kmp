plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Interop variant is chosen by a Gradle property: -Pskie=true
val useSkie = providers.gradleProperty("skie").map { it.toBoolean() }.getOrElse(false)
if (useSkie) apply(plugin = "co.touchlab.skie")

// Each interop variant builds into its own directory so Plain and SKIE outputs never mix.
layout.buildDirectory.set(layout.projectDirectory.dir(if (useSkie) "build-skie" else "build"))

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.core)
            implementation(libs.ktor.contentNegotiation)
            implementation(libs.ktor.json)
            implementation(libs.coroutines.core)
            implementation(libs.serialization.json)
            implementation(libs.datetime)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.resources)
        }
        iosMain.dependencies {
            implementation(libs.ktor.darwin)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.ktor.mock)
            implementation(libs.coroutines.test)
        }
    }
}

compose.resources {
    packageOfResClass = "pet.shared.generated.resources"
    generateResClass = always
}
