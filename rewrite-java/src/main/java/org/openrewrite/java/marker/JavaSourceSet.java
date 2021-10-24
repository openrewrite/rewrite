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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaExecutionContextView;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Marker;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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

    public static JavaSourceSet build(String sourceSetName, Iterable<Path> classpath, ExecutionContext ctx) {
        Builder builder = new Builder(ctx);

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
                try {
                    fqns.add(builder.type(classInfo, new HashMap<>()));
                } catch (Exception e) {
                    ctx.getOnError().accept(e);
                }
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
                try {
                    fqns.add(builder.type(classInfo, new HashMap<>()));
                } catch (Exception e) {
                    ctx.getOnError().accept(e);
                }
            }
        }

        return new JavaSourceSet(Tree.randomId(), sourceSetName, fqns);
    }

    private static class Builder {
        private final JavaExecutionContextView ctx;

        private Builder(ExecutionContext ctx) {
            this.ctx = new JavaExecutionContextView(ctx);
        }

        public JavaType.Class type(@Nullable ClassInfo aClass, Map<String, JavaType.Class> stack) {
            if(aClass == null) {
                //noinspection ConstantConditions
                return null;
            }

            JavaType.Class existingClass = stack.get(aClass.getName());
            if (existingClass != null) {
                return existingClass;
            }

            AtomicBoolean newlyCreated = new AtomicBoolean(false);

            JavaType.Class clazz = ctx.getTypeCache().computeClass(
                    getClasspathElement(aClass),
                    aClass.getName(),
                    () -> {
                        newlyCreated.set(true);

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

                        return new JavaType.Class(
                                Flag.flagsToBitMap(flags),
                                aClass.getName(),
                                kind,
                                null, null, null, null,
                                type(aClass.getSuperclass(), stack),
                                null
                        );
                    }
            );

            if (newlyCreated.get()) {
                Map<String, JavaType.Class> stackWithSym = new HashMap<>(stack);
                stackWithSym.put(aClass.getName(), clazz);

                List<JavaType.Variable> variables = null;
                if (!aClass.getFieldInfo().isEmpty()) {
                    variables = new ArrayList<>(aClass.getFieldInfo().size());
                    for (FieldInfo fieldInfo : aClass.getFieldInfo()) {
                        JavaType.Variable variable = variableType(fieldInfo, stackWithSym);
                        variables.add(variable);
                    }
                }

                List<JavaType.Method> methods = null;
                if (!aClass.getMethodInfo().isEmpty()) {
                    methods = new ArrayList<>(aClass.getMethodInfo().size());
                    for (MethodInfo methodInfo : aClass.getMethodInfo()) {
                        JavaType.Method method = methodType(methodInfo, stackWithSym);
                        if (method != null) {
                            methods.add(method);
                        }
                    }
                }

                clazz.unsafeSet(null, null, variables, methods);
            }

            return clazz;
        }

        private JavaType.Variable variableType(FieldInfo fieldInfo, Map<String, JavaType.Class> stack) {
            return ctx.getTypeCache().computeVariable(
                    getClasspathElement(fieldInfo.getClassInfo()),
                    fieldInfo.getClassName(),
                    fieldInfo.getName(),
                    () -> {
                        JavaType.Class owner = type(fieldInfo.getClassInfo(), stack);

                        List<JavaType.FullyQualified> annotations = emptyList();
                        if (!fieldInfo.getAnnotationInfo().isEmpty()) {
                            annotations = new ArrayList<>(fieldInfo.getAnnotationInfo().size());
                            for (AnnotationInfo annotationInfo : fieldInfo.getAnnotationInfo()) {
                                annotations.add(type(annotationInfo.getClassInfo(), stack));
                            }
                        }

                        return new JavaType.Variable(fieldInfo.getModifiers(), owner, fieldInfo.getName(),
                                JavaType.buildType(fieldInfo.getTypeDescriptor().toString()), annotations);
                    });
        }

        @Nullable
        private JavaType.Method methodType(MethodInfo methodInfo, Map<String, JavaType.Class> stack) {
            try {
                Set<Flag> flags = Flag.bitMapToFlags(methodInfo.getModifiers());
                // The field access modifier "volatile" corresponds to the "bridge" modifier on methods.
                // We don't represent "bridge" because it is a compiler internal that cannot appear in source code.
                // See https://github.com/openrewrite/rewrite/issues/995
                if (flags.contains(Flag.Volatile)) {
                    return null;
                }

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
                        thrownExceptions.add(type(((ClassRefTypeSignature) throwsSignature).getClassInfo(), stack));
                    }
                }

                List<JavaType.FullyQualified> annotations = new ArrayList<>(methodInfo.getAnnotationInfo().size());
                for (AnnotationInfo annotationInfo : methodInfo.getAnnotationInfo()) {
                    annotations.add(type(annotationInfo.getClassInfo(), stack));
                }

                return new JavaType.Method(
                        methodInfo.getModifiers(),
                        type(methodInfo.getClassInfo(), stack),
                        methodInfo.getName(),
                        null,
                        signature,
                        methodParams,
                        thrownExceptions,
                        annotations
                );
            } catch (Exception e) {
                ctx.getOnError().accept(e);
                return null;
            }
        }

        private Path getClasspathElement(ClassInfo classInfo) {
            if (classInfo.getModuleInfo() != null) {
                return Paths.get(classInfo.getModuleInfo().getLocation());
            } else {
                return Paths.get("__source_set__");
            }
        }
    }
}
