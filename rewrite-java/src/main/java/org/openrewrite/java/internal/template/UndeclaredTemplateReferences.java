/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.internal.template;

import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.ImportValidationService;
import org.openrewrite.java.internal.UnimportedReferences;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;

import java.util.*;

/**
 * Records the types and static members templates name without declaring them, while a test harness listens on the
 * thread that applies them.
 */
public final class UndeclaredTemplateReferences {
    private static final ThreadLocal<@Nullable Set<String>> RECORDED = new ThreadLocal<>();

    // Plain and never cleared, so templates pay next to nothing until something records
    private static boolean everRecorded;

    private UndeclaredTemplateReferences() {
    }

    public static ImportValidationService.Recording record() {
        Set<String> previous = RECORDED.get();
        Set<String> recorded = new LinkedHashSet<>();
        everRecorded = true;
        RECORDED.set(recorded);
        return new ImportValidationService.Recording() {
            @Override
            public List<String> getUndeclared() {
                return new ArrayList<>(recorded);
            }

            @Override
            public void close() {
                if (previous == null) {
                    RECORDED.remove();
                } else {
                    RECORDED.set(previous);
                }
            }
        };
    }

    /**
     * Records what the code a template inserts names that the template's own imports don't declare.
     *
     * @param stub      The stub compiled from the template.
     * @param templated The code from the template, as the stub holds it.
     */
    static void check(JavaSourceFile stub, Collection<? extends J> templated, Set<String> templateImports) {
        if (!everRecorded) {
            return;
        }
        Set<String> recorded = RECORDED.get();
        if (recorded == null || !(stub instanceof J.CompilationUnit) || templated.isEmpty()) {
            return;
        }
        List<String> undeclared = UnimportedReferences.ofTemplate((J.CompilationUnit) stub, templated,
                templateImports);
        if (!undeclared.isEmpty()) {
            String appliedAt = appliedAt();
            for (String reference : undeclared) {
                recorded.add(reference + " in a template applied at " + appliedAt);
            }
        }
    }

    /**
     * The frame that called {@code JavaTemplate}, below the visitors that the template engine walks the tree with.
     */
    private static String appliedAt() {
        StackTraceElement[] stack = new Throwable().getStackTrace();
        for (int i = stack.length - 1; i > 0; i--) {
            String className = stack[i - 1].getClassName();
            if ("org.openrewrite.java.JavaTemplate".equals(className) ||
                className.startsWith("org.openrewrite.java.JavaTemplate$")) {
                return stack[i].toString();
            }
        }
        return "an unknown place";
    }
}
