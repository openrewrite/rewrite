/*
 * Copyright 2026 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.JavaTypeSignatureBuilder;
import org.openrewrite.java.tree.JavaType;

import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import static java.util.Collections.newSetFromMap;

/**
 * A {@link JavaTypeSignatureBuilder} that produces cache keys using JVMS §4
 * descriptors for methods and variables, ensuring direct compatibility with
 * type tables built from class files (where ASM provides the descriptors).
 * <p>
 * Type signatures (class, parameterized, generic, array, primitive) use the
 * same format as {@link DefaultJavaTypeSignatureBuilder} since these are
 * used as FQN-based cache keys (the FQN passed to
 * {@link JavaTypeFactory#computeClass}).
 * <p>
 * Method signatures: {@code declaringFQN.methodName:(paramDescriptors)returnDescriptor}
 * <br>Example: {@code java.lang.String.substring:(II)Ljava/lang/String;}
 * <p>
 * Variable signatures: {@code ownerFQN.fieldName:fieldDescriptor}
 * <br>Example: {@code java.lang.String.CASE_INSENSITIVE_ORDER:Ljava/util/Comparator;}
 */
public class JvmsTypeSignatureBuilder implements JavaTypeSignatureBuilder {

    @Nullable
    private Set<String> typeVariableNameStack;

    @Nullable
    private Set<JavaType> parameterizedStack;

    @Override
    public String signature(@Nullable Object type) {
        if (type == null || type instanceof JavaType.Unknown) {
            return "{undefined}";
        }
        if (type instanceof JavaType.Class) {
            return classSignature(type);
        } else if (type instanceof JavaType.Array) {
            return arraySignature(type);
        } else if (type instanceof JavaType.Parameterized) {
            return parameterizedSignature(type);
        } else if (type instanceof JavaType.GenericTypeVariable) {
            return genericSignature(type);
        } else if (type instanceof JavaType.Primitive) {
            return primitiveSignature(type);
        } else if (type instanceof JavaType.Method) {
            return methodSignature((JavaType.Method) type);
        } else if (type instanceof JavaType.Variable) {
            return variableSignature((JavaType.Variable) type);
        } else if (type instanceof JavaType.Intersection) {
            return intersectionSignature(type);
        } else if (type instanceof JavaType.Annotation) {
            return annotationSignature(type);
        } else if (type instanceof JavaType.MultiCatch) {
            return multiCatchSignature(type);
        }
        throw new UnsupportedOperationException("Unexpected type " + type.getClass().getName());
    }

    // ------------------------------------------------------------------
    //  Type signatures — same format as DefaultJavaTypeSignatureBuilder
    // ------------------------------------------------------------------

    @Override
    public String arraySignature(Object type) {
        return signature(((JavaType.Array) type).getElemType()) + "[]";
    }

    @Override
    public String classSignature(Object type) {
        return ((JavaType.Class) type).getFullyQualifiedName();
    }

    @Override
    public String genericSignature(Object type) {
        JavaType.GenericTypeVariable gtv = (JavaType.GenericTypeVariable) type;
        StringBuilder s = new StringBuilder("Generic{" + gtv.getName());

        if (typeVariableNameStack == null) {
            typeVariableNameStack = new LinkedHashSet<>();
        }
        if (!"?".equals(gtv.getName()) && !typeVariableNameStack.add(gtv.getName())) {
            s.append('}');
            return s.toString();
        }

        switch (gtv.getVariance()) {
            case INVARIANT:
                break;
            case COVARIANT:
                s.append(" extends ");
                break;
            case CONTRAVARIANT:
                s.append(" super ");
                break;
        }

        StringJoiner bounds = new StringJoiner(" & ");
        for (JavaType bound : gtv.getBounds()) {
            if (parameterizedStack == null || !parameterizedStack.contains(bound)) {
                bounds.add(signature(bound));
            }
        }

        s.append(bounds).append('}');
        typeVariableNameStack.remove(gtv.getName());

        return s.toString();
    }

    @Override
    public String parameterizedSignature(Object type) {
        JavaType.Parameterized pt = (JavaType.Parameterized) type;

        if (parameterizedStack == null) {
            parameterizedStack = newSetFromMap(new IdentityHashMap<>());
        }
        if (!parameterizedStack.add(pt)) {
            return classSignature(pt.getType());
        }

        try {
            String baseType = signature(pt.getType());
            StringBuilder s = new StringBuilder(baseType);

            StringJoiner typeParameters = new StringJoiner(", ", "<", ">");
            for (JavaType typeParameter : pt.getTypeParameters()) {
                typeParameters.add(signature(typeParameter));
            }
            s.append(typeParameters);

            return s.toString();
        } finally {
            parameterizedStack.remove(pt);
        }
    }

