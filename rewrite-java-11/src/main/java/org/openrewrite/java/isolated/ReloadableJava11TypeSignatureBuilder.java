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
package org.openrewrite.java.isolated;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.JavaTypeSignatureBuilder;
import org.openrewrite.java.tree.JavaType;

import javax.lang.model.type.NullType;
import javax.lang.model.type.TypeMirror;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

class ReloadableJava11TypeSignatureBuilder implements JavaTypeSignatureBuilder {
    @Nullable
    private Set<String> typeVariableNameStack;

    @Override
    public String signature(@Nullable Object t) {
        return signature((Type) t);
    }

    private String signature(@Nullable Type type) {
        if (type == null || type instanceof Type.UnknownType || type instanceof NullType) {
            return "{undefined}";
        } else if (type instanceof Type.IntersectionClassType) {
            return intersectionSignature(type);
        } else if (type instanceof Type.ClassType) {
            try {
                return ((Type.ClassType) type).typarams_field != null && ((Type.ClassType) type).typarams_field.length() > 0 ? parameterizedSignature(type) : classSignature(type);
            } catch (Symbol.CompletionFailure ignored) {
                return ((Type.ClassType) type).typarams_field != null && ((Type.ClassType) type).typarams_field.length() > 0 ? parameterizedSignature(type) : classSignature(type);
            }
        } else if (type instanceof Type.CapturedType) { // CapturedType must be evaluated before TypeVar
            return genericSignature(type);
        } else if (type instanceof Type.TypeVar) {
            return genericSignature(type);
        } else if (type instanceof Type.JCPrimitiveType) {
            return primitiveSignature(type);
        } else if (type instanceof Type.JCVoidType) {
            return "void";
        } else if (type instanceof Type.ArrayType) {
            return arraySignature(type);
        } else if (type instanceof Type.WildcardType) {
            Type.WildcardType wildcard = (Type.WildcardType) type;
            StringBuilder s = new StringBuilder("Generic{" + wildcard.kind.toString());
            if (!type.isUnbound()) {
                s.append(signature(wildcard.type));
            }
            return s.append("}").toString();
        } else if (type instanceof Type.JCNoType) {
            return "{none}";
        }

        throw new IllegalStateException("Unexpected type " + type.getClass().getName());
    }

    private void completeClassSymbol(Symbol.ClassSymbol classSymbol) {
        try {
            classSymbol.complete();
        } catch (Symbol.CompletionFailure ignore) {
        }
    }

    @Override
    public String arraySignature(Object type) {
        Object elemType = type;
        StringBuilder dimensions = new StringBuilder();
        while (elemType instanceof Type.ArrayType) {
            dimensions.append("[]");
            elemType = ((Type.ArrayType) elemType).elemtype;
        }
        return signature(elemType) + dimensions;
    }

    public String annotatedArraySignature(JCTree.JCAnnotatedType annotatedType) {
        JCTree tree = annotatedType;
        StringBuilder dimensions = new StringBuilder();
        while (tree instanceof JCTree.JCAnnotatedType || tree instanceof JCTree.JCArrayTypeTree) {
            if (tree instanceof JCTree.JCAnnotatedType) {
                dimensions.append(mapAnnotations(((JCTree.JCAnnotatedType) tree).annotations));
                tree = ((JCTree.JCAnnotatedType) tree).getUnderlyingType();
            }
            if (tree instanceof JCTree.JCArrayTypeTree) {
                dimensions.append("[]");
                tree = ((JCTree.JCArrayTypeTree) tree).getType();
            }
        }
        return signature(tree.type) + dimensions;
    }

