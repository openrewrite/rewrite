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
package org.openrewrite.kotlin;

import org.jetbrains.kotlin.descriptors.ClassKind;
import org.jetbrains.kotlin.fir.FirElement;
import org.jetbrains.kotlin.fir.FirSession;
import org.jetbrains.kotlin.fir.declarations.*;
import org.jetbrains.kotlin.fir.expressions.FirAnnotation;
import org.jetbrains.kotlin.fir.resolve.LookupTagUtilsKt;
import org.jetbrains.kotlin.fir.symbols.impl.*;
import org.jetbrains.kotlin.fir.types.*;
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.StandardClassIds;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTypeMapping;
import org.openrewrite.java.internal.JavaReflectionTypeMapping;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.COVARIANT;
import static org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.INVARIANT;

public class KotlinTypeMapping implements JavaTypeMapping<FirElement> {
    private final KotlinTypeSignatureBuilder signatureBuilder;
    private final JavaTypeCache typeCache;
    private final FirSession firSession;
    private final JavaReflectionTypeMapping reflectionTypeMapping;


    public KotlinTypeMapping(JavaTypeCache typeCache, FirSession firSession) {
        this.signatureBuilder = new KotlinTypeSignatureBuilder(firSession);
        this.typeCache = typeCache;
        this.firSession = firSession;
        this.reflectionTypeMapping = new JavaReflectionTypeMapping(typeCache);
    }

    public JavaType type(@Nullable FirElement type) {
        if (type == null) {
            return JavaType.Class.Unknown.getInstance();
        }

        String signature = signatureBuilder.signature(type);
        JavaType existing = typeCache.get(signature);
        if (existing != null) {
            return existing;
        }

        if (type instanceof FirClass) {
            return classType((FirClass) type, signature);
        } else if (type instanceof FirResolvedTypeRef) {
            ConeKotlinType coneKotlinType = FirTypeUtilsKt.getConeType((FirResolvedTypeRef) type);
            if (coneKotlinType instanceof ConeTypeParameterType) {
                FirClassifierSymbol<?> classifierSymbol = LookupTagUtilsKt.toSymbol(((ConeTypeParameterType) coneKotlinType).getLookupTag(), firSession);
                if (classifierSymbol != null && classifierSymbol.getFir() instanceof FirTypeParameter) {
                    return generic((FirTypeParameter) classifierSymbol.getFir(), signature);
                }
            }
            return resolvedTypeRef((FirResolvedTypeRef) type, signature);
        } else if (type instanceof FirTypeParameter) {
            return generic((FirTypeParameter) type, signature);
        }

        throw new UnsupportedOperationException("Unknown type " + type.getClass().getName());
    }

