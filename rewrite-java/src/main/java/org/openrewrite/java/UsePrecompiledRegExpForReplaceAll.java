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

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.regex.Pattern;

import static org.openrewrite.Tree.randomId;

public class UsePrecompiledRegExpForReplaceAll extends Recipe {

    public static final String REG_EXP_KEY = "regExp";

    @Override
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    @Override
    public String getDescription() {
        return getClass().getSimpleName() + ".";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ReplaceVisitor();
    }

    public static class ReplaceVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final String PATTERN_VAR = "openRewriteReplaceAllPatternVar";
        JavaTemplate matcherTemplate = JavaTemplate
                .builder("#{any()}.matcher( #{any(java.lang.CharSequence)}).replaceAll( #{any(java.lang.String)})")
                .contextSensitive()
                .imports("java.util.regex.Pattern")
                .build();
        JavaTemplate compilePatternTemplate = JavaTemplate
                .builder("private static final java.util.regex.Pattern " + PATTERN_VAR + "= Pattern.compile(#{});")
                .contextSensitive()
                .imports("java.util.regex.Pattern")
                .build();

        @Override
        public J.Block visitBlock(final J.Block block, final ExecutionContext executionContext) {
            final J.Block processedBlock = super.visitBlock(block, executionContext);
            final Cursor parentOrThrow = getCursor().getParentOrThrow();
            if (parentOrThrow.getValue() instanceof J.ClassDeclaration) {
                Object regExp = parentOrThrow.pollNearestMessage(REG_EXP_KEY);
                if (regExp == null) {
                    return processedBlock;
                }
                return super.visitBlock(
                        compilePatternTemplate.apply(
                                getCursor(),
                                processedBlock.getCoordinates().firstStatement(),
                                regExp),
                        executionContext);
            }
            return processedBlock;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation invocation = super.visitMethodInvocation(method, executionContext);
            JavaType.Method methodType = invocation.getMethodType();
            if (methodType == null
                    || !"java.lang.String".equals(methodType.getDeclaringType().getFullyQualifiedName())
                    || !"replaceAll".equals(invocation.getName().getSimpleName())) {
                return invocation;
            }
            Object varIdentifier = new J.Identifier(
                    randomId(),
                    Space.SINGLE_SPACE,
                    Markers.EMPTY,
                    PATTERN_VAR,
                    JavaType.buildType(Pattern.class.getName()),
                    null);

            getCursor().putMessageOnFirstEnclosing(J.ClassDeclaration.class, REG_EXP_KEY, invocation.getArguments().get(0));
            return matcherTemplate.apply(
                    getCursor(),
                    invocation.getCoordinates().replace(),
                    varIdentifier, invocation.getSelect(),
                    invocation.getArguments().get(1));
        }
    }
}
