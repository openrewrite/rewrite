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
package org.openrewrite.java.internal;

import io.github.classgraph.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTypeMapping;
import org.openrewrite.java.tree.JavaType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.*;

public class ClassgraphTypeMapping implements JavaTypeMapping<ClassInfo> {
    private final ClassgraphJavaTypeSignatureBuilder signatureBuilder;

    private final Map<String, Object> typeBySignature;
    private final JavaReflectionTypeMapping reflectionTypeMapping;
    private final Map<String, JavaType.FullyQualified> jvmTypes;

    public ClassgraphTypeMapping(Map<String, Object> typeBySignature, Map<String, JavaType.FullyQualified> jvmTypes) {
        this.typeBySignature = typeBySignature;
        this.reflectionTypeMapping = new JavaReflectionTypeMapping(typeBySignature);
        this.jvmTypes = jvmTypes;
        this.signatureBuilder = new ClassgraphJavaTypeSignatureBuilder(jvmTypes);
    }

    public JavaType.FullyQualified type(@Nullable ClassInfo aClass) {
        if (aClass == null) {
            return JavaType.Class.Unknown.getInstance();
        }

        String className = aClass.getName();
        JavaType.FullyQualified clazz = (JavaType.FullyQualified) typeBySignature.get(className);

        if (clazz == null) {
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

            clazz = new JavaType.Class(
                    null,
                    aClass.getModifiers(),
                    className,
                    kind,
                    null, null, null, null, null, null
            );

            typeBySignature.put(className, clazz);

            ClassInfo superclassInfo = aClass.getSuperclass();
            JavaType.FullyQualified supertype;
            if (superclassInfo == null) {
                // Classgraph reports null for the supertype of interfaces, for consistency with other TypeMappings we report Object
                supertype = (JavaType.FullyQualified) reflectionTypeMapping.type(Object.class);
            } else {
                supertype = type(superclassInfo);
            }
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
            if (!aClass.getDeclaredFieldInfo().isEmpty()) {
                variables = new ArrayList<>(aClass.getDeclaredFieldInfo().size());
                for (FieldInfo fieldInfo : aClass.getDeclaredFieldInfo()) {
                    if (!fieldInfo.isSynthetic()) {
                        if (!aClass.getName().equals("java.lang.String") || !fieldInfo.getName().equals("serialPersistentFields")) {
                            JavaType.Variable variable = variableType(fieldInfo);
                            variables.add(variable);
                        }
                    }
                }
            }

            List<JavaType.Method> methods = null;
            if (!aClass.getDeclaredMethodInfo().isEmpty()) {
                methods = new ArrayList<>(aClass.getDeclaredMethodInfo().size() + aClass.getDeclaredConstructorInfo().size());
                for (MethodInfo methodInfo : aClass.getDeclaredMethodInfo()) {
                    if (!(methodInfo.isBridge() || methodInfo.isSynthetic())) {
                        methods.add(methodType(methodInfo));
                    }
                }
            }

            if (!aClass.getDeclaredConstructorInfo().isEmpty()) {
                if (methods == null) {
                    methods = new ArrayList<>(aClass.getDeclaredConstructorInfo().size());
                }
                for (MethodInfo ctor : aClass.getDeclaredConstructorInfo()) {
                    if (!(ctor.isBridge() || ctor.isSynthetic())) {
                        methods.add(methodType(ctor));
                    }
                }
            }

            ((JavaType.Class) clazz).unsafeSet(supertype, owner, annotations, interfaces, variables, methods);
        }

        ClassTypeSignature typeSignature = aClass.getTypeSignature();
        if (typeSignature == null || typeSignature.getTypeParameters() == null || typeSignature.getTypeParameters().isEmpty()) {
            return clazz;
        }

        String signature = signatureBuilder.signature(aClass.getTypeSignature());
        JavaType.Parameterized parameterized = (JavaType.Parameterized) typeBySignature.get(signature);
        if (parameterized != null) {
            return parameterized;
        }

        parameterized = new JavaType.Parameterized(null, clazz, null);
        typeBySignature.put(signature, parameterized);

        List<JavaType> typeParameters = new ArrayList<>(typeSignature.getTypeParameters().size());
        for (TypeParameter tParam : typeSignature.getTypeParameters()) {
            typeParameters.add(type(tParam));
        }

        parameterized.unsafeSet(clazz, typeParameters);
        return parameterized;
    }

