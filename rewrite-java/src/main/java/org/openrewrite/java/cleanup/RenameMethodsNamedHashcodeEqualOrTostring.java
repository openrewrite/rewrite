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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class RenameMethodsNamedHashcodeEqualOrTostring extends Recipe {
    @Override
    public String getDisplayName() {
        return "Rename methods named `hashcode`, `equal`, or `tostring`.";
    }

    @Override
    public String getDescription() {
        return "Methods should not be named `hashcode`, `equal`, or `tostring`. " +
                "Any of these are confusing as they appear to be intended as overridden methods from the `Object` base class, despite being case-insensitive.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1221");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(10);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RenameMethodsNamedHashcodeEqualOrTostringVisitor();
    }

    private static class RenameMethodsNamedHashcodeEqualOrTostringVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final MethodMatcher NO_ARGS = new MethodMatcher("*..* *()");
        private static final MethodMatcher OBJECT_ARG = new MethodMatcher("*..* *(java.lang.Object)");

        private static boolean equalsIgnoreCaseExclusive(String inputToCheck, String targetToCheck) {
            return inputToCheck.equalsIgnoreCase(targetToCheck) && !inputToCheck.equals(targetToCheck);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            if (method.getType() != null && method.getReturnTypeExpression() != null) {
                String sn = method.getSimpleName();
                JavaType rte = method.getReturnTypeExpression().getType();
                JavaType t = method.getType();
                if (equalsIgnoreCaseExclusive(sn, "hashCode") && JavaType.Primitive.Int.equals(rte) && NO_ARGS.matches(t)) {
                    doAfterVisit(new ChangeMethodName(MethodMatcher.methodPattern(method), "hashCode", true));
                } else if (sn.equalsIgnoreCase("equal") && JavaType.Primitive.Boolean.equals(rte) && OBJECT_ARG.matches(t)) {
                    doAfterVisit(new ChangeMethodName(MethodMatcher.methodPattern(method), "equals", true));
                } else if (equalsIgnoreCaseExclusive(sn, "toString") && TypeUtils.isString(rte) && NO_ARGS.matches(t)) {
                    doAfterVisit(new ChangeMethodName(MethodMatcher.methodPattern(method), "toString", true));
                }
            }
            return super.visitMethodDeclaration(method, ctx);
        }
    }
}
