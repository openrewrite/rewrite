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
package org.openrewrite.java;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.JCTree;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.*;

@RequiredArgsConstructor
class Java11TypeMapping {
    private static final int KIND_BITMASK_INTERFACE = 1 << 9;
    private static final int KIND_BITMASK_ANNOTATION = 1 << 13;
    private static final int KIND_BITMASK_ENUM = 1 << 14;

    private final Map<Symbol.ClassSymbol, JavaType.Class> classStack = new IdentityHashMap<>();
    private final Java11TypeSignatureBuilder signatureBuilder = new Java11TypeSignatureBuilder();

    private final Map<String, Object> typeBySignature;

    @Nullable
    public JavaType type(@Nullable com.sun.tools.javac.code.Type type) {
        classStack.clear();
        return _type(type);
    }

    @Nullable
    private JavaType _type(@Nullable com.sun.tools.javac.code.Type type) {
        if (type instanceof Type.ClassType) {
            if (type instanceof com.sun.tools.javac.code.Type.ErrorType) {
                return null;
            }

            Type.ClassType classType = (Type.ClassType) type;
            Symbol.ClassSymbol sym = (Symbol.ClassSymbol) type.tsym;
            Type.ClassType symType = (Type.ClassType) sym.type;

            JavaType.Class existingClass = classStack.get(sym);
            if (existingClass != null) {
                return existingClass;
            }

            JavaType.Class clazz = classType(classType);
            if (classType.typarams_field == null || classType.typarams_field.length() == 0) {
                return clazz;
            } else {
                AtomicBoolean newlyCreated = new AtomicBoolean(false);

                JavaType.Parameterized parameterized = (JavaType.Parameterized) typeBySignature.computeIfAbsent(signatureBuilder.signature(type), ignored -> {
                    newlyCreated.set(true);
                    //noinspection ConstantConditions
                    return new JavaType.Parameterized(null, null, null);
                });

                if (newlyCreated.get()) {
                    List<JavaType> typeParameters = new ArrayList<>(classType.typarams_field.length());
                    for (Type tParam : classType.typarams_field) {
                        JavaType javaType = type(tParam);
                        if (javaType != null) {
                            typeParameters.add(javaType);
                        }
                    }

                    parameterized.unsafeSet(clazz, typeParameters);
                }

                return parameterized;
            }
        } else if (type instanceof Type.TypeVar) {
            return (JavaType) typeBySignature.computeIfAbsent(signatureBuilder.signature(type), ignored -> {
                List<JavaType> bounds;
                if (type.getUpperBound() instanceof Type.IntersectionClassType) {
                    bounds = new ArrayList<>();
                    Type.IntersectionClassType intersectionBound = (Type.IntersectionClassType) type.getUpperBound();
                    if (intersectionBound.supertype_field != null) {
                        bounds.add(type(intersectionBound.supertype_field));
                    }
                    for (Type bound : intersectionBound.interfaces_field) {
                        bounds.add(type(bound));
                    }
                } else {
                    bounds = singletonList(type(type.getUpperBound()));
                }

                return new JavaType.GenericTypeVariable(null, type.tsym.name.toString(),
                        COVARIANT, bounds);
            });
        } else if (type instanceof Type.JCPrimitiveType) {
            return primitiveType(type.getTag());
        } else if (type instanceof Type.JCVoidType) {
            return JavaType.Primitive.Void;
        } else if (type instanceof Type.ArrayType) {
            //noinspection ConstantConditions
            return (JavaType) typeBySignature.computeIfAbsent(signatureBuilder.signature(type), ignored ->
                    new JavaType.Array(type(((Type.ArrayType) type).elemtype)));
        } else if (type instanceof Type.WildcardType) {
            Type.WildcardType wildcard = (Type.WildcardType) type;
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

            if (bounds != null && bounds.get(0) instanceof JavaType.FullyQualified && ((JavaType.FullyQualified) bounds.get(0))
                    .getFullyQualifiedName().equals("java.lang.Object")) {
                bounds = null;
            }

            return new JavaType.GenericTypeVariable(null, "?", variance, bounds);
        } else if (com.sun.tools.javac.code.Type.noType.equals(type)) {
            return null;
        } else {
            return null;
        }
    }

