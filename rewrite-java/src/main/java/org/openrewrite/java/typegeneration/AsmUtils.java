/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.typegeneration;

import org.openrewrite.Incubating;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.stream.Collectors;

/**
 * A set of utility methods to convert Rewrite's JavaType.* into their ASM equivalent descriptors/signatures
 *
 * All of the methods in this class are configured to return null for a descriptor/signature if any type information
 * is missing.
 *
 * <PRE>
 * The grammar for the ASM signatures:
 *
 *   ClassSignature     : TypeParams? ClassTypeSignature ClassTypeSignature*
 *   MethodSignature    : TypeParams? "(" TypeSignature* ")" (TypeSignature | V) Exception
 *   TypeSignature      : Z|C|B|S|I|F|J|D|FieldTypeSignature
 *   TypeParams         : "<" TypeParam+ ">"
 *   TypeParam          : Id ":" FieldTypeSignature? ( ":" FieldTypeSignature )*
 *   FieldTypeSignature : ClassTypeSignature | "[" TypeSignature | TypeVar
 *   ClassTypeSignature : "L" Id ( / Id )* TypeArgs? ( . Id TypeArgs? )* ";"
 *   TypeArgs           : "<" TypeArg+ ">"
 *   TypeArg            : * | (+|-) FieldTypeSignature
 *   TypeVar            : T id ";"
 * </PRE>
 */
@Incubating(since="7.5.0")
public class AsmUtils {

    /**
     * This resolves a type to its "simple" asm descriptor form. This handles primitives, arrays and classes (but the classes will
     * not include any type arguments.
     */
    @Nullable
    public static String getAsmDescriptor(@Nullable JavaType type) {

        String signature = null;
        if (type instanceof JavaType.Primitive) {
            //Primitive Z|C|B|S|I|F|J|D
            signature = getPrimitiveAsm((JavaType.Primitive) type);
        } else if (type instanceof JavaType.GenericTypeVariable) {
            // TypeVar
            JavaType.GenericTypeVariable genericType = (JavaType.GenericTypeVariable) type;
            signature = getAsmDescriptor(genericType.getBound());
        } else if (type instanceof JavaType.Array) {
            // "[" AsmDescriptor
            String classTypeSig = getAsmDescriptor(((JavaType.Array) type).getElemType());
            signature = classTypeSig != null ? ("[" + classTypeSig) : null;
        } else if (type instanceof JavaType.FullyQualified) {
            signature = "L" + getAsmName((JavaType.FullyQualified) type) + ";";
        }
        return signature;
    }

    /**
     * This is used for producing the signature for a class declaration.
     *
     * TypeParams? ClassTypeSignature ClassTypeSignature*
     *
     * The first ClassTypeSignature is that of the super class and the following ClassTypeSignatures are each interface
     * implemented by the class.
     *
     * @param type A fully qualified type that will be resolved to a JavaType.Class.
     * @return ASM signature for the class declaration or null if there is any missing type information.
     */
    @Nullable
    public static String getClassSignature(@Nullable JavaType.FullyQualified type) {
        JavaType.Class classType = TypeUtils.asClass(type);
        if (classType == null) {
            return null;
        }
        StringBuilder signature = new StringBuilder();

        //TypeParams?
        if (classType.getTypeParameters() != null && !classType.getTypeParameters().isEmpty()) {
            signature.append("<");
            for (JavaType typeParameter :  classType.getTypeParameters()) {
                if (typeParameter instanceof JavaType.GenericTypeVariable) {
                    JavaType.GenericTypeVariable generic = (JavaType.GenericTypeVariable) typeParameter;
                    String boundSignature = getFieldTypeSignature(generic.getBound());
                    if (boundSignature == null) {
                        return null;
                    }
                    return generic.getFullyQualifiedName() + "::" + ((generic.getBound() == null) ? "Ljava/lang/Object;" : boundSignature);
                } else {
                    return null;
                }
            }
            signature.append(">");
        }

        //Super ClassTypeSignature
        String superSignature = getClassTypeSignature(classType.getSupertype() != null ? classType.getSupertype() : JavaType.Class.OBJECT);
        if (superSignature == null) {
            return null;
        }
        signature.append(superSignature);

        //Interfaces *ClassTypeSignature
        if (classType.getInterfaces() != null && !classType.getInterfaces().isEmpty()) {
            for (JavaType interfaceType : classType.getInterfaces()) {
                String interfaceSig = getClassTypeSignature(TypeUtils.asFullyQualified(interfaceType));
                if (interfaceSig == null) {
                    return null;
                }
                signature.append(interfaceSig);
            }
        }
        return signature.toString();
    }

