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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.format.SpacesVisitor;
import org.openrewrite.java.style.Checkstyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.SpacesStyle;
import org.openrewrite.java.tree.J;

public class MethodParamPad extends Recipe {
    @Override
    public String getDisplayName() {
        return "Method parameter padding";
    }

    @Override
    public String getDescription() {
        return "Fixes whitespace padding between the identifier of a method definition or method invocation and the left parenthesis of the parameter list. " +
                "For example, when configured to remove spacing, `someMethodInvocation (x);` becomes `someMethodInvocation(x)`.";
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new MethodParamPadVisitor();
    }

    private static class MethodParamPadVisitor extends JavaIsoVisitor<ExecutionContext> {
        SpacesStyle spacesStyle;
        MethodParamPadStyle methodParamPadStyle;

        @Nullable
        EmptyForInitializerPadStyle emptyForInitializerPadStyle;

        @Nullable
        EmptyForIteratorPadStyle emptyForIteratorPadStyle;

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            spacesStyle = cu.getStyle(SpacesStyle.class) == null ? IntelliJ.spaces() : cu.getStyle(SpacesStyle.class);
            methodParamPadStyle = cu.getStyle(MethodParamPadStyle.class) == null ? Checkstyle.methodParamPadStyle() : cu.getStyle(MethodParamPadStyle.class);
            emptyForInitializerPadStyle = cu.getStyle(EmptyForInitializerPadStyle.class);
            emptyForIteratorPadStyle = cu.getStyle(EmptyForIteratorPadStyle.class);

            spacesStyle = spacesStyle.withBeforeParentheses(
                    spacesStyle.getBeforeParentheses()
                            .withMethodDeclaration(methodParamPadStyle.getSpace())
                            .withMethodCall(methodParamPadStyle.getSpace())
            );
            return super.visitCompilationUnit(cu, ctx);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);
            if (!methodParamPadStyle.getAllowLineBreaks() && md.getPadding().getParameters().getBefore().getWhitespace().contains("\n")) {
                md = md.getPadding().withParameters(
                        md.getPadding().getParameters().withBefore(
                                md.getPadding().getParameters().getBefore().withWhitespace("")
                        )
                );
            }
            doAfterVisit(new SpacesVisitor<>(spacesStyle, emptyForInitializerPadStyle, emptyForIteratorPadStyle, md));
            return md;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
            if (!methodParamPadStyle.getAllowLineBreaks() && mi.getPadding().getArguments().getBefore().getWhitespace().contains("\n")) {
                mi = mi.getPadding().withArguments(
                        mi.getPadding().getArguments().withBefore(
                                mi.getPadding().getArguments().getBefore().withWhitespace("")
                        )
                );
            }
            doAfterVisit(new SpacesVisitor<>(spacesStyle, emptyForInitializerPadStyle, emptyForIteratorPadStyle, mi));
            return mi;
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
            J.NewClass nc = super.visitNewClass(newClass, ctx);
            if (!methodParamPadStyle.getAllowLineBreaks() && nc.getPadding().getArguments() != null && nc.getPadding().getArguments().getBefore().getWhitespace().contains("\n")) {
                nc = nc.getPadding().withArguments(
                        nc.getPadding().getArguments().withBefore(
                                nc.getPadding().getArguments().getBefore().withWhitespace("")
                        )
                );
            }
            doAfterVisit(new SpacesVisitor<>(spacesStyle, emptyForInitializerPadStyle, emptyForIteratorPadStyle, nc));
            return nc;
        }

    }

}
