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
package org.openrewrite.java.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.marker.JavaSearchResult;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.openrewrite.Tree.randomId;

/**
 * Finds matching method invocations.
 */
@EqualsAndHashCode(callSuper = true)
@Value
public class FindMethods extends Recipe {

    /**
     * A method pattern, expressed as a pointcut expression, that is used to find matching method invocations.
     * See {@link MethodMatcher} for details on the expression's syntax.
     */
    @Option(displayName = "Method pattern",
            description = "A method pattern, expressed as a pointcut expression, that is used to find matching method invocations.",
            example = "java.util.List add(..)")
    String methodPattern;

    UUID id = randomId();

    @Override
    public String getDisplayName() {
        return "Find methods";
    }

    @Override
    public String getDescription() {
        return "Find methods by pattern.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher methodMatcher = new MethodMatcher(methodPattern);
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (methodMatcher.matches(method)) {
                    if(method.getType() != null && method.getType().hasFlags(Flag.Static)) {
                        ctx.putMessageInSet(JavaType.FOUND_TYPE_CONTEXT_KEY,
                                method.getType().getDeclaringType());
                    }
                    m = m.withMarkers(m.getMarkers().addOrUpdate(new JavaSearchResult(id, FindMethods.this)));
                }
                return m;
            }

            @Override
            public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
                J.MemberReference m = super.visitMemberReference(memberRef, ctx);
                if (methodMatcher.matches(m.getReferenceType())) {
                    if(m.getType() != null) {
                        ctx.putMessageInSet(JavaType.FOUND_TYPE_CONTEXT_KEY, m.getType());
                    }
                    m = m.withReference(m.getReference().withMarkers(m.getReference().getMarkers().addOrUpdate(new JavaSearchResult(id, FindMethods.this))));
                }
                return m;
            }
        };
    }

    /**
     * @param j             The subtree to search.
     * @param methodPattern A method pattern. See {@link MethodMatcher} for details about this syntax.
     * @return A set of {@link J.MethodInvocation} and {@link J.MemberReference} representing calls to this method.
     */
    public static Set<J> find(J j, String methodPattern) {
        MethodMatcher methodMatcher = new MethodMatcher(methodPattern);
        JavaIsoVisitor<Set<J>> findVisitor = new JavaIsoVisitor<Set<J>>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Set<J> ms) {
                if (methodMatcher.matches(method)) {
                    ms.add(method);
                }
                return super.visitMethodInvocation(method, ms);
            }

            @Override
            public J.MemberReference visitMemberReference(J.MemberReference memberRef, Set<J> ms) {
                if (methodMatcher.matches(memberRef.getReferenceType())) {
                    ms.add(memberRef);
                }
                return super.visitMemberReference(memberRef, ms);
            }
        };

        Set<J> ms = new HashSet<>();
        findVisitor.visit(j, ms);
        return ms;
    }
}
