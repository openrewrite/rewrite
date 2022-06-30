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
package org.openrewrite.java.tree;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.dataflow2.ProgramPoint;

public interface Statement extends J, ProgramPoint {
    default boolean hasClassType(@Nullable JavaType.Class classType) {
        if (classType == null) {
            return false;
        }

        if (!(this instanceof VariableDeclarations)) {
            return false;
        }

        VariableDeclarations variable = (VariableDeclarations) this;

        if (variable.getTypeExpression() == null) {
            return false;
        }

        return TypeUtils.isOfClassType(variable.getTypeExpression().getType(), classType.getFullyQualifiedName());
    }

    CoordinateBuilder.Statement getCoordinates();
}
