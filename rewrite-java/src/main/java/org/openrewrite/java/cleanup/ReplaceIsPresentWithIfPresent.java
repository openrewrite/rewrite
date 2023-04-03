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
package org.openrewrite.java.cleanup;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceIsPresentWithIfPresent extends Recipe {

    private static final MethodMatcher OPTIONAL_IS_PRESENT = new MethodMatcher("java.util.Optional isPresent()");
    private static final MethodMatcher OPTIONAL_GET = new MethodMatcher("java.util.Optional get()");

    @Override
    public String getDisplayName() {
        return "Replace Optional#isPresent with Optional#IfPresent";
    }

    @Override
    public String getDescription() {
        return "Replace Optional#isPresent with Optional#IfPresent. Please note that this recipe is only suitable for if-blocks that lack an Else-block and have a single condition applied.";
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new ReplaceIsPresentWithIfPresentVisitor();
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(OPTIONAL_IS_PRESENT);
    }

    private static class ReplaceIsPresentWithIfPresentVisitor extends JavaVisitor<ExecutionContext> {

        @Override
        public J visitIf(J.If _if, ExecutionContext context) {
            J.If __if = (J.If) super.visitIf(_if, context);
            if (__if != _if) {
                /* handle J.If ancestors */
                if (!(__if.getIfCondition().getTree() instanceof J.MethodInvocation) || !OPTIONAL_IS_PRESENT.matches((J.MethodInvocation) __if.getIfCondition().getTree())) {
                    return __if;
                }
                /* check if parent is else-if */
                if (getCursor().getParent().getParent().getValue() instanceof J.If.Else) {
                    return _if;
                }
                /* check if return statement is present */
                if(isReturnPresentVisitor.isReturnPresent(__if).get()){
                    return _if;
                }

                /* replace if block with Optional#ifPresent and lambda expression */
                String methodSelector = ((J.MethodInvocation) __if.getIfCondition().getTree()).getSelect().toString();
                String template = String.format("%s.ifPresent((obj) -> #{any()})", methodSelector);
                J ifPresentMi = __if.withTemplate(JavaTemplate.builder(this::getCursor, template).build(), __if.getCoordinates().replace(), __if.getThenPart());

                /* replace Optional#get to lambda parameter */
                J.Identifier lambdaParameterIdentifier = ((J.VariableDeclarations) ((J.Lambda) ((J.MethodInvocation) ifPresentMi).getArguments().get(0)).getParameters().getParameters().get(0)).getVariables().get(0).getName();
                return ReplaceMethodCallWithStringVisitor.replace(ifPresentMi, context, lambdaParameterIdentifier, methodSelector);
            }
            return __if;
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
            J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, p);
            /*
             * Do Nothing if method invocation is not Optional#isPresent OR
             * if method invocation is Optional#isPresent but it is not part of J.If
             * */
            if (!OPTIONAL_IS_PRESENT.matches(mi) || !isIfCondition()) {
                return mi;
            }
            /* Do nothing if J.If has Else part OR J.If control parentheses has multiple conditions */
            J.If _if = getJIF();
            if (!Objects.isNull(_if.getElsePart()) || !(_if.getIfCondition().getTree() instanceof J.MethodInvocation)) {
                return mi;
            }
            /* Check for assignments to variable declared ouside the scope of J.If block*/
            if (!isVariableAssignmentLambdaCompatible(_if)) {
                return mi;
            }
            /* Add marker to notify visitIf that the method is found. */
            return SearchResult.found(mi);
        }

        private J.If getJIF() {
            return (getCursor().dropParentUntil(is -> is instanceof J.If || is instanceof J.CompilationUnit)).getValue();
        }

        private boolean isIfCondition() {
            /* Check if current mi is part of J.If condition*/
            Cursor maybeControlParentheses = getCursor().dropParentUntil(is -> is instanceof J.ControlParentheses || is instanceof J.CompilationUnit);
            return maybeControlParentheses.getValue() instanceof J.ControlParentheses && maybeControlParentheses.getParent().getValue() instanceof J.If;
        }

        private boolean isVariableAssignmentLambdaCompatible(J.If __if) {
            /* Check if any assignments to variables declared outside the scope of If Block*/
            Set<String> declaredVariables = new HashSet<>();
            for (J statement : ((J.Block) __if.getThenPart()).getStatements()) {
                if (statement instanceof J.VariableDeclarations) {
                    ((J.VariableDeclarations) statement).getVariables().forEach((namedVariable -> declaredVariables.add(namedVariable.getName().getSimpleName())));
                }
                if (statement instanceof J.Assignment) {
                    if (!declaredVariables.contains(((J.Identifier) ((J.Assignment) statement).getVariable()).getSimpleName())) {
                        return false;
                    }
                }
            }
            return true;
        }

    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class ReplaceMethodCallWithStringVisitor extends JavaVisitor<ExecutionContext> {

        J.Identifier lambdaParameterIdentifier;
        String methodSelector;

        static J replace(J subtree, ExecutionContext p, J.Identifier lambdaParameterIdentifier, String methodSelector) {
            return new ReplaceMethodCallWithStringVisitor(lambdaParameterIdentifier,methodSelector).visit(subtree, p);
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
            J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, p);

            /* Only replace method invocations that has same method selector as present in if condition */
            if (OPTIONAL_GET.matches(mi)) {
                if(mi.getSelect().toString().equals(methodSelector))
                    return lambdaParameterIdentifier;
            }
            return mi;
        }

    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class isReturnPresentVisitor extends JavaVisitor<AtomicBoolean> {

        static AtomicBoolean isReturnPresent(J subtree) {
            return new isReturnPresentVisitor().reduce(subtree,new AtomicBoolean());
        }

        @Override
        public J visitReturn(J.Return _return, AtomicBoolean isPresent){
            if(isPresent.get()){
                return _return;
            }
            isPresent.set(true);
            return _return;
        }
    }
}