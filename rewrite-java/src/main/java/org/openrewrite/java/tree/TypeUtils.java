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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Incubating;
import org.openrewrite.java.internal.JavaReflectionTypeMapping;
import org.openrewrite.java.internal.JavaTypeCache;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class TypeUtils {
    private static final JavaType.Class TYPE_OBJECT = JavaType.ShallowClass.build("java.lang.Object");
    private static final Set<String> COMMON_JAVA_LANG_TYPES =
            new HashSet<>(Arrays.asList(
                    "Appendable",
                    "AutoCloseable",
                    "Boolean",
                    "Byte",
                    "Character",
                    "CharSequence",
                    "Class",
                    "ClassLoader",
                    "Cloneable",
                    "Comparable",
                    "Double",
                    "Enum",
                    "Error",
                    "Exception",
                    "Float",
                    "FunctionalInterface",
                    "Integer",
                    "Iterable",
                    "Long",
                    "Math",
                    "Number",
                    "Object",
                    "Readable",
                    "Record",
                    "Runnable",
                    "Short",
                    "String",
                    "StringBuffer",
                    "StringBuilder",
                    "System",
                    "Thread",
                    "Throwable",
                    "Void"
            ));
    private static final Map<JavaType.Primitive, JavaType> BOXED_TYPES = new EnumMap<>(JavaType.Primitive.class);

    static {
        JavaReflectionTypeMapping typeMapping = new JavaReflectionTypeMapping(new JavaTypeCache());
        BOXED_TYPES.put(JavaType.Primitive.Boolean, typeMapping.type(Boolean.class));
        BOXED_TYPES.put(JavaType.Primitive.Byte, typeMapping.type(Byte.class));
        BOXED_TYPES.put(JavaType.Primitive.Char, typeMapping.type(Character.class));
        BOXED_TYPES.put(JavaType.Primitive.Short, typeMapping.type(Short.class));
        BOXED_TYPES.put(JavaType.Primitive.Int, typeMapping.type(Integer.class));
        BOXED_TYPES.put(JavaType.Primitive.Long, typeMapping.type(Long.class));
        BOXED_TYPES.put(JavaType.Primitive.Float, typeMapping.type(Float.class));
        BOXED_TYPES.put(JavaType.Primitive.Double, typeMapping.type(Double.class));
        BOXED_TYPES.put(JavaType.Primitive.String, typeMapping.type(String.class));
        BOXED_TYPES.put(JavaType.Primitive.Void, JavaType.Unknown.getInstance());
        BOXED_TYPES.put(JavaType.Primitive.None, JavaType.Unknown.getInstance());
        BOXED_TYPES.put(JavaType.Primitive.Null, JavaType.Unknown.getInstance());
    }

    private TypeUtils() {
    }

    public static boolean isObject(@Nullable JavaType type) {
        return type instanceof JavaType.FullyQualified &&
               "java.lang.Object".equals(((JavaType.FullyQualified) type).getFullyQualifiedName());
    }

    public static @Nullable String findQualifiedJavaLangTypeName(String name) {
        return COMMON_JAVA_LANG_TYPES.contains(name) ? "java.lang." + name : null;
    }

    public static boolean isString(@Nullable JavaType type) {
        return type == JavaType.Primitive.String ||
               (type instanceof JavaType.Class &&
                "java.lang.String".equals(((JavaType.Class) type).getFullyQualifiedName())
               );
    }

    public static String toFullyQualifiedName(String fqn) {
        return fqn.replace('$', '.');
    }

    public static boolean fullyQualifiedNamesAreEqual(@Nullable String fqn1, @Nullable String fqn2) {
        if (fqn1 != null && fqn2 != null) {
            return fqn1.equals(fqn2) || fqn1.length() == fqn2.length() &&
                                        toFullyQualifiedName(fqn1).equals(toFullyQualifiedName(fqn2));
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
        if (type1 instanceof JavaType.Method && type2 instanceof JavaType.Method) {
            JavaType.Method method1 = (JavaType.Method) type1;
            JavaType.Method method2 = (JavaType.Method) type2;
            if (!method1.getName().equals(method2.getName()) ||
                method1.getFlagsBitMap() != method2.getFlagsBitMap() ||
                !TypeUtils.isOfType(method1.getDeclaringType(), method2.getDeclaringType()) ||
                !TypeUtils.isOfType(method1.getReturnType(), method2.getReturnType()) ||
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
            return true;
        }
        if(type1 instanceof JavaType.Variable && type2 instanceof JavaType.Variable) {
            JavaType.Variable var1 = (JavaType.Variable) type1;
            JavaType.Variable var2 = (JavaType.Variable) type2;
            return isOfType((var1).getType(), var2.getType()) && isOfType(var1.getOwner(), var2.getOwner());
        }
        if (type1 instanceof JavaType.Annotation && type2 instanceof JavaType.Annotation) {
            return isOfType((JavaType.Annotation) type1, (JavaType.Annotation) type2);
        }
        // This code used to be returning false, but now returns true as the `isOfType` method changes
        // the JReturn of type1 = JavaType$Method A{name=setN,return=A,parameters=[int]} by looking at its return type now iso simple equality.
        // before  type1 was not equal to type2 (JavaType$Class) but now as the return of the method (=class A) is equal to the class A, this one returns true.
        return new Types(false).isOfType(type1, type2);
        // This used to be the reached code
//        return type1.equals(type2);
    }

    private static boolean isOfType(JavaType.Annotation annotation1, JavaType.Annotation annotation2) {
        if (!isOfType(annotation1.getType(), annotation2.getType())) {
            return false;
        }
        List<JavaType.Annotation.ElementValue> values1 = annotation1.getValues();
        List<JavaType.Annotation.ElementValue> values2 = annotation2.getValues();
        if (values1.size() != values2.size()) {
            return false;
        }
        for (int i = 0; i < values1.size(); i++) {
            if (!isOfType(values1.get(i), values2.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isOfType(JavaType.Annotation.ElementValue value1, JavaType.Annotation.ElementValue value2) {
        if (!isOfType(value1.getElement(), value2.getElement())) {
            return false;
        }
        if (value1 instanceof JavaType.Annotation.SingleElementValue) {
            JavaType.Annotation.SingleElementValue singleValue1 = (JavaType.Annotation.SingleElementValue) value1;
            if (value2 instanceof JavaType.Annotation.SingleElementValue) {
                JavaType.Annotation.SingleElementValue singleValue2 = (JavaType.Annotation.SingleElementValue) value2;
                return isOfType(singleValue1.getReferenceValue(), singleValue2.getReferenceValue()) &&
                       Objects.equals(singleValue1.getConstantValue(), singleValue2.getConstantValue());
            } else {
                JavaType.Annotation.ArrayElementValue arrayValue2 = (JavaType.Annotation.ArrayElementValue) value2;
                return arrayValue2.getReferenceValues() != null && arrayValue2.getReferenceValues().length == 1 && isOfType(singleValue1.getReferenceValue(), arrayValue2.getReferenceValues()[0]) ||
                       arrayValue2.getConstantValues() != null && arrayValue2.getConstantValues().length == 1 && Objects.equals(singleValue1.getConstantValue(), arrayValue2.getConstantValues()[0]);
            }
        } else if (value2 instanceof JavaType.Annotation.ArrayElementValue) {
            JavaType.Annotation.ArrayElementValue arrayValue1 = (JavaType.Annotation.ArrayElementValue) value1;
            JavaType.Annotation.ArrayElementValue arrayValue2 = (JavaType.Annotation.ArrayElementValue) value2;
            if (arrayValue1.getConstantValues() != null) {
                Object[] constantValues1 = arrayValue1.getConstantValues();
                if (arrayValue2.getConstantValues() == null || arrayValue2.getConstantValues().length != constantValues1.length) {
                    return false;
                }
                for (int i = 0; i < constantValues1.length; i++) {
                    if (!Objects.equals(constantValues1[i], arrayValue2.getConstantValues()[i])) {
                        return false;
                    }
                }
                return true;
            } else if (arrayValue1.getReferenceValues() != null) {
                JavaType[] referenceValues1 = arrayValue1.getReferenceValues();
                if (arrayValue2.getReferenceValues() == null || arrayValue2.getReferenceValues().length != referenceValues1.length) {
                    return false;
                }
                for (int i = 0; i < referenceValues1.length; i++) {
                    if (!isOfType(referenceValues1[i], arrayValue2.getReferenceValues()[i])) {
                        return false;
                    }
                }
                return true;
            }
        } else {
            // value1 is an `ArrayElementValue` and value2 is a `SingleElementValue`
            return isOfType(value2, value1);
        }
        return false;
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
            JavaType.@Nullable FullyQualified type,
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
        return new Types(false).isAssignableTo(to, from);
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
        return to.equals("java.lang.Object");
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

    public static JavaType.@Nullable Class asClass(@Nullable JavaType type) {
        return type instanceof JavaType.Class ? (JavaType.Class) type : null;
    }

    public static JavaType.@Nullable Parameterized asParameterized(@Nullable JavaType type) {
        return type instanceof JavaType.Parameterized ? (JavaType.Parameterized) type : null;
    }

    public static JavaType.@Nullable Array asArray(@Nullable JavaType type) {
        return type instanceof JavaType.Array ? (JavaType.Array) type : null;
    }

    public static JavaType.@Nullable GenericTypeVariable asGeneric(@Nullable JavaType type) {
        return type instanceof JavaType.GenericTypeVariable ? (JavaType.GenericTypeVariable) type : null;
    }

    public static JavaType.@Nullable Primitive asPrimitive(@Nullable JavaType type) {
        return type instanceof JavaType.Primitive ? (JavaType.Primitive) type : null;
    }

    public static JavaType.@Nullable FullyQualified asFullyQualified(@Nullable JavaType type) {
        if (type instanceof JavaType.FullyQualified && !(type instanceof JavaType.Unknown)) {
            return (JavaType.FullyQualified) type;
        }
        return null;
    }

    public static JavaType asBoxedType(JavaType.Primitive type) {
        return BOXED_TYPES.get(type);
    }

    /**
     * Determine if a method overrides a method from a superclass or interface.
     *
     * @return `true` if a superclass or implemented interface declares a non-private method with matching signature.
     * `false` if a match is not found or the method, declaring type, or generic signature is null.
     */
    public static boolean isOverride(JavaType.@Nullable Method method) {
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
    public static Optional<JavaType.Method> findOverriddenMethod(JavaType.@Nullable Method method) {
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

    public static Optional<JavaType.Method> findDeclaredMethod(JavaType.@Nullable FullyQualified clazz, String name, List<JavaType> argumentTypes) {
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
            return isWellFormedType(parameterized.getType(), seen) &&
                   parameterized.getTypeParameters().stream().allMatch(it -> isWellFormedType(it, seen));
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
            return isWellFormedType(m.getReturnType(), seen) &&
                   isWellFormedType(m.getDeclaringType(), seen) &&
                   m.getParameterTypes().stream().allMatch(it -> isWellFormedType(it, seen));
        }
        return true;
    }

    public static JavaType.FullyQualified unknownIfNull(JavaType.@Nullable FullyQualified t) {
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
        return toString(type, new IdentityHashMap<>());
    }

    private static String toString(JavaType type, IdentityHashMap<JavaType, Boolean> recursiveTypes) {
        if (type instanceof JavaType.Primitive) {
            return ((JavaType.Primitive) type).getKeyword();
        } else if (type instanceof JavaType.Class) {
            return ((JavaType.Class) type).getFullyQualifiedName();
        } else if (type instanceof JavaType.Parameterized) {
            JavaType.Parameterized parameterized = (JavaType.Parameterized) type;
            String base = toString(parameterized.getType(), recursiveTypes);
            StringJoiner joiner = new StringJoiner(", ", "<", ">");
            for (JavaType parameter : parameterized.getTypeParameters()) {
                joiner.add(toString(parameter, recursiveTypes));
            }
            return base + joiner;
        } else if (type instanceof JavaType.GenericTypeVariable) {
            JavaType.GenericTypeVariable genericType = (JavaType.GenericTypeVariable) type;
            if (!genericType.getName().equals("?")) {
                return genericType.getName();
            } else if (genericType.getVariance() == JavaType.GenericTypeVariable.Variance.INVARIANT ||
                    genericType.getBounds().size() != 1) { // Safe check, wildcards don't allow additional bounds
                return "?";
            } else if (recursiveTypes.containsKey(genericType)) {
                recursiveTypes.put(genericType, true);
                return "?";
            } else {
                recursiveTypes.put(genericType, false);
                String variance = genericType.getVariance() == JavaType.GenericTypeVariable.Variance.COVARIANT ? "? extends " : "? super ";
                String bound = toString(genericType.getBounds().get(0), recursiveTypes);
                if (!recursiveTypes.get(genericType)) {
                    recursiveTypes.remove(genericType);
                    return variance + bound;
                }
                return "?";
            }
        } else if (type instanceof JavaType.Array) {
            return toString(((JavaType.Array) type).getElemType(), recursiveTypes) + "[]";
        } else if (type instanceof JavaType.Intersection) {
            JavaType.Intersection intersection = (JavaType.Intersection) type;
            StringJoiner joiner = new StringJoiner(" & ");
            for (JavaType bound : intersection.getBounds()) {
                joiner.add(toString(bound, recursiveTypes));
            }
            return joiner.toString();
        } else if (type instanceof JavaType.MultiCatch) {
            JavaType.MultiCatch multiCatch = (JavaType.MultiCatch) type;
            StringJoiner joiner = new StringJoiner(" | ");
            for (JavaType throwableType : multiCatch.getThrowableTypes()) {
                joiner.add(toString(throwableType, recursiveTypes));
            }
            return joiner.toString();
        } else if (type instanceof JavaType.Method) {
            return toString(((JavaType.Method) type).getReturnType(), recursiveTypes);
        } else if (type instanceof JavaType.Variable) {
            return toString(((JavaType.Variable) type).getType(), recursiveTypes);
        }
        return type.toString();
    }

    public static String toGenericTypeString(JavaType.GenericTypeVariable type) {
        if (type.getVariance() != JavaType.GenericTypeVariable.Variance.COVARIANT || type.getBounds().isEmpty()) {
            return type.getName();
        } else {
            StringJoiner bounds = new StringJoiner(" & ");
            for (JavaType bound : type.getBounds()) {
                bounds.add(toString(bound));
            }
            return type.getName() + " extends " + bounds;
        }
    }
}
