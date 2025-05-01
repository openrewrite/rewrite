/*
 * Copyright 2025 the original author or authors.
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

import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class Types {
    private final Map<JavaType.GenericTypeVariable, JavaType.Intersection> boundsCache = new IdentityHashMap<>();
    private final Set<TypePair> visiting = new HashSet<>();
    private final boolean infer;

    public Types(boolean infer) {
        this.infer = infer;
    }

    public boolean isOfType(@Nullable JavaType to, @Nullable JavaType from) {
        return isOfType(normalize(to), normalize(from), infer ? InferSide.TO : InferSide.NONE);
    }

    public boolean isAssignableTo(@Nullable JavaType to, @Nullable JavaType from) {
        return isAssignableTo(normalize(to), normalize(from), infer ? InferSide.TO : InferSide.NONE);
    }

    private boolean isOfType(@Nullable JavaType to, @Nullable JavaType from, InferSide mode) {
        if (isUnknown(to) || isUnknown(from)) {
            return false;
        } else if (to == from) {
            return true;
        }

        TypePair key = new TypePair(Operation.IS_OF_TYPE, mode, to, from);
        if (visiting.add(key)) {
            try {
                return isOfTypeCore(to, from, mode);
            } finally {
                visiting.remove(key);
            }
        } else {
            // We are already visiting this type pair, so we can assume it is of the same type.
            return true;
        }
    }

    private boolean isOfTypeCore(JavaType to, JavaType from, InferSide mode) {
        // Try to match captures
        if (mode == InferSide.TO && isTypeVariable(to) ||
                mode == InferSide.FROM && isTypeVariable(from)) {
            return isAssignableTo(to, from, mode);
        }

        // Rest of cases
        if (to instanceof JavaType.GenericTypeVariable && from instanceof JavaType.GenericTypeVariable) {
            return isOfTypeGeneric((JavaType.GenericTypeVariable) to, (JavaType.GenericTypeVariable) from, mode);
        } else if (to instanceof JavaType.FullyQualified && from instanceof JavaType.FullyQualified) {
            return isOfTypeFullyQualified((JavaType.FullyQualified) to, (JavaType.FullyQualified) from, mode);
        } else if (to instanceof JavaType.Primitive && from instanceof JavaType.Primitive) {
            return isOfTypePrimitive((JavaType.Primitive) to, (JavaType.Primitive) from, mode);
        } else if (to instanceof JavaType.Array && from instanceof JavaType.Array) {
            return isOfTypeArray((JavaType.Array) to, (JavaType.Array) from, mode);
        } else if (to instanceof JavaType.Intersection && from instanceof JavaType.Intersection) {
            return isOfTypeList(((JavaType.Intersection) to).getBounds(),
                    ((JavaType.Intersection) from).getBounds(), mode);
        } else if (to instanceof JavaType.MultiCatch && from instanceof JavaType.MultiCatch) {
            return isOfTypeList(((JavaType.MultiCatch) to).getThrowableTypes(),
                    ((JavaType.MultiCatch) from).getThrowableTypes(), mode);
        }
        return false;
    }

    private boolean isOfTypeFullyQualified(JavaType.FullyQualified to, JavaType.FullyQualified from, InferSide mode) {
        if (!TypeUtils.fullyQualifiedNamesAreEqual(to.getFullyQualifiedName(), from.getFullyQualifiedName())) {
            return false;
        }
        if (to instanceof JavaType.Class && from instanceof JavaType.Class) {
            return true;
        } else if (to instanceof JavaType.Parameterized && from instanceof JavaType.Parameterized) {
            if (to.getTypeParameters().size() != from.getTypeParameters().size()) {
                return false;
            }
            List<JavaType> toTypeParams = to.getTypeParameters();
            List<JavaType> fromTypeParams = from.getTypeParameters();
            for (int i = 0; i < toTypeParams.size(); i++) {
                if (!isOfType(toTypeParams.get(i), fromTypeParams.get(i), mode)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean isOfTypePrimitive(JavaType.Primitive to, JavaType.Primitive from, InferSide mode) {
        return to == from;
    }

    private boolean isOfTypeArray(JavaType.Array to, JavaType.Array from, InferSide mode) {
        if (to.getElemType() instanceof JavaType.Primitive || from.getElemType() instanceof JavaType.Primitive) {
            // Avoid incorrect inference of array types
            return to.getElemType() == from.getElemType();
        }
        return isOfType(to.getElemType(), from.getElemType(), mode);
    }

    private boolean isOfTypeGeneric(JavaType.GenericTypeVariable to, JavaType.GenericTypeVariable from, InferSide mode) {
        if (!to.getName().equals(from.getName()) ||
                to.getVariance() != from.getVariance() ||
                to.getBounds().size() != from.getBounds().size()) {
            return false;
        }
        for (int i = 0; i < to.getBounds().size(); i++) {
            if (!isOfType(to.getBounds().get(i), from.getBounds().get(i), mode)) {
                return false;
            }
        }
        return true;
    }

    private boolean isOfTypeList(List<JavaType> to, List<JavaType> from, InferSide mode) {
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
            if (!isOfType(toFq[i], fromFq[i], mode)) {
                return false;
            }
        }
        return true;
    }

    private boolean isAssignableTo(@Nullable JavaType to, @Nullable JavaType from, InferSide mode) {
        if (isUnknown(to) || isUnknown(from)) {
            return false;
        }
        if (from == JavaType.Primitive.Null) {
            return !(to instanceof JavaType.Primitive);
        }
        if (to == from || TypeUtils.isObject(to)) {
            return true;
        }

        TypePair key = new TypePair(Operation.IS_ASSIGNABLE_TO, mode, to, from);
        if (visiting.add(key)) {
            try {
                return isAssignableToCore(to, from, mode);
            } finally {
                visiting.remove(key);
            }
        } else {
            // We are already visiting this type pair, so we can assume it is assignable.
            return true;
        }
    }

    private boolean isAssignableToCore(JavaType to, JavaType from, InferSide mode) {
        // Handle generic type variables (e.g., T extends Collection<String>)
        if (mode == InferSide.FROM && isTypeVariable(from)) {
            // Is there anything we can validate?
            return true;
        } else if (to instanceof JavaType.GenericTypeVariable) {
            return isAssignableToGeneric((JavaType.GenericTypeVariable) to, from, mode);
        } else if (from instanceof JavaType.GenericTypeVariable) {
            return isAssignableFromGeneric(to, (JavaType.GenericTypeVariable) from, mode);
        }

        // Handle intersection types (e.g., T extends A & B)
        if (to instanceof JavaType.Intersection) {
            List<JavaType> bounds = ((JavaType.Intersection) to).getBounds();
            return bounds.stream().allMatch(e -> isAssignableTo(e, from, mode));
        } else if (to instanceof JavaType.MultiCatch) {
            List<JavaType> throwableTypes = ((JavaType.MultiCatch) to).getThrowableTypes();
            return throwableTypes.stream().anyMatch(e -> isAssignableTo(e, from, mode));
        } else if (from instanceof JavaType.Intersection) {
            List<JavaType> bounds = ((JavaType.Intersection) from).getBounds();
            return bounds.stream().anyMatch(e -> isAssignableTo(to, e, mode));
        } else if (from instanceof JavaType.MultiCatch) {
            List<JavaType> throwableTypes = ((JavaType.MultiCatch) from).getThrowableTypes();
            return throwableTypes.stream().allMatch(e -> isAssignableTo(to, e, mode));
        }

        // Handle fully qualified types (e.g., java.util.List, java.util.List<String>)
        if (to instanceof JavaType.FullyQualified) {
            return isAssignableToFullyQualified((JavaType.FullyQualified) to, from, mode);
        }
        // Handle arrays types (e.g., String[])
        else if (to instanceof JavaType.Array) {
            return isAssignableToArray((JavaType.Array) to, from, mode);
        }
        // Handle primitive types (e.g., int, boolean)
        else if (to instanceof JavaType.Primitive) {
            // Primitive handling remains unchanged as they don't involve variance
            return isAssignableToPrimitive((JavaType.Primitive) to, from, mode);
        }

        return false;
    }

    private boolean isAssignableToGeneric(JavaType.GenericTypeVariable to, JavaType from, InferSide mode) {
        if (isWildcard(to) || mode == InferSide.TO) {
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
                    return isAssignableTo(target, source, mode);
                case CONTRAVARIANT:
                    // ? super TARGET. Source must be assignable from target.
                    return isAssignableTo(source, target, mode.reverse());
                default:
                    // In Java, an invariant wildcard with bounds (e.g., ? T) is not valid syntax
                    // Could a capture come this way?
                    assert mode == InferSide.TO;
                    return isAssignableTo(target, source, mode);
            }
        }

        // Only same T or U extends T can be assigned to T
        if (!(from instanceof JavaType.GenericTypeVariable)) {
            // Only a generic type variable can be assigned to another generic type variable in bound mode
            return false;
        }

        JavaType.GenericTypeVariable fromGeneric = (JavaType.GenericTypeVariable) from;
        if (to.getName().equals(fromGeneric.getName())) {
            return isOfType(to, from, mode);
        }

        for (JavaType bound : fromGeneric.getBounds()) {
            if (isAssignableTo(to, bound, mode)) {
                return true;
            }
        }
        return false;
    }

    // Handle cases
    private boolean isAssignableFromGeneric(JavaType to, JavaType.GenericTypeVariable from, InferSide mode) {
        if (from.getVariance() == JavaType.GenericTypeVariable.Variance.CONTRAVARIANT) {
            return isAssignableTo(getBounds(from), to, mode.reverse());
        } else {
            return isAssignableTo(to, getBounds(from), mode);
        }
    }

    // Contract, from is FullyQualified, Array or Primitive
    private boolean isAssignableToFullyQualified(JavaType.FullyQualified to, @Nullable JavaType from, InferSide mode) {
        if (from instanceof JavaType.FullyQualified) {
            JavaType.FullyQualified classFrom = (JavaType.FullyQualified) from;
            if (!TypeUtils.fullyQualifiedNamesAreEqual(to.getFullyQualifiedName(), classFrom.getFullyQualifiedName())) {
                if (isAssignableToFullyQualified(to, maybeResolveParameters(classFrom, classFrom.getSupertype()), mode)) {
                    return true;
                }
                for (JavaType.FullyQualified i : classFrom.getInterfaces()) {
                    if (isAssignableToFullyQualified(to, maybeResolveParameters(classFrom, i), mode)) {
                        return true;
                    }
                }
                return false;
            }

            // If 'to' is not parameterized is safe conversion,
            if (!(to instanceof JavaType.Parameterized)) {
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

                if (isWildcard(toParam) && isAssignableTo(toParam, fromParam, mode) ||
                        !isWildcard(toParam) && isOfType(toParam, fromParam, mode)) {
                    continue;
                }
                return false;
            }
            return true;
        } else if (from instanceof JavaType.Array) {
            String fqn = to.getFullyQualifiedName();
            return "java.io.Serializable".equals(fqn) || "java.lang.Cloneable".equals(fqn);
        } else if (from instanceof JavaType.Primitive) {
            return isAssignableToFullyQualified(to, TypeUtils.asBoxedType((JavaType.Primitive) from), mode);
        }
        return false;
    }

    private @Nullable JavaType maybeResolveParameters(JavaType.FullyQualified source, JavaType.@Nullable FullyQualified target) {
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
            //noinspection SuspiciousMethodCalls
            resolvedTypeParameters.add(typeVariableMap.getOrDefault(tp, tp));
        }
        return tgt.withTypeParameters(resolvedTypeParameters);
    }

    // Contract: from is FullyQualified, Array or Primitive
    private boolean isAssignableToArray(JavaType.Array to, JavaType from, InferSide mode) {
        if (from instanceof JavaType.Array) {
            JavaType.Array fromArray = (JavaType.Array) from;
            if (to.getElemType() instanceof JavaType.Primitive || fromArray.getElemType() instanceof JavaType.Primitive) {
                // Avoid boxing or incorrect inference of array types
                return to.getElemType() == fromArray.getElemType();
            }
            return isAssignableTo(to.getElemType(), fromArray.getElemType(), mode);
        }
        return false;
    }

    // Contract: from is FullyQualified, Array or Primitive
    private boolean isAssignableToPrimitive(JavaType.Primitive to, @Nullable JavaType from, InferSide mode) {
        if (from instanceof JavaType.FullyQualified) {
            // Account for auto-unboxing
            JavaType.FullyQualified fromFq = (JavaType.FullyQualified) from;
            JavaType.Primitive fromPrimitive = JavaType.Primitive.fromClassName(fromFq.getFullyQualifiedName());
            return isAssignableToPrimitive(to, fromPrimitive, mode);
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

    private @Nullable JavaType getBounds(JavaType.GenericTypeVariable type) {
        if (type.getBounds().isEmpty()) {
            return null;
        } else if (type.getBounds().size() == 1) {
            return type.getBounds().get(0);
        } else {
            return boundsCache.computeIfAbsent(type, e -> new JavaType.Intersection(e.getBounds()));
        }
    }

    private boolean isTypeVariable(JavaType type) {
        if (isWildcard(type)) {
            JavaType.GenericTypeVariable generic = (JavaType.GenericTypeVariable) type;
            return !generic.getBounds().isEmpty() && isTypeVariable(generic.getBounds().get(0));
        }
        return type instanceof JavaType.GenericTypeVariable;
    }

    private boolean isWildcard(JavaType type) {
        return type instanceof JavaType.GenericTypeVariable && ((JavaType.GenericTypeVariable) type).getName().equals("?");
    }

    private boolean isUnknown(@Nullable JavaType to) {
        return to == null || to instanceof JavaType.Unknown;
    }

    private @Nullable JavaType normalize(@Nullable JavaType type) {
        if (type instanceof JavaType.Method) {
            return normalize(((JavaType.Method) type).getReturnType());
        } else if (type instanceof JavaType.Variable) {
            return normalize(((JavaType.Variable) type).getType());
        } else if (type == JavaType.Primitive.String) {
            return TypeUtils.asBoxedType(JavaType.Primitive.String);
        } else {
            return type;
        }
    }

    enum Operation {
        IS_OF_TYPE,
        IS_ASSIGNABLE_TO
    }

    enum InferSide {
        NONE,
        TO,
        FROM;

        InferSide reverse() {
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

    @Value
    static class TypePair {
        Operation operation;
        InferSide mode;
        JavaType to;
        JavaType from;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TypePair)) return false;

            TypePair that = (TypePair) o;
            return operation == that.operation && mode == that.mode && to == that.to && from == that.from;
        }

        @Override
        public int hashCode() {
            int result = operation.hashCode();
            result = 31 * result + mode.hashCode();
            result = 31 * result + System.identityHashCode(to);
            result = 31 * result + System.identityHashCode(from);
            return result;
        }
    }
}
