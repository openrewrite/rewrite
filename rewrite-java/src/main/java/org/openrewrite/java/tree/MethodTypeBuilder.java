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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.openrewrite.Incubating;
import org.openrewrite.internal.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This builder provides a fluent API for constructing method types. This builder does not support separate generic
 * and resolved methods signatures.
 * <P>
 * <PRE>
 *     EXAMPLE:
 *
 *     import static org.openrewrite.java.tree.MethodInvocationBuilder.newMethodInvocation;
 *     import static org.openrewrite.java.tree.MethodTypeBuilder.newMethodType;
 *     .
 *     .
 *     .
 *     J.MethodInvocation invocation = newMethodInvocation()
 *          .randomId()
 *          .select(...)
 *          .methodType(newMethodType()
 *             .declaringClass("org.assertj.core.api.Assertions")
 *             .flags(Flag.Public, Flag.Static)
 *             .returnType("org.assertj.core.api.AbstractBooleanAssert")
 *             .name("assertThat")
 *             .parameter("boolean", "actual")
 *             .build()
 *           )
 *           .bulid()
 *
 *             .declaringClass("org.assertj.core.api.Assertions")
 *             .flags(Flag.Public, Flag.Static)
 *             .returnType("org.assertj.core.api.AbstractBooleanAssert")
 *             .name("assertThat")
 *             .parameter("boolean", "actual")
 *             .build();
 * </PRE>
 */
@Incubating(since = "6.1.0")
public class MethodTypeBuilder {

    JavaType.FullyQualified declaringType = null;
    Set<Flag> flags = new HashSet<>();
    JavaType resolvedReturnType;
    JavaType genericReturnType;
    String name;
    List<Parameter> parameters = new ArrayList<>();

    public static MethodTypeBuilder newMethodType() {
        return new MethodTypeBuilder();
    }

    /**
     * @param fullyQualifiedClassName The fully-qualified name of the class on which this method is defined.
     */
    public MethodTypeBuilder declaringClass(String fullyQualifiedClassName) {
        declaringType = JavaType.Class.build(fullyQualifiedClassName);
        return this;
    }

    /**
     * @param flags a list of qualifiers (public, static, etc) for the method
     */
    public MethodTypeBuilder flags(Flag ... flags) {
        this.flags.addAll(Arrays.asList(flags));
        return this;
    }

    /**
     * @param name The name of the method is required
     */
    public MethodTypeBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * @param type A String representation of the return type for the method.
     */
    public MethodTypeBuilder returnType(String type) {
        this.resolvedReturnType = JavaType.buildType(type);
        return this;
    }

    /**
     * @param resolvedType A String representation of the resolved return type for the method.
     * @param genericType A String representation of the generic return type for the method.
     */
    public MethodTypeBuilder returnType(String resolvedType, String genericType) {
        this.resolvedReturnType = JavaType.buildType(resolvedType);
        this.genericReturnType = new JavaType.GenericTypeVariable(genericType, JavaType.Class.build("java.lang.Object"));
        return this;
    }

    /**
     * @param type The return type for the method. The default, if not specified, is "void"
     */
    public MethodTypeBuilder returnType(JavaType type) {
        this.resolvedReturnType = type;
        return this;
    }

    /**
     * @param resolvedType The resolved return type for the method.
     * @param genericType The generic return type for the method.
     */
    public MethodTypeBuilder returnType(JavaType resolvedType, JavaType genericType) {
        this.resolvedReturnType = resolvedType;
        this.genericReturnType = genericType;
        return this;
    }

    /**
     * Add a parameter to the method type. The parameters are added in the same order they are added to the builder.
     *
     * The parameter type is either expressed as a fully-qualified class name or can be one of the primitive
     * {@link JavaType.Primitive} keywords.
     *
     * @param type String representation of the parameter type.
     * @param name The name of the parameter.
     */
    public MethodTypeBuilder parameter(String type, String name) {
        this.parameters.add(
                new Parameter(
                        JavaType.buildType(type),
                        JavaType.buildType(type),
                        name
                )
        );
        return this;
    }

    /**
     * Add a parameter to the method type. The parameters are added in the same order they are added to the builder.
     *
     * The parameter type is either expressed as a fully-qualified class name or can be one of the primitive
     * {@link JavaType.Primitive} keywords.
     *
     * @param resolvedType String representation of the resolved parameter type.
     * @param genericType String representation of the generic parameter type.
     * @param name The name of the parameter.
     */
    public MethodTypeBuilder
    parameter(String resolvedType, String genericType, String name) {
        this.parameters.add(
                new Parameter(
                        JavaType.buildType(resolvedType),
                        new JavaType.GenericTypeVariable(
                                genericType,
                                JavaType.Class.build("java.lang.Object")
                        ),
                        name
                )
        );
        return this;
    }

    /**
     * Add a parameter to the method type. The parameters are added in the same order they are added to the builder.
     *
     * @param type Parameter type
     * @param name The name of the parameter.
     */
    public MethodTypeBuilder parameter(JavaType type, String name) {
        this.parameters.add(new Parameter(type, type, name));
        return this;
    }

    /**
     * Add a parameter to the method type. The parameters are added in the same order they are added to the builder.
     *
     * @param resolvedType The resolved parameter type.
     * @param genericType The generic parameter type.
     * @param name The name of the parameter.
     */
    public MethodTypeBuilder parameter(JavaType resolvedType, JavaType genericType, String name) {
        this.parameters.add(new Parameter(resolvedType, genericType, name));
        return this;
    }

    /**
     * Create a method type based on the values defined on this builder. This method will throw an exception if
     * either the method name or declaring class are not defined. The return type will be defaulted to "void" if not
     * specified.
     */
    public JavaType.Method build() {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("The method name is required.");
        }
        if (declaringType == null) {
            throw new IllegalArgumentException("The declaring type is required.");
        }

        if (resolvedReturnType == null) {
            resolvedReturnType = JavaType.Primitive.Void;
        }

        if (genericReturnType == null) {
            genericReturnType = JavaType.Primitive.Void;
        }

        JavaType.Method.Signature resolvedSignature = new JavaType.Method.Signature(
                resolvedReturnType,
                parameters.stream().map(Parameter::getResolvedType).collect(Collectors.toList())
        );

        JavaType.Method.Signature genericSignature = new JavaType.Method.Signature(
                genericReturnType,
                parameters.stream().map(Parameter::getGenericType).collect(Collectors.toList())
        );

        return JavaType.Method.build(
                declaringType,
                name,
                genericSignature,
                resolvedSignature,
                parameters.stream().map(Parameter::getName).collect(Collectors.toList()),
                flags);
    }

    @Getter
    @AllArgsConstructor
    private static class Parameter {
        JavaType resolvedType;
        JavaType genericType;
        String name;
    }
}
