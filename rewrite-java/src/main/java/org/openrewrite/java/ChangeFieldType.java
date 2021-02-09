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

@EqualsAndHashCode(callSuper = true)
@Data
public class ChangeFieldType<P> extends JavaIsoVisitor<P> {
    private final JavaType.Class type;
    private final String targetType;

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, P p) {
        JavaType.Class typeAsClass = multiVariable.getTypeAsClass();
        J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, p);
        if (typeAsClass != null && typeAsClass.equals(type)) {
            JavaType.Class type = JavaType.Class.build(targetType);

            maybeAddImport(targetType);
            maybeRemoveImport(typeAsClass);

            mv = mv.withTypeExpression(mv.getTypeExpression() == null ?
                    null :
                    J.Identifier.build(mv.getTypeExpression().getId(),
                            mv.getTypeExpression().getPrefix(),
                            Markers.EMPTY,
                            type.getClassName(),
                            type)
            );

            mv = mv.withVariables(ListUtils.map(mv.getVariables(), var -> {
                JavaType.Class varType = TypeUtils.asClass(var.getType());
                if (varType != null && !varType.equals(type)) {
                    return var.withType(type).withName(var.getName().withType(type));
                }
                return var;
            }));
        }

        return mv;
    }
}
