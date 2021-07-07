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

import java.util.List;

import static java.util.Collections.emptyList;

public class TypeUtils {
    private TypeUtils() {
    }

    public static List<JavaType.Variable> getVisibleSupertypeMembers(@Nullable JavaType type) {
        JavaType.FullyQualified classType = TypeUtils.asFullyQualified(type);
        return classType == null ? emptyList() : classType.getVisibleSupertypeMembers();
    }

    public static boolean isString(@Nullable JavaType type) {
        return type == JavaType.Primitive.String ||
                ( type instanceof JavaType.FullyQualified &&
                        "java.lang.String".equals(((JavaType.FullyQualified) type).getFullyQualifiedName())
                );
    }

    public static boolean isOfType(@Nullable JavaType type1, @Nullable JavaType type2) {
        if(type1 == null || type2 == null) {
            return false;
        }
        // Strings, uniquely amongst all other types, can be either primitives or classes depending on the context
        if(TypeUtils.isString(type1) && TypeUtils.isString(type2)) {
            return true;
        }
        if(type1 instanceof JavaType.Primitive && type2 instanceof JavaType.Primitive) {
            return ((JavaType.Primitive) type1).getKeyword().equals(((JavaType.Primitive)type2).getKeyword());
        }
        if(type1 instanceof JavaType.FullyQualified && type2 instanceof JavaType.FullyQualified) {
            return ((JavaType.FullyQualified) type1).getFullyQualifiedName().equals(((JavaType.FullyQualified) type2).getFullyQualifiedName());
        }
        if(type1 instanceof JavaType.Array && type2 instanceof JavaType.Array) {
            return isOfType(((JavaType.Array)type1).getElemType(), ((JavaType.Array)type2).getElemType());
        }

        return type1.deepEquals(type2);
    }

    public static boolean isOfClassType(@Nullable JavaType type, String fqn) {
        JavaType.FullyQualified classType = asFullyQualified(type);
        return classType != null && classType.getFullyQualifiedName().equals(fqn);
    }

    public static boolean isAssignableTo(@Nullable JavaType to, @Nullable JavaType from) {
        if (from == JavaType.Class.OBJECT) {
            return to == JavaType.Class.OBJECT;
        }

        JavaType.FullyQualified classTo = asFullyQualified(to);
        JavaType.FullyQualified classFrom = asFullyQualified(from);

        if (classTo == null || classFrom == null) {
            return false;
        }

        return classTo.getFullyQualifiedName().equals(classFrom.getFullyQualifiedName()) ||
                isAssignableTo(to, classFrom.getSupertype()) ||
                classFrom.getInterfaces().stream().anyMatch(i -> isAssignableTo(to, i));
    }

    /**
     * @deprecated This method is being deprecated, please use asFullyQualified() instead.
     */
    @Nullable
    @Deprecated
    public static JavaType.Class asClass(@Nullable JavaType type) {
        return type instanceof JavaType.Class ? (JavaType.Class) type : null;
    }

    @Nullable
    public static JavaType.Parameterized asParameterized(@Nullable JavaType type) {
        return type instanceof JavaType.Parameterized ? (JavaType.Parameterized) type : null;
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
        if (type instanceof JavaType.Class || type instanceof JavaType.GenericTypeVariable) {
            return isAssignableTo(JavaType.Class.build(fullyQualifiedName), type);
        }
        return false;
    }

    static boolean deepEquals(@Nullable List<? extends JavaType> ts1, @Nullable List<? extends JavaType> ts2) {

        if (ts1 == null || ts2 == null) {
            return ts1 == null && ts2 == null;
        }

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
