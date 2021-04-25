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
package org.openrewrite.java.typegeneration;

import org.openrewrite.Incubating;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;

import java.util.*;

@Incubating(since="7.5.0")
public class TypeExtractionVisitor extends JavaIsoVisitor<TypeInformation> {

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDeclaration, TypeInformation typeInformation) {
        typeInformation.addDeclaredType(classDeclaration.getType());
        return super.visitClassDeclaration(classDeclaration, typeInformation);
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, TypeInformation typeInformation) {
        typeInformation.maybeAddMethod(method.getType());
        return super.visitMethodDeclaration(method, typeInformation);
    }

    @Override
    public J.Annotation visitAnnotation(J.Annotation annotation, TypeInformation typeInformation) {
        J.Annotation a = super.visitAnnotation(annotation, typeInformation);
        @SuppressWarnings("ConstantConditions")
        JavaType.FullyQualified type = a == null ? null : TypeUtils.asFullyQualified(a.getAnnotationType().getType());
        if (type == null || a.getArguments() == null || isKnowType(type)) {
            return a;
        }

        //Each argument of the an annotation maps to a method in the annotation type.
        for (Expression expression : a.getArguments()) {
            if (expression.getType() == null) continue;

            String name = "value";
            if (expression instanceof J.Assignment) {
                //If the expression is an assignment, use the variable name as the method name.
                J.Assignment assignment = (J.Assignment) expression;
                if (assignment.getVariable() instanceof J.Identifier) {
                    name = ((J.Identifier) assignment.getVariable()).getSimpleName();
                }
            }
            JavaType.Method.Signature signature = new JavaType.Method.Signature(expression.getType(), Collections.emptyList());
            JavaType.Method method = JavaType.Method.build(1, type, Collections.emptyList(), name, null, signature, Collections.emptyList(), Collections.emptyList());
            typeInformation.maybeAddType(type);
            typeInformation.maybeAddMethod(method);
        }

        return a;
    }

    @Override
    public <N extends NameTree> N visitTypeName(N name, TypeInformation typeInformation) {
        typeInformation.maybeAddType(name.getType());
        return super.visitTypeName(name, typeInformation);
    }

    @Override
    public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, TypeInformation typeInformation) {
        typeInformation.maybeAddType(fieldAccess.getTarget().getType());
        return super.visitFieldAccess(fieldAccess, typeInformation);
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, TypeInformation typeInformation) {
        typeInformation.maybeAddMethod(method.getType());
        return super.visitMethodInvocation(method, typeInformation);
    }

    @Override
    public J.MemberReference visitMemberReference(J.MemberReference memberRef, TypeInformation typeInformation) {
        typeInformation.maybeAddMethod(memberRef.getReferenceMethodType());
        return super.visitMemberReference(memberRef, typeInformation);
    }

    @Override
    public J.Lambda visitLambda(J.Lambda lambda, TypeInformation typeInformation) {
        if (lambda.getReferenceMethodType() != null) {
            typeInformation.maybeAddMethod(lambda.getReferenceMethodType());
        }
        return super.visitLambda(lambda, typeInformation);
    }

    @Override
    public J.Try visitTry(J.Try _try, TypeInformation typeInformation) {
        return super.visitTry(_try, typeInformation);
    }

    private boolean isKnowType(JavaType type) {
        if (type instanceof JavaType.FullyQualified) {
            return ((JavaType.FullyQualified) type).getPackageName().startsWith("java.");
        }
        return false;
    }
}