    private JavaType type(HierarchicalTypeSignature typeSignature) {
        String signature = signatureBuilder.signature(typeSignature);
        JavaType existing = (JavaType) typeBySignature.get(signature);
        if (existing != null) {
            return existing;
        }

        if (typeSignature instanceof ClassRefTypeSignature) {
            return classType((ClassRefTypeSignature) typeSignature, signature);
        } else if (typeSignature instanceof ClassTypeSignature) {
            return classType((ClassTypeSignature) typeSignature, signature);
        } else if (typeSignature instanceof ArrayTypeSignature) {
            return array((ArrayTypeSignature) typeSignature, signature);
        } else if (typeSignature instanceof BaseTypeSignature) {
            //noinspection ConstantConditions
            return JavaType.Primitive.fromKeyword(((BaseTypeSignature) typeSignature).getTypeStr());
        } else if (typeSignature instanceof TypeVariableSignature) {
            return generic((TypeVariableSignature) typeSignature, signature);
        } else if (typeSignature instanceof TypeArgument) {
            return generic((TypeArgument) typeSignature, signature);
        } else if (typeSignature instanceof TypeParameter) {
            return generic((TypeParameter) typeSignature, signature);
        }

        throw new UnsupportedOperationException("Unexpected signature type " + typeSignature.getClass().getName());
    }

    private JavaType.Variable variableType(FieldInfo fieldInfo) {
        String signature = signatureBuilder.variableSignature(fieldInfo);
        JavaType.Variable existing = (JavaType.Variable) typeBySignature.get(signature);
        if (existing != null) {
            return existing;
        }

        JavaType.Variable variable = new JavaType.Variable(null, fieldInfo.getModifiers(), fieldInfo.getName(),
                null, null, null);
        typeBySignature.put(signature, variable);

        JavaType.FullyQualified owner = type(fieldInfo.getClassInfo());

        List<JavaType.FullyQualified> annotations = emptyList();
        if (!fieldInfo.getAnnotationInfo().isEmpty()) {
            annotations = new ArrayList<>(fieldInfo.getAnnotationInfo().size());
            for (AnnotationInfo annotationInfo : fieldInfo.getAnnotationInfo()) {
                annotations.add(type(annotationInfo.getClassInfo()));
            }
        }

        variable.unsafeSet(owner instanceof JavaType.Parameterized ? ((JavaType.Parameterized) owner).getType() : owner,
                type(fieldInfo.getTypeDescriptor()), annotations);
        return variable;
    }

