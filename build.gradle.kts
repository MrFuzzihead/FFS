import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

tasks.test {
    useJUnitPlatform()
    // The tests must never require a display; CI also runs them under xvfb-run,
    // but local and dedicated-server runs should stay fully headless.
    systemProperty("java.awt.headless", "true")
    maxHeapSize = "1g"
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
    }
}

// Convenience alias used by the README and by headless CI images.
// `build` already depends on `test`; this task exists so the intent is explicit
// and can be invoked on its own: ./gradlew headlessTest
tasks.register("headlessTest") {
    group = "verification"
    description = "Runs the headless JUnit suite (no display or Minecraft client required)."
    dependsOn(tasks.test)
}
