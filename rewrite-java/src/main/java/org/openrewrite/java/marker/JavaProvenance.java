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
package org.openrewrite.java.marker;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.github.classgraph.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.openrewrite.Incubating;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Marker;

import java.nio.file.Path;
import java.util.*;

@Incubating(since = "7.0.0")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Data
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public class JavaProvenance implements Marker {
    UUID id;
    String projectName;
    String sourceSetName;
    BuildTool buildTool;
    JavaVersion javaVersion;
    Set<JavaType.FullyQualified> classpath;

    @Nullable
    Publication publication;

    public static JavaProvenance build(
            @Nullable String projectName,
            String sourceSetName,
            BuildTool buildTool,
            JavaVersion javaVersion,
            Iterable<Path> classpath,
            Publication publication) {

        Set<JavaType.FullyQualified> fqns = new HashSet<>();
        if (classpath.iterator().hasNext()) {
            for (ClassInfo classInfo : new ClassGraph()
                    .overrideClasspath(classpath)
                    .enableMemoryMapping()
                    .enableClassInfo()
                    .enableMethodInfo()
                    .enableFieldInfo()
                    .scan()
                    .getAllClasses()) {
                fqns.add(fromClassGraph(classInfo));
            }

            for (ClassInfo classInfo : new ClassGraph()
                    .enableMemoryMapping()
                    .enableClassInfo()
                    .enableMethodInfo()
                    .enableFieldInfo()
                    .enableSystemJarsAndModules()
                    .acceptPackages("java")
                    .scan()
                    .getAllClasses()) {
                fqns.add(fromClassGraph(classInfo));
            }
        }

        return new JavaProvenance(
                UUID.randomUUID(),
                projectName,
                sourceSetName,
                buildTool,
                javaVersion,
                fqns,
                publication);
    }

    private static JavaType.FullyQualified fromClassGraph(ClassInfo aClass) {
        Set<Flag> flags = fromClassGraphModifiers(aClass.getModifiersStr());

        JavaType.Class.Kind kind;
        if (aClass.isInterface()) {
            kind = JavaType.Class.Kind.Interface;
        } else if (aClass.isEnum()) {
            kind = JavaType.Class.Kind.Enum;
        } else if (aClass.isAnnotation()) {
            kind = JavaType.Class.Kind.Annotation;
        } else {
            kind = JavaType.Class.Kind.Class;
        }

        List<JavaType.Variable> variables = fromFieldInfo(aClass.getFieldInfo());
        List<JavaType.Method> methods = fromMethodInfo(aClass.getMethodInfo());

        return JavaType.Class.build(
                Flag.flagsToBitMap(flags),
                aClass.getName(),
                kind,
                variables,
                new ArrayList<>(),
                methods,
                null,
                null,
                new ArrayList<>(),
                false);
    }

    private static Set<Flag> fromClassGraphModifiers(String modifiers) {
        Set<Flag> flags = new HashSet<>();
        for (String modifier : modifiers.split("\\s+")) {
            Flag flag = Flag.fromKeyword(modifier);
            if (flag != null) {
                flags.add(flag);
            }
        }
        return flags;
    }

    private static List<JavaType.Variable> fromFieldInfo(@Nullable FieldInfoList fieldInfos) {
        List<JavaType.Variable> variables = new ArrayList<>();
        if (fieldInfos != null) {
            for (FieldInfo fieldInfo : fieldInfos) {
                Set<Flag> flags = fromClassGraphModifiers(fieldInfo.getModifiersStr());
                JavaType.Variable variable = JavaType.Variable.build(fieldInfo.getName(), JavaType.buildType(fieldInfo.getTypeDescriptor().toString()), Flag.flagsToBitMap(flags));
                variables.add(variable);
            }
        }
        return variables;
    }

    private static List<JavaType.Method> fromMethodInfo(MethodInfoList methodInfos) {
        List<JavaType.Method> methods = new ArrayList<>();
        for (MethodInfo methodInfo : methodInfos) {
            Set<Flag> flags = fromClassGraphModifiers(methodInfo.getModifiersStr());
            List<JavaType> parameterTypes = new ArrayList<>();
            for (MethodParameterInfo methodParameterInfo : methodInfo.getParameterInfo()) {
                parameterTypes.add(JavaType.buildType(methodParameterInfo.getTypeDescriptor().toString()));
            }
            JavaType.Method.Signature signature = new JavaType.Method.Signature(JavaType.buildType(methodInfo.getTypeDescriptor().getResultType().toString()), parameterTypes);

            List<String> methodParams = new ArrayList<>(methodInfo.getParameterInfo().length);
            for (MethodParameterInfo methodParameterInfo : methodInfo.getParameterInfo()) {
                methodParams.add(methodParameterInfo.getName());
            }

            List<JavaType.FullyQualified> thrownExceptions = new ArrayList<>();
            for (ClassRefOrTypeVariableSignature throwsSignature : methodInfo.getTypeDescriptor().getThrowsSignatures()) {
                if(throwsSignature instanceof ClassRefTypeSignature) {
                    thrownExceptions.add(fromClassGraph(((ClassRefTypeSignature) throwsSignature).getClassInfo()));
                }
            }

            List<JavaType.FullyQualified> annotations = new ArrayList<>();
            for (AnnotationInfo annotationInfo : methodInfo.getAnnotationInfo()) {
                annotations.add(fromClassGraph(annotationInfo.getClassInfo()));
            }

            methods.add(JavaType.Method.build(
                    flags,
                    JavaType.Class.build(methodInfo.getClassName()),
                    methodInfo.getName(),
                    null,
                    signature,
                    methodParams,
                    thrownExceptions,
                    annotations));
        }
        return methods;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class BuildTool {
        Type type;
        String version;

        public enum Type {
            Gradle,
            Maven
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Publication {
        String groupId;
        String artifactId;
        String version;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class JavaVersion {
        String createdBy;
        String vmVendor;
        String sourceCompatibility;
        String targetCompatibility;
    }
}
