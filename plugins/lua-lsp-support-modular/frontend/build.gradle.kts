plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    intellijPlatform {
        bundledModule("intellij.platform.frontend")
        bundledLibrary("lib/intellij.platform.lsp.jar")
        bundledLibrary("lib/intellij.platform.lsp.impl.jar")
    }
}