    private String mapAnnotations(List<JCTree.JCAnnotation> annotations) {
        if (annotations.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (JCTree.JCAnnotation annotation : annotations) {
            joiner.add(signature(annotation.type));
        }
        return joiner.toString();
    }

    @Override
    public String classSignature(Object type) {
        if (type instanceof Type.JCVoidType) {
            return "void";
        } else if (type instanceof Type.JCPrimitiveType) {
            return primitiveSignature(type);
        } else if (type instanceof Type.JCNoType) {
            return "{undefined}";
        }

        Symbol.ClassSymbol sym = (Symbol.ClassSymbol) ((Type.ClassType) type).tsym;
        if (!sym.completer.isTerminal()) {
            completeClassSymbol(sym);
        }
        return sym.flatName().toString();
    }

    @Override
    public String genericSignature(Object type) {
        Type.TypeVar generic = (Type.TypeVar) type;
        String name = generic instanceof Type.CapturedType ? "?" : generic.tsym.name.toString();

        if (typeVariableNameStack == null) {
            typeVariableNameStack = new HashSet<>();
        }

        if (!typeVariableNameStack.add(name)) {
            return "Generic{" + name + "}";
        }

        StringBuilder s = new StringBuilder("Generic{").append(name);

        StringJoiner boundSigs = new StringJoiner(" & ");
        if (generic.getUpperBound() instanceof Type.IntersectionClassType) {
            Type.IntersectionClassType intersectionBound = (Type.IntersectionClassType) generic.getUpperBound();
            if (intersectionBound.supertype_field != null) {
                String bound = signature(intersectionBound.supertype_field);
                if (!"java.lang.Object".equals(bound)) {
                    boundSigs.add(bound);
                }
            }
            for (Type bound : intersectionBound.interfaces_field) {
                boundSigs.add(signature(bound));
            }
        } else {
            String bound = signature(generic.getUpperBound());
            if (!"java.lang.Object".equals(bound)) {
                boundSigs.add(bound);
            }
        }

        String boundSigStr = boundSigs.toString();
        if (!boundSigStr.isEmpty()) {
            s.append(" extends ").append(boundSigStr);
        }

        typeVariableNameStack.remove(name);

        return s.append("}").toString();
    }

    private String intersectionSignature(Object type) {
        Type.IntersectionClassType intersectionClassType = (Type.IntersectionClassType) type;
        StringJoiner joiner = new StringJoiner(" & ");
        for (TypeMirror typeArg : intersectionClassType.getBounds()) {
            joiner.add(signature(typeArg));
        }
        return joiner.toString();
    }

    @Override
    public String parameterizedSignature(Object type) {
        StringBuilder s = new StringBuilder(classSignature(type));
        StringJoiner joiner = new StringJoiner(", ", "<", ">");
        for (Type tp : ((Type.ClassType) type).typarams_field) {
            String signature = signature(tp);
            joiner.add(signature);
        }
        s.append(joiner);
        return s.toString();
    }

    @Override
    public String primitiveSignature(Object type) {
        TypeTag tag = ((Type.JCPrimitiveType) type).getTag();
        switch (tag) {
            case BOOLEAN:
                return JavaType.Primitive.Boolean.getKeyword();
            case BYTE:
                return JavaType.Primitive.Byte.getKeyword();
            case CHAR:
                return JavaType.Primitive.Char.getKeyword();
            case DOUBLE:
                return JavaType.Primitive.Double.getKeyword();
            case FLOAT:
                return JavaType.Primitive.Float.getKeyword();
            case INT:
                return JavaType.Primitive.Int.getKeyword();
            case LONG:
                return JavaType.Primitive.Long.getKeyword();
            case SHORT:
                return JavaType.Primitive.Short.getKeyword();
            case VOID:
                return JavaType.Primitive.Void.getKeyword();
            case NONE:
                return JavaType.Primitive.None.getKeyword();
            case CLASS:
                return JavaType.Primitive.String.getKeyword();
            case BOT:
                return JavaType.Primitive.Null.getKeyword();
            default:
                throw new IllegalArgumentException("Unknown type tag " + tag);
        }
    }

    public String methodSignature(Type selectType, Symbol.MethodSymbol symbol) {
        String s = classSignature(symbol.owner.type);
        if (symbol.isConstructor()) {
            s += "{name=<constructor>,return=" + s;
        } else {
            s += "{name=" + symbol.getSimpleName().toString() +
                 ",return=" + signature(selectType.getReturnType());
        }

        return s + ",parameters=" + methodArgumentSignature(selectType) + '}';
    }

    public String methodSignature(Symbol.MethodSymbol symbol) {
        String s = classSignature(symbol.owner.type);

        String returnType;
        if (symbol.isStaticOrInstanceInit()) {
            returnType = "void";
        } else {
            returnType = signature(symbol.getReturnType());
        }

        if (symbol.isConstructor()) {
            s += "{name=<constructor>,return=" + s;
        } else {
            s += "{name=" + symbol.getSimpleName().toString() +
                 ",return=" + returnType;
        }

        return s + ",parameters=" + methodArgumentSignature(symbol) + '}';
    }

    private String methodArgumentSignature(Symbol.MethodSymbol sym) {
        if (sym.isStaticOrInstanceInit()) {
            return "[]";
        }

        StringJoiner genericArgumentTypes = new StringJoiner(",", "[", "]");
        if (sym.type == null) {
            genericArgumentTypes.add("{undefined}");
        } else {
            for (Symbol.VarSymbol parameter : sym.getParameters()) {
                genericArgumentTypes.add(signature(parameter.type));
            }
        }
        return genericArgumentTypes.toString();
    }

    private String methodArgumentSignature(Type selectType) {
        if (selectType instanceof Type.MethodType) {
            StringJoiner resolvedArgumentTypes = new StringJoiner(",", "[", "]");
            Type.MethodType mt = (Type.MethodType) selectType;
            if (!mt.argtypes.isEmpty()) {
                for (Type argtype : mt.argtypes) {
                    if (argtype != null) {
                        resolvedArgumentTypes.add(signature(argtype));
                    }
                }
            }
            return resolvedArgumentTypes.toString();
        } else if (selectType instanceof Type.ForAll) {
            return methodArgumentSignature(((Type.ForAll) selectType).qtype);
        } else if (selectType instanceof Type.JCNoType || selectType instanceof Type.UnknownType) {
            return "{undefined}";
        }

        throw new UnsupportedOperationException("Unexpected method type " + selectType.getClass().getName());
    }

    public String variableSignature(Symbol symbol) {
        String owner;
        if (symbol.owner instanceof Symbol.MethodSymbol) {
            owner = methodSignature((Symbol.MethodSymbol) symbol.owner);
        } else {
            owner = signature(symbol.owner.type);
            if (owner.contains("<")) {
                owner = owner.substring(0, owner.indexOf('<'));
            }
        }

        return owner + "{name=" + symbol.name.toString() + ",type=" + signature(symbol.type) + '}';
    }
}
