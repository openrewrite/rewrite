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

import org.jetbrains.kotlin.KtFakeSourceElementKind;
import org.jetbrains.kotlin.descriptors.ClassKind;
import org.jetbrains.kotlin.descriptors.Modality;
import org.jetbrains.kotlin.descriptors.Visibility;
import org.jetbrains.kotlin.fir.FirSession;
import org.jetbrains.kotlin.fir.declarations.*;
import org.jetbrains.kotlin.fir.expressions.*;
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference;
import org.jetbrains.kotlin.fir.resolve.LookupTagUtilsKt;
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol;
import org.jetbrains.kotlin.fir.symbols.impl.*;
import org.jetbrains.kotlin.fir.types.*;
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.StandardClassIds;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTypeMapping;
import org.openrewrite.java.internal.JavaReflectionTypeMapping;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.*;
import static org.openrewrite.kotlin.KotlinTypeSignatureBuilder.convertClassIdToFqn;

public class KotlinTypeMapping implements JavaTypeMapping<Object> {
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

    @SuppressWarnings("ConstantConditions")
    public JavaType type(@Nullable Object type) {
        if (type == null) {
            return JavaType.Class.Unknown.getInstance();
        }

        String signature = signatureBuilder.signature(type);
        JavaType existing = typeCache.get(signature);
        if (existing != null) {
            return existing;
        }

        if (type instanceof FirClass) {
            return classType(type, signature);
        } else if (type instanceof FirFunction) {
            return methodDeclarationType((FirFunction) type, null);
        } else if (type instanceof FirVariable) {
            return variableType((FirVariable) type, signature);
        }

        return resolveType(type, signature);
    }

    private JavaType resolveType(Object type, String signature) {
        if (type instanceof ConeTypeProjection) {
            return generic((ConeTypeProjection) type, signature);
        }  else if (type instanceof FirConstExpression) {
            return type(((FirConstExpression<?>) type).getTypeRef());
        } else if (type instanceof FirEqualityOperatorCall) {
            return type(((FirEqualityOperatorCall) type).getTypeRef());
        }  else if (type instanceof FirFunctionTypeRef) {
            return type(((FirFunctionTypeRef) type).getReturnTypeRef());
        } else if (type instanceof FirNamedArgumentExpression) {
            return type(((FirNamedArgumentExpression) type).getTypeRef());
        } else if (type instanceof FirLambdaArgumentExpression) {
            return type(((FirLambdaArgumentExpression) type).getTypeRef());
        } else if (type instanceof FirResolvedNamedReference) {
            FirBasedSymbol<?> resolvedSymbol = ((FirResolvedNamedReference) type).getResolvedSymbol();
            if (resolvedSymbol instanceof FirConstructorSymbol) {
                return type(((FirConstructorSymbol) resolvedSymbol).getResolvedReturnTypeRef());
            } else if (resolvedSymbol instanceof FirEnumEntrySymbol) {
                return type(((FirEnumEntrySymbol) resolvedSymbol).getResolvedReturnTypeRef());
            } else if (resolvedSymbol instanceof FirNamedFunctionSymbol) {
                return type(((FirNamedFunctionSymbol) resolvedSymbol).getResolvedReturnTypeRef());
            } else if (resolvedSymbol instanceof FirPropertySymbol) {
                return type(((FirPropertySymbol) resolvedSymbol).getResolvedReturnTypeRef());
            } else if (resolvedSymbol instanceof FirValueParameterSymbol) {
                return type(((FirValueParameterSymbol) resolvedSymbol).getResolvedReturnType());
            } else {
                throw new UnsupportedOperationException("Unknown type " + type.getClass().getName());
            }
        } else if (type instanceof FirResolvedTypeRef) {
            ConeKotlinType coneKotlinType = FirTypeUtilsKt.getConeType((FirResolvedTypeRef) type);
            if (coneKotlinType instanceof ConeTypeParameterType) {
                FirClassifierSymbol<?> classifierSymbol = LookupTagUtilsKt.toSymbol(((ConeTypeParameterType) coneKotlinType).getLookupTag(), firSession);
                if (classifierSymbol != null && classifierSymbol.getFir() instanceof FirTypeParameter) {
                    return generic((FirTypeParameter) classifierSymbol.getFir(), signature);
                }
            }
            return classType(type, signature);
        } else if (type instanceof FirTypeParameter) {
            return generic((FirTypeParameter) type, signature);
        } else if (type instanceof FirVariableAssignment) {
            return type(((FirVariableAssignment) type).getCalleeReference());
        } else if (type instanceof FirQualifiedAccessExpression) {
            return type(((FirQualifiedAccessExpression) type).getTypeRef());
        }

        throw new UnsupportedOperationException("Unknown type " + type.getClass().getName());
    }

