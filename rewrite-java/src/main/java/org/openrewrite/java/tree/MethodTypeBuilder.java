package org.openrewrite.java.tree;

import com.koloboke.collect.set.hash.HashObjSet;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

public class MethodTypeBuilder {

    JavaType.FullyQualified declaringType = null;
    Set<Flag> flags = new HashSet<>();
    JavaType returnType;
    String name;
    List<Parameter> parameters = new ArrayList<>();

    public static MethodTypeBuilder methodType() {
        return new MethodTypeBuilder();
    }

    public MethodTypeBuilder declaringClass(String fullyQualifiedClassName) {
        declaringType = JavaType.Class.build(fullyQualifiedClassName);
        return this;
    }

    public MethodTypeBuilder flags(Flag ... flags) {
        this.flags.addAll(Arrays.asList(flags));
        return this;
    }

    public MethodTypeBuilder name(String name) {
        this.name = name;
        return this;
    }

    public MethodTypeBuilder returnType(String type) {
        this.returnType = JavaType.buildType(type);
        return this;
    }

    public MethodTypeBuilder parameter(String type, String name) {
        this.parameters.add(new Parameter(JavaType.buildType(type), name));
        return this;
    }

    public JavaType.Method build() {
        JavaType.Method.Signature signature = new JavaType.Method.Signature(
                this.returnType,
                parameters.stream().map(Parameter::getType).collect(Collectors.toList())
        );

        return JavaType.Method.build(
                declaringType,
                name,
                null,
                signature,
                parameters.stream().map(Parameter::getName).collect(Collectors.toList()),
                flags);
    }

    @Getter
    @AllArgsConstructor
    private class Parameter {
        JavaType type;
        String name;
    }

}
