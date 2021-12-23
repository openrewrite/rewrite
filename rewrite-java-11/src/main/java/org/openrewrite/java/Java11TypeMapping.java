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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.*;

@RequiredArgsConstructor
class Java11TypeMapping implements JavaTypeMapping<Type> {
    private static final int KIND_BITMASK_INTERFACE = 1 << 9;
    private static final int KIND_BITMASK_ANNOTATION = 1 << 13;
    private static final int KIND_BITMASK_ENUM = 1 << 14;

    private final Java11TypeSignatureBuilder signatureBuilder = new Java11TypeSignatureBuilder();

    private final Map<String, Object> typeBySignature;

    public JavaType type(@Nullable com.sun.tools.javac.code.Type type) {
        if (type == null || type instanceof Type.ErrorType) {
            return JavaType.Class.Unknown.getInstance();
        }

        String signature = signatureBuilder.signature(type);
        JavaType existing = (JavaType) typeBySignature.get(signature);
        if (existing != null) {
            return existing;
        }

        if (type instanceof Type.ClassType) {
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
        } else if (com.sun.tools.javac.code.Type.noType.equals(type)) {
            return JavaType.Class.Unknown.getInstance();
        }

        throw new UnsupportedOperationException("Unknown type " + type.getClass().getName());
    }

    private JavaType array(Type type, String signature) {
        JavaType.Array arr = new JavaType.Array(type(((Type.ArrayType) type).elemtype));
        typeBySignature.put(signature, arr);
        return arr;
    }

    private JavaType.GenericTypeVariable generic(Type.WildcardType wildcard, String signature) {
        JavaType.GenericTypeVariable gtv = new JavaType.GenericTypeVariable(null, "?", INVARIANT, null);
        typeBySignature.put(signature, gtv);

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

        gtv.unsafeSet(variance, bounds);
        return gtv;
    }

    private JavaType generic(Type.TypeVar type, String signature) {
        String name = type.tsym.name.toString();
        JavaType.GenericTypeVariable gtv = new JavaType.GenericTypeVariable(null,
                name, INVARIANT, null);
        typeBySignature.put(signature, gtv);

        List<JavaType> bounds = null;
        if (type.getUpperBound() instanceof Type.IntersectionClassType) {
            Type.IntersectionClassType intersectionBound = (Type.IntersectionClassType) type.getUpperBound();
            if (intersectionBound.interfaces_field.length() > 0) {
                bounds = new ArrayList<>(intersectionBound.interfaces_field.length());
                for (Type bound : intersectionBound.interfaces_field) {
                    bounds.add(type(bound));
                }
            } else if (intersectionBound.supertype_field != null) {
                JavaType mappedBound = type(intersectionBound.supertype_field);
                if (!(mappedBound instanceof JavaType.FullyQualified) || !((JavaType.FullyQualified) mappedBound).getFullyQualifiedName().equals("java.lang.Object")) {
                    bounds = singletonList(mappedBound);
                }
            }
        } else if (type.getUpperBound() != null) {
            JavaType mappedBound = type(type.getUpperBound());
            if (!(mappedBound instanceof JavaType.FullyQualified) || !((JavaType.FullyQualified) mappedBound).getFullyQualifiedName().equals("java.lang.Object")) {
                bounds = singletonList(mappedBound);
            }
        }

        gtv.unsafeSet(bounds == null ? INVARIANT : COVARIANT, bounds);
        return gtv;
    }