    @Nullable
    public JavaType.Variable variableType(@Nullable FirVariable variable, @Nullable String signature) {
        return variable == null ? null : variableType(variable.getSymbol());
    }

    private JavaType.FullyQualified classType(Object classType, String signature) {
        FirClass firClass;
        FirResolvedTypeRef resolvedTypeRef = null;
        if (classType instanceof FirResolvedTypeRef) {
            resolvedTypeRef = (FirResolvedTypeRef) classType;
            FirRegularClassSymbol symbol = TypeUtilsKt.toRegularClassSymbol(resolvedTypeRef.getType(), firSession);
            if (symbol == null) {
                throw new UnsupportedOperationException("Symbol was not resolved for " + resolvedTypeRef.getType());
            }
            firClass = symbol.getFir();
        } else {
            firClass = (FirClass) classType;
        }
        FirClassSymbol<? extends FirClass> sym = firClass.getSymbol();

        String classFqn = convertClassIdToFqn(sym.getClassId());

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
            typeCache.put(classFqn, clazz);

            FirTypeRef superTypeRef = null;
            List<FirTypeRef> interfaceTypeRefs = null;
            for (FirTypeRef typeRef : firClass.getSuperTypeRefs()) {
                FirRegularClassSymbol symbol = TypeUtilsKt.toRegularClassSymbol(FirTypeUtilsKt.getConeType(typeRef), firSession);
                if (symbol != null && ClassKind.CLASS == symbol.getFir().getClassKind()) {
                    superTypeRef = typeRef;
                } else if (symbol != null && ClassKind.INTERFACE == symbol.getFir().getClassKind()) {
                    if (interfaceTypeRefs == null) {
                        interfaceTypeRefs = new ArrayList<>();
                    }
                    interfaceTypeRefs.add(typeRef);
                }
            }

            JavaType.FullyQualified supertype = superTypeRef == null ? null : TypeUtils.asFullyQualified(type(superTypeRef));

            // TODO: figure out how to access the class owner .. the name exists on the Sym, but there isn't a link through the classId.
            JavaType.FullyQualified owner = null;

            List<FirProperty> properties = new ArrayList<>(firClass.getDeclarations().size());
            List<FirFunction> functions = new ArrayList<>(firClass.getDeclarations().size());
            List<FirEnumEntry> enumEntries = new ArrayList<>(firClass.getDeclarations().size());

            for (FirDeclaration declaration : firClass.getDeclarations()) {
                if (declaration instanceof FirProperty) {
                    if (declaration.getSource() == null || !(declaration.getSource().getKind() instanceof KtFakeSourceElementKind)) {
                        properties.add((FirProperty) declaration);
                    }
                } else if (declaration instanceof FirSimpleFunction) {
                    functions.add((FirFunction) declaration);
                } else if (declaration instanceof FirConstructor) {
                    functions.add((FirFunction) declaration);
                } else if (declaration instanceof FirRegularClass) {
                    // Skipped since inner classes don't exist on the JavaType$Class.
                } else if (declaration instanceof FirEnumEntry) {
                    enumEntries.add((FirEnumEntry) declaration);
                } else {
                    throw new IllegalStateException("Implement me.");
                }
            }

            List<JavaType.Variable> fields = null;
            if (!enumEntries.isEmpty()) {
                fields = new ArrayList<>(properties.size() + enumEntries.size());
                for (FirEnumEntry enumEntry : enumEntries) {
                    fields.add(variableType(enumEntry.getSymbol(), clazz));
                }
            }

            if (!properties.isEmpty()) {
                if (fields == null) {
                    fields = new ArrayList<>(properties.size());
                }

                for (FirProperty property : properties) {
                    fields.add(variableType(property.getSymbol(), clazz));
                }
            }

            List<JavaType.Method> methods = null;
            if(!functions.isEmpty()) {
                methods = new ArrayList<>(functions.size());
                for (FirFunction function : functions) {
                    methods.add(methodDeclarationType(function, clazz));
                }
            }

            List<JavaType.FullyQualified> interfaces = null;
            if (interfaceTypeRefs != null && !interfaceTypeRefs.isEmpty()) {
                interfaces = new ArrayList<>(interfaceTypeRefs.size());
                for (FirTypeRef iParam : interfaceTypeRefs) {
                    JavaType.FullyQualified javaType = TypeUtils.asFullyQualified(type(iParam));
                    if (javaType != null) {
                        interfaces.add(javaType);
                    }
                }
            }

            List<JavaType.FullyQualified> annotations = listAnnotations(firClass.getAnnotations());
            clazz.unsafeSet(null, supertype, owner, annotations, interfaces, fields, methods);
        }

