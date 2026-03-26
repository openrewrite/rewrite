/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.csharp.tree;

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

public class CSharpTypeUtils {

    /**
     * Check if a type is {@code System.Nullable<T>} (i.e., a nullable value type like {@code int?}).
     *
     * @param type the type to check
     * @return {@code true} if the type is {@code Nullable<T>}
     */
    public static boolean isNullableType(@Nullable JavaType type) {
        JavaType.Parameterized parameterized = TypeUtils.asParameterized(type);
        return parameterized != null &&
               TypeUtils.isOfClassType(parameterized.getType(), "System.Nullable");
    }
}
