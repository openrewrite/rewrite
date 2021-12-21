package org.openrewrite.java.internal;

import lombok.RequiredArgsConstructor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.JavaType;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;
import static org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.COVARIANT;
import static org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.INVARIANT;

/**
 * Type mapping from type attribution given from {@link java.lang.reflect} types.
 */
@RequiredArgsConstructor
public class JavaReflectionTypeMapping {
    private static final int KIND_BITMASK_INTERFACE = 1 << 9;
    private static final int KIND_BITMASK_ANNOTATION = 1 << 13;
    private static final int KIND_BITMASK_ENUM = 1 << 14;

    private final JavaReflectionTypeSignatureBuilder signatureBuilder = new JavaReflectionTypeSignatureBuilder();
    private final Map<Class<?>, JavaType.Class> classStack = new IdentityHashMap<>();

    private final Map<String, Object> typeBySignature;

    public <T extends JavaType> T type(@Nullable Type type) {
        if (type == null) {
            //noinspection ConstantConditions
            return null;
        }
        classStack.clear();

        //noinspection unchecked
        return (T) _type(type);
    }

    public JavaType _type(Type type) {
        if (type instanceof Class) {
            return classType((Class<?>) type);
        } else if (type instanceof GenericArrayType) {
            throw new UnsupportedOperationException("Unknown type " + type.getClass().getName());
        } else if (type instanceof TypeVariable) {
            throw new UnsupportedOperationException("Unknown type " + type.getClass().getName());
        } else if (type instanceof WildcardType) {
            throw new UnsupportedOperationException("Unknown type " + type.getClass().getName());
        } else if (type instanceof ParameterizedType) {
            throw new UnsupportedOperationException("Unknown type " + type.getClass().getName());
        }
        throw new UnsupportedOperationException("Unknown type " + type.getClass().getName());
    }

    private JavaType classType(Class<?> clazz) {
        JavaType.Class existingClass = classStack.get(clazz);
        if (existingClass != null) {
            return existingClass;
        }

        AtomicBoolean newlyCreated = new AtomicBoolean(false);

        JavaType mappedClazz = (JavaType) typeBySignature.computeIfAbsent(clazz.getName(), ignored -> {
            if (clazz.isArray()) {
                return new JavaType.Array(classType(clazz.getComponentType()));
            } else if (clazz.isPrimitive()) {
                return JavaType.Primitive.fromKeyword(clazz.getName());
            }

            JavaType.Class.Kind kind;
            if ((clazz.getModifiers() & KIND_BITMASK_ENUM) != 0) {
                kind = JavaType.Class.Kind.Enum;
            } else if ((clazz.getModifiers() & KIND_BITMASK_ANNOTATION) != 0) {
                kind = JavaType.Class.Kind.Annotation;
            } else if ((clazz.getModifiers() & KIND_BITMASK_INTERFACE) != 0) {
                kind = JavaType.Class.Kind.Interface;
            } else {
                kind = JavaType.Class.Kind.Class;
            }

            return new JavaType.Class(
                    null,
                    clazz.getModifiers(),
                    clazz.getName(),
                    kind,
                    null, null, null, null, null, null
            );
        });

        if (mappedClazz instanceof JavaType.Class && newlyCreated.get()) {
            classStack.put(clazz, (JavaType.Class) mappedClazz);

            JavaType.FullyQualified supertype = (JavaType.FullyQualified) classType(clazz.getSuperclass());
            JavaType.FullyQualified owner = (JavaType.FullyQualified) classType(clazz.getDeclaringClass());

            List<JavaType.FullyQualified> annotations = null;
            if (clazz.getDeclaredAnnotations().length > 0) {
                annotations = new ArrayList<>(clazz.getDeclaredAnnotations().length);
                for (Annotation a : clazz.getDeclaredAnnotations()) {
                    JavaType.FullyQualified type = (JavaType.FullyQualified) classType(a.annotationType());
                    annotations.add(type);
                }
            }

            List<JavaType.FullyQualified> interfaces = null;
            if (clazz.getInterfaces().length > 0) {
                interfaces = new ArrayList<>(clazz.getInterfaces().length);
                for (Class<?> i : clazz.getInterfaces()) {
                    JavaType.FullyQualified type = (JavaType.FullyQualified) classType(i);
                    interfaces.add(type);
                }
            }

            List<JavaType.Variable> members = null;
            if (clazz.getDeclaredFields().length > 0) {
                members = new ArrayList<>(clazz.getDeclaredFields().length);
                for (Field f : clazz.getDeclaredFields()) {
                    if (!clazz.getName().equals("java.lang.String") || !f.getName().equals("serialPersistentFields")) {
                        JavaType.Variable field = _field(f);
                        members.add(field);
                    }
                }
            }

            List<JavaType.Method> methods = null;
            if (clazz.getDeclaredMethods().length > 0) {
                methods = new ArrayList<>(clazz.getDeclaredMethods().length);
                for (Method method : clazz.getDeclaredMethods()) {
                    JavaType.Method javaType = _method(method);
                    methods.add(javaType);
                }
            }

            ((JavaType.Class) mappedClazz).unsafeSet(supertype, owner, annotations, interfaces, members, methods);

            classStack.remove(clazz);
        }

        if (mappedClazz instanceof JavaType.Class && clazz.getTypeParameters().length > 0) {
            newlyCreated.set(false);

            JavaType.Parameterized parameterized = (JavaType.Parameterized) typeBySignature.computeIfAbsent(signatureBuilder.signature(clazz), ignored -> {
                newlyCreated.set(true);
                //noinspection ConstantConditions
                return new JavaType.Parameterized(null, null, null);
            });

            if (newlyCreated.get()) {
                List<JavaType> typeParameters = new ArrayList<>(clazz.getTypeParameters().length);
                for (TypeVariable<? extends Class<?>> typeParameter : clazz.getTypeParameters()) {
                    typeParameters.add(typeParameter(typeParameter));
                }
                parameterized.unsafeSet((JavaType.FullyQualified) mappedClazz, typeParameters);
            }

            return parameterized;
        }

        return mappedClazz;
    }