    private JavaType.FullyQualified classType(FirClass firClass, String signature) {
        FirClassSymbol<? extends FirClass> sym = firClass.getSymbol();

        String classFqn = sym.getClassId().asFqNameString();

        JavaType.FullyQualified fq = typeCache.get(classFqn);
        JavaType.Class clazz = (JavaType.Class) (fq instanceof JavaType.Parameterized ? ((JavaType.Parameterized) fq).getType() : fq);
        if (clazz == null) {
            clazz = new JavaType.Class(
                    null,
                    convertToFlagsBitMap(firClass.getStatus()),
                    classFqn,
                    convertToClassKind(firClass.getClassKind()),
                    null, null, null, null, null, null, null
            );
            typeCache.put(signature, clazz);

            // Assumes super type will always exist at pos 0.
            FirTypeRef superType = firClass.getSuperTypeRefs().isEmpty() ? null : firClass.getSuperTypeRefs().get(0);
            JavaType.FullyQualified supertype = TypeUtils.asFullyQualified(type(superType));

            // TODO: figure out how to access the class owner .. the name exists on the Sym, but there isn't a link through the classId.
            JavaType.FullyQualified owner = null;

            List<FirProperty> properties = new ArrayList<>(firClass.getDeclarations().size());
            List<FirSimpleFunction> functions = new ArrayList<>(firClass.getDeclarations().size());
            for (FirDeclaration declaration : firClass.getDeclarations()) {
                if (declaration instanceof FirProperty) {
                    properties.add((FirProperty) declaration);
                } else if (declaration instanceof FirSimpleFunction) {
                    functions.add((FirSimpleFunction) declaration);
                } else if (declaration instanceof FirConstructor) {
                    // TODO: sort out how to detect generated and declared constructor
                    System.out.println("fix fir class declaration FirConstructor");
                } else if (declaration instanceof FirRegularClass) {
                    // TODO: unsure what to do with this yet.
                    System.out.println("fix fir class declaration FirRegularClass");
                } else if (declaration instanceof FirEnumEntry) {
                    // TODO: unsure what to do with this yet.
                    System.out.println("fix fir class declaration FirEnumEntry");
                } else {
                    throw new IllegalStateException("Implement me.");
                }
            }

            // May be helpful.
//            FirStatusUtilsKt
            List<JavaType.Variable> fields = null;
            if (!properties.isEmpty()) {
                fields = new ArrayList<>(properties.size());
                for (FirProperty property : properties) {
                    // TODO: detect and filter out synthetic
                    fields.add(propertyType(property.getSymbol(), clazz));
                }

            }

            List<JavaType.Method> methods = null;
            if(!functions.isEmpty()) {
                methods = new ArrayList<>(functions.size());
                for (FirSimpleFunction function : functions) {
                    // TODO: detect and filter out synthetic
                    methods.add(functionDeclarationType(function.getSymbol(), clazz));
                }
            }

            List<JavaType.FullyQualified> interfaces = null;
//            if (node.getInterfaces().length > 0) {
//                interfaces = new ArrayList<>(node.getInterfaces().length);
//                for (ClassNode iParam : node.getInterfaces()) {
//                    JavaType.FullyQualified javaType = TypeUtils.asFullyQualified(type(iParam));
//                    if (javaType != null) {
//                        interfaces.add(javaType);
//                    }
//                }
//            }
//
            List<JavaType.FullyQualified> annotations = getAnnotations(firClass.getAnnotations());
//
            clazz.unsafeSet(null, supertype, owner, annotations, interfaces, fields, methods);
        }

        return clazz;
    }

    public JavaType.FullyQualified resolvedTypeRef(FirResolvedTypeRef resolvedTypeRef, String signature) {
        FirRegularClass firRegularClass = convertToRegularClass(resolvedTypeRef.getType());
        if (firRegularClass == null) {
            throw new IllegalStateException("unexpected null symbol");
        }
        return classType(firRegularClass, signature);
    }