    private JavaType.FullyQualified classType(Type.ClassType classType, String signature) {
        Symbol.ClassSymbol sym = (Symbol.ClassSymbol) classType.tsym;
        Type.ClassType symType = (Type.ClassType) sym.type;

        if (sym.className().startsWith("com.sun.") ||
                sym.className().startsWith("sun.") ||
                sym.className().startsWith("java.awt.") ||
                sym.className().startsWith("java.applet.") ||
                sym.className().startsWith("jdk.") ||
                sym.className().startsWith("org.graalvm")) {
            return (JavaType.Class) typeBySignature.computeIfAbsent(signatureBuilder.signature(classType), ignored -> new JavaType.Class(
                    null, sym.flags_field, sym.className(), getKind(sym),
                    null, null, null, null, null, null));
        }

        JavaType.Class clazz = (JavaType.Class) typeBySignature.get(sym.className());
        if (clazz == null) {
            if (!sym.completer.isTerminal()) {
                completeClassSymbol(sym);
            }

            clazz = new JavaType.Class(
                    null,
                    sym.flags_field,
                    sym.className(),
                    getKind(sym),
                    null, null, null, null, null, null
            );

            typeBySignature.put(sym.className(), clazz);

            JavaType.FullyQualified supertype = TypeUtils.asFullyQualified(type(classType.supertype_field == null ? symType.supertype_field :
                    classType.supertype_field));

            JavaType.FullyQualified owner = null;
            if (sym.owner instanceof Symbol.ClassSymbol) {
                owner = TypeUtils.asFullyQualified(type(sym.owner.type));
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
                    JavaType.FullyQualified javaType = TypeUtils.asFullyQualified(type(iParam));
                    if (javaType != null) {
                        interfaces.add(javaType);
                    }
                }
            }

            List<JavaType.FullyQualified> annotations = null;
            if (!sym.getDeclarationAttributes().isEmpty()) {
                annotations = new ArrayList<>(sym.getDeclarationAttributes().size());
                for (Attribute.Compound a : sym.getDeclarationAttributes()) {
                    JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type(a.type));
                    if (fq != null) {
                        annotations.add(fq);
                    }
                }
            }

            clazz.unsafeSet(supertype, owner, annotations, interfaces, fields, methods);
        }

        if (classType.typarams_field != null && classType.typarams_field.length() > 0) {
            JavaType.Parameterized pt = new JavaType.Parameterized(null, null, null);
            typeBySignature.put(signature, pt);

            List<JavaType> typeParameters = new ArrayList<>(classType.typarams_field.length());
            for (Type tParam : classType.typarams_field) {
                typeParameters.add(type(tParam));
            }

            pt.unsafeSet(clazz, typeParameters);
            return pt;
        }

        return clazz;
    }

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

    public JavaType type(Tree t) {
        return type(((JCTree) t).type);
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

    @Nullable
    public JavaType.Variable variableType(@Nullable Symbol symbol) {
        return variableType(symbol, null);
    }

    @Nullable
    private JavaType.Variable variableType(@Nullable Symbol symbol,
                                           @Nullable JavaType.FullyQualified owner) {
        if (!(symbol instanceof Symbol.VarSymbol) || symbol.owner instanceof Symbol.MethodSymbol) {
            return null;
        }

        String signature = signatureBuilder.variableSignature(symbol);
        JavaType.Variable existing = (JavaType.Variable) typeBySignature.get(signature);
        if (existing != null) {
            return existing;
        }

        JavaType.Variable variable = new JavaType.Variable(
                symbol.flags_field,
                symbol.name.toString(),
                null, null, null);

        typeBySignature.put(signature, variable);


        JavaType.FullyQualified resolvedOwner = owner;
        if (owner == null) {
            resolvedOwner = TypeUtils.asFullyQualified(type(symbol.owner.type));
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

        variable.unsafeSet(resolvedOwner, type(symbol.type), annotations);
        return variable;
    }

    @Nullable
    public JavaType.Method methodType(@Nullable com.sun.tools.javac.code.Type selectType, @Nullable Symbol symbol) {
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

            String signature = signatureBuilder.methodSignature(selectType, methodSymbol);
            JavaType.Method existing = (JavaType.Method) typeBySignature.get(signature);
            if (existing != null) {
                return existing;
            }


            List<String> paramNames = null;
            if (!methodSymbol.params().isEmpty()) {
                paramNames = new ArrayList<>(methodSymbol.params().size());
                for (Symbol.VarSymbol p : methodSymbol.params()) {
                    String s = p.name.toString();
                    paramNames.add(s);
                }
            }

            JavaType.Method method = new JavaType.Method(
                    methodSymbol.flags_field,
                    null,
                    methodSymbol.getSimpleName().toString(),
                    paramNames,
                    null, null, null, null
            );
            typeBySignature.put(signature, method);

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

            method.unsafeSet(resolvedDeclaringType, methodSignature(genericSignatureType), methodSignature(selectType), exceptionTypes, annotations);
            return method;
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
