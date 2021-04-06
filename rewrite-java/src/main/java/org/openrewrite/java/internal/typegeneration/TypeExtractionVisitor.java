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
package org.openrewrite.java.internal.typegeneration;

import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;


public class TypeExtractionVisitor extends JavaIsoVisitor<TypeInformation> {

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, TypeInformation typeInformation) {
        if (cu == null) {
            return null;
        }
        if (cu.getPackageDeclaration() != null) {
            typeInformation.setSourcePackage(cu.getPackageDeclaration().getExpression().print());
        }
        return super.visitCompilationUnit(cu, typeInformation);
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, TypeInformation typeInformation) {
        typeInformation.addSourceClass(classDecl.getType());
        return super.visitClassDeclaration(classDecl, typeInformation);
    }

    @Override
    public Expression visitExpression(Expression expression, TypeInformation typeInformation) {
        if (expression != null && expression.getType() != null) {
            typeInformation.maybeAddType(expression.getType());
        }
        return super.visitExpression(expression, typeInformation);
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, TypeInformation typeInformation) {
        if (method != null) {
            typeInformation.maybeAddMethod(method.getType());
        }
        return super.visitMethodInvocation(method, typeInformation);
    }
}