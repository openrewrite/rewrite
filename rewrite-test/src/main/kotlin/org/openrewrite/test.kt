package org.openrewrite

import org.openrewrite.Assertions.whenParsedBy
import org.openrewrite.config.ClasspathResourceLoader
import org.openrewrite.config.ProfileConfiguration
import java.nio.file.Path

fun <S: SourceFile> String.whenParsedBy(parser: Parser<S>): Assertions.StringSourceFileAssert<S> =
        whenParsedBy(parser, this)

fun <S: SourceFile> Path.whenParsedBy(parser: Parser<S>): Assertions.PathSourceFileAssert<S> =
        whenParsedBy(parser, this)

/**
 * Retrieve a refactoring plan for the named profile from the classpath.
 */
fun loadRefactorPlan(profileName: String): RefactorPlan {
    val crl = ClasspathResourceLoader(emptyList())
    val profileConfig = crl.loadProfiles().asSequence()
            .filter { it.name == profileName }
            .fold(
                    ProfileConfiguration().apply {
                        name = profileName
                    }) { accumulator, profile -> accumulator.merge(profile) }
            ?: throw RuntimeException("Couldn't load profile named '$profileName'. " +
                    "Verify that there's a yml file defining a profile with this name under src/test/resources/META-INF/rewrite")

    val profile = profileConfig.build(listOf())
    val visitors = crl.loadVisitors()
            .filter { profile.accept(it) == Profile.FilterReply.ACCEPT }
    if(visitors.isEmpty()) {
        throw RuntimeException("Couldn't find any visitors for profile named `$profileName`. " +
                "Verify that your profile has an include pattern that accepts at least one visitor according to SourceVisitor<J>.accept()")
    }
    return RefactorPlan.builder()
            .loadProfile(profileConfig)
            .loadVisitors(visitors)
            .build()
}