    @Nullable
    public JavaType.Method functionDeclarationType(@Nullable FirNamedFunctionSymbol functionSymbol, @Nullable JavaType.FullyQualified declaringType) {
        if (functionSymbol == null) {
            return null;
        }

        String signature = signatureBuilder.methodSignature(functionSymbol);
        JavaType.Method existing = typeCache.get(signature);
        if (existing != null) {
            return existing;
        }

        List<String> paramNames = null;
        if (!functionSymbol.getValueParameterSymbols().isEmpty()) {
            paramNames = new ArrayList<>(functionSymbol.getValueParameterSymbols().size());
            for (FirValueParameterSymbol p : functionSymbol.getValueParameterSymbols()) {
                String s = p.getName().asString();
                paramNames.add(s);
            }
        }
        List<String> defaultValues = null;
//        if (methodSymbol.getDefaultValue() != null) {
//            if (methodSymbol.getDefaultValue() instanceof Attribute.Array) {
//                defaultValues = ((Attribute.Array) methodSymbol.getDefaultValue()).getValue().stream()
//                        .map(attr -> attr.getValue().toString())
//                        .collect(Collectors.toList());
//            } else {
//                defaultValues = Collections.singletonList(methodSymbol.getDefaultValue().getValue().toString());
//            }
//        }
        JavaType.Method method = new JavaType.Method(
                null,
                convertToFlagsBitMap(functionSymbol.getResolvedStatus()),
                null,
//                methodSymbol.isConstructor() ? "<constructor>" :
                        functionSymbol.getName().asString(),
                null,
                paramNames,
                null, null, null,
                defaultValues
        );

        typeCache.put(signature, method);

        FirRegularClass signatureType =
//                functionSymbol.type instanceof Type.ForAll ?
//                ((Type.ForAll) methodSymbol.type).qtype :
                convertToRegularClass(functionSymbol.getDispatchReceiverType());

        List<JavaType.FullyQualified> exceptionTypes = null;

        FirRegularClass selectType = null;
//        Type selectType = functionSymbol.getC.type;
//        if (selectType instanceof Type.ForAll) {
//            selectType = ((Type.ForAll) selectType).qtype;
//        }

//        if (selectType instanceof Type.MethodType) {
//            Type.MethodType methodType = (Type.MethodType) selectType;
//            if (!methodType.thrown.isEmpty()) {
//                exceptionTypes = new ArrayList<>(methodType.thrown.size());
//                for (Type exceptionType : methodType.thrown) {
//                    JavaType.FullyQualified javaType = TypeUtils.asFullyQualified(type(exceptionType));
//                    if (javaType == null) {
//                        // if the type cannot be resolved to a class (it might not be on the classpath, or it might have
//                        // been mapped to cyclic)
//                        if (exceptionType instanceof Type.ClassType) {
//                            Symbol.ClassSymbol sym = (Symbol.ClassSymbol) exceptionType.tsym;
//                            javaType = new JavaType.Class(null, Flag.Public.getBitMask(), sym.flatName().toString(), JavaType.Class.Kind.Class,
//                                    null, null, null, null, null, null, null);
//                        }
//                    }
//                    if (javaType != null) {
//                        // if the exception type is not resolved, it is not added to the list of exceptions
//                        exceptionTypes.add(javaType);
//                    }
//                }
//            }
//        }

        JavaType.FullyQualified resolvedDeclaringType = declaringType;
//        if (declaringType == null) {
//            if (methodSymbol.owner instanceof Symbol.ClassSymbol || methodSymbol.owner instanceof Symbol.TypeVariableSymbol) {
//                resolvedDeclaringType = TypeUtils.asFullyQualified(type(methodSymbol.owner.type));
//            }
//        }

        if (resolvedDeclaringType == null) {
            return null;
        }

        JavaType returnType = type(functionSymbol.getResolvedReturnTypeRef());
        List<JavaType> parameterTypes = null;
//
//        if (signatureType instanceof Type.ForAll) {
//            signatureType = ((Type.ForAll) signatureType).qtype;
//        }
//        if (signatureType instanceof Type.MethodType) {
//            Type.MethodType mt = (Type.MethodType) signatureType;
//
//            if (!mt.argtypes.isEmpty()) {
//                parameterTypes = new ArrayList<>(mt.argtypes.size());
//                for (com.sun.tools.javac.code.Type argtype : mt.argtypes) {
//                    if (argtype != null) {
//                        JavaType javaType = type(argtype);
//                        parameterTypes.add(javaType);
//                    }
//                }
//            }
//
//            returnType = type(mt.restype);
//        } else {
//            throw new UnsupportedOperationException("Unexpected method signature type" + signatureType.getClass().getName());
//        }

        method.unsafeSet(resolvedDeclaringType,
//                methodSymbol.isConstructor() ? resolvedDeclaringType :
                        returnType,
                parameterTypes, exceptionTypes, getAnnotations(functionSymbol.getAnnotations()));
        return method;
    }

    @Nullable
    public JavaType.Variable propertyType(@Nullable FirPropertySymbol symbol) {
        return propertyType(symbol, null);
    }

    @Nullable
    public JavaType.Variable propertyType(@Nullable FirPropertySymbol symbol, @Nullable JavaType.FullyQualified owner) {
        if (symbol == null) {
            return null;
        }

        String signature = signatureBuilder.propertySignature(symbol);
        JavaType.Variable existing = typeCache.get(signature);
        if (existing != null) {
            return existing;
        }

        JavaType.Variable variable = new JavaType.Variable(
                null,
                convertToFlagsBitMap(symbol.getRawStatus()),
                symbol.getName().asString(),
                null, null, null);

        typeCache.put(signature, variable);

        JavaType resolvedOwner = owner;
        if (owner == null) {
            throw new IllegalStateException("implement me.");
        }

        List<JavaType.FullyQualified> annotations = getAnnotations(symbol.getAnnotations());

        variable.unsafeSet(resolvedOwner, type(symbol.getResolvedReturnTypeRef()), annotations);

        return variable;
    }

