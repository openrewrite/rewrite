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

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;

public class TypeUtils {
    private TypeUtils() {
    }

    public static List<JavaType.Variable> getVisibleSupertypeMembers(@Nullable JavaType type) {
        JavaType.Class classType = TypeUtils.asClass(type);
        return classType == null ? emptyList() : classType.getVisibleSupertypeMembers();
    }

    public static boolean isString(@Nullable JavaType type) {
        return type == JavaType.Primitive.String ||
                ( type instanceof JavaType.Class &&
                        "java.lang.String".equals(((JavaType.Class) type).getFullyQualifiedName())
                );
    }

    public static boolean isOfClassType(@Nullable JavaType type, String fqn) {
        JavaType.Class classType = asClass(type);
        return classType != null && classType.getFullyQualifiedName().equals(fqn);
    }

    public static boolean isAssignableTo(@Nullable JavaType to, @Nullable JavaType from) {
        if (from == JavaType.Class.OBJECT) {
            return to == JavaType.Class.OBJECT;
        }

        JavaType.Class classTo = asClass(to);
        JavaType.Class classFrom = asClass(from);

        if (classTo == null || classFrom == null) {
            return false;
        }

        if (classTo.getFullyQualifiedName().equals(classFrom.getFullyQualifiedName()) ||
                isAssignableTo(to, classFrom.getSupertype()) ||
                classFrom.getInterfaces().stream().anyMatch(i -> isAssignableTo(to, i))) {
            return true;
        }

        return false;
    }

    @Nullable
    public static JavaType.Class asClass(@Nullable JavaType type) {
        return type instanceof JavaType.Class ? (JavaType.Class) type : null;
    }

    @Nullable
    public static JavaType.Array asArray(@Nullable JavaType type) {
        return type instanceof JavaType.Array ? (JavaType.Array) type : null;
    }

    @Nullable
    public static JavaType.GenericTypeVariable asGeneric(@Nullable JavaType type) {
        return type instanceof JavaType.GenericTypeVariable ? (JavaType.GenericTypeVariable) type : null;
    }

    @Nullable
    public static JavaType.Method asMethod(@Nullable JavaType type) {
        return type instanceof JavaType.Method ? (JavaType.Method) type : null;
    }

    @Nullable
    public static JavaType.Primitive asPrimitive(@Nullable JavaType type) {
        return type instanceof JavaType.Primitive ? (JavaType.Primitive) type : null;
    }

    @Nullable
    public static JavaType.FullyQualified asFullyQualified(@Nullable JavaType type) {
        return type instanceof JavaType.FullyQualified ? (JavaType.FullyQualified) type : null;
    }

    public static boolean hasElementType(@Nullable JavaType type, String fullyQualifiedName) {
        if (type instanceof JavaType.Array) {
            return hasElementType(((JavaType.Array) type).getElemType(), fullyQualifiedName);
        }
        if (type instanceof JavaType.Class) {
            return ((JavaType.Class) type).getFullyQualifiedName().equals(fullyQualifiedName);
        }
        if (type instanceof JavaType.GenericTypeVariable) {
            return ((JavaType.GenericTypeVariable) type).getFullyQualifiedName().equals(fullyQualifiedName);
        }
        return false;
    }

    static boolean deepEquals(List<? extends JavaType> ts1, List<? extends JavaType> ts2) {
        if (ts1.size() != ts2.size()) {
            return false;
        }

        for (int i = 0; i < ts1.size(); i++) {
            JavaType t1 = ts1.get(i);
            JavaType t2 = ts2.get(i);
            if (t1 == null) {
                if (t2 != null) {
                    return false;
                }
            } else if (!deepEquals(t1, t2)) {
                return false;
            }
        }

        return true;
    }
    static boolean deepEquals(@Nullable JavaType t, @Nullable JavaType t2) {
        return t == null ? t2 == null : t == t2 || t.deepEquals(t2);
    }
}
