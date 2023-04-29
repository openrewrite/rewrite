/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.groovy;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTypeMappingTest;
import org.openrewrite.java.JavaTypeVisitor;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
class GroovyTypeMappingTest implements JavaTypeMappingTest {
    private static final String goat = StringUtils.readFully(GroovyTypeMappingTest.class.getResourceAsStream("/GroovyTypeGoat.groovy"));

    @Override
    public JavaType.FullyQualified classType(String fqn) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public JavaType.Parameterized goatType() {
        //noinspection ConstantConditions
        return TypeUtils.asParameterized(GroovyParser.builder()
          .logCompilationWarningsAndErrors(true)
          .build()
          .parse(new InMemoryExecutionContext(), goat)
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Could not parse as Java"))
          .getClasses()
          .get(0)
          .getType()
        );
    }

    @SuppressWarnings("GroovyUnusedAssignment")
    @Test
    void noDuplicateSignatures() {
        Statement cu = GroovyParser.builder().build()
          .parse(
            """
              def a = "hi"
              """
          )
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Could not parse as Java"))
          .getStatements()
          .get(0);

        Set<JavaType> uniqueTypes = Collections.newSetFromMap(new IdentityHashMap<>());
        Map<String, JavaType> typeBySignatureAfterMapping = new HashMap<>();
        Map<String, Integer> signatureCollisions = new TreeMap<>();

        new GroovyVisitor<Integer>() {
            @Override
            public org.openrewrite.java.tree.JavaType visitType(@Nullable org.openrewrite.java.tree.JavaType javaType, Integer p) {
                return new JavaTypeVisitor<Integer>() {
                    @Override
                    public org.openrewrite.java.tree.JavaType visit(@Nullable org.openrewrite.java.tree.JavaType javaType, Integer p) {
                        if (javaType != null) {
                            if (uniqueTypes.add(javaType)) {
                                typeBySignatureAfterMapping.compute(javaType.toString(), (t, existing) -> {
                                    if (existing != null && javaType != existing) {
                                        signatureCollisions.compute(javaType.toString(), (t2, acc) -> acc == null ? 1 : acc + 1);
                                    }
                                    return javaType;
                                });
                                return super.visit(javaType, p);
                            }
                        }
                        //noinspection ConstantConditions
                        return javaType;
                    }
                }.visit(javaType, 0);
            }
        }.visit(cu, 0);

        System.out.println("Unique signatures: ${typeBySignatureAfterMapping.size}");
        System.out.println("Deep type count: ${uniqueTypes.size}");
        System.out.println("Signature collisions: ${signatureCollisions.size}");

        assertThat(signatureCollisions)
          .as("More than one instance of a type collides on the same signature.")
          .isEmpty();
    }
}
