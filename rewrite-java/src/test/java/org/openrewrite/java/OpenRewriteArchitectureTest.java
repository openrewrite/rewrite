/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.*;
import org.jspecify.annotations.NullMarked;

import static com.tngtech.archunit.base.DescribedPredicate.doNot;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.all;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noCodeUnits;

@AnalyzeClasses(packages = "org.openrewrite.java", importOptions = ImportOption.DoNotIncludeTests.class)
public class OpenRewriteArchitectureTest {

    @ArchTest
    public static final ArchRule packageInfo =
        all(packages("org.openrewrite.java"))
          .that(doNot(name("org.openrewrite.java.internal.grammar")))
          .should(containAnAnnotatedPackageInfo());

    @ArchTest
    public static final ArchRule nullness =
        noCodeUnits()
          .should().beAnnotatedWith("org.jetbrains.annotations.NotNull")
          .orShould().beAnnotatedWith("org.jetbrains.annotations.Nullable");

    private static ClassesTransformer<JavaPackage> packages(String basePackage) {
        return new AbstractClassesTransformer<>("packages") {
            @Override
            public Iterable<JavaPackage> doTransform(JavaClasses classes) {
                return classes.getPackage(basePackage).getSubpackagesInTree();
            }
        };
    }

    private static ArchCondition<JavaPackage> containAnAnnotatedPackageInfo() {
        return new ArchCondition<JavaPackage>("contain a package-info") {
            @Override
            public void check(JavaPackage javaPackage, ConditionEvents events) {
                if (javaPackage.tryGetPackageInfo().isEmpty()) {
                    String message = "Package '%s' does not contain a package-info".formatted(javaPackage.getName());
                    events.add(SimpleConditionEvent.violated(javaPackage, message));
                }
            }
        }.and(new ArchCondition<>("be annotated with @NullMarked") {
            @Override
            public void check(JavaPackage javaPackage, ConditionEvents events) {
                javaPackage.tryGetPackageInfo()
                        .filter(packageInfo -> !packageInfo.isAnnotatedWith(NullMarked.class))
                        .ifPresent(packageInfo -> events.add(SimpleConditionEvent.violated(javaPackage,
                        "Package '%s' is not annotated as @NullMarked".formatted(javaPackage.getName()))));
            }
        });
    }
}
