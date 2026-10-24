plugins {
    alias(libs.plugins.novila.jvm.library)
}

dependencies {
    // The statistics behind the links and trends (Welch, Benjamini–Hochberg, Kruskal–Wallis, least squares).
    implementation(projects.core.science)
}
