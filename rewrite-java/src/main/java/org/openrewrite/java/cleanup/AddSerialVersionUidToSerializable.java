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

import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.*;
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
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            final JavaTemplate template = JavaTemplate.builder(this::getCursor, "private static final long serialVersionUID = 1;").build();

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                // Anonymous classes are not of interest
                return method;
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
                // Anonymous classes are not of interest
                return multiVariable;
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                if(c.getKind() != J.ClassDeclaration.Kind.Type.Class || !requiresSerialVersionField(classDecl.getType())) {
                    return c;
                }
                AtomicBoolean needsSerialVersionId = new AtomicBoolean(true);
                J.Block body = c.getBody();
                c = c.withBody(c.getBody().withStatements(ListUtils.map(c.getBody().getStatements(), s -> {
                    if(!(s instanceof J.VariableDeclarations)) {
                        return s;
                    }
                    J.VariableDeclarations varDecls = (J.VariableDeclarations) s;
                    for(J.VariableDeclarations.NamedVariable v : varDecls.getVariables()) {
                        if("serialVersionUID".equals(v.getSimpleName())) {
                            needsSerialVersionId.set(false);
                            return maybeAutoFormat(varDecls, maybeFixVariableDeclarations(varDecls), ctx, new Cursor(getCursor(), body));
                        }
                    }
                    return s;
                })));
                if (needsSerialVersionId.get()) {
                    c = c.withTemplate(template, c.getBody().getCoordinates().firstStatement());
                }
                return c;
            }

            private J.VariableDeclarations maybeFixVariableDeclarations(J.VariableDeclarations varDecls) {
                List<J.Modifier> modifiers = varDecls.getModifiers();
                if (!J.Modifier.hasModifier(modifiers, J.Modifier.Type.Private)
                        || !J.Modifier.hasModifier(modifiers, J.Modifier.Type.Static)
                        || !J.Modifier.hasModifier(modifiers, J.Modifier.Type.Final)) {
                    varDecls = varDecls.withModifiers(Arrays.asList(
                            new J.Modifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, J.Modifier.Type.Private, Collections.emptyList()),
                            new J.Modifier(Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY, J.Modifier.Type.Static, Collections.emptyList()),
                            new J.Modifier(Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY, J.Modifier.Type.Final, Collections.emptyList())
                    ));
                }
                if (TypeUtils.asPrimitive(varDecls.getType()) != JavaType.Primitive.Long) {
                    varDecls = varDecls.withTypeExpression(new J.Primitive(Tree.randomId(), Space.EMPTY, Markers.EMPTY, JavaType.Primitive.Long));
                }
                return varDecls;
            }

            private boolean requiresSerialVersionField(@Nullable JavaType type) {
                if (type == null) {
                    return false;
                } else if (type instanceof JavaType.Primitive) {
                    return true;
                } else if (type instanceof JavaType.Array) {
                    return requiresSerialVersionField(((JavaType.Array) type).getElemType());
                } else if (type instanceof JavaType.Parameterized) {
                    JavaType.Parameterized parameterized = (JavaType.Parameterized) type;
                    if (parameterized.isAssignableTo("java.util.Collection") || parameterized.isAssignableTo("java.util.Map")) {
                        //If the type is either a collection or a map, make sure the type parameters are serializable. We
                        //force all type parameters to be checked to correctly scoop up all non-serializable candidates.
                        boolean typeParametersSerializable = true;
                        for (JavaType typeParameter : parameterized.getTypeParameters()) {
                            typeParametersSerializable = typeParametersSerializable && requiresSerialVersionField(typeParameter);
                        }
                        return typeParametersSerializable;
                    }
                    //All other parameterized types fall through
                } else if (type instanceof JavaType.FullyQualified) {
                    JavaType.FullyQualified fq = (JavaType.FullyQualified) type;
                    if (fq.getKind() == JavaType.Class.Kind.Enum) return false;

                    if (fq.getKind() != JavaType.Class.Kind.Interface &&
                            !fq.isAssignableTo("java.lang.Throwable")) {
                        return fq.isAssignableTo("java.io.Serializable");
                    }
                }
                return false;
            }
        };
    }

}
