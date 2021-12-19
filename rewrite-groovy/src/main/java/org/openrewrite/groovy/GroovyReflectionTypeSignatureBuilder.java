package org.openrewrite.groovy;

import org.codehaus.groovy.ast.MethodNode;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.JavaTypeSignatureBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.StringJoiner;

public class GroovyReflectionTypeSignatureBuilder implements JavaTypeSignatureBuilder {

    @Override
    public String signature(@Nullable Object t) {
        if(t == null) {
            return "{undefined}";
        }

        Class<?> clazz = (Class<?>) t;
        if (clazz.isArray()) {
            return arraySignature(clazz);
        } else if (clazz.isPrimitive()) {
            return primitiveSignature(clazz);
        }
        return classSignature(clazz);
    }

    @Override
    public String arraySignature(Object type) {
        Class<?> array = (Class<?>) type;
        return signature(array.getComponentType()) + "[]";
    }

    @Override
    public String classSignature(Object type) {
        Class<?> clazz = (Class<?>) type;
        return clazz.getName();
    }

    @Override
    public String genericSignature(Object type) {
        // FIXME implement me
        throw new UnsupportedOperationException("Implement me.");
    }

    @Override
    public String parameterizedSignature(Object type) {
        return null;
    }

    @Override
    public String primitiveSignature(Object type) {
        return ((Class<?>) type).getName();
    }

    public String methodSignature(Method method) {
        // Formatted like com.MyThing{name=add,resolved=Thing(Integer),generic=Thing<?>(Integer)}
        StringBuilder s = new StringBuilder(method.getDeclaringClass().getName());
        s.append("{name=").append(method.getName());

        StringJoiner argumentTypeSignatures = new StringJoiner(",");
        if (method.getParameters().length > 0) {
            for (Parameter parameter : method.getParameters()) {
                argumentTypeSignatures.add(method.getDeclaringClass().getName());
            }
        }
        s.append(",resolved=").append(method.getReturnType().getName()).append('(').append(argumentTypeSignatures).append(')');

        // TODO how do we calculate the generic signature?
        s.append(",generic=").append(method.getReturnType().getName()).append('(').append(argumentTypeSignatures).append(')');
        s.append('}');

        return s.toString();
    }

    public String variableSignature(Field field) {
        // Formatted like com.MyThing{name=MY_FIELD,type=java.lang.String}
        return field.getDeclaringClass().getName() + "{name=" + field.getName() + '}';
    }
}
