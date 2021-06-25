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
package org.openrewrite.java.security;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

public class SecureTempFileCreation extends Recipe {
    private static final MethodMatcher matcher = new MethodMatcher("java.io.File createTempFile(..)");

    @Override
    public String getDisplayName() {
        return "Use secure temporary file creation";
    }

    @Override
    public String getDescription() {
        return "`java.io.File.createTempFile()` has exploitable default file permissions. This recipe migrates to the more secure `java.nio.file.Files.createTempFile()`.";
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(matcher);
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            private final JavaTemplate twoArg = JavaTemplate.builder(this::getCursor, "Files.createTempFile(#{any(String)}, #{any(String)}).toFile();")
                    .imports("java.nio.file.Files")
                    .build();

            private final JavaTemplate threeArg = JavaTemplate.builder(this::getCursor, "Files.createTempFile(#{any(java.io.File)}.toPath(), #{any(String)}, #{any(String)}).toFile();")
                    .imports("java.nio.file.Files")
                    .build();

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation m = method;
                if (matcher.matches(m)) {
                    maybeAddImport("java.nio.file.Files");
                    if (m.getArguments().size() == 2) {
                        // File.createTempFile(String prefix, String suffix)
                        m = m.withTemplate(twoArg,
                                m.getCoordinates().replace(),
                                m.getArguments().get(0),
                                m.getArguments().get(1)
                        );
                    } else if (m.getArguments().size() == 3) {
                        // File.createTempFile(String prefix, String suffix, File dir)
                        m = m.withTemplate(threeArg,
                                m.getCoordinates().replace(),
                                m.getArguments().get(2),
                                m.getArguments().get(0),
                                m.getArguments().get(1)
                        );
                    }
                }
                return m;
            }
        };
    }
}
