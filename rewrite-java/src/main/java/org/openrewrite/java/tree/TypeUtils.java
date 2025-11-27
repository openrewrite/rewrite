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
import org.openrewrite.java.JavaTypeVisitor;
import org.openrewrite.java.internal.JavaReflectionTypeMapping;
import org.openrewrite.java.internal.JavaTypeCache;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.openrewrite.java.tree.TypeUtils.ComparisonContext.InferenceDirection.FROM;
import static org.openrewrite.java.tree.TypeUtils.ComparisonContext.InferenceDirection.TO;

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
            if (fqn1.equals(fqn2)) {
                return true;
            }
            int patternLen = fqn1.length();
            if (patternLen != fqn2.length()) {
                return false;
            }
            for (int i = 0; i < patternLen; i++) {
                char p = fqn1.charAt(i);
                char t = fqn2.charAt(i);
                if (p != t) {
                    if ((p == '$' && t == '.') || (p == '.' && t == '$')) {
                        continue;
                    }
                    return false;
                }
            }
            return true;
        }
        return fqn1 == null && fqn2 == null;
    }

    /**
     * Returns true if the JavaTypes are of the same type.
     * {@link JavaType.Parameterized} will be checked for both the FQN and each of the parameters.
     * {@link JavaType.GenericTypeVariable} will be checked for {@link JavaType.GenericTypeVariable.Variance} and each of the bounds.
     */
    public static boolean isOfType(@Nullable JavaType type1, @Nullable JavaType type2) {
        return isOfType(type1, type2, ComparisonContext.BOUND);
    }

    public static boolean isOfType(@Nullable JavaType type1, @Nullable JavaType type2, ComparisonContext context) {
        if (type1 == type2 && !(type1 instanceof JavaType.Unknown)) {
            return true;
        }
        // Strings, uniquely amongst all other types, can be either primitives or classes depending on the context
        if (TypeUtils.isString(type1) && TypeUtils.isString(type2)) {
            return true;
        }
        if (type1 instanceof JavaType.Method && type2 instanceof JavaType.Method) {
            return isOfTypeMethod((JavaType.Method) type1, (JavaType.Method) type2, context);
        }
        if (type1 instanceof JavaType.Variable && type2 instanceof JavaType.Variable) {
            return isOfTypeVariable((JavaType.Variable) type1, (JavaType.Variable) type2, context);
        }
        if (type1 instanceof JavaType.Annotation && type2 instanceof JavaType.Annotation) {
            return isOfTypeAnnotation((JavaType.Annotation) type1, (JavaType.Annotation) type2);
        }
        return isOfTypeCore(type1, type2, context);
    }

    private static boolean isOfTypeMethod(JavaType.Method type1, JavaType.Method type2, ComparisonContext context) {
        if (!type1.getName().equals(type2.getName()) ||
            type1.getFlagsBitMap() != type2.getFlagsBitMap() ||
            !TypeUtils.isOfType(type1.getDeclaringType(), type2.getDeclaringType(), context) ||
            !TypeUtils.isOfType(type1.getReturnType(), type2.getReturnType(), context) ||
            type1.getThrownExceptions().size() != type2.getThrownExceptions().size() ||
            type1.getParameterTypes().size() != type2.getParameterTypes().size()) {
            return false;
        }

        for (int index = 0; index < type1.getParameterTypes().size(); index++) {
            if (!TypeUtils.isOfType(type1.getParameterTypes().get(index), type2.getParameterTypes().get(index), context)) {
                return false;
            }
        }
        for (int index = 0; index < type1.getThrownExceptions().size(); index++) {
            if (!TypeUtils.isOfType(type1.getThrownExceptions().get(index), type2.getThrownExceptions().get(index), context)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isOfTypeVariable(JavaType.Variable var1, JavaType.Variable var2, ComparisonContext context) {
        return isOfType(var1.getType(), var2.getType(), context) && isOfType(var1.getOwner(), var2.getOwner(), context) &&
                var1.getName().equals(var2.getName());
    }

    private static boolean isOfTypeAnnotation(JavaType.Annotation annotation1, JavaType.Annotation annotation2) {
        if (!isOfType(annotation1.getType(), annotation2.getType())) {
            return false;
        }
        List<JavaType.Annotation.ElementValue> values1 = annotation1.getValues();
        List<JavaType.Annotation.ElementValue> values2 = annotation2.getValues();
        if (values1.size() != values2.size()) {
            return false;
        }
        for (int i = 0; i < values1.size(); i++) {
            if (!isOfTypeAnnotationElement(values1.get(i), values2.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isOfTypeAnnotationElement(JavaType.Annotation.ElementValue value1, JavaType.Annotation.ElementValue value2) {
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
            return isOfTypeAnnotationElement(value2, value1);
        }
        return false;
    }

    private static boolean isOfTypeCore(@Nullable JavaType to, @Nullable JavaType from, ComparisonContext context) {
        if (isPseudoType(to) || isPseudoType(from)) {
            return false;
        }
        if (to == from) {
            return true;
        }

        // Try to match captures
        if (context.getInference() == TO && isTypeVariable(to) || context.getInference() == FROM && isTypeVariable(from)) {
            return isAssignableToCore(to, from, context);
        }

        // Rest of cases
        if (to instanceof JavaType.GenericTypeVariable && from instanceof JavaType.GenericTypeVariable) {
            return isOfTypeGeneric((JavaType.GenericTypeVariable) to, (JavaType.GenericTypeVariable) from, context);
        } else if (to instanceof JavaType.FullyQualified && from instanceof JavaType.FullyQualified) {
            return isOfTypeFullyQualified((JavaType.FullyQualified) to, (JavaType.FullyQualified) from, context);
        } else if (to instanceof JavaType.Primitive && from instanceof JavaType.Primitive) {
            return isOfTypePrimitive((JavaType.Primitive) to, (JavaType.Primitive) from, context);
        } else if (to instanceof JavaType.Array && from instanceof JavaType.Array) {
            return isOfTypeArray((JavaType.Array) to, (JavaType.Array) from, context);
        } else if (to instanceof JavaType.Intersection && from instanceof JavaType.Intersection) {
            return isOfTypeList(((JavaType.Intersection) to).getBounds(),
                    ((JavaType.Intersection) from).getBounds(), context);
        } else if (to instanceof JavaType.MultiCatch && from instanceof JavaType.MultiCatch) {
            return isOfTypeList(((JavaType.MultiCatch) to).getThrowableTypes(),
                    ((JavaType.MultiCatch) from).getThrowableTypes(), context);
        }
        return false;
    }

    private static boolean isOfTypeGeneric(JavaType.GenericTypeVariable to, JavaType.GenericTypeVariable from, ComparisonContext context) {
        if (!to.getName().equals(from.getName()) ||
                to.getVariance() != from.getVariance() ||
                to.getBounds().size() != from.getBounds().size()) {
            return false;
        }
        for (int i = 0; i < to.getBounds().size(); i++) {
            if (!isOfTypeCore(to.getBounds().get(i), from.getBounds().get(i), context)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isOfTypeFullyQualified(JavaType.FullyQualified to, JavaType.FullyQualified from, ComparisonContext context) {
        if (!TypeUtils.fullyQualifiedNamesAreEqual(to.getFullyQualifiedName(), from.getFullyQualifiedName())) {
            return false;
        }
        if (to instanceof JavaType.Class && from instanceof JavaType.Class) {
            return true;
        } else if (to instanceof JavaType.Parameterized && from instanceof JavaType.Parameterized) {
            if (context.isComparing(to, from)) {
                return true;
            }
            if (to.getTypeParameters().size() != from.getTypeParameters().size()) {
                return false;
            }
            List<JavaType> toTypeParams = to.getTypeParameters();
            List<JavaType> fromTypeParams = from.getTypeParameters();
            for (int i = 0; i < toTypeParams.size(); i++) {
                if (!isOfTypeCore(toTypeParams.get(i), fromTypeParams.get(i), context.enterComparison(to, from))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static boolean isOfTypePrimitive(JavaType.Primitive to, JavaType.Primitive from, ComparisonContext context) {
        return to == from;
    }

    private static boolean isOfTypeArray(JavaType.Array to, JavaType.Array from, ComparisonContext context) {
        if (to.getElemType() instanceof JavaType.Primitive || from.getElemType() instanceof JavaType.Primitive) {
            // Avoid incorrect inference of array types
            return to.getElemType() == from.getElemType();
        }
        return isOfTypeCore(to.getElemType(), from.getElemType(), context);
    }

    private static boolean isOfTypeList(List<JavaType> to, List<JavaType> from, ComparisonContext context) {
        if (to.size() != from.size() ||
                to.stream().anyMatch(e -> !(e instanceof JavaType.FullyQualified)) ||
                from.stream().anyMatch(e -> !(e instanceof JavaType.FullyQualified))) {
            return false;
        }

        JavaType.FullyQualified[] toFq = to.stream()
                .map(e -> (JavaType.FullyQualified) e)
                .sorted(Comparator.comparing(JavaType.FullyQualified::getFullyQualifiedName))
                .toArray(JavaType.FullyQualified[]::new);
        JavaType.FullyQualified[] fromFq = from.stream()
                .map(e -> (JavaType.FullyQualified) e)
                .sorted(Comparator.comparing(JavaType.FullyQualified::getFullyQualifiedName))
                .toArray(JavaType.FullyQualified[]::new);
        for (int i = 0; i < toFq.length; i++) {
            if (!isOfTypeCore(toFq[i], fromFq[i], context)) {
                return false;
            }
        }
        return true;
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
        return isAssignableTo(to, from, ComparisonContext.BOUND);
    }

    public static boolean isAssignableTo(@Nullable JavaType to, @Nullable JavaType from, ComparisonContext context) {
        try {
            if (to == from && !(to instanceof JavaType.Unknown)) {
                return true;
            }

            if (to == JavaType.Primitive.String) {
                to = BOXED_TYPES.get(to);
            }
            if (from == JavaType.Primitive.String) {
                from = BOXED_TYPES.get(from);
            }

            if (to instanceof JavaType.Method && from instanceof JavaType.Method) {
                return isAssignableToMethod((JavaType.Method) to, (JavaType.Method) from, context);
            }
            if (to instanceof JavaType.Variable && from instanceof JavaType.Variable) {
                return isAssignableToVariable((JavaType.Variable) to, (JavaType.Variable) from, context);
            }
            if (to instanceof JavaType.Annotation && from instanceof JavaType.Annotation) {
                return isOfTypeAnnotation((JavaType.Annotation) to, (JavaType.Annotation) from);
            }
            return isAssignableToCore(to, from, context);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isAssignableToMethod(JavaType.Method type1, JavaType.Method type2, ComparisonContext context) {
        if (!type1.getName().equals(type2.getName()) ||
                Flag.hasFlags(type1.getFlagsBitMap(), Flag.Static) != Flag.hasFlags(type2.getFlagsBitMap(), Flag.Static) ||
                !TypeUtils.isAssignableTo(type1.getDeclaringType(), type2.getDeclaringType(), context) ||
                !TypeUtils.isAssignableTo(type1.getReturnType(), type2.getReturnType(), context)) {
            return false;
        }

        for (int index = 0; index < type1.getParameterTypes().size(); index++) {
            if (!TypeUtils.isAssignableTo(type1.getParameterTypes().get(index), type2.getParameterTypes().get(index), context)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAssignableToVariable(JavaType.Variable var1, JavaType.Variable var2, ComparisonContext mode) {
        return isAssignableTo(var1.getType(), var2.getType(), mode) && isAssignableTo(var1.getOwner(), var2.getOwner(), mode) &&
                var1.getName().equals(var2.getName());
    }

    private static boolean isAssignableToCore(@Nullable JavaType to, @Nullable JavaType from, ComparisonContext context) {
        if (isPseudoType(to) || isPseudoType(from)) {
            return false;
        }
        if (from == JavaType.Primitive.Null) {
            return !(to instanceof JavaType.Primitive);
        }
        if (to == from || TypeUtils.isObject(to)) {
            return true;
        }

        // Handle generic type variables (e.g., T extends Collection<String>)
        if (context.getInference() == FROM && isTypeVariable(from)) {
            // Is there anything we can validate?
            return true;
        } else if (to instanceof JavaType.GenericTypeVariable) {
            return isAssignableToGeneric((JavaType.GenericTypeVariable) to, from, context);
        } else if (from instanceof JavaType.GenericTypeVariable) {
            return isAssignableFromGeneric(to, (JavaType.GenericTypeVariable) from, context);
        }

        // Handle intersection types (e.g., T extends A & B)
        if (to instanceof JavaType.Intersection) {
            List<JavaType> bounds = ((JavaType.Intersection) to).getBounds();
            return bounds.stream().allMatch(e -> isAssignableToCore(e, from, context));
        } else if (to instanceof JavaType.MultiCatch) {
            List<JavaType> throwableTypes = ((JavaType.MultiCatch) to).getThrowableTypes();
            return throwableTypes.stream().anyMatch(e -> isAssignableToCore(e, from, context));
        } else if (from instanceof JavaType.Intersection) {
            List<JavaType> bounds = ((JavaType.Intersection) from).getBounds();
            return bounds.stream().anyMatch(e -> isAssignableToCore(to, e, context));
        } else if (from instanceof JavaType.MultiCatch) {
            List<JavaType> throwableTypes = ((JavaType.MultiCatch) from).getThrowableTypes();
            return throwableTypes.stream().allMatch(e -> isAssignableToCore(to, e, context));
        }

        // Handle fully qualified types (e.g., java.util.List, java.util.List<String>)
        if (to instanceof JavaType.FullyQualified) {
            return isAssignableToFullyQualified((JavaType.FullyQualified) to, from, context);
        }
        // Handle arrays types (e.g., String[])
        else if (to instanceof JavaType.Array) {
            return isAssignableToArray((JavaType.Array) to, from, context);
        }
        // Handle primitive types (e.g., int, boolean)
        else if (to instanceof JavaType.Primitive) {
            // Primitive handling remains unchanged as they don't involve variance
            return isAssignableToPrimitive((JavaType.Primitive) to, from, context);
        }

        return false;
    }

    private static boolean isAssignableToGeneric(JavaType.GenericTypeVariable to, JavaType from, ComparisonContext context) {
        if (isWildcard(to) || context.getInference() == TO) {
            // If target "to" wildcard is unbounded, it accepts anything
            if (to.getBounds().isEmpty()) {
                return true;
            }

            // Extract the target bound
            JavaType target = getBounds(to);

            // Determine if "from" is a wildcard and handle it unless capture.
            JavaType source = from;
            if (isWildcard(from) && isWildcard(to)) {
                JavaType.GenericTypeVariable fromGeneric = (JavaType.GenericTypeVariable) from;

                // Unbounded "from" wildcard is incompatible with a bounded "to" wildcard
                if (fromGeneric.getBounds().isEmpty()) {
                    return false;
                }

                // If variances mismatch, the wildcards are incompatible
                if (fromGeneric.getVariance() != to.getVariance()) {
                    return false;
                }

                // Set the source to the bound of the "from" wildcard
                source = Objects.requireNonNull(getBounds(fromGeneric));
            }

            // Handle variance and type assignability
            switch (to.getVariance()) {
                case COVARIANT:
                    // ? extends TARGET. Source must be assignable to target
                    return isAssignableToCore(target, source, context);
                case CONTRAVARIANT:
                    // ? super TARGET. Source must be assignable from target.
                    return isAssignableToCore(source, target, context.flipInference());
                default:
                    // In Java, an invariant wildcard with bounds (e.g., ? T) is not valid syntax
                    // Could a capture come this way?
                    assert context.getInference() == TO;
                    return isAssignableToCore(target, source, context);
            }
        }

        // Only same T or U extends T can be assigned to T
        if (!(from instanceof JavaType.GenericTypeVariable)) {
            // Only a generic type variable can be assigned to another generic type variable in bound context
            return false;
        }

        JavaType.GenericTypeVariable fromGeneric = (JavaType.GenericTypeVariable) from;
        if (to.getName().equals(fromGeneric.getName())) {
            return isOfTypeCore(to, from, context);
        }

        for (JavaType bound : fromGeneric.getBounds()) {
            if (isAssignableToCore(to, bound, context)) {
                return true;
            }
        }
        return false;
    }

    // Handle cases
    private static boolean isAssignableFromGeneric(JavaType to, JavaType.GenericTypeVariable from, ComparisonContext context) {
        if (from.getVariance() == JavaType.GenericTypeVariable.Variance.CONTRAVARIANT) {
            return isAssignableToCore(getBounds(from), to, context.flipInference());
        } else {
            return isAssignableToCore(to, getBounds(from), context);
        }
    }

    // Contract, from is FullyQualified, Array or Primitive
    private static boolean isAssignableToFullyQualified(JavaType.FullyQualified to, @Nullable JavaType from, ComparisonContext context) {
        if (from instanceof JavaType.FullyQualified) {
            JavaType.FullyQualified classFrom = (JavaType.FullyQualified) from;
            if (!TypeUtils.fullyQualifiedNamesAreEqual(to.getFullyQualifiedName(), classFrom.getFullyQualifiedName())) {
                if (isAssignableToFullyQualified(to, maybeResolveParameters(classFrom, classFrom.getSupertype()), context)) {
                    return true;
                }
                for (JavaType.FullyQualified i : classFrom.getInterfaces()) {
                    if (isAssignableToFullyQualified(to, maybeResolveParameters(classFrom, i), context)) {
                        return true;
                    }
                }
                return false;
            }

            // If 'to' is not parameterized is safe conversion,
            if (!(to instanceof JavaType.Parameterized)) {
                return true;
            }

            if (context.isComparing(to, from)) {
                return true;
            }

            // If 'from' is not parameterized but the 'to' type is,
            // this would be an unsafe raw type conversion - disallow it unless for wildcards
            if (!(from instanceof JavaType.Parameterized)) {
                for (JavaType typeParameter : to.getTypeParameters()) {
                    if (!isWildcard(typeParameter) || !((JavaType.GenericTypeVariable) typeParameter).getBounds().isEmpty()) {
                        return false;
                    }
                }
                // all wildcards case
                return true;
            }

            // Both are parameterized, check type parameters
            JavaType.Parameterized fromParameterized = (JavaType.Parameterized) from;
            List<JavaType> toParameters = to.getTypeParameters();
            List<JavaType> fromParameters = fromParameterized.getTypeParameters();

            if (toParameters.size() != fromParameters.size()) {
                return false;
            }
            for (int i = 0; i < toParameters.size(); i++) {
                JavaType toParam = toParameters.get(i);
                JavaType fromParam = fromParameters.get(i);

                if (isWildcard(toParam) && isAssignableToCore(toParam, fromParam, context.enterComparison(to, from)) ||
                        !isWildcard(toParam) && isOfTypeCore(toParam, fromParam, context.enterComparison(to, from))) {
                    continue;
                }
                return false;
            }
            return true;
        } else if (from instanceof JavaType.Array) {
            String fqn = to.getFullyQualifiedName();
            return "java.io.Serializable".equals(fqn) || "java.lang.Cloneable".equals(fqn);
        } else if (from instanceof JavaType.Primitive) {
            return isAssignableToFullyQualified(to, BOXED_TYPES.get(from), context);
        }
        return false;
    }

    private static @Nullable JavaType maybeResolveParameters(JavaType.FullyQualified source, JavaType.@Nullable FullyQualified target) {
        if (!(source instanceof JavaType.Parameterized) || !(target instanceof JavaType.Parameterized)) {
            return target;
        }
        JavaType.Parameterized src = (JavaType.Parameterized) source;
        JavaType.Parameterized tgt = (JavaType.Parameterized) target;

        if (src.getTypeParameters().size() != src.getType().getTypeParameters().size()) {
            return tgt;
        }

        Map<JavaType.GenericTypeVariable, JavaType> typeVariableMap = new IdentityHashMap<>();
        for (int i = 0; i < src.getTypeParameters().size(); i++) {
            if (src.getType().getTypeParameters().get(i) instanceof JavaType.GenericTypeVariable) {
                typeVariableMap.put((JavaType.GenericTypeVariable) src.getType().getTypeParameters().get(i), src.getTypeParameters().get(i));
            }
        }
        List<JavaType> resolvedTypeParameters = new ArrayList<>();
        for (JavaType tp : target.getTypeParameters()) {
            resolvedTypeParameters.add(resolveTypeParameters(tp, typeVariableMap));
        }
        return tgt.withTypeParameters(resolvedTypeParameters);
    }

    private static JavaType resolveTypeParameters(JavaType type, Map<JavaType.GenericTypeVariable, JavaType> replacements) {
        return Objects.requireNonNull(new JavaTypeVisitor<Map<JavaType.GenericTypeVariable, JavaType>>() {
            @Override
            public JavaType visitGenericTypeVariable(JavaType.GenericTypeVariable generic, Map<JavaType.GenericTypeVariable, JavaType> replacements) {
                if (!replacements.containsKey(generic)) {
                    replacements.put(generic, generic);
                    JavaType.GenericTypeVariable resolved = (JavaType.GenericTypeVariable) super.visitGenericTypeVariable(generic, replacements);
                    replacements.put(generic, resolved);
                    return resolved;
                } else {
                    return replacements.get(generic);
                }
            }

            @Override
            public JavaType visitClass(JavaType.Class aClass, Map<JavaType.GenericTypeVariable, JavaType> replacements) {
                return aClass;
            }
        }.visit(type, replacements));
    }

    // Contract: from is FullyQualified, Array or Primitive
    private static boolean isAssignableToArray(JavaType.Array to, JavaType from, ComparisonContext context) {
        if (from instanceof JavaType.Array) {
            JavaType.Array fromArray = (JavaType.Array) from;
            if (to.getElemType() instanceof JavaType.Primitive || fromArray.getElemType() instanceof JavaType.Primitive) {
                // Avoid boxing or incorrect inference of array types
                return to.getElemType() == fromArray.getElemType();
            }
            return isAssignableToCore(to.getElemType(), fromArray.getElemType(), context);
        }
        return false;
    }

    // Contract: from is FullyQualified, Array or Primitive
    private static boolean isAssignableToPrimitive(JavaType.Primitive to, @Nullable JavaType from, ComparisonContext context) {
        if (from instanceof JavaType.FullyQualified) {
            // Account for auto-unboxing
            JavaType.FullyQualified fromFq = (JavaType.FullyQualified) from;
            JavaType.Primitive fromPrimitive = JavaType.Primitive.fromClassName(fromFq.getFullyQualifiedName());
            return isAssignableToPrimitive(to, fromPrimitive, context);
        } else if (from instanceof JavaType.Primitive) {
            JavaType.Primitive fromPrimitive = (JavaType.Primitive) from;
            switch (fromPrimitive) {
                case Void:
                case None:
                case Null:
                case String:
                    return false;
                case Boolean:
                    return fromPrimitive == to;
                default:
                    switch (to) {
                        case Byte:
                        case Char:
                            return fromPrimitive == to;
                        case Short:
                            switch (fromPrimitive) {
                                case Byte:
                                case Char:
                                case Short:
                                    return true;
                            }
                            return false;
                        case Int:
                            switch (fromPrimitive) {
                                case Byte:
                                case Char:
                                case Short:
                                case Int:
                                    return true;
                            }
                            return false;
                        case Long:
                            switch (fromPrimitive) {
                                case Byte:
                                case Char:
                                case Short:
                                case Int:
                                case Long:
                                    return true;
                            }
                            return false;
                        case Float:
                            return fromPrimitive != JavaType.Primitive.Double;
                        case Double:
                            return true;
                        default:
                            return false;
                    }
            }
        }
        return false;
    }

    private static @Nullable JavaType getBounds(JavaType.GenericTypeVariable type) {
        if (type.getBounds().isEmpty()) {
            return null;
        } else if (type.getBounds().size() == 1) {
            return type.getBounds().get(0);
        } else {
            return new JavaType.Intersection(type.getBounds());
        }
    }

    private static boolean isTypeVariable(JavaType type) {
        if (isWildcard(type)) {
            JavaType.GenericTypeVariable generic = (JavaType.GenericTypeVariable) type;
            return !generic.getBounds().isEmpty() && isTypeVariable(generic.getBounds().get(0));
        }
        return type instanceof JavaType.GenericTypeVariable;
    }

    private static boolean isWildcard(JavaType type) {
        return type instanceof JavaType.GenericTypeVariable && "?".equals(((JavaType.GenericTypeVariable) type).getName());
    }

    private static boolean isPseudoType(@Nullable JavaType type) {
        return type == null || type instanceof JavaType.Unknown || type instanceof JavaType.Variable ||
                type instanceof JavaType.Method || type instanceof JavaType.Annotation;
    }

    public static boolean isAssignableTo(String to, @Nullable JavaType from) {
        return isAssignableTo(to, from, new HashSet<>());
    }

    private static boolean isAssignableTo(String to, @Nullable JavaType from, Set<JavaType> visited) {
        try {
            if (from instanceof JavaType.FullyQualified) {
                // Prevent infinite recursion by tracking visited types
                if (!visited.add(from)) {
                    return false;
                }
                
                if (from instanceof JavaType.Parameterized) {
                    int lessThanIndex = to.indexOf('<');
                    String fromRawType = ((JavaType.Parameterized) from).getType().getFullyQualifiedName();
                    if (lessThanIndex == fromRawType.length() && to.startsWith(fromRawType) && to.equals(from.toString())) {
                        return true;
                    }
                }
                JavaType.FullyQualified classFrom = (JavaType.FullyQualified) from;
                if (fullyQualifiedNamesAreEqual(to, classFrom.getFullyQualifiedName()) ||
                    isAssignableTo(to, classFrom.getSupertype(), visited)) {
                    return true;
                }
                for (JavaType.FullyQualified i : classFrom.getInterfaces()) {
                    if (isAssignableTo(to, i, visited)) {
                        return true;
                    }
                }
                return false;
            } else if (from instanceof JavaType.GenericTypeVariable) {
                JavaType.GenericTypeVariable genericFrom = (JavaType.GenericTypeVariable) from;
                for (JavaType bound : genericFrom.getBounds()) {
                    if (isAssignableTo(to, bound, visited)) {
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
                return isAssignableTo(to, ((JavaType.Variable) from).getType(), visited);
            } else if (from instanceof JavaType.Method) {
                return isAssignableTo(to, ((JavaType.Method) from).getReturnType(), visited);
            } else if (from instanceof JavaType.Intersection) {
                for (JavaType bound : ((JavaType.Intersection) from).getBounds()) {
                    if (isAssignableTo(to, bound, visited)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
        return "java.lang.Object".equals(to);
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
            if (!"?".equals(genericType.getName())) {
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

    public static class ComparisonContext {
        public static final ComparisonContext INFER = new ComparisonContext(null, null, InferenceDirection.TO, null);
        public static final ComparisonContext BOUND = new ComparisonContext(null, null, InferenceDirection.NONE, null);
        private final @Nullable JavaType to;
        private final @Nullable JavaType from;
        private final InferenceDirection inference;
        private final @Nullable ComparisonContext parent;

        enum InferenceDirection {
            TO, FROM, NONE;

            public InferenceDirection flip() {
                switch (this) {
                    case TO:
                        return FROM;
                    case FROM:
                        return TO;
                    default:
                        return NONE;
                }
            }
        }

        private ComparisonContext(@Nullable JavaType to, @Nullable JavaType from, InferenceDirection inference, @Nullable ComparisonContext parent) {
            this.to = to;
            this.from = from;
            this.inference = inference;
            this.parent = parent;
        }

        public boolean isComparing(JavaType to, JavaType from) {
            return Objects.equals(this.to, to) && Objects.equals(this.from, from) ||
                    parent != null && parent.isComparing(to, from);
        }

        public ComparisonContext enterComparison(JavaType to, JavaType from) {
            return new ComparisonContext(to, from, inference, this);
        }

        public ComparisonContext flipInference() {
            return new ComparisonContext(null, null, inference.flip(), this);
        }

        InferenceDirection getInference() {
            return inference;
        }
    }
}