    public JavaType.Primitive primitive(ConeClassLikeType type) {
        ClassId classId = type.getLookupTag().getClassId();
        if (StandardClassIds.INSTANCE.getByte().equals(classId)) {
            return JavaType.Primitive.Byte;
        } else if (StandardClassIds.INSTANCE.getBoolean().equals(classId)) {
            return JavaType.Primitive.Boolean;
        } else if (StandardClassIds.INSTANCE.getChar().equals(classId)) {
            return JavaType.Primitive.Char;
        } else if (StandardClassIds.INSTANCE.getDouble().equals(classId)) {
            return JavaType.Primitive.Double;
        } else if (StandardClassIds.INSTANCE.getFloat().equals(classId)) {
            return JavaType.Primitive.Float;
        } else if (StandardClassIds.INSTANCE.getInt().equals(classId)) {
            return JavaType.Primitive.Int;
        } else if (StandardClassIds.INSTANCE.getLong().equals(classId)) {
            return JavaType.Primitive.Long;
        } else if (StandardClassIds.INSTANCE.getShort().equals(classId)) {
            return JavaType.Primitive.Short;
        } else if (StandardClassIds.INSTANCE.getString().equals(classId)) {
            return JavaType.Primitive.String;
        } else if (StandardClassIds.INSTANCE.getUnit().equals(classId)) {
            return JavaType.Primitive.Void;
        } else if (StandardClassIds.INSTANCE.getNothing().equals(classId)) {
            return JavaType.Primitive.Null;
        }

        throw new UnsupportedOperationException("Unknown primitive type " + type);
    }

    private JavaType generic(FirTypeParameter typeParameter, String signature) {
        String name = typeParameter.getName().asString();
        JavaType.GenericTypeVariable gtv = new JavaType.GenericTypeVariable(null,
                name, INVARIANT, null);
        typeCache.put(signature, gtv);

        List<JavaType> bounds = null;
        JavaType.GenericTypeVariable.Variance variance = null;
        if (!"INVARIANT".equals(typeParameter.getVariance().name())) {
            bounds = new ArrayList<>(typeParameter.getBounds().size());
            for (FirTypeRef bound : typeParameter.getBounds()) {
                if (!(bound instanceof FirImplicitNullableAnyTypeRef)) {
                    bounds.add(type(bound));
                }
            }
            variance = COVARIANT;
        } else {
            variance = INVARIANT;
        }

        gtv.unsafeSet(variance, bounds);
        return gtv;
    }

    private long convertToFlagsBitMap(FirDeclarationStatus status) {
        // TODO ... map status to eq flags.
        return 0;
    }

    private JavaType.FullyQualified.Kind convertToClassKind(ClassKind classKind) {
        JavaType.FullyQualified.Kind kind;
        if (ClassKind.CLASS == classKind) {
            kind = JavaType.FullyQualified.Kind.Class;
        } else if (ClassKind.ANNOTATION_CLASS == classKind) {
            kind = JavaType.FullyQualified.Kind.Annotation;
        } else if (ClassKind.ENUM_CLASS == classKind) {
            kind = JavaType.FullyQualified.Kind.Enum;
        } else if (ClassKind.INTERFACE == classKind) {
            kind = JavaType.FullyQualified.Kind.Interface;
        } else if (ClassKind.OBJECT == classKind) {
            // TODO: fix me ... public object Name
            /*
                public object Unit {
                    override fun toString() = "kotlin.Unit"
                }
             */
            kind = JavaType.FullyQualified.Kind.Class;
        } else {
            throw new UnsupportedOperationException("Unexpected classKind: " + classKind.name());
        }

        return kind;
    }

    private List<JavaType.FullyQualified> getAnnotations(List<FirAnnotation> firAnnotations) {
        List<JavaType.FullyQualified> annotations = new ArrayList<>(firAnnotations.size());
        for (FirAnnotation firAnnotation : firAnnotations) {
            JavaType.FullyQualified fullyQualified = (JavaType.FullyQualified) type(convertToRegularClass(firAnnotation.getTypeRef()));
            annotations.add(fullyQualified);
        }

        return annotations;
    }

    // This might exist somewhere among Kotlin's many utils.
    @Nullable
    public FirRegularClass convertToRegularClass(@Nullable ConeKotlinType kotlinType) {
        if (kotlinType != null) {
            FirRegularClassSymbol symbol = TypeUtilsKt.toRegularClassSymbol(kotlinType, firSession);
            if (symbol != null) {
                return symbol.getFir();
            }
        }

        return null;
    }

    @Nullable
    public FirRegularClass convertToRegularClass(@Nullable FirTypeRef firTypeRef) {
        if (firTypeRef != null) {
            ConeKotlinType coneKotlinType = FirTypeUtilsKt.getConeType(firTypeRef);
            return convertToRegularClass(coneKotlinType);
        }

        return null;
    }
}
