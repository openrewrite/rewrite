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
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Collections.emptyList;

@RequiredArgsConstructor
class TypeMapping {
    private static final int KIND_BITMASK_INTERFACE = 1 << 9;
    private static final int KIND_BITMASK_ANNOTATION = 1 << 13;
    private static final int KIND_BITMASK_ENUM = 1 << 14;

    private final boolean relaxedClassTypeMatching;
    private final Map<String, JavaType.Class> sharedClassTypes;

    @Nullable
    public JavaType type(@Nullable com.sun.tools.javac.code.Type type) {
        return type(type, emptyList());
    }

    @Nullable
    public JavaType type(@Nullable com.sun.tools.javac.code.Type type, List<Symbol> stack) {
        // Word of caution, during attribution, we will likely encounter symbols that have been parsed but are not
        // on the parser's classpath. Calling a method on the symbol that calls complete() will result in an exception
        // being thrown. That is why this method uses the symbol's underlying fields directly vs the accessor methods.
        if (type instanceof Type.ClassType) {
            if (type instanceof com.sun.tools.javac.code.Type.ErrorType) {
                return null;
            }

            Type.ClassType classType = (Type.ClassType) type;
            Symbol.ClassSymbol sym = (Symbol.ClassSymbol) type.tsym;
            Type.ClassType symType = (Type.ClassType) sym.type;

            if (stack.contains(sym)) {
                return new JavaType.Cyclic(sym.className());
            } else {
                JavaType.Class clazz = sharedClassTypes.get(sym.className());
                List<Symbol> stackWithSym = new ArrayList<>(stack);
                stackWithSym.add(sym);
                if (clazz == null) {

                    List<JavaType.Variable> fields;
                    List<JavaType.Method> methods;
                    if (sym.members_field == null) {
                        fields = emptyList();
                        methods = emptyList();
                    } else {
                        fields = new ArrayList<>();
                        methods = new ArrayList<>();
                        for (Symbol elem : sym.members_field.getSymbols()) {
                            if (elem instanceof Symbol.VarSymbol) {
                                fields.add(JavaType.Variable.build(
                                        elem.name.toString(),
                                        type(elem.type, stackWithSym),
                                        // currently only the first 16 bits are meaningful
                                        (int) elem.flags_field & 0xFFFF
                                ));
                            } else if (elem instanceof Symbol.MethodSymbol) {
                                methods.add(methodType(elem.type, elem, elem.getSimpleName().toString(), stackWithSym));
                            }
                        }
                    }

                    List<JavaType.FullyQualified> interfaces;
                    if (symType.interfaces_field == null) {
                        interfaces = emptyList();
                    } else {
                        interfaces = new ArrayList<>(symType.interfaces_field.length());
                        for (com.sun.tools.javac.code.Type iParam : symType.interfaces_field) {
                            JavaType.FullyQualified javaType = TypeUtils.asFullyQualified(type(iParam, stackWithSym));
                            if (javaType != null) {
                                interfaces.add(javaType);
                            }
                        }
                    }
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

                    JavaType.FullyQualified owner = null;
                    if (sym.owner instanceof Symbol.ClassSymbol) {
                        owner = TypeUtils.asFullyQualified(type(sym.owner.type, stackWithSym));
                    }

                    List<JavaType.FullyQualified> annotations = emptyList();
                    if (!sym.getDeclarationAttributes().isEmpty()) {
                        annotations = new ArrayList<>(sym.getDeclarationAttributes().size());
                        for (Attribute.Compound a : sym.getDeclarationAttributes()) {
                            JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type(a.type, stackWithSym));
                            if (fq != null) {
                                annotations.add(fq);
                            }
                        }
                    }

                    clazz = JavaType.Class.build(
                            //Currently only the first 16 bits are meaninful
                            (int) sym.flags_field & 0xFFFF,
                            sym.className(),
                            kind,
                            fields,
                            interfaces,
                            methods,
                            TypeUtils.asFullyQualified(type(classType.supertype_field, stackWithSym)),
                            owner,
                            annotations,
                            relaxedClassTypeMatching);
                    sharedClassTypes.put(clazz.getFullyQualifiedName(), clazz);
                }

                List<JavaType> typeParameters;
                if (classType.typarams_field == null) {
                    typeParameters = emptyList();
                } else {
                    typeParameters = new ArrayList<>(classType.typarams_field.length());
                    for (com.sun.tools.javac.code.Type tParam : classType.typarams_field) {
                        JavaType javaType = type(tParam, stackWithSym);
                        if (javaType != null) {
                            typeParameters.add(javaType);
                        }
                    }
                }

                if (!typeParameters.isEmpty()) {
                    return JavaType.Parameterized.build(clazz, typeParameters);
                } else {
                    return clazz;
                }
            }
        } else if (type instanceof Type.TypeVar) {
            return new JavaType.GenericTypeVariable(type.tsym.name.toString(),
                    TypeUtils.asFullyQualified(type(type.getUpperBound(), stack)));
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
                return JavaType.Class.OBJECT;
            } else {
                return type(wildcard.type, stack);
            }
        } else if (com.sun.tools.javac.code.Type.noType.equals(type)) {
            return null;
        } else {
            return null;
        }
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
    public JavaType.Method methodType(@Nullable com.sun.tools.javac.code.Type selectType, @Nullable Symbol symbol, String methodName, List<Symbol> stack) {
        // if the symbol is not a method symbol, there is a parser error in play
        Symbol.MethodSymbol methodSymbol = symbol instanceof Symbol.MethodSymbol ? (Symbol.MethodSymbol) symbol : null;

        if (methodSymbol != null && selectType != null) {
            Function<Type, JavaType.Method.Signature> signature = t -> {
                if (t instanceof Type.MethodType) {
                    Type.MethodType mt = (Type.MethodType) t;

                    List<JavaType> paramTypes = new ArrayList<>();
                    for (com.sun.tools.javac.code.Type argtype : mt.argtypes) {
                        if (argtype != null) {
                            JavaType javaType = type(argtype, stack);
                            paramTypes.add(javaType);
                        }
                    }

                    return new JavaType.Method.Signature(type(mt.restype, stack), paramTypes);
                }
                return null;
            };

            JavaType.Method.Signature genericSignature;
            if (methodSymbol.type instanceof com.sun.tools.javac.code.Type.ForAll) {
                genericSignature = signature.apply(((com.sun.tools.javac.code.Type.ForAll) methodSymbol.type).qtype);
            } else {
                genericSignature = signature.apply(methodSymbol.type);
            }

            List<String> paramNames = new ArrayList<>();
            for (Symbol.VarSymbol p : methodSymbol.params()) {
                String s = p.name.toString();
                paramNames.add(s);
            }

            List<JavaType.FullyQualified> exceptionTypes = new ArrayList<>();
            if (selectType instanceof Type.MethodType) {
                for (com.sun.tools.javac.code.Type exceptionType : ((Type.MethodType) selectType).thrown) {
                    JavaType.FullyQualified javaType = TypeUtils.asFullyQualified(type(exceptionType, stack));
                    if (javaType == null) {
                        //If the type cannot be resolved to a class (it might not be on the classpath or it might have
                        //been mapped to cyclic, build the class.
                        if (exceptionType instanceof Type.ClassType) {
                            Symbol.ClassSymbol sym = (Symbol.ClassSymbol) exceptionType.tsym;
                            javaType = JavaType.Class.build(sym.className());
                        }
                    }
                    if (javaType != null) {
                        //If the exception type is not resolved, it is not added to the list of exceptions.
                        exceptionTypes.add(javaType);
                    }
                }
            }

            JavaType.FullyQualified declaringType = null;
            if (methodSymbol.owner instanceof Symbol.ClassSymbol || methodSymbol.owner instanceof Symbol.TypeVariableSymbol) {
                declaringType = TypeUtils.asFullyQualified(type(methodSymbol.owner.type, stack));
            } else if (methodSymbol.owner instanceof Symbol.VarSymbol) {
                declaringType = new JavaType.GenericTypeVariable(methodSymbol.owner.name.toString(), null);
            }

            if (declaringType == null) {
                return null;
            }

            List<JavaType.FullyQualified> annotations = emptyList();
            if (!methodSymbol.getDeclarationAttributes().isEmpty()) {
                annotations = new ArrayList<>(methodSymbol.getDeclarationAttributes().size());
                for (Attribute.Compound a : methodSymbol.getDeclarationAttributes()) {
                    JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type(a.type, stack));
                    if (fq != null) {
                        annotations.add(fq);
                    }
                }
            }

            return JavaType.Method.build(
                    // currently only the first 16 bits are meaningful
                    (int) methodSymbol.flags_field & 0xFFFF,
                    declaringType,
                    methodName,
                    genericSignature,
                    signature.apply(selectType),
                    paramNames,
                    Collections.unmodifiableList(exceptionTypes),
                    annotations
            );
        }

        return null;
    }
}
