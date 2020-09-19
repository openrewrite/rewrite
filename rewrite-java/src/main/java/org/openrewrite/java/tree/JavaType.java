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

import com.fasterxml.jackson.annotation.*;
import com.koloboke.collect.map.hash.HashObjObjMaps;
import com.koloboke.collect.set.hash.HashObjSet;
import com.koloboke.collect.set.hash.HashObjSets;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.With;
import org.openrewrite.internal.lang.Nullable;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Formatting.EMPTY;
import static org.openrewrite.Tree.randomId;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, property = "@c")
public interface JavaType extends Serializable {
    boolean deepEquals(@Nullable JavaType type);

    TypeTree toTypeTree();

    /**
     * Return a JavaType for the specified string.
     * The string is expected to be either a primitive type like "int" or a fully-qualified-class name like "java.lang.String"
     */
    static JavaType buildType(String typeName) {
        JavaType.Primitive primitive = JavaType.Primitive.fromKeyword(typeName);
        if (primitive != null) {
            return primitive;
        } else {
            return JavaType.Class.build(typeName);
        }
    }

    @Data
    class MultiCatch implements JavaType {
        private final List<JavaType> throwableTypes;

        @Override
        public boolean deepEquals(@Nullable JavaType type) {
            return type instanceof MultiCatch &&
                    TypeUtils.deepEquals(throwableTypes, ((MultiCatch) type).throwableTypes);
        }

        @Override
        public TypeTree toTypeTree() {
            return new J.MultiCatch(randomId(), throwableTypes.stream()
                    .map(JavaType::toTypeTree)
                    .collect(toList()), EMPTY);
        }
    }

    abstract class FullyQualified implements JavaType {
        public abstract String getFullyQualifiedName();

        @Override
        public TypeTree toTypeTree() {
            return TreeBuilder.buildName(getFullyQualifiedName())
                    .withType(JavaType.Class.build(getFullyQualifiedName()));
        }

        @JsonIgnore
        public String getClassName() {
            AtomicBoolean dropWhile = new AtomicBoolean(false);
            return Arrays.stream(getFullyQualifiedName().split("\\."))
                    .filter(part -> {
                        dropWhile.set(dropWhile.get() || !Character.isLowerCase(part.charAt(0)));
                        return dropWhile.get();
                    })
                    .collect(joining("."));
        }

        @JsonIgnore
        public String getPackageName() {
            AtomicBoolean takeWhile = new AtomicBoolean(true);
            return Arrays.stream(getFullyQualifiedName().split("\\."))
                    .filter(part -> {
                        takeWhile.set(takeWhile.get() && !Character.isUpperCase(part.charAt(0)));
                        return takeWhile.get();
                    })
                    .collect(joining("."));
        }

        @JsonIgnore
        public boolean isAssignableFrom(@Nullable JavaType.Class clazz) {
            return clazz != null && (this == Class.OBJECT ||
                    getFullyQualifiedName().equals(clazz.fullyQualifiedName) ||
                    isAssignableFrom(clazz.getSupertype()) ||
                    clazz.getInterfaces().stream().anyMatch(i -> i instanceof Class && isAssignableFrom((Class) i))
            );
        }
    }

    /**
     * Reduces memory and CPU footprint when deep class insight isn't necessary, such as
     * for the type parameters of a Type.Class
     */
    @EqualsAndHashCode(callSuper = false)
    @Data
    class ShallowClass extends FullyQualified {
        private final String fullyQualifiedName;

        @Override
        public boolean deepEquals(JavaType type) {
            return type instanceof ShallowClass &&
                    fullyQualifiedName.equals(((ShallowClass) type).fullyQualifiedName);
        }

        @Override
        public String toString() {
            return "ShallowClass{" +  + '}';
        }
    }

    @Getter
    class Class extends FullyQualified {
        // there shouldn't be too many distinct types represented by the same fully qualified name
        private static final Map<String, HashObjSet<Class>> flyweights = HashObjObjMaps.newMutableMap();

        public static final Class OBJECT = build("java.lang.Object");

        private final String fullyQualifiedName;
        private final List<Var> members;
        private final List<JavaType> typeParameters;
        private final List<JavaType> interfaces;

        @Nullable
        private volatile List<Method> constructors;

        @Nullable
        private final Class supertype;

