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
package org.openrewrite.kotlin.cleanup;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.kotlin.internal.KotlinPrinter;
import org.openrewrite.kotlin.marker.Semicolon;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Marker;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveTrailingSemicolon extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove unnecessary trailing semicolon";
    }

    @Override
    public String getDescription() {
        return "Some Java programmers may mistakenly add semicolons at the end when writing Kotlin code, but in " +
               "reality, they are not necessary.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        return new KotlinIsoVisitor<ExecutionContext>() {
            @Nullable
            Set<Marker> semiColonRemovable;

            @Override
            public K.CompilationUnit visitCompilationUnit(K.CompilationUnit cu, ExecutionContext ctx) {
                semiColonRemovable = CollectSemicolonRemovableElements.collect(cu);
                return super.visitCompilationUnit(cu, ctx);
            }

            @Override
            @SuppressWarnings("DataFlowIssue")
            public <M extends Marker> M visitMarker(Marker marker, ExecutionContext ctx) {
                return semiColonRemovable.remove(marker) ? null : super.visitMarker(marker, ctx);
            }
        };
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class CollectSemicolonRemovableElements extends KotlinPrinter<Set<Marker>> {
        Pattern WS = Pattern.compile("^\\s+");

        private class MyKotlinJavaPrinter extends KotlinPrinter.KotlinJavaPrinter<Set<Marker>> {

            @Nullable
            private Integer mark;
            @Nullable
            private Marker semicolonMarker;

            MyKotlinJavaPrinter(KotlinPrinter<Set<Marker>> kp) {
                super(kp);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<Set<Marker>> p) {
                Marker m = super.visitMarker(marker, p);
                if (marker instanceof Semicolon) {
                    mark(marker, p);
                }
                return (M) m;
            }

            @Override
            public Space visitSpace(Space space, Space.Location loc, PrintOutputCapture<Set<Marker>> p) {
                p.append(space.getWhitespace());
                checkMark(p);
                return space;
            }

            private void mark(Marker semicolonMarker, PrintOutputCapture<Set<Marker>> p) {
                mark = p.out.length();
                this.semicolonMarker = semicolonMarker;
            }

            private void checkMark(PrintOutputCapture<Set<Marker>> p) {
                if (mark != null) {
                    String substring = p.out.substring(mark);
                    Matcher matcher = WS.matcher(substring);
                    if (matcher.find()) {
                        if (matcher.group().indexOf('\n') != -1) {
                            p.getContext().add(semicolonMarker);
                        }
                        mark = null;
                    }
                }
            }
        }

        public static Set<Marker> collect(J j) {
            Set<Marker> removable = new HashSet<>();
            new CollectSemicolonRemovableElements().visit(j, new PrintOutputCapture<>(removable));
            return removable;
        }

        @Override
        protected KotlinJavaPrinter<Set<Marker>> delegate() {
            return new MyKotlinJavaPrinter(this);
        }
    }
}
