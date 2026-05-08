/*
 * Copyright 2022 the original author or authors.
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

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.comp.AttrRecover;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Pair;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.JavaTypeMapping;
import org.openrewrite.java.internal.JavaTypeFactory;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import javax.lang.model.type.NullType;
import javax.lang.model.type.TypeMirror;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.*;

@RequiredArgsConstructor
class ReloadableJava17TypeMapping implements JavaTypeMapping<Tree> {

    private final ReloadableJava17TypeSignatureBuilder signatureBuilder = new ReloadableJava17TypeSignatureBuilder();

    private final JavaTypeFactory typeFactory;

    public JavaType type(@Nullable Type type) {
        if (type == null || type instanceof Type.ErrorType || type instanceof Type.PackageType || type instanceof Type.UnknownType ||
                type instanceof NullType) {
            return JavaType.Class.Unknown.getInstance();
        }

        String signature = signatureBuilder.signature(type);
        JavaType existing = typeFactory.get(signature);
        if (existing != null) {
            return existing;
        }

        if (type instanceof Type.IntersectionClassType) {
            return intersectionType((Type.IntersectionClassType) type, signature);
        } else if (type instanceof Type.ClassType) {
            return classType((Type.ClassType) type, signature);
        } else if (type instanceof Type.TypeVar) {
            return generic((Type.TypeVar) type, signature);
        } else if (type instanceof Type.JCPrimitiveType) {
            return primitive(type.getTag());
        } else if (type instanceof Type.JCVoidType) {
            return JavaType.Primitive.Void;
        } else if (type instanceof Type.ArrayType) {
            return array(type, signature);
        } else if (type instanceof Type.WildcardType) {
            return generic((Type.WildcardType) type, signature);
        } else if (type instanceof Type.JCNoType) {
            return JavaType.Class.Unknown.getInstance();
        }

        throw new UnsupportedOperationException("Unknown type " + type.getClass().getName());
    }

    private JavaType intersectionType(Type.IntersectionClassType type, String signature) {
        return typeFactory.computeIntersection(signature, type, intersection -> {
            List<? extends TypeMirror> bounds = type.getBounds();
            List<JavaType> types = new ArrayList<>(bounds.size());
            for (TypeMirror bound : bounds) {
                types.add(type((Type) bound));
            }
            intersection.unsafeSet(types);
        });
    }

    private JavaType array(Type type, String signature) {
        return typeFactory.computeArray(signature, type, arr ->
                arr.unsafeSet(type(((Type.ArrayType) type).elemtype), null));
    }

    /**
     * Maps annotated array types to a JavaType through the JCTree instead of the Type tree.
     * <p>
     * The JCTree is necessary to preserve annotations on multidimensional arrays in Java 8.
     * In Java 11+, annotations are available directly on the `Type` tree through TypeMetadata, but
     * annotations are accessed differently in Java 11 and 17 compared to Java 21.
     * Annotated array types are mapped through the JCTree so that the mapping is consistent for each version of the
     * java compiler.
     */
    private JavaType annotatedArray(JCTree.JCAnnotatedType annotatedType) {
        String signature = signatureBuilder.annotatedArraySignature(annotatedType);
        Tree tree = annotatedType;
        List<Tree> trees = new ArrayList<>();
        while (tree instanceof JCTree.JCAnnotatedType || tree instanceof JCTree.JCArrayTypeTree) {
            if (tree instanceof JCTree.JCAnnotatedType) {
                if (((JCTree.JCAnnotatedType) tree).getUnderlyingType() instanceof JCTree.JCArrayTypeTree) {
                    trees.add(0, tree);
                    tree = ((JCTree.JCArrayTypeTree) ((JCTree.JCAnnotatedType) tree).getUnderlyingType()).getType();
                } else {
                    tree = ((JCTree.JCAnnotatedType) tree).getUnderlyingType();
                }
            } else {
                trees.add(0, tree);
                tree = ((JCTree.JCArrayTypeTree) tree).getType();
            }
        }
        final Tree leafTree = tree;
        final List<Tree> chain = trees;
        return typeFactory.computeArray(signature, annotatedType, arr -> {
            JavaType element = type(leafTree);
            for (int i = 0; i < chain.size() - 1; i++) {
                Tree t = chain.get(i);
                JavaType.@Nullable FullyQualified[] annos = t instanceof JCTree.JCAnnotatedType
                        ? mapAnnotations(((JCTree.JCAnnotatedType) t).annotations) : null;
                element = new JavaType.Array(null, element, annos);
            }
            JavaType.@Nullable FullyQualified[] outerAnnos = !chain.isEmpty() && chain.get(chain.size() - 1) instanceof JCTree.JCAnnotatedType
                    ? mapAnnotations(((JCTree.JCAnnotatedType) chain.get(chain.size() - 1)).annotations) : null;
            arr.unsafeSet(element, outerAnnos);
        });
    }

    private JavaType.@Nullable FullyQualified[] mapAnnotations(List<JCTree.JCAnnotation> annotations) {
        List<JavaType.FullyQualified> types = new ArrayList<>(annotations.size());
        for (JCTree.JCAnnotation annotation : annotations) {
            JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type(annotation));
            if (fq != null) {
                types.add(fq);
            }
        }
        return types.toArray(new JavaType.FullyQualified[0]);
    }

    private JavaType.GenericTypeVariable generic(Type.WildcardType wildcard, String signature) {
        return typeFactory.computeGenericTypeVariable(signature, "?", INVARIANT, wildcard, gtv -> {
            JavaType.GenericTypeVariable.Variance variance;
            List<JavaType> bounds;

            switch (wildcard.kind) {
                case SUPER:
                    variance = CONTRAVARIANT;
                    bounds = singletonList(type(wildcard.getSuperBound()));
                    break;
                case EXTENDS:
                    variance = COVARIANT;
                    bounds = singletonList(type(wildcard.getExtendsBound()));
                    break;
                case UNBOUND:
                default:
                    variance = INVARIANT;
                    bounds = null;
                    break;
            }

            if (bounds != null && bounds.get(0) instanceof JavaType.FullyQualified && "java.lang.Object".equals(((JavaType.FullyQualified) bounds.get(0))
                    .getFullyQualifiedName())) {
                bounds = null;
            }

            gtv.unsafeSet(gtv.getName(), variance, bounds);
        });
    }

    private JavaType generic(Type.TypeVar type, String signature) {
        String name;
        if (type instanceof Type.CapturedType && ((Type.CapturedType) type).wildcard.kind == BoundKind.UNBOUND) {
            name = "?";
        } else {
            name = type.tsym.name.toString();
        }
        return typeFactory.computeGenericTypeVariable(signature, name, INVARIANT, type, gtv -> {
            List<JavaType> bounds = null;
            if (type.getUpperBound() instanceof Type.IntersectionClassType) {
                Type.IntersectionClassType intersectionBound = (Type.IntersectionClassType) type.getUpperBound();
                boolean isIntersectionSuperType = !intersectionBound.supertype_field.tsym.getQualifiedName().toString().equals("java.lang.Object");
                bounds = new ArrayList<>((isIntersectionSuperType ? 1 : 0) + intersectionBound.interfaces_field.length());

                if (isIntersectionSuperType) {
                    bounds.add(type(intersectionBound.supertype_field));
                }
                for (Type bound : intersectionBound.interfaces_field) {
                    bounds.add(type(bound));
                }
            } else if (type.getUpperBound() != null) {
                JavaType mappedBound = type(type.getUpperBound());
                if (!(mappedBound instanceof JavaType.FullyQualified) || !"java.lang.Object".equals(((JavaType.FullyQualified) mappedBound).getFullyQualifiedName())) {
                    bounds = singletonList(mappedBound);
                }
            }

            gtv.unsafeSet(gtv.getName(), bounds == null ? INVARIANT : COVARIANT, bounds);
        });
    }

    private JavaType.FullyQualified classType(Type.ClassType classType, String signature) {
        Symbol.ClassSymbol sym = (Symbol.ClassSymbol) classType.tsym;
        Type.ClassType symType = (Type.ClassType) sym.type;
        String fqn = sym.flatName().toString();

        // The cache may hold a Parameterized under fqn if some other path stored it
        // there; defer to the underlying Class in that case rather than reconstructing.
        JavaType.FullyQualified fq = typeFactory.get(fqn);
        JavaType.Class clazz = fq instanceof JavaType.Parameterized
                ? (JavaType.Class) ((JavaType.Parameterized) fq).getType()
                : (JavaType.Class) fq;
        if (clazz == null) {
            if (!sym.completer.isTerminal()) {
                completeClassSymbol(sym);
            }
            clazz = typeFactory.computeClass(fqn, fqn, sym.flags_field, getKind(sym), sym, stub -> {
                JavaType.FullyQualified supertype = TypeUtils.asFullyQualified(type(symType.supertype_field));

                JavaType.FullyQualified owner = null;
                if (sym.owner instanceof Symbol.ClassSymbol) {
                    owner = TypeUtils.asFullyQualified(type(sym.owner.type));
                }

                List<JavaType.FullyQualified> interfaces = null;
                if (symType.interfaces_field != null) {
                    interfaces = new ArrayList<>(symType.interfaces_field.length());
                    for (Type iParam : symType.interfaces_field) {
                        JavaType.FullyQualified javaType = TypeUtils.asFullyQualified(type(iParam));
                        if (javaType != null) {
                            interfaces.add(javaType);
                        }
                    }
                }

                List<JavaType.Variable> fields = null;
                List<JavaType.Method> methods = null;

                if (sym.members_field != null) {
                    for (Symbol elem : sym.members_field.getSymbols()) {
                        if (elem instanceof Symbol.VarSymbol &&
                                (elem.flags_field & (Flags.SYNTHETIC | Flags.BRIDGE | Flags.HYPOTHETICAL |
                                        Flags.GENERATEDCONSTR | Flags.ANONCONSTR)) == 0) {
                            if ("java.lang.String".equals(fqn) && elem.name.toString().equals("serialPersistentFields")) {
                                continue;
                            }

                            if (fields == null) {
                                fields = new ArrayList<>();
                            }
                            fields.add(variableType(elem, stub));
                        } else if (elem instanceof Symbol.MethodSymbol &&
                                (elem.flags_field & (Flags.SYNTHETIC | Flags.BRIDGE | Flags.HYPOTHETICAL | Flags.ANONCONSTR)) == 0) {
                            if (methods == null) {
                                methods = new ArrayList<>();
                            }
                            Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) elem;
                            if (!methodSymbol.isStaticOrInstanceInit()) {
                                methods.add(methodDeclarationType(methodSymbol, stub));
                            }
                        }
                    }
                }

                List<JavaType> typeParameters = null;
                if (symType.typarams_field != null && symType.typarams_field.length() > 0) {
                    typeParameters = new ArrayList<>(symType.typarams_field.length());
                    for (Type tParam : symType.typarams_field) {
                        typeParameters.add(type(tParam));
                    }
                }
                stub.unsafeSet(typeParameters, supertype, owner, listAnnotations(sym), interfaces, fields, methods);
            });
        }

        if (classType.typarams_field != null && classType.typarams_field.length() > 0) {
            JavaType.Class finalClazz = clazz;
            return typeFactory.computeParameterized(signature, classType, pt -> {
                List<JavaType> typeParameters = new ArrayList<>(classType.typarams_field.length());
                for (Type tParam : classType.typarams_field) {
                    typeParameters.add(type(tParam));
                }
                pt.unsafeSet(finalClazz, typeParameters);
            });
        }
        return clazz;
    }

    private JavaType.FullyQualified.Kind getKind(Symbol.ClassSymbol sym) {
        switch (sym.getKind()) {
            case ENUM:
                return JavaType.FullyQualified.Kind.Enum;
            case ANNOTATION_TYPE:
                return JavaType.FullyQualified.Kind.Annotation;
            case INTERFACE:
                return JavaType.FullyQualified.Kind.Interface;
            case RECORD:
                return JavaType.FullyQualified.Kind.Record;
            default:
                return JavaType.FullyQualified.Kind.Class;
        }
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public @Nullable JavaType type(@Nullable Tree tree) {
        if (tree == null) {
            return null;
        }

        Symbol symbol = null;
        if (tree instanceof JCTree.JCIdent) {
            symbol = ((JCTree.JCIdent) tree).sym;
        } else if (tree instanceof JCTree.JCMethodDecl) {
            symbol = ((JCTree.JCMethodDecl) tree).sym;
        } else if (tree instanceof JCTree.JCVariableDecl) {
            return variableType(((JCTree.JCVariableDecl) tree).sym);
        } else if (tree instanceof JCTree.JCAnnotatedType && ((JCTree.JCAnnotatedType) tree).getUnderlyingType() instanceof JCTree.JCArrayTypeTree) {
            return annotatedArray((JCTree.JCAnnotatedType) tree);
        } else if (tree instanceof JCTree.JCClassDecl) {
            symbol = ((JCTree.JCClassDecl) tree).sym;
        } else if (tree instanceof JCTree.JCFieldAccess) {
            symbol = ((JCTree.JCFieldAccess) tree).sym;
        }

        return type(((JCTree) tree).type, symbol);
    }

    private @Nullable JavaType type(@Nullable Type type, @Nullable Symbol symbol) {
        if (type == null && symbol != null) {
            type = symbol.type;
        }
        if (type instanceof Type.MethodType || type instanceof Type.ForAll || (type instanceof Type.ErrorType && type.getOriginalType() instanceof Type.MethodType)) {
            return methodInvocationType(type, symbol);
        }
        return type(type);
    }

    public JavaType.Primitive primitive(TypeTag tag) {
        switch (tag) {
            case BOOLEAN:
                return JavaType.Primitive.Boolean;
            case BYTE:
                return JavaType.Primitive.Byte;
            case CHAR:
                return JavaType.Primitive.Char;
            case DOUBLE:
                return JavaType.Primitive.Double;
            case FLOAT:
                return JavaType.Primitive.Float;
            case INT:
                return JavaType.Primitive.Int;
            case LONG:
                return JavaType.Primitive.Long;
            case SHORT:
                return JavaType.Primitive.Short;
            case VOID:
                return JavaType.Primitive.Void;
            case NONE:
                return JavaType.Primitive.None;
            case CLASS:
                return JavaType.Primitive.String;
            case BOT:
                return JavaType.Primitive.Null;
            default:
                throw new IllegalArgumentException("Unknown type tag " + tag);
        }
    }

    public JavaType.@Nullable Variable variableType(@Nullable Symbol symbol) {
        return variableType(symbol, null);
    }

    private JavaType.@Nullable Variable variableType(@Nullable Symbol symbol,
                                                     JavaType.@Nullable FullyQualified owner) {
        if (!(symbol instanceof Symbol.VarSymbol)) {
            return null;
        }

        String signature = signatureBuilder.variableSignature(symbol);
        return typeFactory.computeVariable(signature, symbol.flags_field, symbol.name.toString(), symbol, variable -> {
            JavaType resolvedOwner = owner;
            if (owner == null) {
                Type type = symbol.owner.type;
                Symbol sym = symbol.owner;

                if (sym.type instanceof Type.ForAll) {
                    type = ((Type.ForAll) type).qtype;
                }

                resolvedOwner = type instanceof Type.MethodType ?
                        methodDeclarationType(sym, (JavaType.FullyQualified) type(sym.owner.type)) :
                        type(type);
                assert resolvedOwner != null;
            }

            variable.unsafeSet(resolvedOwner, type(symbol.type), listAnnotations(symbol));
        });
    }

    /**
     * Method type of a method invocation. Parameters and return type represent resolved types when they are generic
     * in the method declaration.
     *
     * @param selectType The method type.
     * @param symbol     The method symbol.
     * @return Method type attribution.
     */
    public JavaType.@Nullable Method methodInvocationType(@Nullable Type selectType, @Nullable Symbol symbol) {
        if (selectType instanceof Type.ErrorType) {
            try {
                // Ugly reflection solution, because AttrRecover$RecoveryErrorType is private inner class
                for (Class<?> targetClass : Class.forName(AttrRecover.class.getCanonicalName()).getDeclaredClasses()) {
                    if ("RecoveryErrorType".equals(targetClass.getSimpleName()) && targetClass.isInstance(selectType)) {
                        Field field = targetClass.getDeclaredField("candidateSymbol");
                        field.setAccessible(true);
                        Symbol originalSymbol = (Symbol) field.get(selectType);
                        return methodInvocationType(selectType.getOriginalType(), originalSymbol);
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        }

        if (selectType == null || selectType instanceof Type.ErrorType || symbol == null || symbol.kind == Kinds.Kind.ERR || symbol.type instanceof Type.UnknownType) {
            return null;
        }

        Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) symbol;

        if (selectType instanceof Type.ForAll) {
            Type.ForAll fa = (Type.ForAll) selectType;
            return methodInvocationType(fa.qtype, methodSymbol);
        }

        // Resolve declaring type before compute so a null result skips inserting an
        // unpopulated stub into the cache.
        JavaType.FullyQualified resolvedDeclaringType = TypeUtils.asFullyQualified(type(methodSymbol.owner.type));
        if (resolvedDeclaringType == null) {
            return null;
        }

        String signature = signatureBuilder.methodSignature(selectType, methodSymbol);
        String[] paramNames = null;
        if (!methodSymbol.params().isEmpty()) {
            paramNames = new String[methodSymbol.params().size()];
            com.sun.tools.javac.util.List<Symbol.VarSymbol> params = methodSymbol.params();
            for (int i = 0; i < params.size(); i++) {
                Symbol.VarSymbol p = params.get(i);
                String s = p.name.toString();
                paramNames[i] = s;
            }
        }

        return typeFactory.computeMethod(signature, methodSymbol.flags_field,
                methodSymbol.isConstructor() ? "<constructor>" : methodSymbol.getSimpleName().toString(),
                paramNames, null, null, methodSymbol, method -> {
            JavaType returnType = null;
            List<JavaType> parameterTypes = null;
            List<JavaType> exceptionTypes = null;

            if (selectType instanceof Type.MethodType) {
                Type.MethodType methodType = (Type.MethodType) selectType;

                if (!methodType.argtypes.isEmpty()) {
                    parameterTypes = new ArrayList<>(methodType.argtypes.size());
                    for (Type argtype : methodType.argtypes) {
                        if (argtype != null) {
                            JavaType javaType = type(argtype);
                            parameterTypes.add(javaType);
                        }
                    }
                }

                returnType = type(methodType.restype);

                if (!methodType.thrown.isEmpty()) {
                    exceptionTypes = new ArrayList<>(methodType.thrown.size());
                    for (Type exceptionType : methodType.thrown) {
                        JavaType javaType = type(exceptionType);
                        exceptionTypes.add(javaType);
                    }
                }
            } else if (selectType instanceof Type.UnknownType) {
                returnType = JavaType.Unknown.getInstance();
            }

            assert returnType != null;

            method.unsafeSet(resolvedDeclaringType,
                    methodSymbol.isConstructor() ? resolvedDeclaringType : returnType,
                    parameterTypes, exceptionTypes, listAnnotations(methodSymbol));
        });
    }

    /**
     * Method type of a method declaration. Parameters and return type represent generic signatures when applicable.
     *
     * @param symbol        The method symbol.
     * @param declaringType The method's declaring type.
     * @return Method type attribution.
     */
    public JavaType.@Nullable Method methodDeclarationType(@Nullable Symbol symbol, JavaType.@Nullable FullyQualified declaringType) {
        // if the symbol is not a method symbol, there is a parser error in play
        Symbol.MethodSymbol methodSymbol = symbol instanceof Symbol.MethodSymbol ? (Symbol.MethodSymbol) symbol : null;

        if (methodSymbol == null) {
            return null;
        }

        // Resolve declaring type before compute so a null result skips inserting
        // an unpopulated stub into the cache.
        JavaType.FullyQualified resolvedDeclaringType = declaringType;
        if (declaringType == null) {
            if (methodSymbol.owner instanceof Symbol.ClassSymbol || methodSymbol.owner instanceof Symbol.TypeVariableSymbol) {
                resolvedDeclaringType = TypeUtils.asFullyQualified(type(methodSymbol.owner.type));
            }
        }
        if (resolvedDeclaringType == null) {
            return null;
        }
        JavaType.FullyQualified finalDeclaringType = resolvedDeclaringType;

        String signature = signatureBuilder.methodSignature(methodSymbol);
        String[] paramNames = null;
        if (!methodSymbol.params().isEmpty()) {
            paramNames = new String[methodSymbol.params().size()];
            com.sun.tools.javac.util.List<Symbol.VarSymbol> params = methodSymbol.params();
            for (int i = 0; i < params.size(); i++) {
                Symbol.VarSymbol p = params.get(i);
                String s = p.name.toString();
                paramNames[i] = s;
            }
        }
        List<String> defaultValues = null;
        if (methodSymbol.getDefaultValue() != null) {
            if (methodSymbol.getDefaultValue() instanceof Attribute.Array) {
                defaultValues = ((Attribute.Array) methodSymbol.getDefaultValue()).getValue().stream()
                        .map(attr -> attr.getValue().toString())
                        .collect(toList());
            } else {
                try {
                    defaultValues = singletonList(methodSymbol.getDefaultValue().getValue().toString());
                } catch (UnsupportedOperationException e) {
                    // not all Attribute implementations define `getValue()`
                }
            }
        }

        List<String> declaredFormalTypeNames = null;
        for (Symbol.TypeVariableSymbol typeParam : methodSymbol.getTypeParameters()) {
            if (typeParam.owner == methodSymbol) {
                if (declaredFormalTypeNames == null) {
                    declaredFormalTypeNames = new ArrayList<>();
                }
                declaredFormalTypeNames.add(typeParam.name.toString());
            }
        }

        return typeFactory.computeMethod(signature, methodSymbol.flags_field,
                methodSymbol.isConstructor() ? "<constructor>" : methodSymbol.getSimpleName().toString(),
                paramNames, defaultValues,
                declaredFormalTypeNames == null ? null : declaredFormalTypeNames.toArray(new String[0]),
                methodSymbol, method -> {
            Type signatureType = methodSymbol.type instanceof Type.ForAll ?
                    ((Type.ForAll) methodSymbol.type).qtype :
                    methodSymbol.type;

            List<JavaType> exceptionTypes = null;

            Type selectType = methodSymbol.type;
            if (selectType instanceof Type.ForAll) {
                selectType = ((Type.ForAll) selectType).qtype;
            }

            if (selectType instanceof Type.MethodType) {
                Type.MethodType methodType = (Type.MethodType) selectType;
                if (!methodType.thrown.isEmpty()) {
                    exceptionTypes = new ArrayList<>(methodType.thrown.size());
                    for (Type exceptionType : methodType.thrown) {
                        JavaType javaType = type(exceptionType);
                        exceptionTypes.add(javaType);
                    }
                }
            }

            JavaType returnType;
            List<JavaType> parameterTypes = null;

            if (signatureType instanceof Type.ForAll) {
                signatureType = ((Type.ForAll) signatureType).qtype;
            }
            if (signatureType instanceof Type.MethodType) {
                Type.MethodType mt = (Type.MethodType) signatureType;

                if (!mt.argtypes.isEmpty()) {
                    parameterTypes = new ArrayList<>(mt.argtypes.size());
                    for (Type argtype : mt.argtypes) {
                        if (argtype != null) {
                            JavaType javaType = type(argtype);
                            parameterTypes.add(javaType);
                        }
                    }
                }

                returnType = type(mt.restype);
            } else {
                throw new UnsupportedOperationException("Unexpected method signature type" + signatureType.getClass().getName());
            }

            method.unsafeSet(finalDeclaringType,
                    methodSymbol.isConstructor() ? finalDeclaringType : returnType,
                    parameterTypes, exceptionTypes, listAnnotations(methodSymbol));
        });
    }

    private void completeClassSymbol(Symbol.ClassSymbol classSymbol) {
        try {
            classSymbol.complete();
        } catch (Symbol.CompletionFailure ignore) {
        }
    }

    private @Nullable List<JavaType.FullyQualified> listAnnotations(Symbol sym) {
        List<JavaType.FullyQualified> annotations = null;
        if (!sym.getDeclarationAttributes().isEmpty()) {
            annotations = new ArrayList<>(sym.getDeclarationAttributes().size());
            for (Attribute.Compound a : sym.getDeclarationAttributes()) {
                JavaType.Annotation annotation = annotationType(a);
                if (annotation == null) continue;
                annotations.add(annotation);
            }
        }
        return annotations;
    }

    private JavaType.@Nullable Annotation annotationType(Attribute.Compound compound) {
        JavaType.FullyQualified annotType = TypeUtils.asFullyQualified(type(compound.type));
        if (annotType == null) {
            return null;
        }
        List<JavaType.Annotation.ElementValue> elementValues = new ArrayList<>();
        for (Pair<Symbol.MethodSymbol, Attribute> attr : compound.values) {
            Object value = annotationElementValue(attr.snd.getValue());
            JavaType.Method element = requireNonNull(methodDeclarationType(attr.fst, annotType));
            JavaType.Annotation.ElementValue elementValue = value instanceof Object[] ?
                    JavaType.Annotation.ArrayElementValue.from(element, ((Object[]) value)) :
                    JavaType.Annotation.SingleElementValue.from(element, value);
            elementValues.add(elementValue);
        }
        return new JavaType.Annotation(annotType, elementValues);
    }

    private Object annotationElementValue(Object value) {
        if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Character) {
            return value;
        } else if (value instanceof Symbol.VarSymbol) {
            JavaType.Variable mapped = variableType((Symbol.VarSymbol) value);
            if (mapped != null) {
                return mapped;
            }
        } else if (value instanceof Type) {
            return type((Type) value);
        } else if (value instanceof Attribute.Array) {
            List<@Nullable Object> list = new ArrayList<>();
            for (Attribute attribute : ((Attribute.Array) value).values) {
                list.add(annotationElementValue(attribute));
            }
            return list.toArray(!list.isEmpty() && list.get(0) instanceof JavaType ? JavaType.EMPTY_JAVA_TYPE_ARRAY : new Object[0]);
        } else if (value instanceof List<?>) {
            List<@Nullable Object> list = new ArrayList<>();
            for (Object o : ((List<?>) value)) {
                list.add(annotationElementValue(o));
            }
            return list.toArray(!list.isEmpty() && list.get(0) instanceof JavaType ? JavaType.EMPTY_JAVA_TYPE_ARRAY : new Object[0]);
        } else if (value instanceof Attribute.Class) {
            return type(((Attribute.Class) value).classType);
        } else if (value instanceof Attribute.Compound) {
            JavaType.Annotation mapped = annotationType((Attribute.Compound) value);
            if (mapped != null) {
                return mapped;
            }
        } else if (value instanceof Attribute.Constant) {
            return annotationElementValue(((Attribute.Constant) value).value);
        } else if (value instanceof Attribute.Enum) {
            return annotationElementValue(((Attribute.Enum) value).value);
        }
        return JavaType.Unknown.getInstance();
    }
}
