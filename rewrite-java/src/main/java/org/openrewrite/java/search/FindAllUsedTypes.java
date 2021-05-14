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

import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;

import java.util.HashSet;
import java.util.Set;

public class FindAllUsedTypes {
    public static Set<JavaType> findAll(J j) {
        Set<JavaType> types = new HashSet<>();
        new JavaIsoVisitor<Set<JavaType>>() {
            @Override
            public <N extends NameTree> N visitTypeName(N nameTree, Set<JavaType> javaTypes) {
                javaTypes.add(nameTree.getType());
                return super.visitTypeName(nameTree, javaTypes);
            }

            @Override
            public J.ArrayAccess visitArrayAccess(J.ArrayAccess arrayAccess, Set<JavaType> javaTypes) {
                javaTypes.add(arrayAccess.getType());
                return super.visitArrayAccess(arrayAccess, javaTypes);
            }

            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, Set<JavaType> javaTypes) {
                javaTypes.add(assignment.getType());
                return super.visitAssignment(assignment, javaTypes);
            }

            @Override
            public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, Set<JavaType> javaTypes) {
                javaTypes.add(assignOp.getType());
                return super.visitAssignmentOperation(assignOp, javaTypes);
            }

            @Override
            public J.Binary visitBinary(J.Binary binary, Set<JavaType> javaTypes) {
                javaTypes.add(binary.getType());
                return super.visitBinary(binary, javaTypes);
            }

            @Override
            public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf, Set<JavaType> javaTypes) {
                javaTypes.add(instanceOf.getType());
                return super.visitInstanceOf(instanceOf, javaTypes);
            }

            @Override
            public J.Lambda visitLambda(J.Lambda lambda, Set<JavaType> javaTypes) {
                javaTypes.add(lambda.getType());
                return super.visitLambda(lambda, javaTypes);
            }

            @Override
            public J.Literal visitLiteral(J.Literal literal, Set<JavaType> javaTypes) {
                javaTypes.add(literal.getType());
                return super.visitLiteral(literal, javaTypes);
            }

            @Override
            public J.MemberReference visitMemberReference(J.MemberReference memberRef, Set<JavaType> javaTypes) {
                javaTypes.add(memberRef.getType());
                javaTypes.add(memberRef.getReferenceType());
                return super.visitMemberReference(memberRef, javaTypes);
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Set<JavaType> javaTypes) {
                javaTypes.add(method.getType());
                return super.visitMethodDeclaration(method, javaTypes);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Set<JavaType> javaTypes) {
                javaTypes.add(method.getType());
                javaTypes.add(method.getReturnType());
                return super.visitMethodInvocation(method, javaTypes);
            }

            @Override
            public J.MultiCatch visitMultiCatch(J.MultiCatch multiCatch, Set<JavaType> javaTypes) {
                javaTypes.add(multiCatch.getType());
                return super.visitMultiCatch(multiCatch, javaTypes);
            }

            @Override
            public J.NewArray visitNewArray(J.NewArray newArray, Set<JavaType> javaTypes) {
                javaTypes.add(newArray.getType());
                return super.visitNewArray(newArray, javaTypes);
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, Set<JavaType> javaTypes) {
                javaTypes.add(newClass.getType());
                return super.visitNewClass(newClass, javaTypes);
            }

            @Override
            public J.ParameterizedType visitParameterizedType(J.ParameterizedType type, Set<JavaType> javaTypes) {
                javaTypes.add(type.getType());
                return super.visitParameterizedType(type, javaTypes);
            }

            @Override
            public J.Primitive visitPrimitive(J.Primitive primitive, Set<JavaType> javaTypes) {
                javaTypes.add(primitive.getType());
                return super.visitPrimitive(primitive, javaTypes);
            }

            @Override
            public J.Ternary visitTernary(J.Ternary ternary, Set<JavaType> javaTypes) {
                javaTypes.add(ternary.getType());
                return super.visitTernary(ternary, javaTypes);
            }

            @Override
            public J.TypeCast visitTypeCast(J.TypeCast typeCast, Set<JavaType> javaTypes) {
                javaTypes.add(typeCast.getType());
                return super.visitTypeCast(typeCast, javaTypes);
            }

            @Override
            public J.Unary visitUnary(J.Unary unary, Set<JavaType> javaTypes) {
                javaTypes.add(unary.getType());
                return super.visitUnary(unary, javaTypes);
            }

            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Set<JavaType> javaTypes) {
                javaTypes.add(variable.getType());
                return super.visitVariable(variable, javaTypes);
            }
        }.visit(j, types);
        return types;
    }
}
