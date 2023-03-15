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
package org.openrewrite.java.trait;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.Optional;
import java.util.function.Function;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "org.openrewrite.java.trait")
public class TraitArchitectureTest {

    private static ArchCondition<JavaClass> haveExplicitlyDeclaredMethodWithName(Function<JavaClass, Optional<JavaMethod>> getMethod, String description) {
        return new ArchCondition<>("must have equals(Object) method") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                Optional<JavaMethod> m = getMethod.apply(item);
                if (m.isEmpty()) {
                    String message = String.format(description, item.getFullName());
                    events.add(SimpleConditionEvent.violated(item, message));
                }
            }
        };
    }

    private static final ArchCondition<JavaClass> haveExplicitlyDeclaredEqualsMethod =
        haveExplicitlyDeclaredMethodWithName(javaClass -> javaClass.tryGetMethod("equals", Object.class), "Class %s has no equals(Object) method declared");

    private static final ArchCondition<JavaClass> haveExplicitlyDeclaredHashCodeMethod =
        haveExplicitlyDeclaredMethodWithName(javaClass -> javaClass.tryGetMethod("hashCode"), "Class %s has no hashCode() method declared");

    @ArchTest
    public static final ArchRule topImplementationClassesMustOverrideEqualsAndHashCode =
      classes().that().implement(Top.class)
        .should(haveExplicitlyDeclaredEqualsMethod)
        .andShould(haveExplicitlyDeclaredHashCodeMethod);
}
