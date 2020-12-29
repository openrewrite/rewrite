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

import org.openrewrite.Tree;
import org.openrewrite.java.AbstractJavaSourceVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

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
public class FindMethods extends AbstractJavaSourceVisitor<List<J.MethodInvocation>> {
    private final MethodMatcher matcher;

    /**
     * See {@link FindMethods} for details on how the signature should be formatted.
     *
     * @param signature Pointcut expression for matching methods.
     */
    public FindMethods(String signature) {
        this.matcher = new MethodMatcher(signature);
    }

    @Override
    public List<J.MethodInvocation> defaultTo(Tree t) {
        return emptyList();
    }

    @Override
    public List<J.MethodInvocation> visitMethodInvocation(J.MethodInvocation method) {
        return matcher.matches(method) ? singletonList(method) : super.visitMethodInvocation(method);
    }
}
