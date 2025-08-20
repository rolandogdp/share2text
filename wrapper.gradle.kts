// wrapper.gradle.kts (standalone script just to generate the wrapper)
tasks.register<Wrapper>("wrapper") {
    gradleVersion = "8.9"
    distributionType = Wrapper.DistributionType.BIN
}
