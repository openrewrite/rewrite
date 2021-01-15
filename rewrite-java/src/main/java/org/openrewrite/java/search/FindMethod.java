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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Validated;
import org.openrewrite.java.JavaIsoProcessor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

import java.util.Set;

import static org.openrewrite.Validated.required;

/**
 * A Java search visitor that will return a list of matching method invocations within the abstract syntax tree.
 * This visitor uses an AspectJ pointcut expression to identify methods by their declaring class, method name, and
 * arguments. The syntax for the expression is:
 * <P><P><B>
 * #declaring class# #method name#(#argument list#)
 * </B><P>
 * <li>The declaring class must be fully qualified.</li>
 * <li>A wildcard character, "*", may be used in either the declaring class or method name.</li>
 * <li>The argument list is expressed as a comma-separated list of the argument types</li>
 * <li>".." can be used in the argument list to match zero or more arguments of any type.</li>
 * <P><PRE>
 * EXAMPLES:
 *
 *      * *(..)                                 - All method invocations
 *      java.util.* *(..)                       - All method invocations to classes belonging to java.util (including sub-packages)
 *      java.util.Collections *(..)             - All method invocations on java.util.Collections class
 *      java.util.Collections unmodifiable*(..) - All method invocations starting with "unmodifiable" on java.util.Collections
 *      java.util.Collections min(..)           - All method invocations for all overloads of "min"
 *      java.util.Collections emptyList()       - All method invocations on java.util.Collections.emptyList()
 *      my.org.MyClass *(boolean, ..)           - All method invocations where the first arg is a boolean in my.org.MyClass
 * </PRE>
 */
public final class FindMethod extends Recipe {
    private String signature;

    public FindMethod() {
        this.processor = () -> new FindMethodProcessor(signature);
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public Validated validate() {
        return required("signature", signature);
    }

    public static Set<J.MethodInvocation> find(J j, String clazz) {
        //noinspection ConstantConditions
        return new FindMethodProcessor(clazz)
                .visit(j, ExecutionContext.builder().build())
                .findMarkedWith(SearchResult.class);
    }

    private static class FindMethodProcessor extends JavaIsoProcessor<ExecutionContext> {
        private final MethodMatcher matcher;

        /**
         * See {@link FindMethod} for details on how the signature should be formatted.
         *
         * @param signature Pointcut expression for matching methods.
         */
        public FindMethodProcessor(String signature) {
            this.matcher = new MethodMatcher(signature);
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (matcher.matches(method)) {
                return m.withMarkers(m.getMarkers().add(new SearchResult()));
            }
            return super.visitMethodInvocation(m, ctx);
        }
    }
}