    private JavaType.Class classType(Type.ClassType type) {
        Symbol.ClassSymbol sym = (Symbol.ClassSymbol) type.tsym;
        Type.ClassType symType = (Type.ClassType) sym.type;

        if (sym.className().startsWith("com.sun.") ||
                sym.className().startsWith("sun.") ||
                sym.className().startsWith("java.awt.") ||
                sym.className().startsWith("java.applet.") ||
                sym.className().startsWith("jdk.") ||
                sym.className().startsWith("org.graalvm")) {
            return (JavaType.Class) typeBySignature.computeIfAbsent(signatureBuilder.signature(type), ignored -> new JavaType.Class(
                    null, sym.flags_field, sym.className(), getKind(sym),
                    null, null, null, null, null, null));
        }

        AtomicBoolean newlyCreated = new AtomicBoolean(false);

        JavaType.Class clazz = (JavaType.Class) typeBySignature.computeIfAbsent(
                signatureBuilder.classSignature(type), ignored -> {
                    newlyCreated.set(true);

                    if (!sym.completer.isTerminal()) {
                        completeClassSymbol(sym);
                    }

                    return new JavaType.Class(
                            null,
                            sym.flags_field,
                            sym.className(),
                            getKind(sym),
                            null, null, null, null, null, null
                    );
                });

        // adding methods and fields after the class is created and cached is how we avoid
        // infinite recursing due to the fact that e.g. the method declaration is the same
        // class as the type on the class containing the method
        if (newlyCreated.get()) {
            classStack.put(sym, clazz);

            JavaType.FullyQualified supertype = TypeUtils.asFullyQualified(_type(type.supertype_field == null ? symType.supertype_field :
                    type.supertype_field));

            JavaType.FullyQualified owner = null;
            if (sym.owner instanceof Symbol.ClassSymbol) {
                owner = TypeUtils.asFullyQualified(_type(sym.owner.type));
            }

            List<JavaType.Variable> fields = null;
            List<JavaType.Method> methods = null;

            if (sym.members_field != null) {
                for (Symbol elem : sym.members_field.getSymbols()) {
                    if (elem instanceof Symbol.VarSymbol &&
                            (elem.flags_field & (Flags.SYNTHETIC | Flags.BRIDGE | Flags.HYPOTHETICAL |
                                    Flags.GENERATEDCONSTR | Flags.ANONCONSTR)) == 0) {
                        if (sym.className().equals("java.lang.String") && sym.name.toString().equals("serialPersistentFields")) {
                            // there is a "serialPersistentFields" member within the String class which is used in normal Java
                            // serialization to customize how the String field is serialized. This field is tripping up Jackson
                            // serialization and is intentionally filtered to prevent errors.
                            continue;
                        }

                        if (fields == null) {
                            fields = new ArrayList<>();
                        }
                        fields.add(variableType(elem, clazz));
                    } else if (elem instanceof Symbol.MethodSymbol &&
                            (elem.flags_field & (Flags.SYNTHETIC | Flags.BRIDGE | Flags.HYPOTHETICAL |
                                    Flags.GENERATEDCONSTR | Flags.ANONCONSTR)) == 0) {
                        if (methods == null) {
                            methods = new ArrayList<>();
                        }
                        methods.add(methodType(elem.type, elem, clazz));
                    }
                }
            }

            List<JavaType.FullyQualified> interfaces = null;
            if (symType.interfaces_field != null) {
                interfaces = new ArrayList<>(symType.interfaces_field.length());
                for (com.sun.tools.javac.code.Type iParam : symType.interfaces_field) {
                    JavaType.FullyQualified javaType = TypeUtils.asFullyQualified(_type(iParam));
                    if (javaType != null) {
                        interfaces.add(javaType);
                    }
                }
            }

            List<JavaType.FullyQualified> annotations = null;
            if (!sym.getDeclarationAttributes().isEmpty()) {
                annotations = new ArrayList<>(sym.getDeclarationAttributes().size());
                for (Attribute.Compound a : sym.getDeclarationAttributes()) {
                    JavaType.FullyQualified fq = TypeUtils.asFullyQualified(_type(a.type));
                    if (fq != null) {
                        annotations.add(fq);
                    }
                }
            }

            clazz.unsafeSet(supertype, owner, annotations, interfaces, fields, methods);
        }

        return clazz;
    }

