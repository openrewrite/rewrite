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
import org.openrewrite.java.cache.JavaTypeCache;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.joining;

@RequiredArgsConstructor
public class TypeMapping {
    private static final int KIND_BITMASK_INTERFACE = 1 << 9;
    private static final int KIND_BITMASK_ANNOTATION = 1 << 13;
    private static final int KIND_BITMASK_ENUM = 1 << 14;

    private final JavaTypeCache typeCache;

    @Nullable
    public JavaType type(@Nullable com.sun.tools.javac.code.Type type) {
        return type(type, emptyMap());
    }

    @Nullable
    private JavaType type(@Nullable com.sun.tools.javac.code.Type type, Map<String, JavaType.Class> stack) {
        if (type instanceof Type.ClassType) {
            if (type instanceof com.sun.tools.javac.code.Type.ErrorType) {
                return null;
            }

            Type.ClassType classType = (Type.ClassType) type;
            Symbol.ClassSymbol sym = (Symbol.ClassSymbol) type.tsym;
            Type.ClassType symType = (Type.ClassType) sym.type;

            JavaType.Class existingClass = stack.get(sym.className());
            if (existingClass != null) {
                return existingClass;
            }

            if (sym.className().equals("java.lang.Class")) {
                return JavaType.Class.CLASS;
            } else if (sym.className().equals("java.lang.Enum")) {
                return JavaType.Class.ENUM;
            } else if (sym.className().startsWith("com.sun.") ||
                    sym.className().startsWith("sun.") ||
                    sym.className().startsWith("java.") ||
                    sym.className().startsWith("jdk.") ||
                    sym.className().startsWith("org.graalvm")) {
                return typeCache.computeClass(sym.className(), () -> new JavaType.Class(
                        null, sym.flags_field, sym.className(), getKind(sym),
                        null, null, null, null, null, null));
            } else {
                AtomicBoolean newlyCreated = new AtomicBoolean(false);

                JavaType.Class clazz = typeCache.computeClass(
                        sym.className(), () -> {
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
                    Map<String, JavaType.Class> stackWithSym = new HashMap<>(stack);
                    stackWithSym.put(sym.className(), clazz);

                    JavaType.FullyQualified supertype = TypeUtils.asFullyQualified(type(classType.supertype_field == null ? symType.supertype_field :
                            classType.supertype_field, stack));

                    JavaType.FullyQualified owner = null;
                    if (sym.owner instanceof Symbol.ClassSymbol) {
                        owner = TypeUtils.asFullyQualified(type(sym.owner.type, stack));
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
                                fields.add(variableType(elem, stackWithSym, clazz));
                            } else if (elem instanceof Symbol.MethodSymbol &&
                                    (elem.flags_field & (Flags.SYNTHETIC | Flags.BRIDGE | Flags.HYPOTHETICAL |
                                            Flags.GENERATEDCONSTR | Flags.ANONCONSTR)) == 0) {
                                if (methods == null) {
                                    methods = new ArrayList<>();
                                }
                                methods.add(methodType(elem.type, elem, elem.getSimpleName().toString(), stackWithSym, clazz));
                            }
                        }
                    }

                    List<JavaType.FullyQualified> interfaces = null;
                    if (symType.interfaces_field != null) {
                        interfaces = new ArrayList<>(symType.interfaces_field.length());
                        for (com.sun.tools.javac.code.Type iParam : symType.interfaces_field) {
                            JavaType.FullyQualified javaType = TypeUtils.asFullyQualified(type(iParam, stack));
                            if (javaType != null) {
                                interfaces.add(javaType);
                            }
                        }
                    }

                    List<JavaType.FullyQualified> annotations = null;
                    if (!sym.getDeclarationAttributes().isEmpty()) {
                        annotations = new ArrayList<>(sym.getDeclarationAttributes().size());
                        for (Attribute.Compound a : sym.getDeclarationAttributes()) {
                            JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type(a.type, stackWithSym));
                            if (fq != null) {
                                annotations.add(fq);
                            }
                        }
                    }

                    clazz.unsafeSet(supertype, owner, annotations, interfaces, fields, methods);
                }

                if (classType.typarams_field == null || classType.typarams_field.length() == 0) {
                    return clazz;
                } else {
                    StringJoiner shallowGenericTypeVariables = new StringJoiner(",");
                    for (Type typeParameter : classType.typarams_field) {
                        String typeParameterSignature = signature(typeParameter);
                        if (typeParameterSignature != null) {
                            shallowGenericTypeVariables.add(typeParameterSignature);
                        }
                    }

                    newlyCreated.set(false);
                    JavaType.Parameterized parameterized = typeCache.computeParameterized(sym.className(),
                            shallowGenericTypeVariables.toString(), () -> {
                                newlyCreated.set(true);
                                return new JavaType.Parameterized(null, null, null);
                            });

                    if (newlyCreated.get()) {
                        List<JavaType> typeParameters = new ArrayList<>(classType.typarams_field.length());
                        for (Type tParam : classType.typarams_field) {
                            JavaType javaType = type(tParam, stack);
                            if (javaType != null) {
                                typeParameters.add(javaType);
                            }
                        }

                        parameterized.unsafeSet(clazz, typeParameters);
                    }

                    return parameterized;
                }
            }
        } else if (type instanceof Type.TypeVar) {
            return typeCache.computeGeneric(
                    type.tsym.name.toString(),
                    signature(type.getUpperBound()),
                    () -> new JavaType.GenericTypeVariable(null, type.tsym.name.toString(),
                            TypeUtils.asFullyQualified(type(type.getUpperBound(), stack)))
            );
        } else if (type instanceof Type.JCPrimitiveType) {
            return primitiveType(type.getTag());
        } else if (type instanceof Type.JCVoidType) {
            return JavaType.Primitive.Void;
        } else if (type instanceof Type.ArrayType) {
            return new JavaType.Array(type(((Type.ArrayType) type).elemtype, stack));
        } else if (type instanceof Type.WildcardType) {
            // TODO: For now we are just mapping wildcards into their bound types and we are not accounting for the
            //       "bound kind"
            // <?>                --> java.lang.Object
            // <? extends Number> --> Number
            // <? super Number>   --> Number
            // <? super T>        --> GenericTypeVariable
            Type.WildcardType wildcard = (Type.WildcardType) type;
            if (wildcard.kind == BoundKind.UNBOUND) {
                return typeCache.computeClass("java.lang.Class", () -> JavaType.Class.build("java.lang.Object"));
            } else {
                return type(wildcard.type, stack);
            }
        } else if (com.sun.tools.javac.code.Type.noType.equals(type)) {
            return null;
        } else {
            return null;
        }

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
    public JavaType.Variable variableType(@Nullable Symbol symbol, Map<String, JavaType.Class> stack) {
        return variableType(symbol, stack, null);
    }

    @Nullable
    private JavaType.Variable variableType(@Nullable Symbol symbol, Map<String, JavaType.Class> stack,
                                           @Nullable JavaType.FullyQualified owner) {
        if (!(symbol instanceof Symbol.VarSymbol) || symbol.owner instanceof Symbol.MethodSymbol) {
            return null;
        }

        return typeCache.computeVariable(signature(symbol.owner.type), symbol.name.toString(),
                () -> {
                    JavaType.FullyQualified resolvedOwner = owner;
                    if (owner == null) {
                        resolvedOwner = TypeUtils.asFullyQualified(type(symbol.owner.type, stack));
                    }
                    if (resolvedOwner == null) {
                        return null;
                    }

                    List<JavaType.FullyQualified> annotations = null;
                    if (!symbol.getDeclarationAttributes().isEmpty()) {
                        annotations = new ArrayList<>(symbol.getDeclarationAttributes().size());
                        for (Attribute.Compound a : symbol.getDeclarationAttributes()) {
                            JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type(a.type, stack));
                            if (fq != null) {
                                annotations.add(fq);
                            }
                        }
                    }

                    return new JavaType.Variable(
                            symbol.flags_field,
                            symbol.name.toString(),
                            resolvedOwner,
                            type(symbol.type, stack),
                            annotations
                    );
                });
    }

    @Nullable
    public JavaType.Method methodType(@Nullable com.sun.tools.javac.code.Type selectType, @Nullable Symbol symbol,
                                      String methodName, Map<String, JavaType.Class> stack) {
        return methodType(selectType, symbol, methodName, stack, null);
    }

    @Nullable
    private JavaType.Method methodType(@Nullable com.sun.tools.javac.code.Type selectType, @Nullable Symbol symbol,
                                       String methodName, Map<String, JavaType.Class> stack,
                                       @Nullable JavaType.FullyQualified declaringType) {
        // if the symbol is not a method symbol, there is a parser error in play
        Symbol.MethodSymbol methodSymbol = symbol instanceof Symbol.MethodSymbol ? (Symbol.MethodSymbol) symbol : null;

        if (methodSymbol != null && selectType != null) {
            StringJoiner argumentTypeSignatures = new StringJoiner(",");
            if (selectType instanceof Type.MethodType) {
                Type.MethodType mt = (Type.MethodType) selectType;
                if (!mt.argtypes.isEmpty()) {
                    for (com.sun.tools.javac.code.Type argtype : mt.argtypes) {
                        if (argtype != null) {
                            argumentTypeSignatures.add(signature(argtype));
                        }
                    }
                }
            } else if (selectType instanceof Type.ForAll) {
                Type.ForAll fa = (Type.ForAll) selectType;
                return methodType(fa.qtype, symbol, methodName, stack, declaringType);
            }

            return typeCache.computeMethod(signature(symbol.owner.type),
                    methodName,
                    signature(selectType.getReturnType()),
                    argumentTypeSignatures.toString(), () -> {
                        List<String> paramNames = null;
                        if (!methodSymbol.params().isEmpty()) {
                            paramNames = new ArrayList<>(methodSymbol.params().size());
                            for (Symbol.VarSymbol p : methodSymbol.params()) {
                                String s = p.name.toString();
                                paramNames.add(s);
                            }
                        }

                        Type genericSignatureType = methodSymbol.type instanceof com.sun.tools.javac.code.Type.ForAll ?
                                ((com.sun.tools.javac.code.Type.ForAll) methodSymbol.type).qtype :
                                methodSymbol.type;

                        List<JavaType.FullyQualified> exceptionTypes = null;
                        if (selectType instanceof Type.MethodType) {
                            Type.MethodType methodType = (Type.MethodType) selectType;
                            if (!methodType.thrown.isEmpty()) {
                                exceptionTypes = new ArrayList<>(methodType.thrown.size());
                                for (com.sun.tools.javac.code.Type exceptionType : methodType.thrown) {
                                    JavaType.FullyQualified javaType = TypeUtils.asFullyQualified(type(exceptionType, stack));
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
                                resolvedDeclaringType = TypeUtils.asFullyQualified(type(methodSymbol.owner.type, stack));
                            } else if (methodSymbol.owner instanceof Symbol.VarSymbol) {
                                resolvedDeclaringType = new JavaType.GenericTypeVariable(null, methodSymbol.owner.name.toString(), null);
                            }
                        }

                        if (resolvedDeclaringType == null) {
                            return null;
                        }

                        List<JavaType.FullyQualified> annotations = null;
                        if (!methodSymbol.getDeclarationAttributes().isEmpty()) {
                            annotations = new ArrayList<>(methodSymbol.getDeclarationAttributes().size());
                            for (Attribute.Compound a : methodSymbol.getDeclarationAttributes()) {
                                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type(a.type, stack));
                                if (fq != null) {
                                    annotations.add(fq);
                                }
                            }
                        }

                        return new JavaType.Method(
                                methodSymbol.flags_field,
                                resolvedDeclaringType,
                                methodName,
                                paramNames,
                                methodSignature(genericSignatureType, stack),
                                methodSignature(selectType, stack),
                                exceptionTypes,
                                annotations
                        );
                    });
        }

        return null;
    }

    @Nullable
    private JavaType.Method.Signature methodSignature(Type signatureType, Map<String, JavaType.Class> stack) {
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
                        JavaType javaType = type(argtype, stack);
                        paramTypes.add(javaType);
                    }
                }
            }

            return new JavaType.Method.Signature(type(mt.restype, stack), paramTypes);
        }
        return null;
    }

    @Nullable
    private String signature(@Nullable com.sun.tools.javac.code.Type type) {
        return signature(type, new HashSet<>());
    }

    @Nullable
    private String signature(@Nullable com.sun.tools.javac.code.Type type, Set<com.sun.tools.javac.code.Type> boundedTypes) {
        if (type == null) {
            return null;
        } else if (type instanceof Type.ClassType) {
            Symbol.ClassSymbol sym = (Symbol.ClassSymbol) type.tsym;
            if (!sym.completer.isTerminal()) {
                completeClassSymbol(sym);
            }
            return sym.className() + (type.isParameterized() ?
                    ((Type.ClassType) type).typarams_field.stream()
                            .map(tp -> signature(tp, boundedTypes))
                            .filter(Objects::nonNull)
                            .collect(joining(",", "<", ">")) :
                    "");
        } else if (type instanceof Type.TypeVar) {
            // Prevent infinite recursion on parameterized types with a super type bound to the original type.
            // Example: `interface Recursive<T> { <U extends Recursive<? super U>> Recursive<T> method() {} }`
            if (boundedTypes.contains(type.getUpperBound())) {
                return type.tsym.name.toString() + " extends java.lang.Object";
            } else {
                boundedTypes.add(type.getUpperBound());
                return type.tsym.name.toString() + " extends " + signature(type.getUpperBound(), boundedTypes);
            }
        } else if (type instanceof Type.JCPrimitiveType) {
            return primitiveType(type.getTag()).getKeyword();
        } else if (type instanceof Type.JCVoidType) {
            return "void";
        } else if (type instanceof Type.ArrayType) {
            return signature(((Type.ArrayType) type).elemtype, boundedTypes) + "[]";
        } else if (type instanceof Type.WildcardType) {
            if (type.isUnbound()) {
                return "java.lang.Object";
            } else {
                return "? extends " + signature(((Type.WildcardType) type).type, boundedTypes);
            }
        } else if (com.sun.tools.javac.code.Type.noType.equals(type)) {
            return null;
        }
        return null;
    }

    private void completeClassSymbol(Symbol.ClassSymbol classSymbol) {
        String packageName = classSymbol.packge().fullname.toString();
        if (!packageName.startsWith("com.sun.") &&
                !packageName.startsWith("jdk.")) {
            try {
                classSymbol.complete();
            } catch (Symbol.CompletionFailure ignore) {
            }
        }
    }

    private Path classfile(com.sun.tools.javac.code.Type type) {
        if (type instanceof Type.ClassType) {
            Type.ClassType classType = (Type.ClassType) type;
            Symbol.ClassSymbol sym = (Symbol.ClassSymbol) type.tsym;
            return getClasspathElement(sym);
        } else if (type instanceof Type.ArrayType) {
            return classfile(((Type.ArrayType) type).elemtype);
        } else if (type instanceof Type.MethodType) {
            return classfile(type.getReceiverType());
        } else if (type instanceof Type.TypeVar) {
            return classfile(type.getUpperBound());
        } else if (type instanceof Type.JCPrimitiveType || type instanceof Type.JCVoidType) {
            //Primitives and void have a null classpath, hard-coding the path.
            return Paths.get("__primitive__");
        } else if (type == Type.noType || type == Type.stuckType) {
            return Paths.get("__does_not_exist__");
        }
        throw new IllegalStateException("Attempted to get a classfile path from a type of " + type.getClass().getName());
    }

    private Path getClasspathElement(Symbol.ClassSymbol sym) {
        if (sym.classfile == null) {
            return Paths.get("__source_set__");
        } else if (sym.classfile.getClass().getSimpleName().equals("JarFileObject")) {
            try {
                String pathWithClass = sym.classfile.toUri().toString();
                return Paths.get(new URI(pathWithClass.substring("jar:".length(), pathWithClass.indexOf('!'))));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return Paths.get(sym.classfile.toUri());
    }
}
