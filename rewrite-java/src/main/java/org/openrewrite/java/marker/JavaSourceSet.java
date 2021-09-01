/*
 * Copyright 2021 the original author or authors.
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
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Marker;

import java.nio.file.Path;
import java.util.*;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
@With
public class JavaSourceSet implements Marker {
    @EqualsAndHashCode.Include
    UUID id;

    String name;
    Set<JavaType.FullyQualified> classpath;

    public static JavaSourceSet build(String sourceSetName, Iterable<Path> classpath) {
        Set<JavaType.FullyQualified> fqns = new HashSet<>();
        if (classpath.iterator().hasNext()) {
            for (ClassInfo classInfo : new ClassGraph()
                    .overrideClasspath(classpath)
                    .enableAnnotationInfo()
                    .enableMemoryMapping()
                    .enableClassInfo()
                    .enableMethodInfo()
                    .enableFieldInfo()
                    .scan()
                    .getAllClasses()) {
                fqns.add(fromClassInfo(classInfo, new Stack<>()));
            }

            for (ClassInfo classInfo : new ClassGraph()
                    .enableMemoryMapping()
                    .enableAnnotationInfo()
                    .enableClassInfo()
                    .enableMethodInfo()
                    .enableFieldInfo()
                    .enableSystemJarsAndModules()
                    .acceptPackages("java")
                    .scan()
                    .getAllClasses()) {
                fqns.add(fromClassInfo(classInfo, new Stack<>()));
            }
        }

        return new JavaSourceSet(Tree.randomId(), sourceSetName, fqns);
    }

    private static JavaType.FullyQualified fromClassInfo(ClassInfo aClass, Stack<ClassInfo> stack) {
        JavaType.Class existing = JavaType.Class.find(aClass.getName());
        if (existing != null) {
            return existing;
        }

        if (stack.contains(aClass)) {
            return new JavaType.ShallowClass(aClass.getName());
        }

        stack.add(aClass);

        Set<Flag> flags = Flag.bitMapToFlags(aClass.getModifiers());

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

        List<JavaType.Variable> variables = fromFieldInfo(aClass.getFieldInfo(), stack);
        List<JavaType.Method> methods = fromMethodInfo(aClass.getMethodInfo(), stack);

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

    private static List<JavaType.Variable> fromFieldInfo(@Nullable FieldInfoList fieldInfos, Stack<ClassInfo> stack) {
        if (fieldInfos != null) {
            List<JavaType.Variable> variables = new ArrayList<>(fieldInfos.size());
            for (FieldInfo fieldInfo : fieldInfos) {
                JavaType.FullyQualified owner = fromClassInfo(fieldInfo.getClassInfo(), stack);

                List<JavaType.FullyQualified> annotations = new ArrayList<>(fieldInfo.getAnnotationInfo().size());
                for (AnnotationInfo annotationInfo : fieldInfo.getAnnotationInfo()) {
                    annotations.add(fromClassInfo(annotationInfo.getClassInfo(), stack));
                }

                Set<Flag> flags = Flag.bitMapToFlags(fieldInfo.getModifiers());
                JavaType.Variable variable = JavaType.Variable.build(fieldInfo.getName(), owner,
                        JavaType.buildType(fieldInfo.getTypeDescriptor().toString()), annotations, Flag.flagsToBitMap(flags));
                variables.add(variable);
            }
            return variables;
        }
        return emptyList();
    }

    private static List<JavaType.Method> fromMethodInfo(MethodInfoList methodInfos, Stack<ClassInfo> stack) {
        List<JavaType.Method> methods = new ArrayList<>(methodInfos.size());
        for (MethodInfo methodInfo : methodInfos) {
            Set<Flag> flags = Flag.bitMapToFlags(methodInfo.getModifiers());
            List<JavaType> parameterTypes = new ArrayList<>(methodInfo.getParameterInfo().length);
            for (MethodParameterInfo methodParameterInfo : methodInfo.getParameterInfo()) {
                parameterTypes.add(JavaType.buildType(methodParameterInfo.getTypeDescriptor().toString()));
            }

            JavaType.Method.Signature signature = new JavaType.Method.Signature(JavaType.buildType(methodInfo.getTypeDescriptor().getResultType().toString()), parameterTypes);

            List<String> methodParams = new ArrayList<>(methodInfo.getParameterInfo().length);
            for (MethodParameterInfo methodParameterInfo : methodInfo.getParameterInfo()) {
                methodParams.add(methodParameterInfo.getName());
            }

            List<JavaType.FullyQualified> thrownExceptions = new ArrayList<>(methodInfo.getTypeDescriptor()
                    .getThrowsSignatures().size());
            for (ClassRefOrTypeVariableSignature throwsSignature : methodInfo.getTypeDescriptor().getThrowsSignatures()) {
                if (throwsSignature instanceof ClassRefTypeSignature) {
                    thrownExceptions.add(fromClassInfo(((ClassRefTypeSignature) throwsSignature).getClassInfo(), stack));
                }
            }

            List<JavaType.FullyQualified> annotations = new ArrayList<>(methodInfo.getAnnotationInfo().size());
            for (AnnotationInfo annotationInfo : methodInfo.getAnnotationInfo()) {
                annotations.add(fromClassInfo(annotationInfo.getClassInfo(), stack));
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
}
