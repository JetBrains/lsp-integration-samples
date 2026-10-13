plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    testImplementation(libs.junit)

    intellijPlatform {
        bundledModule("intellij.platform.frontend")
        bundledModule("intellij.platform.eel.provider")
        bundledModule("intellij.platform.lsp")
        bundledModule("intellij.platform.lsp.impl")
    }
}