        private Class(String fullyQualifiedName,
                      List<Var> members,
                      List<JavaType> typeParameters,
                      List<JavaType> interfaces,
                      @Nullable List<Method> constructors,
                      @Nullable Class supertype) {
            this.fullyQualifiedName = fullyQualifiedName;
            this.members = members;
            this.typeParameters = typeParameters;
            this.interfaces = interfaces;
            this.constructors = constructors;
            this.supertype = supertype;
        }

        /**
         * Build a class type only from the class' fully qualified name. Since we are not providing any member, type parameter,
         * interface, or supertype information, this fully qualified name could potentially match on more than one version of
         * the class found in the type cache. This method will simply pick one of them, because there is no way of selecting
         * between the versions of the class based solely on the fully qualified class name.
         *
         * @param fullyQualifiedName The fully qualified name of the class to build
         * @return Any class found in the type cache
         */
        public static Class build(String fullyQualifiedName) {
            return build(fullyQualifiedName, emptyList(), emptyList(), emptyList(), null, null, true);
        }

        @JsonCreator
        public static Class build(@JsonProperty("fullyQualifiedName") String fullyQualifiedName,
                                  @JsonProperty("members") List<Var> members,
                                  @JsonProperty("typeParameters") List<JavaType> typeParameters,
                                  @JsonProperty("interfaces") List<JavaType> interfaces,
                                  @JsonProperty("constructors") List<Method> constructors,
                                  @JsonProperty("supertype") @Nullable Class supertype) {
            return build(fullyQualifiedName, members, typeParameters, interfaces, constructors, supertype, false);
        }

        public static Class build(String fullyQualifiedName,
                                  List<Var> members,
                                  List<JavaType> typeParameters,
                                  List<JavaType> interfaces,
                                  @Nullable List<Method> constructors,
                                  @Nullable Class supertype,
                                  boolean relaxedClassTypeMatching) {

            // when class type matching is NOT relaxed, the variants are the various versions of this fully qualified
            // name, where equality is determined by whether the supertype hierarchy and members through the entire
            // supertype hierarchy are equal
            JavaType.Class test = new Class(fullyQualifiedName,
                    members.stream().sorted(comparing(Var::getName)).collect(toList()),
                    typeParameters, interfaces, constructors, supertype);

            synchronized (flyweights) {
                Set<JavaType.Class> variants = flyweights.computeIfAbsent(fullyQualifiedName, fqn -> HashObjSets.newMutableSet());

                if (relaxedClassTypeMatching) {
                    return variants.stream()
                            .findFirst()
                            .orElseGet(() -> {
                                variants.add(test);
                                return test;
                            });
                } else {
                    return variants.stream().filter(v -> v.deepEquals(test))
                            .findFirst()
                            .orElseGet(() -> {
                                if (test.supertype == null) {
                                    return variants.stream().findFirst().orElseGet(() -> {
                                        variants.add(test);
                                        return test;
                                    });
                                }
                                variants.add(test);
                                return test;
                            });
                }
            }
        }

        /**
         * Lazily built so that a {@link org.openrewrite.java.internal.grammar.JavaParser} operating over a set of code
         * has an opportunity to build {@link Class} instances for sources found in the repo that can provide richer information
         * for constructor parameter types.
         *
         * @return The set of public constructors for a class.
         */
        public List<Method> getConstructors() {
            if (constructors != null) {
                return constructors;
            }

            synchronized (flyweights) {
                List<Method> reflectedConstructors = new ArrayList<>();
                try {
                    java.lang.Class<?> reflectionClass = java.lang.Class.forName(fullyQualifiedName, false, JavaType.class.getClassLoader());
                    for (Constructor<?> constructor : reflectionClass.getConstructors()) {
                        ShallowClass selfType = new ShallowClass(fullyQualifiedName);

                        // TODO can we generate a generic signature as well?
                        Method.Signature resolvedSignature = new Method.Signature(selfType, Arrays.stream(constructor.getParameterTypes())
                                .map(pt -> Class.build(pt.getName()))
                                .collect(toList()));

                        List<String> parameterNames = Arrays.stream(constructor.getParameters()).map(Parameter::getName).collect(toList());

                        // Name each constructor "<reflection_constructor>" to intentionally disambiguate from method signatures parsed
                        // by JavaParser, which may have richer information but which would only be available for types found in the source
                        // repository.
                        reflectedConstructors.add(Method.build(selfType, "<reflection_constructor>", resolvedSignature, resolvedSignature,
                                parameterNames, singleton(Flag.Public)));
                    }
                } catch (ClassNotFoundException ignored) {
                    // oh well, we tried
                }
                return reflectedConstructors;
            }
        }