    /**
     * This is used for generating an ASM method signature from a JavaType.Method.
     *
     * <method.typeParameters>(method.parameters)method.returnType^method.thrownExceptions
     *
     * @param method Rewrite's method type
     * @return The asm signature for the method type or null if there is any missing type information.
     */
    @Nullable
    public static String getMethodTypeSignature(@Nullable JavaType.Method method) {

        //TypeParams? "(" TypeSignature* ")" (TypeSignature | V) Exception

        if (method == null || (method.getGenericSignature() == null && method.getResolvedSignature() == null)) {
            return null;
        }
        JavaType.Method.Signature methodSignature = method.getResolvedSignature() != null ? method.getResolvedSignature() : method.getGenericSignature();
        StringBuilder signature = new StringBuilder();

        //TypeParams?
        if (method.getTypeParameters() != null && !method.getTypeParameters().isEmpty()) {
            signature.append("<");
            for (JavaType typeParameter :  method.getTypeParameters()) {
                if (typeParameter instanceof JavaType.GenericTypeVariable) {
                    JavaType.GenericTypeVariable generic = (JavaType.GenericTypeVariable) typeParameter;
                    String boundSignature = getFieldTypeSignature(generic.getBound());
                    if (boundSignature == null) {
                        return null;
                    }
                    signature.append(generic.getFullyQualifiedName())
                            .append("::")
                            .append((generic.getBound() == null) ? "Ljava/lang/Object;" : boundSignature);
                } else {
                    return null;
                }
            }
            signature.append(">");
        }
        signature.append('(');
        //Method Parameters : TypeSignature*
        for (JavaType methodParameterType : methodSignature.getParamTypes()) {
            String methodParameterSig = getTypeSignature(methodParameterType);
            if (methodParameterSig == null) {
                return null;
            }
            signature.append(methodParameterSig);
        }
        signature.append(')');

        //Return Type can be either : TypeSignature | V
        if (method.getName().equals("<init>") || methodSignature.getReturnType() == null) {
            signature.append("V");
        } else {
            String typeSig = getTypeSignature(methodSignature.getReturnType());
            if (typeSig == null) {
                return null;
            }
            signature.append(typeSig);
        }

        //Exceptions* "^" ( ClassTypeSignature | TypeVar)
        if (method.getThrownExceptions() != null && !method.getThrownExceptions().isEmpty()) {
            for (JavaType.FullyQualified exceptionType : method.getThrownExceptions()) {
                signature.append('^');
                if (exceptionType instanceof JavaType.GenericTypeVariable) {
                    signature.append('T').append(exceptionType.getFullyQualifiedName()).append(';');
                } else {
                    String exceptionSignature = getClassTypeSignature(exceptionType);
                    if (exceptionSignature == null) {
                        return null;
                    }
                    signature.append(exceptionSignature);
                }
            }
        }
        return signature.toString();
    }

    /**
     * This returns the type signature for the given rewrite type.
     *
     * <PRE>
     *     GRAMMAR for TypeSignature:
     *
     *     Z|C|B|S|I|F|J|D|FieldTypeSignature
     *
     *   FieldTypeSignature : ClassTypeSignature | "[" TypeSignature | TypeVar
     *   ClassTypeSignature : "L" Id ( / Id )* TypeArgs? ( . Id TypeArgs? )* ";"
     *   TypeArgs           : "<" TypeArg+ ">"
     *   TypeArg            : * | (+|-) FieldTypeSignature
     *   TypeVar            : T id ";"
     *
     * </PRE>
     *
     * @param type Rewrite type
     * @return The ASM type signature or null if there is any missing type information.
     */
    @Nullable
    public static String getTypeSignature(@Nullable JavaType type) {
        //TypeSignature Z|C|B|S|I|F|J|D|FieldTypeSignature
        if (type instanceof JavaType.Primitive) {
            //Primitive Z|C|B|S|I|F|J|D
            return getPrimitiveAsm((JavaType.Primitive) type);
        } else {
            return getFieldTypeSignature(type);
        }
    }

