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
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, property = "@c")
public interface JavaType extends Serializable {

    boolean deepEquals(@Nullable JavaType type);

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
            return this == type  || (type instanceof MultiCatch &&
                    TypeUtils.deepEquals(throwableTypes, ((MultiCatch) type).throwableTypes));
        }
    }

    abstract class FullyQualified implements JavaType {
        public abstract String getFullyQualifiedName();

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
            return this == type  || (type instanceof ShallowClass &&
                    fullyQualifiedName.equals(((ShallowClass) type).fullyQualifiedName));
        }

        @Override
        public String toString() {
            return "ShallowClass(" + fullyQualifiedName + ")";
        }
    }

    @Getter
    class Class extends FullyQualified {
        // there shouldn't be too many distinct types represented by the same fully qualified name
        private static final Map<String, Set<Class>> flyweights = new HashMap<>();

        public static final Class OBJECT = build("java.lang.Object");

        private final String fullyQualifiedName;
        private final List<Var> members;
        private final List<JavaType> typeParameters;
        private final List<JavaType> interfaces;

        @Nullable
        private volatile List<Method> constructors;

        @Nullable
        private final Class supertype;

        private final String flyweightId;

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


            //The flyweight ID is used to group class variants by their class names. The one execption to this rule
            //are classes that include generic types, their IDs need to be a function of the class name plus the generic
            //parameter types.
            StringBuilder tag = new StringBuilder(fullyQualifiedName);
            if (!typeParameters.isEmpty()) {
                tag.append("<").append(typeParameters.stream().map(JavaType::toString).collect(Collectors.joining(","))).append(">");
            }
            this.flyweightId = tag.toString();
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

            List<Var> sortedMembers;
            if (fullyQualifiedName.equals("java.lang.String")) {
                //There is a "serialPersistentFields" member within the String class which is used in normal Java
                //serialization to customize how the String field is serialized. This field is tripping up Jackson
                //serialization and is intentionally filtered to prevent errors.
                sortedMembers = members.stream().filter(m -> !m.getName().equals("serialPersistentFields")).collect(Collectors.toList());
            } else {
                sortedMembers = new ArrayList<>(members);
            }
            sortedMembers.sort(comparing(Var::getName));

            JavaType.Class candidate = new Class(fullyQualifiedName, sortedMembers, typeParameters, interfaces, constructors, supertype);

            // This logic will attempt to match the candidate against a candidate in the flyweights. If a match is found,
            // that instance is used over the new candidate to prevent a large memory footprint. If relaxed class type
            // matching is "true" any variant with the same ID will be used. If the relaxed class type matching is "false",
            // equality is determined by comparing the immediate structure of the class and also comparing the supertype
            // hierarchies.

            synchronized (flyweights) {
                Set<JavaType.Class> variants = flyweights.computeIfAbsent(candidate.flyweightId, fqn -> new HashSet<>());

                if (relaxedClassTypeMatching) {
                    if (variants.isEmpty()) {
                        variants.add(candidate);
                        return candidate;
                    }
                    return variants.iterator().next();
                } else {
                    for (Class v : variants) {
                        if (v.deepEquals(candidate)) {
                            return v;
                        }
                    }

                    if (candidate.supertype == null) {

                        return variants.stream().filter(v -> v.supertype != null).findFirst().orElseGet(() -> {
                            variants.add(candidate);
                            return candidate;
                        });
                    }
                    variants.add(candidate);
                    return candidate;
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
                //Double checked locking.
                if (constructors != null) {
                    return constructors;
                }
                List<Method> reflectedConstructors = new ArrayList<>();
                try {
                    java.lang.Class<?> reflectionClass = java.lang.Class.forName(fullyQualifiedName, false, JavaType.class.getClassLoader());
                    for (Constructor<?> constructor : reflectionClass.getConstructors()) {
                        ShallowClass selfType = new ShallowClass(fullyQualifiedName);

                        // TODO can we generate a generic signature as well?
                        Method.Signature resolvedSignature = new Method.Signature(selfType, Arrays.stream(constructor.getParameterTypes())
                                .map(Class::resolveTypeFromClass)
                                .collect(toList()));

                        List<String> parameterNames = Arrays.stream(constructor.getParameters()).map(Parameter::getName).collect(toList());

                        // Name each constructor "<reflection_constructor>" to intentionally disambiguate from method signatures parsed
                        // by JavaParser, which may have richer information but which would only be available for types found in the source
                        // repository.
                        reflectedConstructors.add(Method.build(selfType, "<reflection_constructor>", resolvedSignature, resolvedSignature,
                                parameterNames, singleton(Flag.Public)));
                    }
                    constructors = reflectedConstructors;
                } catch (ClassNotFoundException ignored) {
                    // oh well, we tried
                }
                return reflectedConstructors;
            }
        }
        private static JavaType resolveTypeFromClass(java.lang.Class<?> _class) {

            if (!_class.isPrimitive() && !_class.isArray()) {
                return Class.build(_class.getName());
            } else if (_class.isPrimitive()) {
                if (_class ==  boolean.class) {
                    return Primitive.Boolean;
                } else if (_class == String.class) {
                    return Primitive.String;
                } else if (_class == int.class) {
                    return Primitive.Int;
                } else if (_class == long.class) {
                    return Primitive.Long;
                } else if (_class == double.class) {
                    return Primitive.Double;
                } else if (_class == char.class) {
                    return Primitive.Char;
                } else if (_class == byte.class) {
                    return Primitive.Byte;
                } else if (_class == float.class) {
                    return Primitive.Float;
                } else if (_class == short.class) {
                    return Primitive.Short;
                } else {
                    throw new IllegalArgumentException("Unknown primitive argument");
                }
            } else {
                return new JavaType.Array(resolveTypeFromClass(_class.getComponentType()));
            }
        }

        @JsonIgnore
        public List<JavaType.Var> getVisibleSupertypeMembers() {
            List<JavaType.Var> members = new ArrayList<>();
            if (supertype != null) {
                for (Var member : supertype.getMembers()) {
                    if (!member.hasFlags(Flag.Private)) {
                        members.add(member);
                    }
                }
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
            return
                    this == c || (
                    fullyQualifiedName.equals(c.fullyQualifiedName) &&
                    TypeUtils.deepEquals(members, c.members) &&
                    TypeUtils.deepEquals(supertype, c.supertype) &&
                    TypeUtils.deepEquals(typeParameters, c.typeParameters));
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
            for (Flag flag : test) {
                if (!flags.contains(flag)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean deepEquals(@Nullable JavaType type) {
            if (!(type instanceof Var)) {
                return false;
            }

            Var v = (Var) type;
            return this == v  || (name.equals(v.name) && TypeUtils.deepEquals(this.type, v.type) &&
                    flags.equals(v.flags));
        }
    }

    @Getter
    class Method implements JavaType {
        private static final Map<FullyQualified, Map<String, Set<Method>>> flyweights = new HashMap<>();

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
                        .computeIfAbsent(declaringType, dt -> new HashMap<>())
                        .computeIfAbsent(name, n -> new HashSet<>());

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
            return s1 == null ? s2 == null : s1 == s2  || (s2 != null &&
                    TypeUtils.deepEquals(s1.returnType, s2.returnType) &&
                    TypeUtils.deepEquals(s1.paramTypes, s2.paramTypes));
        }

        public boolean hasFlags(Flag... test) {
            for (Flag flag : test) {
                if (!flags.contains(flag)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean deepEquals(JavaType type) {
            if (!(type instanceof Method)) {
                return false;
            }

            Method m = (Method) type;
            return this == m || (
                    paramNames.equals(m.paramNames) &&
                    flags.equals(m.flags) &&
                    declaringType.deepEquals(m.declaringType) &&
                    signatureDeepEquals(genericSignature, m.genericSignature) &&
                    signatureDeepEquals(resolvedSignature, m.resolvedSignature));
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
            return this == generic || (fullyQualifiedName.equals(generic.fullyQualifiedName) &&
                    TypeUtils.deepEquals(bound, generic.bound));
        }
    }

    @Data
    class Array implements JavaType {
        private final JavaType elemType;

        @Override
        public boolean deepEquals(JavaType type) {
            return type instanceof Array && (this == type || (elemType != null && elemType.deepEquals(((Array) type).elemType)));
        }
    }

    enum Primitive implements JavaType {
        Boolean("boolean", "false"),
        Byte("byte", "0"),
        Char("char", "'\u0000'"),
        Double("double", "0.0d"),
        Float("float", "0.0f"),
        Int("int", "0"),
        Long("long", "0L"),
        Short("short", "0"),
        Void("void", null),
        String("String", null),
        None("", null),
        Wildcard("*", null),
        Null("null", null);

        private final String keyword;
        private final String defaultValue;

        Primitive(String keyword, String defaultValue) {
            this.keyword = keyword;
            this.defaultValue = defaultValue;
        }

        public static Primitive fromKeyword(String keyword) {
            for (Primitive p : values()) {
                if (p.keyword.equals(keyword)) {
                    return p;
                }
            }
            return null;
        }

        @JsonIgnore
        public String getKeyword() {
            return this.keyword;
        }

        @JsonIgnore
        public String getDefaultValue() {
            return this.defaultValue;
        }

        @Override
        public boolean deepEquals(JavaType type) {
            return this == type;
        }
    }
}
