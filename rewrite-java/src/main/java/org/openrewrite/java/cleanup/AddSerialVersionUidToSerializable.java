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
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.*;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class AddSerialVersionUidToSerializable extends Recipe {

    @Override
    public String getDisplayName() {
        return "Add `serialVersionUID` to a `Serializable` class when missing";
    }

    @Override
    public String getDescription() {
        return "A `serialVersionUID` field is strongly recommended in all `Serializable` classes. If this is not " +
                "defined on a `Serializable` class, the compiler will generate this value. If a change is later made " +
                "to the class, the generated value will change and attempts to deserialize the class will fail.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-2057");
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            final JavaTemplate template = JavaTemplate.builder(this::getCursor, "private static final long serialVersionUID = 1;").build();

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                // only interested in class declaration variables
                return super.visitMethodDeclaration(method, executionContext);
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
                J.VariableDeclarations varDecls = super.visitVariableDeclarations(multiVariable, executionContext);
                boolean serializedFound = false;
                for (J.VariableDeclarations.NamedVariable v : varDecls.getVariables()) {
                    if ("serialVersionUID".equals((v.getSimpleName()))) {
                        serializedFound = true;
                        doAfterVisit(new MaybeFixSerialVersionUidVar(varDecls));
                        break;
                    }
                }
                if (serializedFound) {
                    getCursor().dropParentUntil(J.ClassDeclaration.class::isInstance).putMessage("has-serial-version-id", Boolean.TRUE);
                }
                return varDecls;
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                Boolean needsSerialVersionId = getCursor().pollMessage("has-serial-version-id");
                if (needsSerialVersionId == null && implementsSerializable(c.getType())) {
                    c = c.withTemplate(template, c.getBody().getCoordinates().firstStatement());
                }
                return c;
            }
        };
    }

    private static final class MaybeFixSerialVersionUidVar extends JavaIsoVisitor<ExecutionContext> {
        private final J.VariableDeclarations searialVersionUidVar;

        public MaybeFixSerialVersionUidVar(J.VariableDeclarations searialVersionUidVar) {
            this.searialVersionUidVar = searialVersionUidVar;
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
            J.VariableDeclarations varDecls = super.visitVariableDeclarations(multiVariable, ctx);
            if (varDecls.equals(searialVersionUidVar)){
                List<J.Modifier> modifiers = varDecls.getModifiers();
                if (!J.Modifier.hasModifier(modifiers, J.Modifier.Type.Private)
                        || !J.Modifier.hasModifier(modifiers, J.Modifier.Type.Static)
                        || !J.Modifier.hasModifier(modifiers, J.Modifier.Type.Final)) {
                    varDecls = varDecls.withModifiers(Arrays.asList(
                            new J.Modifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, J.Modifier.Type.Private, Collections.emptyList()),
                            new J.Modifier(Tree.randomId(), Space.format(" "), Markers.EMPTY, J.Modifier.Type.Static, Collections.emptyList()),
                            new J.Modifier(Tree.randomId(), Space.format(" "), Markers.EMPTY, J.Modifier.Type.Final, Collections.emptyList())
                    ));
                }
                JavaType.Primitive variableType = TypeUtils.asPrimitive(varDecls.getType());
                if (variableType != JavaType.Primitive.Long) {
                    varDecls = varDecls.withTypeExpression(new J.Primitive(Tree.randomId(), Space.EMPTY, Markers.EMPTY, JavaType.Primitive.Long));
                }
                if (multiVariable != varDecls) {
                    return autoFormat(varDecls, ctx).withPrefix(varDecls.getPrefix());
                }
            }
            return varDecls;
        }
    }

    public static boolean implementsSerializable(@Nullable JavaType type) {
        if (type == null) {
            return false;
        } else if (type instanceof JavaType.Primitive) {
            return true;
        } else if (type instanceof JavaType.Array) {
            return implementsSerializable(((JavaType.Array) type).getElemType());
        } else if (type instanceof JavaType.Parameterized) {
            JavaType.Parameterized parameterized = (JavaType.Parameterized) type;
            if (parameterized.isAssignableTo("java.util.Collection") || parameterized.isAssignableTo("java.util.Map")) {
                //If the type is either a collection or a map, make sure the type parameters are serializable. We
                //force all type parameters to be checked to correctly scoop up all non-serializable candidates.
                boolean typeParametersSerializable = true;
                for (JavaType typeParameter : parameterized.getTypeParameters()) {
                    typeParametersSerializable = typeParametersSerializable && implementsSerializable(typeParameter);
                }
                return typeParametersSerializable;
            }
            //All other parameterized types fall through
        } else if (type instanceof JavaType.FullyQualified) {
            JavaType.FullyQualified fq = (JavaType.FullyQualified) type;
            if (fq.getKind() != JavaType.Class.Kind.Interface &&
                    !fq.isAssignableTo("java.lang.Throwable")) {
                return fq.isAssignableTo("java.io.Serializable");
            }
        }
        return false;
    }

}