        if (!firClass.getTypeParameters().isEmpty()) {
            JavaType.Parameterized pt = typeCache.get(signature);
            if (pt == null) {
                pt = new JavaType.Parameterized(null, null, null);
                typeCache.put(signature, pt);

                List<JavaType> typeParameters = new ArrayList<>(firClass.getTypeParameters().size());
                if (resolvedTypeRef != null && resolvedTypeRef.getType().getTypeArguments().length > 0) {
                    for (ConeTypeProjection typeArgument : resolvedTypeRef.getType().getTypeArguments()) {
                         typeParameters.add(type(typeArgument));
                    }
                } else {
                    for (FirTypeParameterRef tParam : firClass.getTypeParameters()) {
                        typeParameters.add(type(tParam));
                    }
                }
                pt.unsafeSet(clazz, typeParameters);
            }
            return pt;
        }

        return clazz;
    }

    @Nullable
    public JavaType.Method methodDeclarationType(@Nullable FirFunction function, @Nullable JavaType.FullyQualified declaringType) {
        FirFunctionSymbol<?> methodSymbol = function == null ? null : function.getSymbol();
        if (methodSymbol != null) {
            String signature = signatureBuilder.methodDeclarationSignature(function.getSymbol());
            JavaType.Method existing = typeCache.get(signature);
            if (existing != null) {
                return existing;
            }

            List<String> paramNames = null;
            if (!methodSymbol.getValueParameterSymbols().isEmpty()) {
                paramNames = new ArrayList<>(methodSymbol.getValueParameterSymbols().size());
                for (FirValueParameterSymbol p : methodSymbol.getValueParameterSymbols()) {
                    String s = p.getName().asString();
                    paramNames.add(s);
                }
            }
            List<String> defaultValues = null;

            JavaType.Method method = new JavaType.Method(
                    null,
                    convertToFlagsBitMap(methodSymbol.getResolvedStatus()),
                    null,
                    methodSymbol instanceof FirConstructorSymbol ? "<constructor>" : methodSymbol.getName().asString(),
                    null,
                    paramNames,
                    null, null, null,
                    defaultValues
            );
            typeCache.put(signature, method);

            List<JavaType.FullyQualified> exceptionTypes = null;

            JavaType.FullyQualified resolvedDeclaringType = declaringType;
            if (declaringType == null) {
                if (methodSymbol instanceof FirConstructorSymbol) {
                    JavaType type = type(methodSymbol.getResolvedReturnType());
                    resolvedDeclaringType = type instanceof JavaType.FullyQualified ? (JavaType.FullyQualified) type : null;
                } else if (methodSymbol.getDispatchReceiverType() != null) {
                    JavaType type = type(methodSymbol.getDispatchReceiverType());
                    resolvedDeclaringType = type instanceof JavaType.FullyQualified ? (JavaType.FullyQualified) type : null;
                }
                // Find a means to distinguish the owner when a method is declared in a root.
            }

            if (resolvedDeclaringType == null) {
                return null;
            }

            JavaType returnType = type(methodSymbol.getResolvedReturnTypeRef());
            List<JavaType> parameterTypes = null;
            if (!methodSymbol.getValueParameterSymbols().isEmpty()) {
                parameterTypes = new ArrayList<>(methodSymbol.getValueParameterSymbols().size());
                for (FirValueParameterSymbol valueParameterSymbol : methodSymbol.getValueParameterSymbols()) {
                    JavaType javaType = type(valueParameterSymbol.getResolvedReturnTypeRef());
                    parameterTypes.add(javaType);
                }
            }

            method.unsafeSet(resolvedDeclaringType,
                    methodSymbol instanceof FirConstructorSymbol ? resolvedDeclaringType : returnType,
                    parameterTypes, exceptionTypes, listAnnotations(methodSymbol.getAnnotations()));
            return method;
        }

        return null;
    }

    @Nullable
    public JavaType.Method methodInvocationType(@Nullable FirFunctionCall functionCall) {
        if (functionCall == null) {
            return null;
        }

        String signature = signatureBuilder.methodSignature(functionCall);
        JavaType.Method existing = typeCache.get(signature);
        if (existing != null) {
            return existing;
        }

        // Time constrained.
        FirBasedSymbol<?> symbol = ((FirResolvedNamedReference) functionCall.getCalleeReference()).getResolvedSymbol();
        FirConstructor constructor = null;
        FirSimpleFunction simpleFunction = null;
        if (symbol instanceof FirConstructorSymbol) {
            constructor = (FirConstructor) symbol.getFir();
        } else {
            simpleFunction = (FirSimpleFunction) symbol.getFir();
        }

        List<String> paramNames = null;
        if (simpleFunction != null && !simpleFunction.getValueParameters().isEmpty()) {
            paramNames = new ArrayList<>(simpleFunction.getValueParameters().size());
            for (FirValueParameter p : simpleFunction.getValueParameters()) {
                String s = p.getName().asString();
                paramNames.add(s);
            }
        } else if (constructor != null && !constructor.getValueParameters().isEmpty()) {
            paramNames = new ArrayList<>(constructor.getValueParameters().size());
            for (FirValueParameter p : constructor.getValueParameters()) {
                String s = p.getName().asString();
                paramNames.add(s);
            }
        }

        JavaType.Method method = new JavaType.Method(
                null,
                convertToFlagsBitMap(constructor != null ? constructor.getStatus() : simpleFunction.getStatus()),
                null,
                constructor != null ? "<constructor>" : simpleFunction.getName().asString(),
                null,
                paramNames,
                null, null, null, null
        );
        typeCache.put(signature, method);

        JavaType returnType = null;
        List<JavaType> parameterTypes = null;
        List<JavaType.FullyQualified> exceptionTypes = null;

//        if (selectType instanceof Type.MethodType) {
//            Type.MethodType methodType = (Type.MethodType) selectType;

            if (constructor != null && !constructor.getValueParameters().isEmpty()) {
                parameterTypes = new ArrayList<>(constructor.getValueParameters().size());
                for (FirValueParameter argtype : constructor.getValueParameters()) {
                    if (argtype != null) {
                        JavaType javaType = type(argtype);
                        parameterTypes.add(javaType);
                    }
                }
            }

//            returnType = type(methodType.restype);

//        } else if (selectType instanceof Type.UnknownType) {
//            returnType = JavaType.Unknown.getInstance();
//        }

        // Currently impossible to set.
        JavaType.FullyQualified resolvedDeclaringType = null;
        if (resolvedDeclaringType == null) {
            return null;
        }

        assert returnType != null;

        method.unsafeSet(resolvedDeclaringType,
                constructor != null ? resolvedDeclaringType : returnType,
                parameterTypes, exceptionTypes, listAnnotations(constructor != null ? constructor.getAnnotations() : simpleFunction.getAnnotations()));
        return method;
    }

    @Nullable
    public JavaType.Variable variableType(@Nullable FirVariableSymbol<? extends FirVariable> symbol) {
        return variableType(symbol, null);
    }

    @Nullable
    public JavaType.Variable variableType(@Nullable FirVariableSymbol<? extends FirVariable> symbol, @Nullable JavaType.FullyQualified owner) {
        if (symbol == null) {
            return null;
        }

        String signature = signatureBuilder.variableSignature(symbol);
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

        List<JavaType.FullyQualified> annotations = listAnnotations(symbol.getAnnotations());

        JavaType resolvedOwner = owner;
        // Resolve owner ... there isn't a clear way to access this yet.
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

    private JavaType generic(ConeTypeProjection type, String signature) {
        // TODO: fix for multiple bounds.
        String name = "";
        JavaType.GenericTypeVariable.Variance variance = INVARIANT;
        List<JavaType> bounds = null;
        if (type instanceof ConeKotlinTypeProjectionIn) {
            ConeKotlinTypeProjectionIn in = (ConeKotlinTypeProjectionIn) type;
            variance = CONTRAVARIANT;
            FirRegularClassSymbol classSymbol = TypeUtilsKt.toRegularClassSymbol(in.getType(), firSession);
            bounds = new ArrayList<>(1);
            bounds.add(classSymbol != null ? type(classSymbol.getFir()) : JavaType.Unknown.getInstance());
        } else if (type instanceof ConeKotlinTypeProjectionOut) {
            ConeKotlinTypeProjectionOut out = (ConeKotlinTypeProjectionOut) type;
            variance = COVARIANT;
            FirRegularClassSymbol classSymbol = TypeUtilsKt.toRegularClassSymbol(out.getType(), firSession);
            bounds = new ArrayList<>(1);
            bounds.add(classSymbol != null ? type(classSymbol.getFir()) : JavaType.Unknown.getInstance());
        } else if (type instanceof ConeStarProjection) {
            name = "*";
        } else if (type instanceof ConeClassLikeType) {
            ConeClassLikeType classLikeType = (ConeClassLikeType) type;
            FirRegularClassSymbol classSymbol = TypeUtilsKt.toRegularClassSymbol(classLikeType, firSession);
            return classSymbol != null ? type(classSymbol.getFir()) : JavaType.Unknown.getInstance();
        } else if (type instanceof ConeTypeParameterType) {
            name = type.toString();
        } else {
            throw new IllegalStateException("Implement me.");
        }

        JavaType.GenericTypeVariable gtv = new JavaType.GenericTypeVariable(null,
                name, variance, null);
        typeCache.put(signature, gtv);

        gtv.unsafeSet(variance, bounds);
        return gtv;
    }

    private JavaType generic(FirTypeParameter typeParameter, String signature) {
        String name = typeParameter.getName().asString();
        JavaType.GenericTypeVariable gtv = new JavaType.GenericTypeVariable(null,
                name, INVARIANT, null);
        typeCache.put(signature, gtv);

        List<JavaType> bounds = null;
        JavaType.GenericTypeVariable.Variance variance = INVARIANT;
        if (!(typeParameter.getBounds().size() == 1 && typeParameter.getBounds().get(0) instanceof FirImplicitNullableAnyTypeRef)) {
            bounds = new ArrayList<>(typeParameter.getBounds().size());
            for (FirTypeRef bound : typeParameter.getBounds()) {
                bounds.add(type(bound));
            }
            if ("out".equals(typeParameter.getVariance().getLabel())) {
                variance = COVARIANT;
            } else if ("in".equals(typeParameter.getVariance().getLabel())) {
                variance = CONTRAVARIANT;
            }
        }

        gtv.unsafeSet(variance, bounds);
        return gtv;
    }

    private long convertToFlagsBitMap(FirDeclarationStatus status) {
        long bitMask = 0;
        Visibility visibility = status.getVisibility();
        switch (visibility.getName()) {
            case "public":
                bitMask += 1L;
                break;
            case "private":
                bitMask += 1L << 1;
                break;
            case "protected":
                bitMask += 1L << 2;
                break;
            case "internal":
                // Kotlin specific
                break;
            default:
                // are there more?
                System.out.println();
                break;
        }

        Modality modality = status.getModality();
        if (Modality.FINAL == modality) {
            bitMask += 1L << 4;
        } else if (Modality.ABSTRACT == modality) {
            bitMask += 1L << 10;
        }
//        else if (Modality.OPEN == modality) {
            // Kotlin specific
//        } else if (Modality.SEALED == modality) {
            // Kotlin specific
//        }

        if (status.isStatic()) {
            // Not sure how this happens, since Kotlin does not have a static modifier.
            bitMask += 1L << 3;
        }

        return bitMask;
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
            kind = JavaType.FullyQualified.Kind.Class;
        } else {
            throw new UnsupportedOperationException("Unexpected classKind: " + classKind.name());
        }

        return kind;
    }

    private List<JavaType.FullyQualified> listAnnotations(List<FirAnnotation> firAnnotations) {
        List<JavaType.FullyQualified> annotations = new ArrayList<>(firAnnotations.size());
        // TODO: find a cleaner way to access to retention policy of annotations from a reference. There isn't time to sort this out properly.
        outer:
        for (FirAnnotation firAnnotation : firAnnotations) {
            FirRegularClassSymbol symbol = TypeUtilsKt.toRegularClassSymbol(FirTypeUtilsKt.getConeType(firAnnotation.getTypeRef()), firSession);
            if (symbol != null) {
                for (FirAnnotation annotation : symbol.getAnnotations()) {
                    if (annotation instanceof FirAnnotationCall && !((FirAnnotationCall) annotation).getArgumentList().getArguments().isEmpty()) {
                        for (FirExpression argument : ((FirAnnotationCall) annotation).getArgumentList().getArguments()) {
                            if (argument instanceof FirPropertyAccessExpression) {
                                FirPropertyAccessExpression accessExpression = (FirPropertyAccessExpression) argument;
                                FirBasedSymbol<?> callRefSymbol = (((FirResolvedNamedReference) accessExpression.getCalleeReference()).getResolvedSymbol());
                                if (callRefSymbol instanceof FirEnumEntrySymbol) {
                                    FirEnumEntrySymbol enumEntrySymbol = (FirEnumEntrySymbol) callRefSymbol;
                                    if ("kotlin.annotation.AnnotationRetention$SOURCE".equals(KotlinTypeSignatureBuilder.convertKotlinFqToJavaFq(enumEntrySymbol.getCallableId().toString()))) {
                                        continue outer;
                                    }
                                }
                            }
                        }

                    }
                }
            }
            JavaType.FullyQualified fullyQualified = (JavaType.FullyQualified) type(firAnnotation.getTypeRef());
            annotations.add(fullyQualified);
        }

        return annotations;
    }
}
