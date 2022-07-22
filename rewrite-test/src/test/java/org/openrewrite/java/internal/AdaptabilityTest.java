/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.internal;

import org.junit.jupiter.api.Test;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

@SuppressWarnings("unchecked")
public class AdaptabilityTest {

    @Test
    void typesInUse() {
        new TypesInUse.FindTypesInUse().adapt(GroovyVisitor.class);
    }

    @Test
    void usesMethod() {
        new UsesMethod<>("java.util.List add(..)").adapt(GroovyVisitor.class);
    }

    @Test
    void isBuildGradle() {
        new IsBuildGradle<>().adapt(GroovyVisitor.class);
    }

    @Test
    void unboundedVisitor() {
        new VisitLiteral<>().adapt(GroovyVisitor.class);
    }

    public static class VisitLiteral<P> extends JavaIsoVisitor<P> {
        @Override
        public J.Literal visitLiteral(J.Literal literal, P p) {
            return super.visitLiteral(literal, p);
        }
    }
}
