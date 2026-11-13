/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java;

import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * Gives a type attribution the classes that a rename or a move replaces, wherever the attribution refers to them.
 * A type that refers to none of them stays the same instance.
 */
class TypeReplacer {
    private final Function<JavaType.FullyQualified, JavaType.@Nullable FullyQualified> replacement;
    private final Map<JavaType, JavaType> replaced = new IdentityHashMap<>();
    private final Map<JavaType.FullyQualified, JavaType.FullyQualified> replacements = new IdentityHashMap<>();

    /**
     * @param replacement The class that takes the place of a given class, or null for a class that stays. It is never
     *                    asked about a parameterized type, only about the class that is parameterized.
     */
    TypeReplacer(Function<JavaType.FullyQualified, JavaType.@Nullable FullyQualified> replacement) {
        this.replacement = replacement;
    }

    /**
     * Makes {@code changed} the replacement of {@code oldType}, for a type the caller changes itself.
     */
    void replaceWith(JavaType oldType, JavaType changed) {
        replaced.put(oldType, changed);
        replaced.put(changed, changed);
    }

    @Nullable JavaType replace(@Nullable JavaType oldType) {
        if (oldType == null || oldType instanceof JavaType.Unknown) {
            return oldType;
        }

        JavaType type = replaced.get(oldType);
        if (type != null) {
            return type;
        }

        if (oldType instanceof JavaType.Parameterized) {
            JavaType.Parameterized pt = (JavaType.Parameterized) oldType;
            // in the map before its arguments change, so that a bound referring back to it, as in
            // `Node<T extends Node<T>>`, refers to the changed type
            JavaType.Parameterized changed = new JavaType.Parameterized(null, null, emptyList());
            replaced.put(oldType, changed);
            List<JavaType> typeParameters = pt.getTypeParameters();
            List<JavaType> changedTypeParameters = ListUtils.map(typeParameters, this::replaceTypeArgument);

            JavaType.FullyQualified changedType = pt.getType();
            if (isReplaced(changedType)) {
                changedType = (JavaType.FullyQualified) replace(changedType);
            }
            if (changedTypeParameters == typeParameters && changedType == pt.getType()) {
                replaced.put(oldType, oldType);
                return oldType;
            }
            changed.unsafeSet(changedType, changedTypeParameters);
            replaced.put(changed, changed);
            return changed;
        } else if (oldType instanceof JavaType.FullyQualified) {
            JavaType.FullyQualified original = (JavaType.FullyQualified) oldType;
            JavaType.FullyQualified changed = replacementFor(original);
            if (changed != null) {
                replaced.put(oldType, changed);
                // a replacement is final, even where it would itself be replaced, as a class moved to a subpackage
                replaced.put(changed, changed);
                return changed;
            } else if (oldType instanceof JavaType.Class) {
                JavaType.Class clazz = ((JavaType.Class) oldType)
                        .withInterfaces(ListUtils.map(original.getInterfaces(),
                                t -> (JavaType.FullyQualified) replace(t)))
                        .withSupertype((JavaType.FullyQualified) replace(original.getSupertype()));
                replaced.put(oldType, clazz);
                replaced.put(clazz, clazz);
                return clazz;
            }
        } else if (oldType instanceof JavaType.GenericTypeVariable) {
            JavaType.GenericTypeVariable gtv = (JavaType.GenericTypeVariable) oldType;
            // in the map before its bounds change, for a bound that refers back to it
            JavaType.GenericTypeVariable changed = new JavaType.GenericTypeVariable(null, gtv.getName(),
                    gtv.getVariance(), emptyList());
            replaced.put(oldType, changed);
            List<JavaType> bounds = gtv.getBounds();
            List<JavaType> changedBounds = ListUtils.map(bounds, this::replaceTypeArgument);
            if (changedBounds == bounds) {
                replaced.put(oldType, oldType);
                return oldType;
            }
            changed.unsafeSet(gtv.getName(), gtv.getVariance(), changedBounds);
            replaced.put(changed, changed);
            return changed;
        } else if (oldType instanceof JavaType.Variable) {
            JavaType.Variable variable = (JavaType.Variable) oldType;
            variable = variable.withOwner(replace(variable.getOwner()));
            variable = variable.withType(replace(variable.getType()));
            replaced.put(oldType, variable);
            replaced.put(variable, variable);
            return variable;
        } else if (oldType instanceof JavaType.Array) {
            JavaType.Array array = (JavaType.Array) oldType;
            array = array.withElemType(replace(array.getElemType()));
            replaced.put(oldType, array);
            replaced.put(array, array);
            return array;
        } else if (oldType instanceof JavaType.Method) {
            return replace((JavaType.Method) oldType);
        }

        return oldType;
    }

    JavaType.@Nullable Method replace(JavaType.@Nullable Method oldMethodType) {
        if (oldMethodType == null) {
            return null;
        }
        JavaType.Method method = (JavaType.Method) replaced.get(oldMethodType);
        if (method != null) {
            return method;
        }

        method = oldMethodType.withDeclaringType((JavaType.FullyQualified) replace(oldMethodType.getDeclaringType()))
                .withReturnType(replace(oldMethodType.getReturnType()))
                .withParameterTypes(ListUtils.map(oldMethodType.getParameterTypes(), this::replace))
                // the exception itself or a type variable's bound, without the supertypes that replace walks
                .withThrownExceptions(ListUtils.map(oldMethodType.getThrownExceptions(), e ->
                        e instanceof JavaType.GenericTypeVariable || isReplaced(TypeUtils.asFullyQualified(e)) ?
                                replace(e) : e));
        replaced.put(oldMethodType, method);
        replaced.put(method, method);
        return method;
    }

    /**
     * A type argument or bound changes only where it is, or contains, a replaced class. Any other stays the same
     * instance, such as a class whose supertypes {@link #replace(JavaType)} would walk, or a type variable whose bound
     * refers back to it, as in {@code T extends Comparable<? super T>}.
     */
    private JavaType replaceTypeArgument(JavaType type) {
        return containsReplaced(type, Collections.newSetFromMap(new IdentityHashMap<>())) ?
                requireNonNull(replace(type)) : type;
    }

    private boolean containsReplaced(JavaType type, Set<JavaType> visited) {
        if (!visited.add(type)) {
            return false;
        } else if (type instanceof JavaType.Parameterized) {
            JavaType.Parameterized pt = (JavaType.Parameterized) type;
            if (containsReplaced(pt.getType(), visited)) {
                return true;
            }
            for (JavaType typeParameter : pt.getTypeParameters()) {
                if (containsReplaced(typeParameter, visited)) {
                    return true;
                }
            }
        } else if (type instanceof JavaType.GenericTypeVariable) {
            for (JavaType bound : ((JavaType.GenericTypeVariable) type).getBounds()) {
                if (containsReplaced(bound, visited)) {
                    return true;
                }
            }
        } else if (type instanceof JavaType.Array) {
            return containsReplaced(((JavaType.Array) type).getElemType(), visited);
        } else if (type instanceof JavaType.FullyQualified) {
            return isReplaced((JavaType.FullyQualified) type);
        }
        return false;
    }

    private boolean isReplaced(JavaType.@Nullable FullyQualified type) {
        if (type instanceof JavaType.Parameterized) {
            type = ((JavaType.Parameterized) type).getType();
        }
        return type != null && replacementFor(type) != null;
    }

    private JavaType.@Nullable FullyQualified replacementFor(JavaType.FullyQualified type) {
        if (replaced.get(type) == type) {
            return null;
        }
        return replacements.computeIfAbsent(type, replacement);
    }
}
