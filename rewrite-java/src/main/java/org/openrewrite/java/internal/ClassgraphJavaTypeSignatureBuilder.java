package org.openrewrite.java.internal;

import io.github.classgraph.*;
import org.openrewrite.internal.lang.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.StringJoiner;

public class ClassgraphJavaTypeSignatureBuilder implements JavaTypeSignatureBuilder {
    @Nullable
    private Set<TypeSignature> typeStack;

    @Override
    public String signature(@Nullable Object type) {
        if (type == null) {
            return "{undefined}";
        }

        HierarchicalTypeSignature typeSignature = (HierarchicalTypeSignature) type;

        if (typeSignature instanceof ClassRefTypeSignature) {
            ClassRefTypeSignature classRef = (ClassRefTypeSignature) typeSignature;
            if (!classRef.getTypeArguments().isEmpty()) {
                return parameterizedSignature(typeSignature);
            }
            return classSignature(typeSignature);
        } else if (typeSignature instanceof ClassTypeSignature) {
            return parameterizedSignature(typeSignature);
        } else if (typeSignature instanceof ArrayTypeSignature) {
            return arraySignature(typeSignature);
        } else if (typeSignature instanceof TypeVariableSignature) {
            TypeVariableSignature typeVariableSignature = (TypeVariableSignature) typeSignature;
            try {
                // resolves to a TypeParameter
                return signature(typeVariableSignature.resolve());
            } catch (IllegalArgumentException ignored) {
                return typeVariableSignature.getName();
            }
        } else if (typeSignature instanceof TypeParameter) {
            return genericSignature(typeSignature);
        } else if (typeSignature instanceof BaseTypeSignature) {
            return primitiveSignature(typeSignature);
        } else if (typeSignature instanceof TypeArgument) {
            TypeArgument typeArgument = (TypeArgument) typeSignature;
            if (typeArgument.getWildcard().equals(TypeArgument.Wildcard.ANY)) {
                return "?";
            } else {
                return signature(typeArgument.getTypeSignature());
            }
        }

        throw new UnsupportedOperationException("Unexpected signature type " + type.getClass().getName());
    }

    @Override
    public String arraySignature(Object type) {
        ArrayTypeSignature arrSignature = (ArrayTypeSignature) type;
        StringBuilder signature = new StringBuilder(signature(arrSignature.getElementTypeSignature()));
        for (int i = 0; i < arrSignature.getNumDimensions(); i++) {
            signature.append("[]");
        }
        return signature.toString();
    }

    @Override
    public String classSignature(Object type) {
        ClassRefTypeSignature classRefTypeSignature = (ClassRefTypeSignature) type;
        return classRefTypeSignature.getBaseClassName();
    }

    @Override
    public String genericSignature(Object type) {
        TypeParameter generic = (TypeParameter) type;

        StringBuilder s = new StringBuilder(generic.getName());

        // TODO how to determine variance?
        s.append(" extends ");

        if (generic.getClassBound() != null) {
            s.append(genericBound(generic.getClassBound()));
        } else if (generic.getInterfaceBounds() != null) {
            StringJoiner interfaceBounds = new StringJoiner(" & ");
            for (ReferenceTypeSignature interfaceBound : generic.getInterfaceBounds()) {
                interfaceBounds.add(genericBound(interfaceBound));
            }
            s.append(interfaceBounds);
        }

        return s.toString();
    }

    private String genericBound(TypeSignature bound) {
        if (typeStack != null && typeStack.contains(bound)) {
            return "(*)";
        }

        if (typeStack == null) {
            typeStack = Collections.newSetFromMap(new IdentityHashMap<>());
        }
        typeStack.add(bound);
        return signature(bound);
    }

    @Override
    public String parameterizedSignature(Object type) {
        StringBuilder s = new StringBuilder();
        StringJoiner typeParameters = new StringJoiner(",", "<", ">");

        if (type instanceof ClassTypeSignature) {
            ClassTypeSignature clazz = (ClassTypeSignature) type;
            try {
                Method getClassName = type.getClass().getDeclaredMethod("getClassName");
                getClassName.setAccessible(true);
                s.append(getClassName.invoke(clazz));
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }

            for (TypeParameter typeParameter : clazz.getTypeParameters()) {
                typeParameters.add(signature(typeParameter));
            }
        } else if (type instanceof ClassRefTypeSignature) {
            ClassRefTypeSignature clazz = (ClassRefTypeSignature) type;
            s.append(clazz.getBaseClassName());
            for (TypeArgument typeArgument : clazz.getTypeArguments()) {
                typeParameters.add(signature(typeArgument));
            }
        } else {
            throw new UnsupportedOperationException("Unexpected parameterized type " + type.getClass().getName());
        }

        s.append(typeParameters);

        return s.toString();
    }

    @Override
    public String primitiveSignature(Object type) {
        BaseTypeSignature baseTypeSignature = (BaseTypeSignature) type;
        return baseTypeSignature.getTypeStr();
    }

    public String methodSignature(MethodInfo methodInfo) {
        StringBuilder s = new StringBuilder(signature(methodInfo.getClassInfo().getTypeSignature()));
        s.append("{name=").append(methodInfo.getName());

        StringJoiner resolvedArgumentTypes = new StringJoiner(",");
        for (MethodParameterInfo methodParameterInfo : methodInfo.getParameterInfo()) {
            resolvedArgumentTypes.add(signature(methodParameterInfo.getTypeDescriptor()));
        }
        s.append(",resolved=");
        s.append(signature(methodInfo.getTypeDescriptor().getResultType()));
        s.append('(').append(resolvedArgumentTypes).append(')');

        StringJoiner genericArgumentTypes = new StringJoiner(",");
        for (MethodParameterInfo methodParameterInfo : methodInfo.getParameterInfo()) {
            genericArgumentTypes.add(methodParameterInfo.getTypeSignature() == null ?
                    signature(methodParameterInfo.getTypeDescriptor()) :
                    signature(methodParameterInfo.getTypeSignature()));
        }
        s.append(",generic=");
        s.append(methodInfo.getTypeSignature() == null ?
                signature(methodInfo.getTypeDescriptor().getResultType()) :
                signature(methodInfo.getTypeSignature().getResultType()));
        s.append('(').append(genericArgumentTypes).append(')');

        s.append('}');

        return s.toString();
    }

    public String variableSignature(FieldInfo fieldInfo) {
        // Formatted like com.MyThing{name=MY_FIELD}
        return fieldInfo.getClassName() + "{name=" + fieldInfo.getName() + '}';
    }
}
