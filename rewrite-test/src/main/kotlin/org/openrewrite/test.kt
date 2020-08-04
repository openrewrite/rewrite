/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

/**
 * Retrieve the visitors for the named profile from the classpath
 */
fun loadVisitors(profileName: String): Collection<RefactorVisitor<*>> = loadRefactorPlan(profileName).visitors(profileName)
