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

import org.intellij.lang.annotations.Language;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;

/**
 * Produce a part by user provided sample code and type, make sure the part can be produced from the sample code and only once.
 */
public final class PartProvider {
    private PartProvider() {
    }

    public static <J2 extends J> J2 buildPart(@Language("java") String codeToProvideAPart,
                                              Class<J2> expected,
                                              String... classpath) {
        JavaParser.Builder<? extends JavaParser, ?> builder = JavaParser.fromJavaVersion();
        if (classpath.length != 0) {
            builder.classpathFromResources(new InMemoryExecutionContext(), classpath);
        }

        J.CompilationUnit cu = builder.build()
                .parse(codeToProvideAPart)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Could not parse as Java"));

        List<J2> parts = new ArrayList<>(1);
        new JavaVisitor<List<J2>>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, List<J2> j2s) {
                if (expected.isInstance(tree)) {
                    //noinspection unchecked
                    J2 j2 = (J2) tree;
                    j2s.add(j2);
                    return j2;
                }
                return super.visit(tree, j2s);
            }
        }.visit(cu, parts);

        if (parts.size() != 1) {
            throw new IllegalStateException("Expected to produce only 1 part but produced " + parts.size() + "parts");
        }

        return parts.get(0);
    }
}
