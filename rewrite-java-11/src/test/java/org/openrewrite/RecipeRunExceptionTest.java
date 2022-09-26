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
package org.openrewrite;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.Markers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class RecipeRunExceptionTest {

    @Test
    void addExceptionToTree() {
        J.CompilationUnit cu = JavaParser.fromJavaVersion().build().parse("class Test {}").get(0);

        cu = (J.CompilationUnit) new JavaIsoVisitor<Integer>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier ident, Integer p) {
                J.Identifier i = super.visitIdentifier(ident, p);
                return i.withExceptionMarker(new IllegalStateException("boom"));
            }
        }.visitNonNull(cu, 0);

        List<RecipeRunException> exceptions = new ArrayList<>();
        new TreeVisitor<Tree, Integer>() {
            @Nullable
            @Override
            public Tree visit(@Nullable Tree tree, Integer p) {
                if (tree != null) {
                    try {
                        Method getMarkers = tree.getClass().getDeclaredMethod("getMarkers");
                        Markers markers = (Markers) getMarkers.invoke(tree);
                        markers.findFirst(RecipeRunExceptionResult.class)
                                .ifPresent(e -> exceptions.add(e.getException()));
                    } catch (Throwable ignored) {
                    }
                }
                return super.visit(tree, p);
            }
        }.visit(cu, 0);

        Assertions.assertThat(exceptions).hasSize(1);
        Assertions.assertThat(exceptions.get(0).getCause()).isInstanceOf(IllegalStateException.class);
    }
}