    private JavaType.Method methodType(MethodInfo methodInfo) {
        long flags = methodInfo.getModifiers();

        String signature = signatureBuilder.methodSignature(methodInfo);
        JavaType.Method existing = (JavaType.Method) typeBySignature.get(signature);
        if (existing != null) {
            return existing;
        }

        List<String> paramNames = null;
        if (methodInfo.getParameterInfo().length > 0) {
            paramNames = new ArrayList<>(methodInfo.getParameterInfo().length);
            for (MethodParameterInfo methodParameterInfo : methodInfo.getParameterInfo()) {
                paramNames.add(methodParameterInfo.getName());
            }
        }

        JavaType.Method method = new JavaType.Method(
                null,
                methodInfo.getModifiers(),
                null,
                methodInfo.isConstructor() ? "<constructor>" : methodInfo.getName(),
                null,
                paramNames,
                null, null, null
        );
        typeBySignature.put(signature, method);

        JavaType returnType = methodInfo.getTypeSignature() == null ?
                type(methodInfo.getTypeDescriptor().getResultType()) :
                type(methodInfo.getTypeSignature().getResultType());

        List<JavaType> parameterTypes = new ArrayList<>(methodInfo.getParameterInfo().length);
        for (MethodParameterInfo methodParameterInfo : methodInfo.getParameterInfo()) {
            parameterTypes.add(methodParameterInfo.getTypeSignature() == null ?
                    type(methodParameterInfo.getTypeDescriptor()) :
                    type(methodParameterInfo.getTypeSignature()));
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

        JavaType.FullyQualified type = type(methodInfo.getClassInfo());
        JavaType.FullyQualified declaringType = type instanceof JavaType.Parameterized ? ((JavaType.Parameterized) type).getType() : type;
        method.unsafeSet(declaringType, methodInfo.isConstructor() ? declaringType : returnType, parameterTypes, thrownExceptions, annotations);
        return method;
    }

    private JavaType.GenericTypeVariable generic(TypeParameter typeParameter, String signature) {
        JavaType.GenericTypeVariable gtv = new JavaType.GenericTypeVariable(null, typeParameter.getName(),
                INVARIANT, null);
        typeBySignature.put(signature, gtv);

        List<JavaType> bounds = null;
        if (typeParameter.getClassBound() != null) {
            JavaType mappedBound = type(typeParameter.getClassBound());
            if (typeParameter.getClassBound() instanceof ClassRefTypeSignature) {
                ReferenceTypeSignature bound = typeParameter.getClassBound();
                ClassRefTypeSignature classBound = (ClassRefTypeSignature) bound;
                if (!"java.lang.Object".equals(classBound.getFullyQualifiedClassName())) {
                    bounds = singletonList(mappedBound);
                }
            } else {
                bounds = singletonList(mappedBound);
            }
        } else if (typeParameter.getInterfaceBounds() != null && !typeParameter.getInterfaceBounds().isEmpty()) {
            bounds = new ArrayList<>(typeParameter.getInterfaceBounds().size());
            for (ReferenceTypeSignature interfaceBound : typeParameter.getInterfaceBounds()) {
                bounds.add(type(interfaceBound));
            }
        }

        gtv.unsafeSet(bounds == null ? INVARIANT : COVARIANT, bounds);
        return gtv;
    }

    private JavaType.GenericTypeVariable generic(TypeVariableSignature typeVariableSignature, String signature) {
        try {
            return (JavaType.GenericTypeVariable) type(typeVariableSignature.resolve());
        } catch (IllegalArgumentException ignored) {
            JavaType.GenericTypeVariable gtv = new JavaType.GenericTypeVariable(null, typeVariableSignature.getName(),
                    INVARIANT, null);
            typeBySignature.put(signature, gtv);
            return gtv;
        }
    }

    private JavaType generic(TypeArgument typeArgument, String signature) {
        List<JavaType> bounds = null;

        JavaType.GenericTypeVariable gtv;

        switch (typeArgument.getWildcard()) {
            case NONE:
                return type(typeArgument.getTypeSignature());
            case EXTENDS: {
                gtv = new JavaType.GenericTypeVariable(null, "?", COVARIANT, null);
                typeBySignature.put(signature, gtv);
                JavaType mappedBound = type(typeArgument.getTypeSignature());
                if (!(mappedBound instanceof JavaType.FullyQualified) || !((JavaType.FullyQualified) mappedBound)
                        .getFullyQualifiedName().equals("java.lang.Object")) {
                    bounds = singletonList(mappedBound);
                }
                break;
            }
            case SUPER: {
                gtv = new JavaType.GenericTypeVariable(null, "?", CONTRAVARIANT, null);
                typeBySignature.put(signature, gtv);
                JavaType mappedBound = type(typeArgument.getTypeSignature());
                if (!(mappedBound instanceof JavaType.FullyQualified) || !((JavaType.FullyQualified) mappedBound)
                        .getFullyQualifiedName().equals("java.lang.Object")) {
                    bounds = singletonList(mappedBound);
                }
                break;
            }
            case ANY:
            default:
                gtv = new JavaType.GenericTypeVariable(null, "?", INVARIANT, null);
                typeBySignature.put(signature, gtv);
                break;
        }

        bounds = ListUtils.map(bounds, b -> b instanceof JavaType.FullyQualified &&
                ((JavaType.FullyQualified) b).getFullyQualifiedName().equals("java.lang.Object") ? null : b);
        gtv.unsafeSet(bounds == null || bounds.isEmpty() ? INVARIANT : gtv.getVariance(), bounds);
        return gtv;
    }

    private JavaType classType(ClassRefTypeSignature classRefSignature, String signature) {
        ClassInfo classInfo = classRefSignature.getClassInfo();

        JavaType.FullyQualified type;
        if (classInfo == null) {
            String className = classRefSignature.getBaseClassName();
            type = jvmTypes.get(className);
            if (type == null) {
                if (className.equals("java.lang.Object")) {
                    type = (JavaType.FullyQualified) reflectionTypeMapping.type(Object.class);
                } else {
                    type = JavaType.Unknown.getInstance();
                    typeBySignature.put(className, type);
                }
            }
        } else {
            type = type(classInfo);
        }

        if (!classRefSignature.getTypeArguments().isEmpty()) {
            JavaType existing = (JavaType) typeBySignature.get(signature);
            if (existing != null) {
                return existing;
            }

            JavaType.Parameterized parameterized = new JavaType.Parameterized(null,
                    type instanceof JavaType.Parameterized ? ((JavaType.Parameterized) type).getType() : type, null);

            typeBySignature.put(signature, parameterized);

            List<JavaType> typeParameters = new ArrayList<>(classRefSignature.getTypeArguments().size());
            for (TypeArgument typeArgument : classRefSignature.getTypeArguments()) {
                typeParameters.add(type(typeArgument));
            }

            parameterized.unsafeSet(parameterized.getType(), typeParameters);
            return parameterized;
        }

        return type;
    }

    private JavaType classType(ClassTypeSignature classSignature, String signature) {
        try {
            Method getClassInfo = classSignature.getClass().getDeclaredMethod("getClassInfo");
            getClassInfo.setAccessible(true);

            ClassInfo classInfo = (ClassInfo) getClassInfo.invoke(classSignature);
            if (classInfo == null) {
                Method getClassName = classSignature.getClass().getDeclaredMethod("getClassName");
                getClassName.setAccessible(true);
                String className = (String) getClassName.invoke(classSignature);

                JavaType fallback = jvmTypes.get(className);
                if (fallback == null) {
                    if (className.equals("java.lang.Object")) {
                        fallback = reflectionTypeMapping.type(Object.class);
                    } else {
                        fallback = JavaType.Unknown.getInstance();
                    }
                }
                typeBySignature.put(signature, fallback);
                return fallback;
            }

            JavaType.Class clazz = (JavaType.Class) type(classInfo);

            if (classSignature.getTypeParameters().isEmpty()) {
                return clazz;
            }

            JavaType.Parameterized parameterized = new JavaType.Parameterized(null, null, null);
            typeBySignature.put(signature, parameterized);

            List<JavaType> typeParameters = new ArrayList<>(classSignature.getTypeParameters().size());
            for (TypeParameter typeParameter : classSignature.getTypeParameters()) {
                typeParameters.add(type(typeParameter));
            }

            parameterized.unsafeSet(clazz, typeParameters);
            return parameterized;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private JavaType array(ArrayTypeSignature typeSignature, String signature) {
        JavaType.Array arr = new JavaType.Array(null, null);
        typeBySignature.put(signature, arr);
        arr.unsafeSet(type(typeSignature.getNestedType()));
        return arr;
    }
}
