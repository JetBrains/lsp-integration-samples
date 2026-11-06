plugins {
    id("org.jetbrains.intellij.platform.module")
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        bundledModule("intellij.platform.frontend")
        bundledModule("intellij.platform.eel.provider")
        bundledModule("intellij.platform.lsp")
        bundledModule("intellij.platform.lsp.impl")
    }
}
