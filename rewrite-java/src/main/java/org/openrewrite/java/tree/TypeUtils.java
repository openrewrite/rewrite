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

import org.openrewrite.Incubating;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTypeSignatureBuilder;
import org.openrewrite.java.internal.DefaultJavaTypeSignatureBuilder;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class TypeUtils {
    private static final JavaType.Class TYPE_OBJECT = JavaType.ShallowClass.build("java.lang.Object");

    private TypeUtils() {
    }

    public static boolean isObject(@Nullable JavaType type) {
        return type instanceof JavaType.FullyQualified &&
               "java.lang.Object".equals(((JavaType.FullyQualified) type).getFullyQualifiedName());
    }

    public static boolean isString(@Nullable JavaType type) {
        return type == JavaType.Primitive.String ||
               (type instanceof JavaType.FullyQualified &&
                "java.lang.String".equals(((JavaType.FullyQualified) type).getFullyQualifiedName())
               );
    }

    public static String toFullyQualifiedName(String fqn) {
        return fqn.replace('$', '.');
    }

    public static boolean fullyQualifiedNamesAreEqual(@Nullable String fqn1, @Nullable String fqn2) {
        if (fqn1 != null && fqn2 != null) {
            return fqn1.equals(fqn2) || fqn1.length() == fqn2.length()
                                        && toFullyQualifiedName(fqn1).equals(toFullyQualifiedName(fqn2));
        }
        return fqn1 == null && fqn2 == null;
    }

    /**
     * Returns true if the JavaTypes are of the same type.
     * {@link JavaType.Parameterized} will be checked for both the FQN and each of the parameters.
     * {@link JavaType.GenericTypeVariable} will be checked for {@link JavaType.GenericTypeVariable.Variance} and each of the bounds.
     */
    public static boolean isOfType(@Nullable JavaType type1, @Nullable JavaType type2) {
        if (type1 instanceof JavaType.Unknown || type2 instanceof JavaType.Unknown) {
            return false;
        }
        if (type1 == type2) {
            return true;
        }
        if (type1 == null || type2 == null) {
            return false;
        }
        // Strings, uniquely amongst all other types, can be either primitives or classes depending on the context
        if (TypeUtils.isString(type1) && TypeUtils.isString(type2)) {
            return true;
        }
        if (type1 instanceof JavaType.Primitive && type2 instanceof JavaType.Primitive) {
            return ((JavaType.Primitive) type1).getKeyword().equals(((JavaType.Primitive) type2).getKeyword());
        }
        if (type1 instanceof JavaType.FullyQualified && type2 instanceof JavaType.FullyQualified) {
            if (TypeUtils.fullyQualifiedNamesAreEqual(
                    ((JavaType.FullyQualified) type1).getFullyQualifiedName(),
                    ((JavaType.FullyQualified) type2).getFullyQualifiedName())) {
                if (type1 instanceof JavaType.Class && type2 instanceof JavaType.Class) {
                    return true;
                } else if (type1 instanceof JavaType.Parameterized && type2 instanceof JavaType.Parameterized) {
                    JavaTypeSignatureBuilder signatureBuilder = new DefaultJavaTypeSignatureBuilder();
                    return signatureBuilder.signature(type1).equals(signatureBuilder.signature(type2));
                }
            }
        }
        if (type1 instanceof JavaType.Array && type2 instanceof JavaType.Array) {
            return isOfType(((JavaType.Array) type1).getElemType(), ((JavaType.Array) type2).getElemType());
        }
        if (type1 instanceof JavaType.GenericTypeVariable && type2 instanceof JavaType.GenericTypeVariable) {
            JavaTypeSignatureBuilder signatureBuilder = new DefaultJavaTypeSignatureBuilder();
            return signatureBuilder.signature(type1).equals(signatureBuilder.signature(type2));
        }
        if (type1 instanceof JavaType.Method && type2 instanceof JavaType.Method) {
            JavaType.Method method1 = (JavaType.Method) type1;
            JavaType.Method method2 = (JavaType.Method) type2;
            if (!method1.getName().equals(method2.getName()) ||
                method1.getFlags().size() != method2.getFlags().size() ||
                !method1.getFlags().containsAll(method2.getFlags()) ||
                !TypeUtils.isOfType(method1.getDeclaringType(), method2.getDeclaringType()) ||
                !TypeUtils.isOfType(method1.getReturnType(), method2.getReturnType()) ||
                method1.getAnnotations().size() != method2.getAnnotations().size() ||
                method1.getThrownExceptions().size() != method2.getThrownExceptions().size() ||
                method1.getParameterTypes().size() != method2.getParameterTypes().size()) {
                return false;
            }

            for (int index = 0; index < method1.getParameterTypes().size(); index++) {
                if (!TypeUtils.isOfType(method1.getParameterTypes().get(index), method2.getParameterTypes().get(index))) {
                    return false;
                }
            }
            for (int index = 0; index < method1.getThrownExceptions().size(); index++) {
                if (!TypeUtils.isOfType(method1.getThrownExceptions().get(index), method2.getThrownExceptions().get(index))) {
                    return false;
                }
            }
            for (int index = 0; index < method1.getAnnotations().size(); index++) {
                if (!TypeUtils.isOfType(method1.getAnnotations().get(index), method2.getAnnotations().get(index))) {
                    return false;
                }
            }
            return true;
        }
        return type1.equals(type2);
    }

    /**
     * Returns true if the JavaType matches the FQN.
     */
    public static boolean isOfClassType(@Nullable JavaType type, String fqn) {
        if (type instanceof JavaType.FullyQualified) {
            return TypeUtils.fullyQualifiedNamesAreEqual(((JavaType.FullyQualified) type).getFullyQualifiedName(), fqn);
        } else if (type instanceof JavaType.Variable) {
            return isOfClassType(((JavaType.Variable) type).getType(), fqn);
        } else if (type instanceof JavaType.Method) {
            return isOfClassType(((JavaType.Method) type).getReturnType(), fqn);
        } else if (type instanceof JavaType.Array) {
            return isOfClassType(((JavaType.Array) type).getElemType(), fqn);
        } else if (type instanceof JavaType.Primitive) {
            return type == JavaType.Primitive.fromKeyword(fqn);
        }
        return false;
    }

    /**
     * @param type          The declaring type of the method invocation or constructor.
     * @param matchOverride Whether to match the {@code Object} type.
     * @return True if the declaring type matches the criteria of this matcher.
     */
    @Incubating(since = "8.1.4")
    public static boolean isOfTypeWithName(
            @Nullable JavaType.FullyQualified type,
            boolean matchOverride,
            Predicate<String> matcher
    ) {
        if (type == null || type instanceof JavaType.Unknown) {
            return false;
        }
        if (matcher.test(type.getFullyQualifiedName())) {
            return true;
        }
        if (matchOverride) {
            if (!"java.lang.Object".equals(type.getFullyQualifiedName()) &&
                isOfTypeWithName(TYPE_OBJECT, true, matcher)) {
                return true;
            }

            if (isOfTypeWithName(type.getSupertype(), true, matcher)) {
                return true;
            }

            for (JavaType.FullyQualified anInterface : type.getInterfaces()) {
                if (isOfTypeWithName(anInterface, true, matcher)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isAssignableTo(@Nullable JavaType to, @Nullable JavaType from) {
        try {
            if (to instanceof JavaType.Unknown || from instanceof JavaType.Unknown) {
                return false;
            }
            if (to == from) {
                return true;
            } else if (to instanceof JavaType.Parameterized) {
                JavaType.Parameterized toParameterized = (JavaType.Parameterized) to;
                if (!(from instanceof JavaType.Parameterized)) {
                    return from instanceof JavaType.FullyQualified &&
                           isAssignableTo(toParameterized.getType(), from) &&
                           toParameterized.getTypeParameters().stream()
                                   .allMatch(p -> (p instanceof JavaType.GenericTypeVariable &&
                                                   ((JavaType.GenericTypeVariable) p).getName().equals("?")) ||
                                                  isAssignableTo(p, TYPE_OBJECT));
                }
                JavaType.Parameterized fromParameterized = (JavaType.Parameterized) from;
                List<JavaType> toParameters = toParameterized.getTypeParameters();
                List<JavaType> fromParameters = fromParameterized.getTypeParameters();
                int parameterCount = toParameters.size();
                return parameterCount == fromParameters.size() &&
                       isAssignableTo(toParameterized.getType(), fromParameterized.getType()) &&
                       IntStream.range(0, parameterCount).allMatch(i -> isAssignableTo(toParameters.get(i), fromParameters.get(i)));
            } else if (to instanceof JavaType.FullyQualified) {
                JavaType.FullyQualified toFq = (JavaType.FullyQualified) to;
                if (from instanceof JavaType.Primitive) {
                    JavaType.Primitive toPrimitive = JavaType.Primitive.fromClassName(toFq.getFullyQualifiedName());
                    if (toPrimitive != null) {
                        return isAssignableTo(toPrimitive, from);
                    } else if (toFq.getFullyQualifiedName().equals("java.lang.Object")) {
                        return true;
                    }
                } else if (from instanceof JavaType.Intersection) {
                    for (JavaType intersectionType : ((JavaType.Intersection) from).getBounds()) {
                        if (isAssignableTo(to, intersectionType)) {
                            return true;
                        }
                    }
                    return false;
                }
                return !(from instanceof JavaType.GenericTypeVariable) && isAssignableTo(toFq.getFullyQualifiedName(), from);
            } else if (to instanceof JavaType.GenericTypeVariable) {
                JavaType.GenericTypeVariable toGeneric = (JavaType.GenericTypeVariable) to;
                List<JavaType> toBounds = toGeneric.getBounds();
                if (!toGeneric.getName().equals("?")) {
                    return false;
                } else if (toGeneric.getVariance() == JavaType.GenericTypeVariable.Variance.COVARIANT) {
                    for (JavaType toBound : toBounds) {
                        if (!isAssignableTo(toBound, from)) {
                            return false;
                        }
                    }
                    return true;
                } else if (toGeneric.getVariance() == JavaType.GenericTypeVariable.Variance.CONTRAVARIANT) {
                    for (JavaType toBound : toBounds) {
                        if (!isAssignableTo(from, toBound)) {
                            return false;
                        }
                    }
                    return true;
                } else if (toBounds.isEmpty()) {
                    return from instanceof JavaType.FullyQualified;
                }
                return false;
            } else if (to instanceof JavaType.Variable) {
                return isAssignableTo(((JavaType.Variable) to).getType(), from);
            } else if (to instanceof JavaType.Method) {
                return isAssignableTo(((JavaType.Method) to).getReturnType(), from);
            } else if (to instanceof JavaType.Primitive) {
                JavaType.Primitive toPrimitive = (JavaType.Primitive) to;
                if (from instanceof JavaType.FullyQualified) {
                    // Account for auto-unboxing
                    JavaType.FullyQualified boxed = JavaType.ShallowClass.build(toPrimitive.getClassName());
                    return isAssignableTo(boxed, from);
                } else if (from instanceof JavaType.Primitive) {
                    JavaType.Primitive fromPrimitive = (JavaType.Primitive) from;
                    if (fromPrimitive == JavaType.Primitive.Boolean || fromPrimitive == JavaType.Primitive.Void) {
                        return false;
                    } else {
                        switch (toPrimitive) {
                            case Char:
                                return fromPrimitive == JavaType.Primitive.Byte;
                            case Short:
                                switch (fromPrimitive) {
                                    case Byte:
                                    case Char:
                                        return true;
                                }
                                return false;
                            case Int:
                                switch (fromPrimitive) {
                                    case Byte:
                                    case Char:
                                    case Short:
                                        return true;
                                }
                                return false;
                            case Long:
                                switch (fromPrimitive) {
                                    case Byte:
                                    case Char:
                                    case Short:
                                    case Int:
                                        return true;
                                }
                                return false;
                            case Float:
                                return fromPrimitive != JavaType.Primitive.Double;
                            case Double:
                                return true;
                            case String:
                                return fromPrimitive == JavaType.Primitive.Null;
                            default:
                                return false;
                        }
                    }
                }
            } else if (to instanceof JavaType.Array && from instanceof JavaType.Array) {
                return isAssignableTo(((JavaType.Array) to).getElemType(), ((JavaType.Array) from).getElemType());
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public static boolean isAssignableTo(String to, @Nullable JavaType from) {
        try {
            if (from instanceof JavaType.FullyQualified) {
                if (from instanceof JavaType.Parameterized) {
                    if (to.equals(from.toString())) {
                        return true;
                    }
                }
                JavaType.FullyQualified classFrom = (JavaType.FullyQualified) from;
                if (fullyQualifiedNamesAreEqual(to, classFrom.getFullyQualifiedName()) ||
                    isAssignableTo(to, classFrom.getSupertype())) {
                    return true;
                }
                for (JavaType.FullyQualified i : classFrom.getInterfaces()) {
                    if (isAssignableTo(to, i)) {
                        return true;
                    }
                }
                return false;
            } else if (from instanceof JavaType.GenericTypeVariable) {
                JavaType.GenericTypeVariable genericFrom = (JavaType.GenericTypeVariable) from;
                for (JavaType bound : genericFrom.getBounds()) {
                    if (isAssignableTo(to, bound)) {
                        return true;
                    }
                }
            } else if (from instanceof JavaType.Primitive) {
                JavaType.Primitive toPrimitive = JavaType.Primitive.fromKeyword(to);
                if (toPrimitive != null) {
                    return isAssignableTo(toPrimitive, from);
                } else if ("java.lang.String".equals(to)) {
                    return isAssignableTo(JavaType.Primitive.String, from);
                }
            } else if (from instanceof JavaType.Variable) {
                return isAssignableTo(to, ((JavaType.Variable) from).getType());
            } else if (from instanceof JavaType.Method) {
                return isAssignableTo(to, ((JavaType.Method) from).getReturnType());
            } else if (from instanceof JavaType.Intersection) {
                for (JavaType bound : ((JavaType.Intersection) from).getBounds()) {
                    if (isAssignableTo(to, bound)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public static boolean isAssignableTo(Pattern to, @Nullable JavaType from) {
        return isAssignableTo(type -> {
            if (type instanceof JavaType.FullyQualified) {
                return to.matcher(((JavaType.FullyQualified) type).getFullyQualifiedName()).matches();
            } else if (type instanceof JavaType.Primitive) {
                return to.matcher(((JavaType.Primitive) type).getKeyword()).matches();
            }
            return false;
        }, from);
    }

    public static boolean isAssignableTo(Predicate<JavaType> predicate, @Nullable JavaType from) {
        try {
            if (from instanceof JavaType.FullyQualified) {
                JavaType.FullyQualified classFrom = (JavaType.FullyQualified) from;

                if (predicate.test(classFrom) || isAssignableTo(predicate, classFrom.getSupertype())) {
                    return true;
                }
                for (JavaType.FullyQualified anInterface : classFrom.getInterfaces()) {
                    if (isAssignableTo(predicate, anInterface)) {
                        return true;
                    }
                }
                return false;
            } else if (from instanceof JavaType.GenericTypeVariable) {
                JavaType.GenericTypeVariable genericFrom = (JavaType.GenericTypeVariable) from;
                for (JavaType bound : genericFrom.getBounds()) {
                    if (isAssignableTo(predicate, bound)) {
                        return true;
                    }
                }
            } else if (from instanceof JavaType.Variable) {
                return isAssignableTo(predicate, ((JavaType.Variable) from).getType());
            } else if (from instanceof JavaType.Method) {
                return isAssignableTo(predicate, ((JavaType.Method) from).getReturnType());
            } else if (from instanceof JavaType.Primitive) {
                JavaType.Primitive primitive = (JavaType.Primitive) from;
                return predicate.test(primitive);
            }
        } catch (Exception e) {
            return false;
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
        if (type instanceof JavaType.FullyQualified && !(type instanceof JavaType.Unknown)) {
            return (JavaType.FullyQualified) type;
        }
        return null;
    }

    /**
     * Determine if a method overrides a method from a superclass or interface.
     *
     * @return `true` if a superclass or implemented interface declares a non-private method with matching signature.
     * `false` if a match is not found or the method, declaring type, or generic signature is null.
     */
    public static boolean isOverride(@Nullable JavaType.Method method) {
        return findOverriddenMethod(method).isPresent();
    }

    /**
     * Given a method type, searches the declaring type's parent and interfaces for a method with the same name and
     * signature.
     * <p>
     * NOTE: This method will return an empty optional if the method, the method's declaring type, or the method's
     * generic signature is null.
     *
     * @return An optional overridden method type declared in the parent.
     */
    public static Optional<JavaType.Method> findOverriddenMethod(@Nullable JavaType.Method method) {
        if (method == null) {
            return Optional.empty();
        }
        JavaType.FullyQualified dt = method.getDeclaringType();
        List<JavaType> argTypes = method.getParameterTypes();
        Optional<JavaType.Method> methodResult = findDeclaredMethod(dt.getSupertype(), method.getName(), argTypes);
        if (!methodResult.isPresent()) {
            for (JavaType.FullyQualified i : dt.getInterfaces()) {
                methodResult = findDeclaredMethod(i, method.getName(), argTypes);
                if (methodResult.isPresent()) {
                    break;
                }
            }
        }

        return methodResult
                .filter(m -> !m.getFlags().contains(Flag.Private))
                .filter(m -> !m.getFlags().contains(Flag.Static))
                // If access level is default then check if subclass package is the same from parent class
                .filter(m -> m.getFlags().contains(Flag.Public) || m.getDeclaringType().getPackageName().equals(dt.getPackageName()));
    }

    public static Optional<JavaType.Method> findDeclaredMethod(@Nullable JavaType.FullyQualified clazz, String name, List<JavaType> argumentTypes) {
        if (clazz == null) {
            return Optional.empty();
        }
        for (JavaType.Method method : clazz.getMethods()) {
            if (methodHasSignature(clazz, method, name, argumentTypes)) {
                return Optional.of(method);
            }
        }

        Optional<JavaType.Method> methodResult = findDeclaredMethod(clazz.getSupertype(), name, argumentTypes);
        if (methodResult.isPresent()) {
            return methodResult;
        }

        for (JavaType.FullyQualified i : clazz.getInterfaces()) {
            methodResult = findDeclaredMethod(i, name, argumentTypes);
            if (methodResult.isPresent()) {
                return methodResult;
            }
        }
        return Optional.empty();
    }

    private static boolean methodHasSignature(JavaType.FullyQualified clazz, JavaType.Method m, String name, List<JavaType> argTypes) {
        if (!name.equals(m.getName())) {
            return false;
        }

        List<JavaType> mArgs = m.getParameterTypes();
        if (mArgs.size() != argTypes.size()) {
            return false;
        }

        Map<JavaType, JavaType> parameterMap = new IdentityHashMap<>();
        List<JavaType> declaringTypeParams = m.getDeclaringType().getTypeParameters();

        List<JavaType> typeParameters = clazz.getTypeParameters();
        if (typeParameters.size() != declaringTypeParams.size()) {
            return false;
        }
        for (int j = 0; j < typeParameters.size(); j++) {
            JavaType typeAttributed = typeParameters.get(j);
            JavaType generic = declaringTypeParams.get(j);
            parameterMap.put(generic, typeAttributed);
        }

        for (int i = 0; i < mArgs.size(); i++) {
            JavaType declared = mArgs.get(i);
            JavaType actual = argTypes.get(i);
            if (!TypeUtils.isOfType(declared, actual)) {
                if (parameterMap.get(declared) != actual) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks whether a type is non-null, non-unknown, and is composed entirely of non-null, non-unknown types.
     *
     * @return true when a type has no null, unknown, or invalid parts
     */
    public static boolean isWellFormedType(@Nullable JavaType type) {
        return isWellFormedType(type, new HashSet<>());
    }

    public static boolean isWellFormedType(@Nullable JavaType type, Set<JavaType> seen) {
        if (type == null || type instanceof JavaType.Unknown) {
            return false;
        }
        return isWellFormedType0(type, seen);
    }

    private static boolean isWellFormedType0(JavaType type, Set<JavaType> seen) {
        if (!seen.add(type)) {
            return true;
        }
        if (type instanceof JavaType.Parameterized) {
            JavaType.Parameterized parameterized = (JavaType.Parameterized) type;
            return isWellFormedType(parameterized.getType(), seen) && parameterized.getTypeParameters().stream().allMatch(it -> isWellFormedType(it, seen));
        } else if (type instanceof JavaType.Array) {
            JavaType.Array arr = (JavaType.Array) type;
            return isWellFormedType(arr.getElemType(), seen);
        } else if (type instanceof JavaType.GenericTypeVariable) {
            JavaType.GenericTypeVariable gen = (JavaType.GenericTypeVariable) type;
            return gen.getBounds().stream().allMatch(it -> isWellFormedType(it, seen));
        } else if (type instanceof JavaType.Variable) {
            JavaType.Variable var = (JavaType.Variable) type;
            return isWellFormedType(var.getType(), seen) && isWellFormedType(var.getOwner(), seen);
        } else if (type instanceof JavaType.MultiCatch) {
            JavaType.MultiCatch mc = (JavaType.MultiCatch) type;
            return mc.getThrowableTypes().stream().allMatch(it -> isWellFormedType(it, seen));
        } else if (type instanceof JavaType.Method) {
            JavaType.Method m = (JavaType.Method) type;
            return isWellFormedType(m.getReturnType(), seen) && isWellFormedType(m.getDeclaringType(), seen) && m.getParameterTypes().stream().allMatch(it -> isWellFormedType(it, seen));
        }

        return true;
    }

    public static JavaType.FullyQualified unknownIfNull(@Nullable JavaType.FullyQualified t) {
        if (t == null) {
            return JavaType.Unknown.getInstance();
        }
        return t;
    }

    public static JavaType unknownIfNull(@Nullable JavaType t) {
        if (t == null) {
            return JavaType.Unknown.getInstance();
        }
        return t;
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

    public static String toString(JavaType type) {
        if (type instanceof JavaType.Primitive) {
            return ((JavaType.Primitive) type).getKeyword();
        } else if (type instanceof JavaType.Class) {
            return ((JavaType.Class) type).getFullyQualifiedName();
        } else if (type instanceof JavaType.Parameterized) {
            JavaType.Parameterized parameterized = (JavaType.Parameterized) type;
            StringBuilder builder = new StringBuilder();
            builder.append(toString(parameterized.getType()));
            builder.append('<');
            List<JavaType> typeParameters = parameterized.getTypeParameters();
            for (int i = 0, typeParametersSize = typeParameters.size(); i < typeParametersSize; i++) {
                JavaType parameter = typeParameters.get(i);
                builder.append(toString(parameter));
                if (i < typeParametersSize - 1) {
                    builder.append(", ");
                }
            }
            builder.append('>');
            return builder.toString();
        } else if (type instanceof JavaType.GenericTypeVariable) {
            JavaType.GenericTypeVariable genericTypeVariable = (JavaType.GenericTypeVariable) type;
            StringBuilder builder = new StringBuilder();
            builder.append(genericTypeVariable.getName());
            if (genericTypeVariable.getVariance() != JavaType.GenericTypeVariable.Variance.INVARIANT) {
                builder.append(' ');
                builder.append(toString(genericTypeVariable.getVariance()));
            }

            List<JavaType> bounds = genericTypeVariable.getBounds();
            if (!bounds.isEmpty()) {
                builder.append(' ');
                int boundsSize = bounds.size();
                if (boundsSize == 1) {
                    builder.append(toString(bounds.get(0)));
                } else {
                    for (int i = 0; i < boundsSize; i++) {
                        JavaType bound = bounds.get(i);
                        builder.append(toString(bound));
                        if (i < boundsSize - 1) {
                            builder.append(" & ");
                        }
                    }
                }
            }
            return builder.toString();
        } else if (type instanceof JavaType.Array) {
            return toString(((JavaType.Array) type).getElemType()) + "[]";
        }
        return type.toString();
    }

    private static String toString(JavaType.GenericTypeVariable.Variance variance) {
        switch (variance) {
            case COVARIANT:
                return "extends";
            case CONTRAVARIANT:
                return "super";
            default:
                return "";
        }
    }
}
