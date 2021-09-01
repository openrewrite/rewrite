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
package org.openrewrite.java.internal;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class TypeCache {
    private final J.CompilationUnit cu;
    private final Set<JavaType> typesInUse;
    private final Set<JavaType.Method> declaredMethods;

    public static TypeCache build(J.CompilationUnit cu) {
        Set<JavaType> types = new HashSet<JavaType>() {
            @Override
            public boolean add(@Nullable JavaType javaType) {
                if (javaType != null) {
                    return super.add(javaType);
                }
                return false;
            }
        };

        Set<JavaType.Method> declaredMethods = new HashSet<JavaType.Method>() {
            @Override
            public boolean add(@Nullable JavaType.Method javaType) {
                if (javaType != null) {
                    return super.add(javaType);
                }
                return false;
            }
        };

        new JavaIsoVisitor<Integer>() {

            @Override
            public <N extends NameTree> N visitTypeName(N nameTree, Integer p) {
                visitSpace(nameTree.getPrefix(), Space.Location.ANY, p);
                types.add(nameTree.getType());
                return super.visitTypeName(nameTree, p);
            }

            @Override
            public J.ArrayAccess visitArrayAccess(J.ArrayAccess arrayAccess, Integer p) {
                visitSpace(arrayAccess.getPrefix(), Space.Location.ANY, p);
                types.add(arrayAccess.getType());
                return super.visitArrayAccess(arrayAccess, p);
            }

            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, Integer p) {
                visitSpace(assignment.getPrefix(), Space.Location.ANY, p);
                types.add(assignment.getType());
                return super.visitAssignment(assignment, p);
            }

            @Override
            public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, Integer p) {
                visitSpace(assignOp.getPrefix(), Space.Location.ANY, p);
                types.add(assignOp.getType());
                return super.visitAssignmentOperation(assignOp, p);
            }

            @Override
            public J.Binary visitBinary(J.Binary binary, Integer p) {
                visitSpace(binary.getPrefix(), Space.Location.ANY, p);
                types.add(binary.getType());
                return super.visitBinary(binary, p);
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration c, Integer p) {
                visitSpace(c.getPrefix(), Space.Location.ANY, p);
                for (J.Annotation annotation : c.getAllAnnotations()) {
                    visit(annotation, p);
                }
                if (c.getPadding().getTypeParameters() != null) {
                    visitContainer(c.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, p);
                }
                if (c.getPadding().getExtends() != null) {
                    visitLeftPadded(c.getPadding().getExtends(), JLeftPadded.Location.EXTENDS, p);
                }
                if (c.getPadding().getImplements() != null) {
                    visitContainer(c.getPadding().getImplements(), JContainer.Location.IMPLEMENTS, p);
                }
                visit(c.getBody(), p);
                return c;
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                visitSpace(identifier.getPrefix(), Space.Location.ANY, p);
                types.add(identifier.getType());
                types.add(identifier.getFieldType());
                return super.visitIdentifier(identifier, p);
            }

            @Override
            public J.Import visitImport(J.Import impoort, Integer p) {
                visitSpace(impoort.getPrefix(), Space.Location.ANY, p);
                return impoort;
            }

            @Override
            public J.Package visitPackage(J.Package pkg, Integer p) {
                visitSpace(pkg.getPrefix(), Space.Location.ANY, p);
                for (J.Annotation annotation : pkg.getAnnotations()) {
                    visit(annotation, p);
                }
                return pkg;
            }

            @Override
            public J.InstanceOf visitInstanceOf(J.InstanceOf instanceOf, Integer p) {
                visitSpace(instanceOf.getPrefix(), Space.Location.ANY, p);
                types.add(instanceOf.getType());
                return super.visitInstanceOf(instanceOf, p);
            }

            @Override
            public J.Lambda visitLambda(J.Lambda lambda, Integer p) {
                visitSpace(lambda.getPrefix(), Space.Location.ANY, p);
                types.add(lambda.getType());
                return super.visitLambda(lambda, p);
            }

            @Override
            public J.Literal visitLiteral(J.Literal literal, Integer p) {
                visitSpace(literal.getPrefix(), Space.Location.ANY, p);
                types.add(literal.getType());
                return super.visitLiteral(literal, p);
            }

            @Override
            public J.MemberReference visitMemberReference(J.MemberReference memberRef, Integer p) {
                visitSpace(memberRef.getPrefix(), Space.Location.ANY, p);
                types.add(memberRef.getType());
                types.add(memberRef.getReferenceType());
                return super.visitMemberReference(memberRef, p);
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Integer p) {
                visitSpace(method.getPrefix(), Space.Location.ANY, p);
                declaredMethods.add(method.getType());
                return super.visitMethodDeclaration(method, p);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                visitSpace(method.getPrefix(), Space.Location.ANY, p);
                types.add(method.getType());
                types.add(method.getReturnType());
                return super.visitMethodInvocation(method, p);
            }

            @Override
            public J.MultiCatch visitMultiCatch(J.MultiCatch multiCatch, Integer p) {
                visitSpace(multiCatch.getPrefix(), Space.Location.ANY, p);
                types.add(multiCatch.getType());
                return super.visitMultiCatch(multiCatch, p);
            }

            @Override
            public J.NewArray visitNewArray(J.NewArray newArray, Integer p) {
                visitSpace(newArray.getPrefix(), Space.Location.ANY, p);
                types.add(newArray.getType());
                return super.visitNewArray(newArray, p);
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, Integer p) {
                visitSpace(newClass.getPrefix(), Space.Location.ANY, p);
                types.add(newClass.getType());
                return super.visitNewClass(newClass, p);
            }

            @Override
            public J.ParameterizedType visitParameterizedType(J.ParameterizedType type, Integer p) {
                visitSpace(type.getPrefix(), Space.Location.ANY, p);
                types.add(type.getType());
                return super.visitParameterizedType(type, p);
            }

            @Override
            public J.Primitive visitPrimitive(J.Primitive primitive, Integer p) {
                visitSpace(primitive.getPrefix(), Space.Location.ANY, p);
                types.add(primitive.getType());
                return super.visitPrimitive(primitive, p);
            }

            @Override
            public J.Ternary visitTernary(J.Ternary ternary, Integer p) {
                visitSpace(ternary.getPrefix(), Space.Location.ANY, p);
                types.add(ternary.getType());
                return super.visitTernary(ternary, p);
            }

            @Override
            public J.TypeCast visitTypeCast(J.TypeCast typeCast, Integer p) {
                visitSpace(typeCast.getPrefix(), Space.Location.ANY, p);
                types.add(typeCast.getType());
                return super.visitTypeCast(typeCast, p);
            }

            @Override
            public J.Unary visitUnary(J.Unary unary, Integer p) {
                visitSpace(unary.getPrefix(), Space.Location.ANY, p);
                types.add(unary.getType());
                return super.visitUnary(unary, p);
            }

            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Integer p) {
                visitSpace(variable.getPrefix(), Space.Location.ANY, p);
                types.add(variable.getType());
                return super.visitVariable(variable, p);
            }
        }.visit(cu, 0);

        return new TypeCache(cu, types, declaredMethods);
    }
}
