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
import java.util.Optional;

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
        if (type1 == type2) {
            return true;
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
        if (type1 instanceof JavaType.GenericTypeVariable && type2 instanceof JavaType.GenericTypeVariable) {
            JavaType.GenericTypeVariable generic1 = (JavaType.GenericTypeVariable) type1;
            JavaType.GenericTypeVariable generic2 = (JavaType.GenericTypeVariable) type2;
            if (generic1.getBounds().size() == generic2.getBounds().size()) {
                for (int index = 0; index < generic1.getBounds().size(); index ++) {
                    if (!isOfType(generic1.getBounds().get(index), generic2.getBounds().get(index))) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
        return type1.equals(type2);
    }

    public static boolean isOfClassType(@Nullable JavaType type, String fqn) {
        JavaType.FullyQualified classType = asFullyQualified(type);
        return classType != null && classType.getFullyQualifiedName().equals(fqn);
    }

    public static boolean isAssignableTo(@Nullable JavaType to, @Nullable JavaType from) {
        if (to instanceof JavaType.FullyQualified) {
            JavaType.FullyQualified toFq = (JavaType.FullyQualified) to;
            return isAssignableTo(toFq.getFullyQualifiedName(), from);
        } else if (to instanceof JavaType.GenericTypeVariable){
            JavaType.GenericTypeVariable genericTo = (JavaType.GenericTypeVariable) to;
            for (JavaType bound : genericTo.getBounds()) {
                if (isAssignableTo(bound, from)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isAssignableTo(String to, @Nullable JavaType from) {

        if (from instanceof  JavaType.FullyQualified) {
            JavaType.FullyQualified classFrom = (JavaType.FullyQualified) from;

            return to.equals(classFrom.getFullyQualifiedName()) ||
                    isAssignableTo(to, classFrom.getSupertype()) ||
                    classFrom.getInterfaces().stream().anyMatch(i -> isAssignableTo(to, i));
        } else if (from instanceof JavaType.GenericTypeVariable) {
            JavaType.GenericTypeVariable genericFrom = (JavaType.GenericTypeVariable) from;
            for (JavaType bound : genericFrom.getBounds()) {
                if (isAssignableTo(to, bound)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
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
    public static JavaType.Primitive asPrimitive(@Nullable JavaType type) {
        return type instanceof JavaType.Primitive ? (JavaType.Primitive) type : null;
    }

    @Nullable
    public static JavaType.FullyQualified asFullyQualified(@Nullable JavaType type) {
        return type instanceof JavaType.FullyQualified ? (JavaType.FullyQualified) type : null;
    }

    /**
     * Determine if a method overrides a method from a superclass or interface.
     *
     * @return `true` if a superclass or implemented interface declares a non-private method with matching signature.
     *         `false` if a match is not found or the method, declaring type, or generic signature is null.
     */
    public static boolean isOverride(@Nullable JavaType.Method method) {
        return findOverriddenMethod(method).isPresent();
    }

    /**
     * Given a method type, searches the declaring type's parent and interfaces for a method with the same name and
     * signature.
     *
     * NOTE: This method will return an empty optional if the method, the method's declaring type, or the method's
     *       generic signature is null.
     *
     * @return An optional overridden method type declared in the parent.
     */
    public static Optional<JavaType.Method> findOverriddenMethod(@Nullable JavaType.Method method) {
        if(method == null || method.getGenericSignature() == null || method.getDeclaringType() == null) {
            return Optional.empty();
        }
        JavaType.FullyQualified dt = method.getDeclaringType();
        List<JavaType> argTypes = method.getGenericSignature().getParamTypes();
        Optional<JavaType.Method> methodResult =  findDeclaredMethod(dt.getSupertype(), method.getName(), argTypes);
        if (!methodResult.isPresent()) {
            for (JavaType.FullyQualified i : dt.getInterfaces()) {
                methodResult =  findDeclaredMethod(i, method.getName(), argTypes);
                if (methodResult.isPresent()) {
                    break;
                }
            }
        }
        return methodResult.filter(m -> !m.getFlags().contains(Flag.Private) && !m.getFlags().contains(Flag.Static));
    }

    public static Optional<JavaType.Method> findDeclaredMethod(@Nullable JavaType.FullyQualified clazz, String name, List<JavaType> argumentTypes) {
        if (clazz == null) {
            return Optional.empty();
        }
        for (JavaType.Method method : clazz.getMethods()) {
            if (methodHasSignature(method, name, argumentTypes)) {
                return Optional.of(method);
            }
        }

        Optional<JavaType.Method> methodResult = findDeclaredMethod(clazz.getSupertype(), name, argumentTypes);
        if (methodResult.isPresent()) {
            return methodResult;
        }

        for(JavaType.FullyQualified i : clazz.getInterfaces()) {
            methodResult = findDeclaredMethod(i, name, argumentTypes);
            if (methodResult.isPresent()) {
                return methodResult;
            }
        }
        return Optional.empty();
    }

    private static boolean methodHasSignature(JavaType.Method m, String name, List<JavaType> argTypes) {
        if(!name.equals(m.getName())) {
            return false;
        }
        if(m.getGenericSignature() == null) {
            return false;
        }
        List<JavaType> mArgs = m.getGenericSignature().getParamTypes();
        if(mArgs.size() != argTypes.size()) {
            return false;
        }
        for(int i = 0; i < mArgs.size(); i++) {
            if(!TypeUtils.isOfType(mArgs.get(i), argTypes.get(i))) {
                return false;
            }
        }
        return true;
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
        return t == null ? t2 == null : t == t2 || t.equals(t2);
    }
}
