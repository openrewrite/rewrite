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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;
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
    String FOUND_TYPE_CONTEXT_KEY = "org.openrewrite.java.FoundType";

    boolean deepEquals(@Nullable JavaType type);

    /**
     * Return a JavaType for the specified string.
     * The string is expected to be either a primitive type like "int" or a fully-qualified-class name like "java.lang.String"
     */
    static JavaType buildType(String typeName) {
        Primitive primitive = Primitive.fromKeyword(typeName);
        if (primitive != null) {
            return primitive;
        } else {
            return Class.build(typeName);
        }
    }

    @Data
    class MultiCatch implements JavaType {
        private final List<JavaType> throwableTypes;

        public MultiCatch(List<JavaType> throwableTypes) {
            this.throwableTypes = throwableTypes;
        }

        @Override
        public boolean deepEquals(@Nullable JavaType type) {
            return this == type || (type instanceof MultiCatch &&
                    TypeUtils.deepEquals(throwableTypes, ((MultiCatch) type).throwableTypes));
        }
    }

    abstract class FullyQualified implements JavaType {
        public abstract String getFullyQualifiedName();

        public String getClassName() {
            AtomicBoolean dropWhile = new AtomicBoolean(false);
            return Arrays.stream(getFullyQualifiedName().split("\\."))
                    .filter(part -> {
                        dropWhile.set(dropWhile.get() || !Character.isLowerCase(part.charAt(0)));
                        return dropWhile.get();
                    })
                    .collect(joining("."));
        }

        public String getPackageName() {
            AtomicBoolean takeWhile = new AtomicBoolean(true);
            return Arrays.stream(getFullyQualifiedName().split("\\."))
                    .filter(part -> {
                        takeWhile.set(takeWhile.get() && !Character.isUpperCase(part.charAt(0)));
                        return takeWhile.get();
                    })
                    .collect(joining("."));
        }

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

        public ShallowClass(String fullyQualifiedName) {
            this.fullyQualifiedName = fullyQualifiedName;
        }

        @Override
        public boolean deepEquals(@Nullable JavaType type) {
            return this == type || (type instanceof ShallowClass &&
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
        private static final Map<String, Set<Class>> flyweights = new WeakHashMap<>();

        public static final Class OBJECT = build("java.lang.Object");

        private final String fullyQualifiedName;

        @Getter(AccessLevel.NONE)
        private final int flagsBitMap;

        private final Kind kind;

        private final List<Variable> members;
        private final List<JavaType> typeParameters;
        private final List<JavaType> interfaces;

        @Nullable
        private volatile List<Method> constructors;

        @Nullable
        private final Class supertype;

        @Nullable
        private final Class owningClass;

        private final String flyweightId;

        private Class(String fullyQualifiedName,
                      int flagsBitMap,
                      Kind kind,
                      List<Variable> members,
                      List<JavaType> typeParameters,
                      List<JavaType> interfaces,
                      @Nullable List<Method> constructors,
                      @Nullable Class supertype,
                      @Nullable Class owningClass) {
            this.fullyQualifiedName = fullyQualifiedName;
            this.flagsBitMap = flagsBitMap;
            this.kind = kind;
            this.members = members;
            this.typeParameters = typeParameters;
            this.interfaces = interfaces;
            this.constructors = constructors;
            this.supertype = supertype;
            this.owningClass = owningClass;


            //The flyweight ID is used to group class variants by their class names. The one execption to this rule
            //are classes that include generic types, their IDs need to be a function of the class name plus the generic
            //parameter types.
            StringBuilder tag = new StringBuilder(fullyQualifiedName);
            if (!typeParameters.isEmpty()) {
                tag.append("<").append(typeParameters.stream().map(JavaType::toString).collect(Collectors.joining(","))).append(">");
            }
            this.flyweightId = tag.toString();
        }

        public boolean hasFlags(Flag... test) {
            return Flag.hasFlags(flagsBitMap, test);
        }

        public Set<Flag> getFlags() {
            return Flag.bitMapToFlags(flagsBitMap);
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
            return build(fullyQualifiedName, 1, Kind.Class, emptyList(), emptyList(), emptyList(), null, null, null,true);
        }

        /**
         * Build a class type only from the class' fully qualified name and kind. Since we are not providing any member,
         * type parameter, interface, or supertype information, this fully qualified name could potentially match on
         * more than one version of the class found in the type cache. This method will simply pick one of them, because
         * there is no way of selecting between the versions of the class based solely on the fully qualified class name.
         *
         * @param fullyQualifiedName The fully qualified name of the class to build
         * @param kind The class kind : Class, Annotation, Enum, or Interface
         * @return Any class found in the type cache
         */
        public static Class build(String fullyQualifiedName, Kind kind) {
            return build(fullyQualifiedName, 1, kind, emptyList(), emptyList(), emptyList(), null, null, null,true);
        }

        public static Class build(String fullyQualifiedName,
                                  Set<Flag> flags,
                                  Kind kind,
                                  List<Variable> members,
                                  List<JavaType> typeParameters,
                                  List<JavaType> interfaces,
                                  List<Method> constructors,
                                  @Nullable Class supertype,
                                  @Nullable Class owningClass) {
            return build(fullyQualifiedName, Flag.flagsToBitMap(flags), kind, members, typeParameters, interfaces, constructors, supertype, owningClass, false);
        }

        @JsonCreator
        protected static Class build(String fullyQualifiedName,
                                  int flagsBitMap,
                                  Kind kind,
                                  List<Variable> members,
                                  List<JavaType> typeParameters,
                                  List<JavaType> interfaces,
                                  @Nullable List<Method> constructors,
                                  @Nullable Class supertype,
                                  @Nullable Class owningClass) {
            return build(fullyQualifiedName, flagsBitMap, kind, members, typeParameters, interfaces, constructors, supertype, owningClass, false);
        }

        public static Class build(String fullyQualifiedName,
                                  int flagsBitMap,
                                  Kind kind,
                                  List<Variable> members,
                                  List<JavaType> typeParameters,
                                  List<JavaType> interfaces,
                                  @Nullable List<Method> constructors,
                                  @Nullable Class supertype,
                                  @Nullable Class owningClass,
                                  boolean relaxedClassTypeMatching) {

            List<Variable> sortedMembers;
            if (fullyQualifiedName.equals("java.lang.String")) {
                //There is a "serialPersistentFields" member within the String class which is used in normal Java
                //serialization to customize how the String field is serialized. This field is tripping up Jackson
                //serialization and is intentionally filtered to prevent errors.
                sortedMembers = members.stream().filter(m -> !m.getName().equals("serialPersistentFields")).collect(Collectors.toList());
            } else {
                sortedMembers = new ArrayList<>(members);
            }
            sortedMembers.sort(comparing(Variable::getName));

            JavaType.Class candidate = new Class(fullyQualifiedName, flagsBitMap, kind, sortedMembers, typeParameters, interfaces, constructors, supertype, owningClass);

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
            List<Method> constructorsTemp = constructors;
            if (constructorsTemp != null) {
                return constructorsTemp;
            }

            synchronized (flyweights) {
                //Double checked locking.
                constructorsTemp = constructors;
                if (constructorsTemp != null) {
                    return constructorsTemp;
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
                if (_class == boolean.class) {
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

        public List<Variable> getVisibleSupertypeMembers() {
            List<Variable> members = new ArrayList<>();
            if (supertype != null) {
                for (Variable member : supertype.getMembers()) {
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
                    this == c || (kind == c.kind && flagsBitMap == flagsBitMap &&
                            fullyQualifiedName.equals(c.fullyQualifiedName) &&
                                    TypeUtils.deepEquals(members, c.members) &&
                                    TypeUtils.deepEquals(supertype, c.supertype) &&
                                    TypeUtils.deepEquals(typeParameters, c.typeParameters));
        }

        @Override
        public String toString() {
            return "Class{" + fullyQualifiedName + '}';
        }

        public enum Kind {
            Class,
            Enum,
            Interface,
            Annotation
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @Data
    class Cyclic extends FullyQualified {
        private final String fullyQualifiedName;

        public Cyclic(String fullyQualifiedName) {
            this.fullyQualifiedName = fullyQualifiedName;
        }

        @Override
        public boolean deepEquals(@Nullable JavaType type) {
            return this.equals(type);
        }

        @Override
        public String toString() {
            return "Cyclic{" + fullyQualifiedName + '}';
        }
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    class Variable implements JavaType {
        private final String name;

        @Nullable
        private final JavaType type;

        @Getter(AccessLevel.NONE)
        private final int flagsBitMap;

        public Variable(String name, @Nullable JavaType type, Set<Flag> flags) {
            this(name, type, Flag.flagsToBitMap(flags));
        }

        @JsonCreator
        public Variable(String name, @Nullable JavaType type, int flagsBitMap) {
            this.name = name;
            this.type = type;
            this.flagsBitMap = flagsBitMap;
        }

        public boolean hasFlags(Flag... test) {
            return Flag.hasFlags(flagsBitMap, test);
        }

        public Set<Flag> getFlags() {
            return Flag.bitMapToFlags(flagsBitMap);
        }

        @Override
        public boolean deepEquals(@Nullable JavaType type) {
            if (!(type instanceof Variable)) {
                return false;
            }

            Variable v = (Variable) type;
            return this == v || (name.equals(v.name) && TypeUtils.deepEquals(this.type, v.type) && flagsBitMap == v.flagsBitMap);
        }
    }

    @Getter
    class Method implements JavaType {
        private static final Map<FullyQualified, Map<String, Set<Method>>> flyweights = new WeakHashMap<>();

        private final FullyQualified declaringType;
        private final String name;
        private final Signature genericSignature;
        private final Signature resolvedSignature;
        private final List<String> paramNames;

        @Getter(AccessLevel.NONE)
        private final int flagsBitMap;

        private Method(FullyQualified declaringType, String name, Signature genericSignature, Signature resolvedSignature, List<String> paramNames, int flagsBitMap) {
            this.declaringType = declaringType;
            this.name = name;
            this.genericSignature = genericSignature;
            this.resolvedSignature = resolvedSignature;
            this.paramNames = paramNames;
            this.flagsBitMap = flagsBitMap;
        }

        public static Method build(FullyQualified declaringType, String name, Signature genericSignature,
                                   Signature resolvedSignature, List<String> paramNames, Set<Flag> flags) {
            return build(declaringType, name, genericSignature, resolvedSignature, paramNames, Flag.flagsToBitMap(flags));
        }

        @JsonCreator
        public static Method build(FullyQualified declaringType, String name, Signature genericSignature,
                                   Signature resolvedSignature, List<String> paramNames, int flagsBitMap) {

            Method test = new Method(declaringType, name, genericSignature, resolvedSignature, paramNames, flagsBitMap);

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
            return s1 == null ? s2 == null : s1 == s2 || (s2 != null &&
                    TypeUtils.deepEquals(s1.returnType, s2.returnType) &&
                    TypeUtils.deepEquals(s1.paramTypes, s2.paramTypes));
        }

        public boolean hasFlags(Flag... test) {
            return Flag.hasFlags(flagsBitMap, test);
        }

        public Set<Flag> getFlags() {
            return Flag.bitMapToFlags(flagsBitMap);
        }

        public Method withFlags(Set<Flag> flags) {
            return new Method(this.declaringType, this.name, this.genericSignature, this.resolvedSignature, this.paramNames, Flag.flagsToBitMap(flags));
        }

        public Method withDeclaringType(FullyQualified declaringType) {
            return new Method(declaringType, this.name, this.genericSignature, this.resolvedSignature, this.paramNames, this.flagsBitMap);
        }

        @Override
        public boolean deepEquals(@Nullable JavaType type) {
            if (!(type instanceof Method)) {
                return false;
            }

            Method m = (Method) type;
            return this == m || (
                    paramNames.equals(m.paramNames) &&
                            flagsBitMap == m.flagsBitMap &&
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
        public boolean deepEquals(@Nullable JavaType type) {
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
        @Nullable
        private final JavaType elemType;

        public Array(@Nullable JavaType elemType) {
            this.elemType = elemType;
        }

        @Override
        public boolean deepEquals(@Nullable JavaType type) {
            return type instanceof Array && (this == type || (elemType != null && elemType.deepEquals(((Array) type).elemType)));
        }
    }

    enum Primitive implements JavaType {
        Boolean("boolean"),
        Byte("byte"),
        Char("char"),
        Double("double"),
        Float("float"),
        Int("int"),
        Long("long"),
        Short("short"),
        Void("void"),
        String("String"),
        None(""),
        Wildcard("*"),
        Null("null");

        private final String keyword;

        Primitive(String keyword) {
            this.keyword = keyword;
        }

        @Nullable
        public static Primitive fromKeyword(String keyword) {
            for (Primitive p : values()) {
                if (p.keyword.equals(keyword)) {
                    return p;
                }
            }
            return null;
        }

        public String getKeyword() {
            return this.keyword;
        }

        @Override
        public boolean deepEquals(@Nullable JavaType type) {
            return this == type;
        }
    }
}