    @Override
    public String primitiveSignature(Object type) {
        return ((JavaType.Primitive) type).getKeyword();
    }

    private String intersectionSignature(Object type) {
        JavaType.Intersection it = (JavaType.Intersection) type;
        StringJoiner bounds = new StringJoiner(" & ");
        for (JavaType bound : it.getBounds()) {
            if (parameterizedStack == null || !parameterizedStack.contains(bound)) {
                bounds.add(signature(bound));
            }
        }
        return bounds.toString();
    }

    private String annotationSignature(Object type) {
        JavaType.Annotation annotation = (JavaType.Annotation) type;
        StringBuilder s = new StringBuilder("@");
        s.append(signature(annotation.getType()));
        List<JavaType.Annotation.ElementValue> values = annotation.getValues();
        if (!values.isEmpty()) {
            s.append('(');
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) {
                    s.append(',');
                }
                JavaType.Annotation.ElementValue value = values.get(i);
                s.append(signature(value.getElement())).append('=').append(value.getValue());
            }
            s.append(')');
        }
        return s.toString();
    }

    private String multiCatchSignature(Object type) {
        JavaType.MultiCatch multiCatch = (JavaType.MultiCatch) type;
        StringBuilder s = new StringBuilder();
        List<JavaType> throwableTypes = multiCatch.getThrowableTypes();
        for (int i = 0; i < throwableTypes.size(); i++) {
            JavaType throwableType = throwableTypes.get(i);
            if (parameterizedStack != null && parameterizedStack.contains(throwableType)) {
                continue;
            }
            if (i > 0 && s.length() > 0) {
                s.append('|');
            }
            s.append(signature(throwableType));
        }
        return s.toString();
    }

    // ------------------------------------------------------------------
    //  Method and variable signatures — JVMS §4.3.3 descriptor format
    // ------------------------------------------------------------------

    public String methodSignature(JavaType.Method method) {
        String declaringFqn = method.getDeclaringType() != null
                ? method.getDeclaringType().getFullyQualifiedName()
                : "{undefined}";
        return declaringFqn + "." + method.getName() + ":" + methodDescriptor(method);
    }

    public String variableSignature(JavaType.Variable variable) {
        String ownerFqn;
        JavaType owner = variable.getOwner();
        if (owner instanceof JavaType.FullyQualified) {
            ownerFqn = ((JavaType.FullyQualified) owner).getFullyQualifiedName();
        } else if (owner instanceof JavaType.Method) {
            ownerFqn = methodSignature((JavaType.Method) owner);
        } else {
            ownerFqn = "{undefined}";
        }
        return ownerFqn + "." + variable.getName() + ":" + fieldDescriptor(variable.getType());
    }

    // ------------------------------------------------------------------
    //  JVMS §4.3 descriptor computation from JavaType
    // ------------------------------------------------------------------

    public static String methodDescriptor(JavaType.Method method) {
        StringBuilder sb = new StringBuilder("(");
        List<JavaType> paramTypes = method.getParameterTypes();
        if (paramTypes != null) {
            for (JavaType paramType : paramTypes) {
                sb.append(fieldDescriptor(paramType));
            }
        }
        sb.append(')');
        sb.append(fieldDescriptor(method.getReturnType()));
        return sb.toString();
    }

    public static String fieldDescriptor(@Nullable JavaType type) {
        if (type instanceof JavaType.Primitive) {
            JavaType.Primitive p = (JavaType.Primitive) type;
            switch (p) {
                case Boolean: return "Z";
                case Byte:    return "B";
                case Char:    return "C";
                case Double:  return "D";
                case Float:   return "F";
                case Int:     return "I";
                case Long:    return "J";
                case Short:   return "S";
                case Void:
                case None:    return "V";
                case String:
                case Null:    return "Ljava/lang/String;";
                default:      return "Ljava/lang/Object;";
            }
        } else if (type instanceof JavaType.Parameterized) {
            return "L" + ((JavaType.Parameterized) type).getType().getFullyQualifiedName().replace('.', '/') + ";";
        } else if (type instanceof JavaType.Class) {
            return "L" + ((JavaType.Class) type).getFullyQualifiedName().replace('.', '/') + ";";
        } else if (type instanceof JavaType.Array) {
            return "[" + fieldDescriptor(((JavaType.Array) type).getElemType());
        } else if (type instanceof JavaType.GenericTypeVariable) {
            List<JavaType> bounds = ((JavaType.GenericTypeVariable) type).getBounds();
            if (bounds != null && !bounds.isEmpty()) {
                JavaType firstBound = bounds.get(0);
                if (firstBound instanceof JavaType.FullyQualified) {
                    return fieldDescriptor(firstBound);
                }
            }
            return "Ljava/lang/Object;";
        }
        return "Ljava/lang/Object;";
    }
}