    @NotNull
    private JavaType.Class.Kind getKind(Symbol.ClassSymbol sym) {
        JavaType.Class.Kind kind;
        if ((sym.flags_field & KIND_BITMASK_ENUM) != 0) {
            kind = JavaType.Class.Kind.Enum;
        } else if ((sym.flags_field & KIND_BITMASK_ANNOTATION) != 0) {
            kind = JavaType.Class.Kind.Annotation;
        } else if ((sym.flags_field & KIND_BITMASK_INTERFACE) != 0) {
            kind = JavaType.Class.Kind.Interface;
        } else {
            kind = JavaType.Class.Kind.Class;
        }
        return kind;
    }

    @Nullable
    public JavaType type(Tree t) {
        return type(((JCTree) t).type);
    }

    public JavaType.Primitive primitiveType(TypeTag tag) {
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

    @Nullable
    public JavaType.Variable variableType(@Nullable Symbol symbol) {
        classStack.clear();
        return variableType(symbol, null);
    }

    @Nullable
    private JavaType.Variable variableType(@Nullable Symbol symbol,
                                           @Nullable JavaType.FullyQualified owner) {
        if (!(symbol instanceof Symbol.VarSymbol) || symbol.owner instanceof Symbol.MethodSymbol) {
            return null;
        }

        return (JavaType.Variable) typeBySignature.computeIfAbsent(signatureBuilder.variableSignature(symbol), ignored -> {
            JavaType.FullyQualified resolvedOwner = owner;
            if (owner == null) {
                resolvedOwner = TypeUtils.asFullyQualified(_type(symbol.owner.type));
            }
            if (resolvedOwner == null) {
                return null;
            }

            List<JavaType.FullyQualified> annotations = null;
            if (!symbol.getDeclarationAttributes().isEmpty()) {
                annotations = new ArrayList<>(symbol.getDeclarationAttributes().size());
                for (Attribute.Compound a : symbol.getDeclarationAttributes()) {
                    JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type(a.type));
                    if (fq != null) {
                        annotations.add(fq);
                    }
                }
            }

            return new JavaType.Variable(
                    symbol.flags_field,
                    symbol.name.toString(),
                    resolvedOwner,
                    type(symbol.type),
                    annotations
            );
        });
    }

    @Nullable
    public JavaType.Method methodType(@Nullable com.sun.tools.javac.code.Type selectType, @Nullable Symbol symbol) {
        classStack.clear();
        return methodType(selectType, symbol, null);
    }

