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
package org.openrewrite.kotlin;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.kotlin.tree.K;

@Value
@EqualsAndHashCode(callSuper = true)
public class RenameTypeAlias extends Recipe {

    @Option(displayName = "Old alias name",
            description = "Name of the alias type.",
            example = "OldAlias")
    String aliasName;

    @Option(displayName = "New alias name",
            description = "Name of the alias type.",
            example = "NewAlias")
    String newName;

    @Option(displayName = "Target fully qualified type",
            description = "Fully-qualified class name of the aliased type.",
            example = "org.junit.Assume")
    String fullyQualifiedAliasedType;


    @Override
    public String getDisplayName() {
        return "Rename type alias";
    }

    @Override
    public String getDescription() {
        return "Change the name of a given type alias.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new KotlinIsoVisitor<ExecutionContext>() {
            @Override
            public K.TypeAlias visitTypeAlias(K.TypeAlias typeAlias, ExecutionContext executionContext) {
                if (!aliasName.equals(typeAlias.getSimpleName()) || !TypeUtils.isOfClassType(typeAlias.getType(), fullyQualifiedAliasedType)) {
                    return typeAlias;
                }
                return typeAlias.withName(typeAlias.getName().withSimpleName(newName));
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier i, ExecutionContext ctx) {
                if (!i.getSimpleName().equals(aliasName) || !TypeUtils.isOfClassType(i.getType(), fullyQualifiedAliasedType)) {
                    return i;
                }
                if (!isVariableName(getCursor().getParentTreeCursor(), i) ||
                        isAliasImport(getCursor().getParentTreeCursor(), i)) {
                    i = i.withSimpleName(newName);
                }
                return i;
            }
        };
    }

    private boolean isVariableName(Cursor cursor, J.Identifier ident) {
        Object value = cursor.getValue();
        if (value instanceof J.MethodInvocation) {
            J.MethodInvocation m = (J.MethodInvocation) value;
            return m.getName() != ident;
        } else if (value instanceof J.NewClass) {
            J.NewClass m = (J.NewClass) value;
            return m.getClazz() != ident;
        } else if (value instanceof J.NewArray) {
            J.NewArray a = (J.NewArray) value;
            return a.getTypeExpression() != ident;
        } else if (value instanceof J.VariableDeclarations) {
            J.VariableDeclarations v = (J.VariableDeclarations) value;
            return ident != v.getTypeExpression();
        } else if (value instanceof J.VariableDeclarations.NamedVariable) {
            Object maybeVd = cursor.getParentTreeCursor().getValue();
            if (maybeVd instanceof J.VariableDeclarations) {
                J.VariableDeclarations vd = (J.VariableDeclarations) maybeVd;
                return vd.getModifiers().stream().noneMatch(x -> x.getType() == J.Modifier.Type.LanguageExtension && "typealias".equals(x.getKeyword()));
            }
            return true;
        } else return !(value instanceof J.ParameterizedType);
    }

    private boolean isAliasImport(Cursor cursor, J.Identifier id) {
        if (cursor.getValue() instanceof J.Import) {
            J.Import ji = cursor.getValue();
            return ji.getAlias() == id;
        }

        return false;
    }
}
