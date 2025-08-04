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
package org.openrewrite.java;

import lombok.AllArgsConstructor;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import static java.util.Collections.emptyList;

@AllArgsConstructor
public class FullyQualifyMemberReference<P> extends JavaVisitor<P> {

    private final JavaType memberToFullyQualify;

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu, P p) {
        if (memberToFullyQualify instanceof JavaType.Method || memberToFullyQualify instanceof JavaType.Variable) {
            return super.visitCompilationUnit(cu, p);
        }
        return cu;
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation methodInvocation, P p) {
        J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(methodInvocation, p);
        if (!(memberToFullyQualify instanceof JavaType.Method) || methodInvocation.getSelect() != null ||
                methodInvocation.getMethodType() == null) {
            return mi;
        }
        JavaType.Method m = (JavaType.Method) memberToFullyQualify;
        if (m.getName().equals(methodInvocation.getMethodType().getName()) &&
                m.getDeclaringType().getFullyQualifiedName().equals(methodInvocation.getMethodType().getDeclaringType().getFullyQualifiedName())) {
            return mi.withSelect(toIdentifier(m.getDeclaringType()));
        }
        return mi;
    }

    @Override
    public J visitIdentifier(J.Identifier identifier, P p) {
        if (!isUnqualifiedVarAccess(identifier) || !(this.memberToFullyQualify instanceof JavaType.Variable)) {
            return super.visitIdentifier(identifier, p);
        }

        JavaType.Variable memberToFullyQualify = (JavaType.Variable) this.memberToFullyQualify;
        JavaType.FullyQualified memberToFullyQualifyOwner = TypeUtils.asFullyQualified(memberToFullyQualify.getOwner());
        JavaType.FullyQualified identifierOwner = TypeUtils.asFullyQualified(identifier.getFieldType().getOwner());

        if (memberToFullyQualifyOwner == null || identifierOwner == null) {
            return super.visitIdentifier(identifier, p);
        }

        if (memberToFullyQualifyOwner.getFullyQualifiedName().equals(identifierOwner.getFullyQualifiedName()) &&
                memberToFullyQualify.getName().equals(identifier.getSimpleName())) {
            return toFieldAccess(memberToFullyQualify, identifier);
        }
        return super.visitIdentifier(identifier, p);
    }

    private boolean isUnqualifiedVarAccess(J.Identifier identifier) {
        return identifier.getFieldType() != null // is variable
                && !(getCursor().getParentTreeCursor().getValue() instanceof J.FieldAccess); // not qualified
    }

    private J.Identifier toIdentifier(JavaType.FullyQualified fullyQualified) {
        return new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(),
                fullyQualified.getFullyQualifiedName(), fullyQualified, null);
    }

    private J.FieldAccess toFieldAccess(JavaType.Variable variableType, J.Identifier identifier) {
        return new J.FieldAccess(
                Tree.randomId(),
                identifier.getPrefix(),
                Markers.EMPTY,
                toIdentifier(TypeUtils.asFullyQualified(variableType.getOwner())),
                JLeftPadded.build(identifier.withPrefix(Space.EMPTY)),
                variableType.getType()
        );
    }
}
