import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.predictiveSelection
import org.gradle.kotlin.dsl.withType

tasks.withType<Test>().configureEach {
    // recently failed tests will get selected, so let's DISABLE for the nightly
    // scheduled builds and releases

    predictiveSelection {
        enabled.set(providers.gradleProperty("enablePredictiveTestSelection").map(String::toBoolean).orElse(true))
    }
}