    @Nullable
    private JavaType.Method methodType(@Nullable com.sun.tools.javac.code.Type selectType, @Nullable Symbol symbol,
                                       @Nullable JavaType.FullyQualified declaringType) {
        // if the symbol is not a method symbol, there is a parser error in play
        Symbol.MethodSymbol methodSymbol = symbol instanceof Symbol.MethodSymbol ? (Symbol.MethodSymbol) symbol : null;

        if (methodSymbol != null && selectType != null) {
            StringJoiner argumentTypeSignatures = new StringJoiner(",");
            if (selectType instanceof Type.ForAll) {
                Type.ForAll fa = (Type.ForAll) selectType;
                return methodType(fa.qtype, symbol, declaringType);
            }

            return (JavaType.Method) typeBySignature.computeIfAbsent(signatureBuilder.methodSignature(selectType, methodSymbol), ignored -> {
                List<String> paramNames = null;
                if (!methodSymbol.params().isEmpty()) {
                    paramNames = new ArrayList<>(methodSymbol.params().size());
                    for (Symbol.VarSymbol p : methodSymbol.params()) {
                        String s = p.name.toString();
                        paramNames.add(s);
                    }
                }

                Type genericSignatureType = methodSymbol.type instanceof Type.ForAll ?
                        ((Type.ForAll) methodSymbol.type).qtype :
                        methodSymbol.type;

                List<JavaType.FullyQualified> exceptionTypes = null;
                if (selectType instanceof Type.MethodType) {
                    Type.MethodType methodType = (Type.MethodType) selectType;
                    if (!methodType.thrown.isEmpty()) {
                        exceptionTypes = new ArrayList<>(methodType.thrown.size());
                        for (Type exceptionType : methodType.thrown) {
                            JavaType.FullyQualified javaType = TypeUtils.asFullyQualified(type(exceptionType));
                            if (javaType == null) {
                                // if the type cannot be resolved to a class (it might not be on the classpath, or it might have
                                // been mapped to cyclic)
                                if (exceptionType instanceof Type.ClassType) {
                                    Symbol.ClassSymbol sym = (Symbol.ClassSymbol) exceptionType.tsym;
                                    javaType = new JavaType.Class(null, Flag.Public.getBitMask(), sym.className(), JavaType.Class.Kind.Class,
                                            null, null, null, null, null, null);
                                }
                            }
                            if (javaType != null) {
                                // if the exception type is not resolved, it is not added to the list of exceptions
                                exceptionTypes.add(javaType);
                            }
                        }
                    }
                }

                JavaType.FullyQualified resolvedDeclaringType = declaringType;
                if (declaringType == null) {
                    if (methodSymbol.owner instanceof Symbol.ClassSymbol || methodSymbol.owner instanceof Symbol.TypeVariableSymbol) {
                        resolvedDeclaringType = TypeUtils.asFullyQualified(type(methodSymbol.owner.type));
                    }
                }

                if (resolvedDeclaringType == null) {
                    return null;
                }

                List<JavaType.FullyQualified> annotations = null;
                if (!methodSymbol.getDeclarationAttributes().isEmpty()) {
                    annotations = new ArrayList<>(methodSymbol.getDeclarationAttributes().size());
                    for (Attribute.Compound a : methodSymbol.getDeclarationAttributes()) {
                        JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type(a.type));
                        if (fq != null) {
                            annotations.add(fq);
                        }
                    }
                }

                return new JavaType.Method(
                        methodSymbol.flags_field,
                        resolvedDeclaringType,
                        methodSymbol.getSimpleName().toString(),
                        paramNames,
                        methodSignature(genericSignatureType),
                        methodSignature(selectType),
                        exceptionTypes,
                        annotations
                );
            });
        }

        return null;
    }

    @Nullable
    private JavaType.Method.Signature methodSignature(Type signatureType) {
        if (signatureType instanceof Type.ForAll) {
            signatureType = ((Type.ForAll) signatureType).qtype;
        }

        if (signatureType instanceof Type.MethodType) {
            Type.MethodType mt = (Type.MethodType) signatureType;

            List<JavaType> paramTypes = emptyList();
            if (!mt.argtypes.isEmpty()) {
                paramTypes = new ArrayList<>(mt.argtypes.size());
                for (com.sun.tools.javac.code.Type argtype : mt.argtypes) {
                    if (argtype != null) {
                        JavaType javaType = type(argtype);
                        paramTypes.add(javaType);
                    }
                }
            }

            return new JavaType.Method.Signature(type(mt.restype), paramTypes);
        }
        return null;
    }

    private void completeClassSymbol(Symbol.ClassSymbol classSymbol) {
        String packageName = classSymbol.packge().fullname.toString();
        if (packageName.startsWith("com.sun.") ||
                packageName.startsWith("sun.") ||
                packageName.startsWith("java.awt.") ||
                packageName.startsWith("java.applet.") ||
                packageName.startsWith("jdk.") ||
                packageName.startsWith("org.graalvm")) {
            return;
        }

        try {
            classSymbol.complete();
        } catch (Symbol.CompletionFailure ignore) {
        }
    }
}