    /**
     * This returns the field type signature for the given rewrite type.
     *
     * <PRE>
     *      GRAMMAR for FeildTypeSignature:
     *
     *      ClassTypeSignature | "[" TypeSignature | TypeVar
     *
     *   TypeSignature      : Z|C|B|S|I|F|J|D|FieldTypeSignature
     *   ClassTypeSignature : "L" Id ( / Id )* TypeArgs? ( . Id TypeArgs? )* ";"
     *   TypeArgs           : "<" TypeArg+ ">"
     *   TypeArg            : * | (+|-) FieldTypeSignature
     *   TypeVar            : T id ";"
     *
     * </PRE>
     *
     * @param type Rewrite type
     * @return The ASM field type signature or null if there is any missing type information.
     */
    @Nullable
    public static String getFieldTypeSignature(@Nullable JavaType type) {

        if (type instanceof JavaType.GenericTypeVariable) {
            // TypeVar
            return "T" + ((JavaType.GenericTypeVariable) type).getFullyQualifiedName() + ";";
        } else if (type instanceof JavaType.Array) {
            // "[TypeSignature"
            String signature = getTypeSignature(((JavaType.Array) type).getElemType());
            return signature != null ? ("[" + signature) : null;
        } else if (type instanceof JavaType.FullyQualified) {
            //ClassTypeSignature
            return getClassTypeSignature((JavaType.FullyQualified) type);
        } else {
            return null;
        }
    }

    /**
     * This returns the class type signature for the given fully qualified type.
     *
     * <PRE>
     *      GRAMMAR for ClassTypeSignature:
     *
     *   ClassTypeSignature : "L" Id ( / Id )* TypeArgs? ( . Id TypeArgs? )* ";"
     *   TypeArgs           : "<" TypeArg+ ">"
     *   TypeArg            : * | (+|-) FieldTypeSignature
     *
     * </PRE>
     *
     * @param type Rewrite type
     * @return The ASM field type signature or null if there is any missing type information.
     */
    @Nullable
    public static String getClassTypeSignature(@Nullable JavaType.FullyQualified type) {
        if (type == null) {
            return null;
        }
        JavaType.Class classType = JavaType.Class.build(type.getFullyQualifiedName());
        StringBuilder signature = new StringBuilder();
        signature.append("L").append(getAsmName(classType));
        if (classType.getTypeParameters() != null && !classType.getTypeParameters().isEmpty()) {
            signature.append("<");
            for (JavaType typeParameter : classType.getTypeParameters()) {
                if (typeParameter instanceof JavaType.Wildcard) {
                    JavaType.Wildcard wildcardType = (JavaType.Wildcard) typeParameter;
                    String boundSignature;
                    switch (wildcardType.getKind()) {
                        case Extends:
                            boundSignature = getFieldTypeSignature(wildcardType.getType());
                            if (boundSignature == null) {
                                return null;
                            }
                            signature.append('+').append(boundSignature);
                            break;
                        case Super:
                            boundSignature = getFieldTypeSignature(wildcardType.getType());
                            if (boundSignature == null) {
                                return null;
                            }
                            signature.append('-').append(boundSignature);
                            break;
                        case Unbound:
                            signature.append('*');
                            break;
                    }
                } else {
                    String parameterSignature = getFieldTypeSignature(typeParameter);
                    if (parameterSignature == null) {
                        return null;
                    }
                    signature.append(parameterSignature);
                }
            }
            signature.append(">");
        }
        return signature.append(";").toString();
    }

    /**
     * @param type Fully qualified type
     * @return ASM name
     */
    @Nullable
    public static String getAsmName(@Nullable JavaType.FullyQualified type) {
        if (type == null) {
            return null;
        }
        JavaType.Class javaClass = TypeUtils.asClass(type);
        if (javaClass == null || javaClass.getOwningClass() == null) {
            return type.getFullyQualifiedName().replace(".", "/");
        } else {
            return type.getPackageName().replace(".", "/") + "/" + javaClass.getClassName().replace(".", "$");
        }
    }

    /**
     * Map a primitive to its ASM representation
     */
    private static String getPrimitiveAsm(JavaType.Primitive primitive) {
        switch (primitive) {
            case Boolean:
                return "Z";
            case Byte:
                return "B";
            case Char:
                return "C";
            case Double:
                return "D";
            case Float:
                return "F";
            case Int:
                return "I";
            case Long:
                return "J";
            case Short:
                return "S";
            case Void:
                return "V";
            case String:
                return "Ljava/lang/String;";
            default:
                throw new IllegalArgumentException("Primitive cannot be converted to ASM Reference");
        }
    }



}
