/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.rewrite.tree;

import com.netflix.rewrite.internal.lang.Nullable;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;

public class TypeUtils {
    private TypeUtils() {
    }

    public static List<Type.Var> getVisibleSupertypeMembers(@Nullable Type type) {
        Type.Class classType = TypeUtils.asClass(type);
        return classType == null ? emptyList() : classType.getVisibleSupertypeMembers();
    }

    public static boolean isString(@Nullable Type type) {
        return type instanceof Type.Class && "java.lang.String".equals(((Type.Class) type).getFullyQualifiedName());
    }

    public static boolean isOfClassType(@Nullable Type type, String fqn) {
        Type.Class classType = asClass(type);
        return classType != null && classType.getFullyQualifiedName().equals(fqn);
    }

    @Nullable
    public static Type.Class asClass(@Nullable Type type) {
        return type instanceof Type.Class ? (Type.Class) type : null;
    }

    @Nullable
    public static Type.Array asArray(@Nullable Type type) {
        return type instanceof Type.Array ? (Type.Array) type : null;
    }

    @Nullable
    public static Type.GenericTypeVariable asGeneric(@Nullable Type type) {
        return type instanceof Type.GenericTypeVariable ? (Type.GenericTypeVariable) type : null;
    }

    @Nullable
    public static Type.Method asMethod(@Nullable Type type) {
        return type instanceof Type.Method ? (Type.Method) type : null;
    }

    @Nullable
    public static Type.Primitive asPrimitive(@Nullable Type type) {
        return type instanceof Type.Primitive ? (Type.Primitive) type : null;
    }

    public static boolean hasElementType(@Nullable Type type, String fullyQualifiedName) {
        if (type instanceof Type.Array) {
            return hasElementType(((Type.Array) type).getElemType(), fullyQualifiedName);
        }
        if (type instanceof Type.Class) {
            return ((Type.Class) type).getFullyQualifiedName().equals(fullyQualifiedName);
        }
        if (type instanceof Type.GenericTypeVariable) {
            return ((Type.GenericTypeVariable) type).getFullyQualifiedName().equals(fullyQualifiedName);
        }
        return false;
    }

    static boolean deepEquals(List<? extends Type> ts1, List<? extends Type> ts2) {
        if (ts1.size() != ts2.size()) {
            return false;
        }

        for (int i = 0; i < ts1.size(); i++) {
            Type t1 = ts1.get(i);
            Type t2 = ts2.get(i);
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

    static boolean deepEquals(@Nullable Type t, @Nullable Type t2) {
        return t == null ? t2 == null : t.deepEquals(t2);
    }
}