        @JsonIgnore
        public List<JavaType.Var> getVisibleSupertypeMembers() {
            List<JavaType.Var> members = new ArrayList<>();
            if (supertype != null) {
                supertype.getMembers().stream()
                        .filter(member -> !member.hasFlags(Flag.Private))
                        .forEach(members::add);
                members.addAll(supertype.getVisibleSupertypeMembers());
            }
            return members;
        }

        @Override
        public boolean deepEquals(@Nullable JavaType type) {
            if (!(type instanceof Class)) {
                return false;
            }

            Class c = (Class) type;
            return fullyQualifiedName.equals(c.fullyQualifiedName) &&
                    TypeUtils.deepEquals(members, c.members) &&
                    TypeUtils.deepEquals(supertype, c.supertype) &&
                    TypeUtils.deepEquals(typeParameters, c.typeParameters);
        }

        @Override
        public String toString() {
            return "Class{" + fullyQualifiedName + '}';
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @Data
    class Cyclic extends FullyQualified {
        private final String fullyQualifiedName;

        @Override
        public boolean deepEquals(JavaType type) {
            return this.equals(type);
        }

        @Override
        public String toString() {
            return "Cyclic{" + fullyQualifiedName + '}';
        }
    }

    @Data
    class Var implements JavaType {
        private final String name;

        @Nullable
        private final JavaType type;

        private final Set<Flag> flags;

        public boolean hasFlags(Flag... test) {
            return Arrays.stream(test).allMatch(flags::contains);
        }

        @Override
        public boolean deepEquals(@Nullable JavaType type) {
            if (!(type instanceof Var)) {
                return false;
            }

            Var v = (Var) type;
            return name.equals(v.name) && TypeUtils.deepEquals(this.type, v.type) &&
                    flags.equals(v.flags);
        }

        @Override
        public TypeTree toTypeTree() {
            return type == null ? null : type.toTypeTree();
        }
    }

    @Getter
    class Method implements JavaType {
        private static final Map<FullyQualified, Map<String, Set<Method>>> flyweights = HashObjObjMaps.newMutableMap();

        @With
        private final FullyQualified declaringType;

        private final String name;
        private final Signature genericSignature;
        private final Signature resolvedSignature;
        private final List<String> paramNames;

        @With
        private final Set<Flag> flags;

        private Method(FullyQualified declaringType, String name, Signature genericSignature, Signature resolvedSignature, List<String> paramNames, Set<Flag> flags) {
            this.declaringType = declaringType;
            this.name = name;
            this.genericSignature = genericSignature;
            this.resolvedSignature = resolvedSignature;
            this.paramNames = paramNames;
            this.flags = flags;
        }

        @JsonCreator
        public static Method build(@JsonProperty("declaringType") FullyQualified declaringType,
                                   @JsonProperty("name") String name,
                                   @JsonProperty("genericSignature") Signature genericSignature,
                                   @JsonProperty("resolvedSignature") Signature resolvedSignature,
                                   @JsonProperty("paramNames") List<String> paramNames,
                                   @JsonProperty("flags") Set<Flag> flags) {
            Method test = new Method(declaringType, name, genericSignature, resolvedSignature, paramNames, flags);

            synchronized (flyweights) {
                Set<Method> methods = flyweights
                        .computeIfAbsent(declaringType, dt -> HashObjObjMaps.newMutableMap())
                        .computeIfAbsent(name, n -> HashObjSets.newMutableSet());

                return methods
                        .stream()
                        .filter(m -> m.deepEquals(test))
                        .findAny()
                        .orElseGet(() -> {
                            methods.add(test);
                            return test;
                        });
            }
        }

        @Data
        public static class Signature implements Serializable {
            @Nullable
            private final JavaType returnType;

            private final List<JavaType> paramTypes;
        }

        private static boolean signatureDeepEquals(@Nullable Signature s1, @Nullable Signature s2) {
            return s1 == null ? s2 == null : s2 != null &&
                    TypeUtils.deepEquals(s1.returnType, s2.returnType) &&
                    TypeUtils.deepEquals(s1.paramTypes, s2.paramTypes);
        }

        public boolean hasFlags(Flag... test) {
            return Arrays.stream(test).allMatch(flags::contains);
        }

        @Override
        public boolean deepEquals(JavaType type) {
            if (!(type instanceof Method)) {
                return false;
            }

            Method m = (Method) type;
            return paramNames.equals(m.paramNames) &&
                    flags.equals(m.flags) &&
                    declaringType.deepEquals(m.declaringType) &&
                    signatureDeepEquals(genericSignature, m.genericSignature) &&
                    signatureDeepEquals(resolvedSignature, m.resolvedSignature);
        }

        @Override
        public TypeTree toTypeTree() {
            throw new UnsupportedOperationException("Cannot build a type tree for a Method");
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @Data
    class GenericTypeVariable extends FullyQualified {
        private final String fullyQualifiedName;

        @Nullable
        private final Class bound;

        @Override
        public boolean deepEquals(JavaType type) {
            if (!(type instanceof GenericTypeVariable)) {
                return false;
            }

            GenericTypeVariable generic = (GenericTypeVariable) type;
            return fullyQualifiedName.equals(generic.fullyQualifiedName) &&
                    TypeUtils.deepEquals(bound, generic.bound);
        }

        @Override
        public TypeTree toTypeTree() {
            throw new UnsupportedOperationException("Cannot build a type tree for a GenericTypeVariable");
        }
    }

    @Data
    class Array implements JavaType {
        private final JavaType elemType;

        @Override
        public boolean deepEquals(JavaType type) {
            return type instanceof Array && elemType.deepEquals(((Array) type).elemType);
        }

        @Override
        public TypeTree toTypeTree() {
            return new J.ArrayType(randomId(), elemType.toTypeTree(), emptyList(), EMPTY);
        }
    }

    enum Primitive implements JavaType {
        Boolean, Byte, Char, Double, Float, Int, Long, Short, Void, String, None, Wildcard, Null;

        public static Primitive fromKeyword(String keyword) {
            switch (keyword) {
                case "boolean":
                    return Boolean;
                case "byte":
                    return Byte;
                case "char":
                    return Char;
                case "double":
                    return Double;
                case "float":
                    return Float;
                case "int":
                    return Int;
                case "long":
                    return Long;
                case "short":
                    return Short;
                case "void":
                    return Void;
                case "String":
                    return String;
                case "":
                    return None;
                case "*":
                    return Wildcard;
                case "null":
                    return Null;
                default:
                    return null;
            }
        }

        @JsonIgnore
        public String getKeyword() {
            switch (this) {
                case Boolean:
                    return "boolean";
                case Byte:
                    return "byte";
                case Char:
                    return "char";
                case Double:
                    return "double";
                case Float:
                    return "float";
                case Int:
                    return "int";
                case Long:
                    return "long";
                case Short:
                    return "short";
                case Void:
                    return "void";
                case String:
                    return "String";
                case Wildcard:
                    return "*";
                case Null:
                    return "null";
                case None:
                default:
                    return "";
            }
        }

        @Override
        public boolean deepEquals(JavaType type) {
            return this == type;
        }

        @Override
        public TypeTree toTypeTree() {
            return new J.Primitive(randomId(), this, EMPTY);
        }

        public J.Literal toLiteral(String value) {
            Object primitiveValue;

            switch (this) {
                case Int:
                    primitiveValue = Integer.parseInt(value);
                    break;
                case Boolean:
                    primitiveValue = java.lang.Boolean.parseBoolean(value);
                    break;
                case Byte:
                case Char:
                    primitiveValue = "'" + (value.length() > 0 ? value.charAt(0) : 0) + "'";
                    break;
                case Double:
                    primitiveValue = java.lang.Double.parseDouble(value);
                    break;
                case Float:
                    primitiveValue = java.lang.Float.parseFloat(value);
                    break;
                case Long:
                    primitiveValue = java.lang.Long.parseLong(value);
                    break;
                case Short:
                    primitiveValue = java.lang.Short.parseShort(value);
                    break;
                case Null:
                    primitiveValue = null;
                    break;
                case String:
                    primitiveValue = "\"" + value + "\"";
                    break;
                case Void:
                case None:
                case Wildcard:
                default:
                    throw new IllegalArgumentException("Unable to build literals for void, none, and wildcards");
            }

            return new J.Literal(randomId(), primitiveValue, value, this, EMPTY);
        }
    }
}
