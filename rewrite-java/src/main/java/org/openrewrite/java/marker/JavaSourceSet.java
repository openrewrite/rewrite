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

import io.github.classgraph.*;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.With;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Marker;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@With
public class JavaSourceSet implements Marker {
    private static Map<String, JavaType.FullyQualified> JAVA8_CLASSPATH;
    private static Map<String, JavaType.FullyQualified> JAVA11_CLASSPATH;

    @EqualsAndHashCode.Include
    UUID id;

    String name;
    List<JavaType.FullyQualified> classpath;

    public static JavaSourceSet build(String sourceSetName, Iterable<Path> classpath,
                                      JavaTypeCache typeCache, ExecutionContext ctx) {

        Map<String, JavaType.FullyQualified> jvmClasses = jvmClasses(typeCache, ctx);
        List<JavaType.FullyQualified> fqns = new ArrayList<>(jvmClasses.values());

        Builder builder = new Builder(typeCache, ctx, jvmClasses);

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
                    fqns.add(builder.type(classInfo));
                } catch (Exception e) {
                    ctx.getOnError().accept(e);
                }
            }
        }

        return new JavaSourceSet(randomId(), sourceSetName, fqns);
    }

    private static Map<String, JavaType.FullyQualified> jvmClasses(JavaTypeCache typeCache, ExecutionContext ctx) {
        boolean java8 = System.getProperty("java.version").startsWith("1.8");

        if (java8 && JAVA8_CLASSPATH != null) {
            return JAVA8_CLASSPATH;
        } else if (!java8 && JAVA11_CLASSPATH != null) {
            return JAVA11_CLASSPATH;
        }

        ClassInfoList classInfos = new ClassGraph()
                .enableMemoryMapping()
                .enableAnnotationInfo()
                .enableClassInfo()
                .enableMethodInfo()
                .enableFieldInfo()
                .enableSystemJarsAndModules()
                .acceptPackages("java")
                .rejectPackages("java.awt")
                .rejectPackages("java.applet")
                .scan()
                .getAllClasses();

        Builder builder = new Builder(typeCache, ctx, emptyMap());
        Map<String, JavaType.FullyQualified> fqns = new HashMap<>(classInfos.size());
        for (ClassInfo classInfo : classInfos) {
            try {
                fqns.put(classInfo.getName(), builder.type(classInfo));
            } catch (Exception e) {
                ctx.getOnError().accept(e);
            }
        }

        if (java8) {
            JAVA8_CLASSPATH = fqns;
        } else {
            JAVA11_CLASSPATH = fqns;
        }

        return fqns;
    }

    @RequiredArgsConstructor
    private static class Builder {
        private final JavaTypeCache typeCache;
        private final ExecutionContext ctx;
        private final Map<String, JavaType.FullyQualified> jvmTypes;
        private final Map<String, JavaType.FullyQualified> stack = new HashMap<>();

        public JavaType.FullyQualified type(@Nullable ClassInfo aClass) {
            if (aClass == null) {
                //noinspection ConstantConditions
                return null;
            }

            JavaType.FullyQualified existingClass = stack.get(aClass.getName());
            if (existingClass != null) {
                return existingClass;
            }

            AtomicBoolean newlyCreated = new AtomicBoolean(false);

            JavaType.Class clazz = typeCache.computeClass(
                    aClass.getName(),
                    () -> {
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

                        if (aClass.getName().startsWith("com.sun.") ||
                                aClass.getName().startsWith("sun.") ||
                                aClass.getName().startsWith("java.awt.") ||
                                aClass.getName().startsWith("jdk.") ||
                                aClass.getName().startsWith("org.graalvm")) {
                            return new JavaType.Class(
                                    null, aClass.getModifiers(), aClass.getName(), kind,
                                    null, null, null, null, null, null);
                        }

                        newlyCreated.set(true);

                        return new JavaType.Class(
                                null,
                                aClass.getModifiers(),
                                aClass.getName(),
                                kind,
                                null, null, null, null, null, null
                        );
                    }
            );

            if (newlyCreated.get()) {
                stack.put(aClass.getName(), clazz);

                JavaType.FullyQualified supertype = type(aClass.getSuperclass());
                JavaType.FullyQualified owner = aClass.getOuterClasses().isEmpty() ? null :
                        type(aClass.getOuterClasses().get(0));

                List<JavaType.FullyQualified> annotations = null;
                if (!aClass.getAnnotationInfo().isEmpty()) {
                    annotations = new ArrayList<>(aClass.getAnnotationInfo().size());
                    for (AnnotationInfo annotationInfo : aClass.getAnnotationInfo()) {
                        annotations.add(type(annotationInfo.getClassInfo()));
                    }
                }

                List<JavaType.FullyQualified> interfaces = null;
                if (!aClass.getInterfaces().isEmpty()) {
                    interfaces = new ArrayList<>(aClass.getInterfaces().size());
                    for (ClassInfo anInterface : aClass.getInterfaces()) {
                        interfaces.add(type(anInterface));
                    }
                }

                List<JavaType.Variable> variables = null;
                if (!aClass.getFieldInfo().isEmpty()) {
                    variables = new ArrayList<>(aClass.getFieldInfo().size());
                    for (FieldInfo fieldInfo : aClass.getFieldInfo()) {
                        JavaType.Variable variable = variableType(fieldInfo);
                        variables.add(variable);
                    }
                }

                List<JavaType.Method> methods = null;
                if (!aClass.getMethodInfo().isEmpty()) {
                    methods = new ArrayList<>(aClass.getMethodInfo().size());
                    for (MethodInfo methodInfo : aClass.getMethodInfo()) {
                        JavaType.Method method = methodType(methodInfo);
                        if (method != null) {
                            methods.add(method);
                        }
                    }
                }

                clazz.unsafeSet(supertype, owner, annotations, interfaces, variables, methods);

                stack.remove(aClass.getName());
            }

            ClassTypeSignature typeSignature = aClass.getTypeSignature();
            if (typeSignature == null || typeSignature.getTypeParameters() == null || typeSignature.getTypeParameters().isEmpty()) {
                return clazz;
            }

            StringJoiner shallowGenericTypeVariables = new StringJoiner(",");
            for (TypeParameter typeParameter : typeSignature.getTypeParameters()) {
                shallowGenericTypeVariables.add(signature(typeParameter));
            }

            newlyCreated.set(false);

            JavaType.Parameterized parameterized = typeCache.computeParameterized(aClass.getName(),
                    shallowGenericTypeVariables.toString(), () -> {
                        newlyCreated.set(true);
                        return new JavaType.Parameterized(null, null, null);
                    });

            if (newlyCreated.get()) {
                List<JavaType> typeParameters = new ArrayList<>(typeSignature.getTypeParameters().size());
                for (TypeParameter tParam : typeSignature.getTypeParameters()) {
                    JavaType javaType = type(tParam);
                    typeParameters.add(javaType);
                }

                parameterized.unsafeSet(clazz, typeParameters);
            }

            return parameterized;
        }

        private JavaType.Variable variableType(FieldInfo fieldInfo) {
            return typeCache.computeVariable(
                    fieldInfo.getClassName(),
                    fieldInfo.getName(),
                    () -> {
                        JavaType.FullyQualified owner = type(fieldInfo.getClassInfo());

                        List<JavaType.FullyQualified> annotations = emptyList();
                        if (!fieldInfo.getAnnotationInfo().isEmpty()) {
                            annotations = new ArrayList<>(fieldInfo.getAnnotationInfo().size());
                            for (AnnotationInfo annotationInfo : fieldInfo.getAnnotationInfo()) {
                                annotations.add(type(annotationInfo.getClassInfo()));
                            }
                        }

                        return new JavaType.Variable(fieldInfo.getModifiers(), fieldInfo.getName(), owner,
                                type(fieldInfo.getTypeDescriptor()), annotations);
                    });
        }

        @Nullable
        private JavaType.Method methodType(MethodInfo methodInfo) {
            try {
                long flags = methodInfo.getModifiers();

                // The field access modifier "volatile" corresponds to the "bridge" modifier on methods.
                // We don't represent "bridge" because it is a compiler internal that cannot appear in source code.
                // See https://github.com/openrewrite/rewrite/issues/995
                if ((flags & Flag.Volatile.getBitMask()) != 0) {
                    return null;
                }

                List<JavaType> genericParameterTypes = new ArrayList<>(methodInfo.getParameterInfo().length);
                for (MethodParameterInfo methodParameterInfo : methodInfo.getParameterInfo()) {
                    genericParameterTypes.add(methodParameterInfo.getTypeSignature() == null ?
                            type(methodParameterInfo.getTypeDescriptor()) :
                            type(methodParameterInfo.getTypeSignature()));
                }
                JavaType.Method.Signature genericSignature = new JavaType.Method.Signature(
                        methodInfo.getTypeSignature() == null ?
                                type(methodInfo.getTypeDescriptor().getResultType()) :
                                type(methodInfo.getTypeSignature().getResultType()),
                        genericParameterTypes
                );

                List<JavaType> resolvedParameterTypes = new ArrayList<>(methodInfo.getParameterInfo().length);
                for (MethodParameterInfo methodParameterInfo : methodInfo.getParameterInfo()) {
                    resolvedParameterTypes.add(type(methodParameterInfo.getTypeDescriptor()));
                }
                JavaType.Method.Signature resolvedSignature = new JavaType.Method.Signature(type(methodInfo.getTypeDescriptor().getResultType()), resolvedParameterTypes);

                List<String> paramNames = null;
                if (methodInfo.getParameterInfo().length > 0) {
                    paramNames = new ArrayList<>(methodInfo.getParameterInfo().length);
                    for (MethodParameterInfo methodParameterInfo : methodInfo.getParameterInfo()) {
                        paramNames.add(methodParameterInfo.getName());
                    }
                }

                List<JavaType.FullyQualified> thrownExceptions = null;
                if (!methodInfo.getTypeDescriptor().getThrowsSignatures().isEmpty()) {
                    thrownExceptions = new ArrayList<>(methodInfo.getTypeDescriptor().getThrowsSignatures().size());
                    for (ClassRefOrTypeVariableSignature throwsSignature : methodInfo.getTypeDescriptor().getThrowsSignatures()) {
                        if (throwsSignature instanceof ClassRefTypeSignature) {
                            thrownExceptions.add(type(((ClassRefTypeSignature) throwsSignature).getClassInfo()));
                        }
                    }
                }

                List<JavaType.FullyQualified> annotations = null;
                if (!methodInfo.getAnnotationInfo().isEmpty()) {
                    annotations = new ArrayList<>(methodInfo.getAnnotationInfo().size());
                    for (AnnotationInfo annotationInfo : methodInfo.getAnnotationInfo()) {
                        annotations.add(type(annotationInfo.getClassInfo()));
                    }
                }

                return new JavaType.Method(
                        methodInfo.getModifiers(),
                        type(methodInfo.getClassInfo()),
                        methodInfo.getName(),
                        paramNames,
                        genericSignature,
                        resolvedSignature,
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

        private JavaType.GenericTypeVariable type(TypeParameter typeParameter) {
            String signature = "";
            if (typeParameter.getClassBound() != null) {
                signature(typeParameter.getClassBound());
            } else if (typeParameter.getInterfaceBounds() != null) {
                StringJoiner interfaceBounds = new StringJoiner(" & ");
                for (ReferenceTypeSignature interfaceBound : typeParameter.getInterfaceBounds()) {
                    interfaceBounds.add(signature(interfaceBound));
                }
                signature = interfaceBounds.toString();
            }

            return typeCache.computeGeneric(
                    typeParameter.getName(),
                    signature,
                    () -> {
                        List<JavaType> bounds = null;
                        if(typeParameter.getClassBound() != null) {
                            if(typeParameter.getClassBound() instanceof ClassRefTypeSignature) {
                                ReferenceTypeSignature bound = typeParameter.getClassBound();
                                ClassRefTypeSignature classBound = (ClassRefTypeSignature) bound;
                                if (classBound.getClassInfo() != null && !"java.lang.Object".equals(classBound.getFullyQualifiedClassName())) {
                                    bounds = new ArrayList<>();
                                    bounds.add(type(typeParameter.getClassBound()));
                                }
                            } else {
                                bounds = new ArrayList<>();
                                bounds.add(type(typeParameter.getClassBound()));
                            }
                        }
                        if(typeParameter.getInterfaceBounds() != null && !typeParameter.getInterfaceBounds().isEmpty()) {
                            if(bounds == null) {
                                bounds = new ArrayList<>();
                            }
                            for (ReferenceTypeSignature interfaceBound : typeParameter.getInterfaceBounds()) {
                                bounds.add(type(interfaceBound));
                            }
                        }
                        if(bounds == null) {
                            bounds = emptyList();
                        }
                        return new JavaType.GenericTypeVariable(null, typeParameter.getName(), bounds);
                    }
            );
        }

        private JavaType type(TypeSignature typeSignature) {
            if (typeSignature instanceof ClassRefTypeSignature) {
                ClassRefTypeSignature classRefSig = (ClassRefTypeSignature) typeSignature;
                ClassInfo classInfo = classRefSig.getClassInfo();
                if (classInfo == null) {
                    JavaType.FullyQualified jvmType = jvmTypes.get(classRefSig.getBaseClassName());
                    if (jvmType != null) {
                        return jvmType;
                    } else {
                        // FIXME more of these
                    }
                }
                return type(classInfo);
            } else if (typeSignature instanceof ArrayTypeSignature) {
                ArrayClassInfo arrClassInfo = ((ArrayTypeSignature) typeSignature).getArrayClassInfo();
                JavaType type = type(arrClassInfo.getElementClassInfo());
                for (int i = 0; i < arrClassInfo.getNumDimensions(); i++) {
                    type = new JavaType.Array(type);
                }
                return type;
            } else if (typeSignature instanceof TypeVariableSignature) {
                return new JavaType.GenericTypeVariable(null, ((TypeVariableSignature) typeSignature).getName(), emptyList());
            } else if (typeSignature instanceof BaseTypeSignature) {
                //noinspection ConstantConditions
                return JavaType.Primitive.fromKeyword(((BaseTypeSignature) typeSignature).getTypeStr());
            }
            throw new UnsupportedOperationException("Unexpected signature type " + typeSignature.getClass().getName());
        }

        private String signature(TypeParameter typeParameter) {
            StringBuilder s = new StringBuilder(typeParameter.getName());
            s.append(" extends ");

            if (typeParameter.getClassBound() != null) {
                s.append(signature(typeParameter.getClassBound()));
            } else if (typeParameter.getInterfaceBounds() != null) {
                StringJoiner interfaceBounds = new StringJoiner(" & ");
                for (ReferenceTypeSignature interfaceBound : typeParameter.getInterfaceBounds()) {
                    interfaceBounds.add(signature(interfaceBound));
                }
                s.append(interfaceBounds);
            }

            return s.toString();
        }

        private String signature(TypeSignature typeSignature) {
            if (typeSignature instanceof ClassRefTypeSignature) {
                return ((ClassRefTypeSignature) typeSignature).getBaseClassName();
            } else if (typeSignature instanceof ArrayTypeSignature) {
                ArrayTypeSignature arrSignature = (ArrayTypeSignature) typeSignature;
                StringBuilder signature = new StringBuilder(signature(arrSignature.getElementTypeSignature()));
                for (int i = 0; i < arrSignature.getNumDimensions(); i++) {
                    signature.append("[]");
                }
                return signature.toString();
            } else if (typeSignature instanceof TypeVariableSignature) {
                return ((TypeVariableSignature) typeSignature).getName();
            } else if (typeSignature instanceof BaseTypeSignature) {
                return ((BaseTypeSignature) typeSignature).getTypeStr();
            }
            throw new UnsupportedOperationException("Unexpected signature type " + typeSignature.getClass().getName());
        }
    }
}
