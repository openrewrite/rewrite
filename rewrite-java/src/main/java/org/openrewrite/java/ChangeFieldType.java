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
package org.openrewrite.java;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

import static java.util.Collections.emptyList;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChangeFieldType<P> extends JavaIsoVisitor<P> {

    private final String oldFullyQualifiedTypeName;
    private final JavaType.FullyQualified newFieldType;

    public ChangeFieldType(JavaType.FullyQualified oldFieldType, JavaType.FullyQualified newFieldType) {
        this.oldFullyQualifiedTypeName = oldFieldType.getFullyQualifiedName();
        this.newFieldType  = newFieldType;
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, P p) {
        JavaType.FullyQualified typeAsClass = multiVariable.getTypeAsFullyQualified();
        J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, p);
        if (typeAsClass != null && oldFullyQualifiedTypeName.equals(typeAsClass.getFullyQualifiedName())) {

            maybeAddImport(newFieldType);
            maybeRemoveImport(typeAsClass);

            mv = mv.withTypeExpression(mv.getTypeExpression() == null ?
                    null :
                    new J.Identifier(mv.getTypeExpression().getId(),
                            mv.getTypeExpression().getPrefix(),
                            Markers.EMPTY,
                            emptyList(),
                            newFieldType.getClassName(),
                            newFieldType,
                            null
                    )
            );

            mv = mv.withVariables(ListUtils.map(mv.getVariables(), var -> {
                JavaType.FullyQualified varType = TypeUtils.asFullyQualified(var.getType());
                if (varType != null && !varType.equals(newFieldType)) {
                    return var.withType(newFieldType).withName(var.getName().withType(newFieldType));
                }
                return var;
            }));
        }

        return mv;
    }
}