    private JavaType typeParameter(TypeVariable<?> typeParameter) {
        return (JavaType) typeBySignature.computeIfAbsent(signatureBuilder.signature(typeParameter), ignored -> {
            List<JavaType> bounds = null;
            for (Type bound : typeParameter.getBounds()) {
                if(bound instanceof JavaType.Class && ((JavaType.Class) bound).getFullyQualifiedName().equals("java.lang.Object")) {
                    continue;
                }
                if(bounds == null) {
                    bounds = new ArrayList<>(typeParameter.getBounds().length);
                }
                bounds.add(_type(bound));
            }

            // FIXME how to determine contravariance?
            return new JavaType.GenericTypeVariable(null, typeParameter.getName(),
                    bounds == null ? INVARIANT : COVARIANT, bounds);
        });
    }

    private JavaType.Variable field(Field field) {
        classStack.clear();
        return _field(field);
    }

    private JavaType.Variable _field(Field field) {
        return (JavaType.Variable) typeBySignature.computeIfAbsent(signatureBuilder.variableSignature(field), ignored -> {
            List<JavaType.FullyQualified> annotations = null;
            if (field.getDeclaredAnnotations().length > 0) {
                annotations = new ArrayList<>(field.getDeclaredAnnotations().length);
                for (Annotation a : field.getDeclaredAnnotations()) {
                    JavaType.FullyQualified type = (JavaType.FullyQualified) classType(a.annotationType());
                    annotations.add(type);
                }
            }

            return new JavaType.Variable(
                    field.getModifiers(),
                    field.getName(),
                    (JavaType.FullyQualified) classType(field.getDeclaringClass()),
                    classType(field.getType()),
                    annotations
            );
        });
    }

    public JavaType.Method method(Method method) {
        classStack.clear();
        return _method(method);
    }

    private JavaType.Method _method(Method method) {
        return (JavaType.Method) typeBySignature.computeIfAbsent(signatureBuilder.methodSignature(method), ignored -> {
            List<String> paramNames = null;
            if (method.getParameters().length > 0) {
                paramNames = new ArrayList<>(method.getParameters().length);
                for (Parameter p : method.getParameters()) {
                    paramNames.add(p.getName());
                }
            }

            List<JavaType.FullyQualified> thrownExceptions = null;
            if (method.getExceptionTypes().length > 0) {
                thrownExceptions = new ArrayList<>(method.getExceptionTypes().length);
                for (Class<?> e : method.getExceptionTypes()) {
                    JavaType.FullyQualified fullyQualified = (JavaType.FullyQualified) classType(e);
                    thrownExceptions.add(fullyQualified);
                }
            }

            List<JavaType.FullyQualified> annotations = new ArrayList<>();
            if (method.getDeclaredAnnotations().length > 0) {
                annotations = new ArrayList<>(method.getDeclaredAnnotations().length);
                for (Annotation a : method.getDeclaredAnnotations()) {
                    JavaType.FullyQualified fullyQualified = (JavaType.FullyQualified) classType(a.annotationType());
                    annotations.add(fullyQualified);
                }
            }

            List<JavaType> resolvedArgumentTypes = emptyList();
            if (method.getParameters().length > 0) {
                resolvedArgumentTypes = new ArrayList<>(method.getParameters().length);
                for (Parameter parameter : method.getParameters()) {
                    resolvedArgumentTypes.add(classType(method.getDeclaringClass()));
                }
            }

            JavaType.Method.Signature signature = new JavaType.Method.Signature(
                    classType(method.getReturnType()),
                    resolvedArgumentTypes
            );

            return new JavaType.Method(
                    method.getModifiers(),
                    (JavaType.FullyQualified) classType(method.getDeclaringClass()),
                    method.getName(),
                    paramNames,
                    signature,
                    signature,
                    thrownExceptions,
                    annotations
            );
        });
    }
}
