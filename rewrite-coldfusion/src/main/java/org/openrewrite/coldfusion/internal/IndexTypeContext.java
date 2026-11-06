/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.coldfusion.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.coldfusion.CallKey;
import org.openrewrite.coldfusion.ColdFusionIsoVisitor;
import org.openrewrite.coldfusion.TypeContext;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The {@link TypeContext} of one parse, over the parser's index of the repository once it is linked.
 */
public final class IndexTypeContext implements TypeContext {
    private final ComponentIndex index;
    private boolean solving;

    public IndexTypeContext(ComponentIndex index) {
        this.index = index;
    }

    /**
     * Marks when the inference solves, the one step that may change a function's type.
     */
    public void solving(boolean solving) {
        this.solving = solving;
    }

    @Override
    public JavaType.@Nullable FullyQualified resolve(String name, @Nullable Path caller) {
        return index.resolve(name, caller == null ? "" : caller.toString().replace('\\', '/'));
    }

    @Override
    public JavaType.@Nullable Method method(JavaType.@Nullable FullyQualified type, String name) {
        return index.method(type, name);
    }

    @Override
    public JavaType.@Nullable Variable variable(JavaType.@Nullable FullyQualified type, String name) {
        return ComponentIndex.variable(type, name);
    }

    @Override
    public boolean isComponent(@Nullable JavaType type) {
        return index.isComponent(type);
    }

    @Override
    public boolean isAssignedToOtherObjects(JavaType.Method function) {
        return index.isAssignedToOtherObjects(function);
    }

    @Override
    public boolean isDependency(@Nullable JavaType type) {
        return index.isDependency(type);
    }

    @Override
    public List<JavaType.FullyQualified> getComponents() {
        return index.components();
    }

    @Override
    public List<JavaType.FullyQualified> includedTemplates(JavaType.FullyQualified type) {
        return index.mixins(type);
    }

    @Override
    public JavaType.@Nullable FullyQualified sharedScopeVariableType(String dottedName) {
        return index.sharedScopeVariableType(dottedName);
    }

    @Override
    public boolean isColdBoxView(Path sourcePath) {
        return TypeAttribution.isColdBoxView(index, sourcePath.toString());
    }

    @Override
    public JavaType.FullyQualified getArrayType() {
        return BuiltIns.ARRAY;
    }

    @Override
    public JavaType.FullyQualified getStructType() {
        return BuiltIns.STRUCT;
    }

    @Override
    public JavaType.FullyQualified getQueryType() {
        return BuiltIns.QUERY;
    }

    @Override
    public boolean isValueType(@Nullable JavaType type) {
        return BuiltIns.isValueType(type);
    }

    @Override
    public JavaType.@Nullable Method memberFunction(@Nullable JavaType receiver, String name) {
        return BuiltIns.member(receiver, name);
    }

    @Override
    public JavaType.@Nullable FullyQualified javaClass(String className) {
        return JavaClasses.type(className);
    }

    @Override
    public boolean isJava(@Nullable JavaType type) {
        return JavaClasses.isJava(type);
    }

    @Override
    public JavaType.@Nullable Method javaMethod(JavaType.FullyQualified type, String name, int arguments) {
        return JavaClasses.method(type, name, arguments);
    }

    @Override
    public <T extends J> T withType(T tree, JavaType type) {
        return TypeNames.withType(tree, type);
    }

    @Override
    public boolean isPlaceholder(@Nullable Expression initializer) {
        return ComponentIndex.isPlaceholder(initializer);
    }

    @Override
    public Map<UUID, CallKey> callKeys(Cf.CompilationUnit cu) {
        Map<UUID, CallKey> keys = new HashMap<>();
        new ColdFusionIsoVisitor<Integer>() {
            private int position;

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                keys.put(method.getId(), new CallKey(cu.getSourcePath(), position++));
                return super.visitMethodInvocation(method, p);
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, Integer p) {
                keys.put(newClass.getId(), new CallKey(cu.getSourcePath(), position++));
                return super.visitNewClass(newClass, p);
            }

            @Override
            public Cf.Invocation visitInvocation(Cf.Invocation invocation, Integer p) {
                keys.put(invocation.getId(), new CallKey(cu.getSourcePath(), position++));
                return super.visitInvocation(invocation, p);
            }
        }.visit(cu, 0);
        return keys;
    }

    @Override
    public boolean setReturnType(JavaType.Method function, JavaType returnType) {
        requireSolving();
        return index.setReturnType(function, returnType);
    }

    @Override
    public boolean setParameterType(JavaType.Method function, int parameter, JavaType component) {
        requireSolving();
        return index.setParameterType(function, parameter, component);
    }

    private void requireSolving() {
        if (!solving) {
            throw new IllegalStateException("Only TypeInference.solve may change a function's type, since every tree shares it");
        }
    }
}
