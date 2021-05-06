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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.HasTypes;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;

public class BigDecimalRoundingConstantsToEnums extends Recipe {

    private static final String BIG_DECIMAL_FQN = BigDecimal.class.getName();
    private static final String ROUNDING_MODE_FQN = RoundingMode.class.getName();

    private static final MethodMatcher BIG_DECIMAL_DIVIDE_METHOD_MATCHER = new MethodMatcher(BIG_DECIMAL_FQN + " divide(java.math.BigDecimal, int)");
    private static final MethodMatcher BIG_DECIMAL_DIVIDE_WITH_SCALE_METHOD_MATCHER = new MethodMatcher(BIG_DECIMAL_FQN + " divide(java.math.BigDecimal, int, int)");
    private static final MethodMatcher BIG_DECIMAL_SET_SCALE_METHOD_MATCHER = new MethodMatcher(BIG_DECIMAL_FQN + " setScale(int, int)");

    private static final ThreadLocal<JavaParser> JAVA_PARSER_THREAD_LOCAL = ThreadLocal.withInitial(() -> JavaParser.fromJavaVersion().build());

    @Override
    public String getDisplayName() {
        return "`BigDecimal` rounding constants to `RoundingMode` enums";
    }

    @Override
    public String getDescription() {
        return "Convert `BigDecimal` rounding constants to the equivalent `RoundingMode` enum.";
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return new HasTypes(Collections.singletonList(BIG_DECIMAL_FQN)).getVisitor();
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, executionContext);
                if ((BIG_DECIMAL_DIVIDE_METHOD_MATCHER.matches(mi) || BIG_DECIMAL_SET_SCALE_METHOD_MATCHER.matches(mi)) &&
                    isConvertibleBigDecimalConstant(mi.getArguments().get(1))) {
                    mi = mi.withTemplate(template("(#{}, #{})")
                            .imports(ROUNDING_MODE_FQN)
                            .javaParser(JAVA_PARSER_THREAD_LOCAL.get()).build(), mi.getCoordinates().replaceArguments(), mi.getArguments().get(0), getTemplateValue(mi.getArguments().get(1)));
                    maybeAddImport(ROUNDING_MODE_FQN);
                } else if (BIG_DECIMAL_DIVIDE_WITH_SCALE_METHOD_MATCHER.matches(mi) &&
                        isConvertibleBigDecimalConstant(mi.getArguments().get(2))) {
                    mi = mi.withTemplate(template("(#{}, #{}, #{})")
                            .imports(ROUNDING_MODE_FQN)
                            .javaParser(JAVA_PARSER_THREAD_LOCAL.get()).build(), mi.getCoordinates().replaceArguments(), mi.getArguments().get(0), mi.getArguments().get(1), getTemplateValue(mi.getArguments().get(2)));
                    maybeAddImport(ROUNDING_MODE_FQN);
                }
                return mi;
            }

            private boolean isConvertibleBigDecimalConstant(J elem) {
                boolean isBigDecimal = false;
                if (elem instanceof J.Literal) {
                    isBigDecimal = true;
                } else if (elem instanceof J.FieldAccess && ((J.FieldAccess)elem).getTarget().getType() instanceof JavaType.FullyQualified) {
                    J.FieldAccess fa = (J.FieldAccess)elem;
                    if (fa.getTarget().getType() != null && BIG_DECIMAL_FQN.equals(((JavaType.FullyQualified) fa.getTarget().getType()).getFullyQualifiedName())) {
                        isBigDecimal = true;
                    }
                }
                return isBigDecimal;
            }

            @Nullable
            private String getTemplateValue(J elem) {
                String roundingName = null;
                if (elem instanceof J.FieldAccess && ((J.FieldAccess)elem).getTarget().getType() instanceof JavaType.FullyQualified) {
                    J.FieldAccess fa = (J.FieldAccess)elem;
                    if (fa.getTarget().getType() != null && BIG_DECIMAL_FQN.equals(((JavaType.FullyQualified) fa.getTarget().getType()).getFullyQualifiedName())) {
                        roundingName = fa.getSimpleName();
                    }
                } else if (elem instanceof J.Literal){
                    roundingName = ((J.Literal)elem).getValueSource();
                }
                if (roundingName != null) {
                    switch (roundingName) {
                        case "ROUND_UP":
                        case "0":
                            return "RoundingMode.UP";
                        case "ROUND_DOWN":
                        case "1":
                            return "RoundingMode.DOWN";
                        case "ROUND_CEILING":
                        case "2":
                            return "RoundingMode.CEILING";
                        case "ROUND_FLOOR":
                        case "3":
                            return "RoundingMode.FLOOR";
                        case "ROUND_HALF_UP":
                        case "4":
                            return "RoundingMode.HALF_UP";
                        case "ROUND_HALF_DOWN":
                        case "5":
                            return "RoundingMode.HALF_DOWN";
                        case "ROUND_HALF_EVEN":
                        case "6":
                            return "RoundingMode.HALF_EVEN";
                        case "ROUND_UNNECESSARY":
                        case "7":
                            return "RoundingMode.UNNECESSARY";
                        default:
                            break;
                    }
                }
                return null;
            }
        };
    }
}
