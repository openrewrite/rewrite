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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static org.openrewrite.Tree.randomId;

public class UsePrecompiledRegExpForReplaceAll extends Recipe {

    public static final String REG_EXP_KEY = "regExp";

    @Override
    public String getDisplayName() {
        return "Replace `String.replaceAll` with a compiled regular expression";
    }

    @Override
    public String getDescription() {
        return "Replace `String.replaceAll` with a compiled regular expression.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ReplaceVisitor();
    }

    public static class ReplaceVisitor extends JavaIsoVisitor<ExecutionContext> {
        // use boolean flag to only process one pattern at a time
        private final AtomicBoolean patternProcessingInProgress = new AtomicBoolean(false);
        private static final String PATTERN_VAR = "openRewriteReplaceAllPattern";
        private static final JavaTemplate MATCHER_TEMPLATE = JavaTemplate
                .builder("#{any()}.matcher( #{any(java.lang.CharSequence)}).replaceAll( #{any(java.lang.String)})")
                .contextSensitive()
                .imports("java.util.regex.Pattern")
                .build();
        private static final MethodMatcher REPLACE_ALL_METHOD_MATCHER = new MethodMatcher("java.lang.String replaceAll(String, String)");

        @Override
        public J.Block visitBlock(final J.Block block, final ExecutionContext executionContext) {
            final J.Block processedBlock = super.visitBlock(block, executionContext);
            final Cursor parentOrThrow = getCursor().getParentOrThrow();
            if (parentOrThrow.getValue() instanceof J.ClassDeclaration) {
                final Object regExp = parentOrThrow.pollNearestMessage(REG_EXP_KEY);
                if (regExp == null) {
                    return processedBlock;
                }
                patternProcessingInProgress.set(false);

                final String regExpName = parentOrThrow.pollNearestMessage(REG_EXP_KEY + "_NAME");
                return super.visitBlock(
                        JavaTemplate
                                .builder("private static final java.util.regex.Pattern " + regExpName + " = Pattern.compile(#{});")
                                .contextSensitive()
                                .imports("java.util.regex.Pattern")
                                .build()
                                .apply(
                                        getCursor(),
                                        processedBlock.getCoordinates().firstStatement(),
                                        regExp),
                        executionContext);
            }
            return processedBlock;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation invocation, ExecutionContext executionContext) {
            final boolean alreadyProcessingAPattern = patternProcessingInProgress.get();
            if (!REPLACE_ALL_METHOD_MATCHER.matches(invocation) || alreadyProcessingAPattern) {
                return super.visitMethodInvocation(invocation, executionContext);
            }

            patternProcessingInProgress.set(true);

            final String regexPatternVariableName =
                    VariableNameUtils
                            .generateVariableName(
                                    PATTERN_VAR,
                                    getCursor(),
                                    VariableNameUtils.GenerationStrategy.INCREMENT_NUMBER
                            );

            final Object regexPatternIdentifier = new J.Identifier(
                    randomId(),
                    Space.SINGLE_SPACE,
                    Markers.EMPTY,
                    regexPatternVariableName,
                    JavaType.buildType(Pattern.class.getName()),
                    null);

            final Cursor cursor = updateCursor(invocation);
            cursor.putMessageOnFirstEnclosing(J.ClassDeclaration.class, REG_EXP_KEY, invocation.getArguments().get(0));
            cursor.putMessageOnFirstEnclosing(J.ClassDeclaration.class, REG_EXP_KEY + "_NAME", regexPatternVariableName);

            return super.visitMethodInvocation(MATCHER_TEMPLATE.apply(
                    cursor,
                    invocation.getCoordinates().replace(),
                    regexPatternIdentifier, invocation.getSelect(),
                    invocation.getArguments().get(1)), executionContext);
        }
    }
}
