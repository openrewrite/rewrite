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
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.kotlin.descriptors.ClassKind;
import org.jetbrains.kotlin.descriptors.Modality;
import org.jetbrains.kotlin.descriptors.Visibility;
import org.jetbrains.kotlin.fir.ClassMembersKt;
import org.jetbrains.kotlin.fir.FirSession;
import org.jetbrains.kotlin.fir.declarations.*;
import org.jetbrains.kotlin.fir.expressions.*;
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField;
import org.jetbrains.kotlin.fir.java.declarations.FirJavaMethod;
import org.jetbrains.kotlin.fir.java.declarations.FirJavaValueParameter;
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference;
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference;
import org.jetbrains.kotlin.fir.resolve.LookupTagUtilsKt;
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderKt;
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag;
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol;
import org.jetbrains.kotlin.fir.symbols.impl.*;
import org.jetbrains.kotlin.fir.types.*;
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef;
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef;
import org.jetbrains.kotlin.load.java.structure.*;
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaClass;
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaConstructor;
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaMethod;
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.StandardClassIds;
import org.openrewrite.Incubating;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTypeMapping;
import org.openrewrite.java.internal.JavaReflectionTypeMapping;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.*;
import static org.openrewrite.kotlin.KotlinTypeSignatureBuilder.convertClassIdToFqn;
import static org.openrewrite.kotlin.KotlinTypeSignatureBuilder.convertKotlinFqToJavaFq;

@Incubating(since = "0.0")
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
        return type(type, null);
    }

    @SuppressWarnings("ConstantConditions")
    public JavaType type(@Nullable Object type, @Nullable FirBasedSymbol<?> ownerFallBack) {
        if (type == null) {
            return JavaType.Class.Unknown.getInstance();
        }

        String signature = signatureBuilder.signature(type, ownerFallBack);
        JavaType existing = typeCache.get(signature);
        if (existing != null) {
            return existing;
        }

        if (type instanceof String) {
            // Kotlin only resolves the members necessary in a file like `Collection.kt` that wraps `listOf`, `mapOf`, etc.
            // The owner type may be constructed through a String and is represented with a ShallowClass.
            // type(..) handles the string value to reuse the same shallow class.
            JavaType javaType = JavaType.ShallowClass.build((String) type);
            typeCache.put(signature, javaType);
            return javaType;
        } else if (type instanceof FirClass) {
            return classType(type, signature, ownerFallBack);
        } else if (type instanceof FirFunction) {
            return methodDeclarationType((FirFunction) type, null, ownerFallBack);
        } else if (type instanceof FirVariable) {
            return variableType(((FirVariable) type).getSymbol(), null, ownerFallBack);
        } else if (type instanceof FirFile) {
            return JavaType.ShallowClass.build(((FirFile) type).getName());
        } else if (type instanceof FirJavaTypeRef) {
            return type(((FirJavaTypeRef) type).getType(), ownerFallBack);
        } else if (type instanceof org.jetbrains.kotlin.load.java.structure.JavaType) {
            return mapJavaType((org.jetbrains.kotlin.load.java.structure.JavaType) type, signature);
        } else if (type instanceof JavaElement) {
            return mapJavaElementType((JavaElement) type, signature);
        }

        return resolveType(type, signature, ownerFallBack);
    }

    @Nullable
    private JavaType resolveType(Object type, String signature, @Nullable FirBasedSymbol<?> ownerFallBack) {
        if (type instanceof ConeTypeProjection) {
            return resolveConeTypeProjection((ConeTypeProjection) type, signature, ownerFallBack);
        } else if (type instanceof FirExpression) {
            return type(((FirExpression) type).getTypeRef(), ownerFallBack);
        } else if (type instanceof FirFunctionTypeRef) {
            return type(((FirFunctionTypeRef) type).getReturnTypeRef(), ownerFallBack);
        } else if (type instanceof FirResolvedNamedReference) {
            FirBasedSymbol<?> resolvedSymbol = ((FirResolvedNamedReference) type).getResolvedSymbol();
            if (resolvedSymbol instanceof FirConstructorSymbol) {
                return type(((FirConstructorSymbol) resolvedSymbol).getResolvedReturnTypeRef(), ownerFallBack);
            } else if (resolvedSymbol instanceof FirEnumEntrySymbol) {
                return type(((FirEnumEntrySymbol) resolvedSymbol).getResolvedReturnTypeRef(), ownerFallBack);
            } else if (resolvedSymbol instanceof FirNamedFunctionSymbol) {
                return type(((FirNamedFunctionSymbol) resolvedSymbol).getResolvedReturnTypeRef(), ownerFallBack);
            } else if (resolvedSymbol instanceof FirPropertySymbol) {
                return type(((FirPropertySymbol) resolvedSymbol).getResolvedReturnTypeRef(), ownerFallBack);
            } else if (resolvedSymbol instanceof FirValueParameterSymbol) {
                return type(((FirValueParameterSymbol) resolvedSymbol).getResolvedReturnType(), ownerFallBack);
            } else if (resolvedSymbol instanceof FirFieldSymbol) {
                return type(((FirFieldSymbol) resolvedSymbol).getResolvedReturnType(), ownerFallBack);
            }
        } else if (type instanceof FirResolvedTypeRef) {
            ConeKotlinType coneKotlinType = FirTypeUtilsKt.getConeType((FirResolvedTypeRef) type);
            if (coneKotlinType instanceof ConeTypeParameterType) {
                FirClassifierSymbol<?> classifierSymbol = LookupTagUtilsKt.toSymbol(((ConeTypeParameterType) coneKotlinType).getLookupTag(), firSession);
                if (classifierSymbol != null && classifierSymbol.getFir() instanceof FirTypeParameter) {
                    return resolveConeTypeProjection((FirTypeParameter) classifierSymbol.getFir(), signature);
                }
            }
            return classType(type, signature, ownerFallBack);
        } else if (type instanceof FirTypeParameter) {
            return resolveConeTypeProjection((FirTypeParameter) type, signature);
        } else if (type instanceof FirVariableAssignment) {
            return type(((FirVariableAssignment) type).getCalleeReference(), ownerFallBack);
        }

        return null;
    }

    private JavaType array(JavaArrayType type, String signature) {
        JavaType.Array arr = new JavaType.Array(null, null);
        typeCache.put(signature, arr);
        arr.unsafeSet(type(type.getComponentType()));
        return arr;
    }

    private JavaType.FullyQualified classType(Object classType, String signature, @Nullable FirBasedSymbol<?> ownerFallBack) {
        FirClass firClass;
        FirResolvedTypeRef resolvedTypeRef = null;
        if (classType instanceof FirResolvedTypeRef) {
            // The resolvedTypeRef is used to create parameterized types.
            resolvedTypeRef = (FirResolvedTypeRef) classType;
            ConeKotlinType type = resolvedTypeRef.getType();
            if (type instanceof ConeFlexibleType) {
                // for platform types the lower bound is the nullable type
                type = ((ConeFlexibleType) type).getLowerBound();
            }
            FirRegularClassSymbol symbol = TypeUtilsKt.toRegularClassSymbol(type, firSession);
            if (symbol == null) {
                typeCache.put(signature, JavaType.Unknown.getInstance());
                return JavaType.Unknown.getInstance();
            }
            firClass = symbol.getFir();
        } else {
            firClass = (FirClass) classType;
        }
        FirClassSymbol<? extends FirClass> sym = firClass.getSymbol();

        String classFqn = convertClassIdToFqn(sym.getClassId());

        JavaType.FullyQualified fq = typeCache.get(classFqn);
        if (fq instanceof JavaType.Unknown) {
            return fq;
        }

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

            JavaType.FullyQualified supertype = superTypeRef == null || "java.lang.Object".equals(signature) ? null : TypeUtils.asFullyQualified(type(superTypeRef));

            JavaType.FullyQualified owner = null;
            if (firClass.getSymbol().getClassId().getOuterClassId() != null) {
                FirClassLikeSymbol<?> ownerSymbol = FirSymbolProviderKt.getSymbolProvider(firSession)
                        .getClassLikeSymbolByClassId(firClass.getSymbol().getClassId().getOuterClassId());
                if (ownerSymbol != null) {
                    owner = TypeUtils.asFullyQualified(type(ownerSymbol.getFir()));
                }
            }

            List<FirProperty> properties = new ArrayList<>(firClass.getDeclarations().size());
            List<FirJavaField> javaFields = new ArrayList<>(firClass.getDeclarations().size());
            List<FirFunction> functions = new ArrayList<>(firClass.getDeclarations().size());
            List<FirEnumEntry> enumEntries = new ArrayList<>(firClass.getDeclarations().size());

            for (FirDeclaration declaration : firClass.getDeclarations()) {
                if (declaration instanceof FirProperty) {
                    if (declaration.getSource() == null || !(declaration.getSource().getKind() instanceof KtFakeSourceElementKind)) {
                        properties.add((FirProperty) declaration);
                    }
                } else if (declaration instanceof FirJavaField) {
                    javaFields.add((FirJavaField) declaration);
                } else if (declaration instanceof FirSimpleFunction) {
                    functions.add((FirFunction) declaration);
                } else if (declaration instanceof FirConstructor) {
                    functions.add((FirFunction) declaration);
                } else if (declaration instanceof FirRegularClass) {
                    // Skipped since inner classes don't exist on the JavaType$Class.
                } else if (declaration instanceof FirEnumEntry) {
                    enumEntries.add((FirEnumEntry) declaration);
                }
            }

            List<JavaType.Variable> fields = null;
            if (!enumEntries.isEmpty()) {
                fields = new ArrayList<>(properties.size() + enumEntries.size());
                for (FirEnumEntry enumEntry : enumEntries) {
                    fields.add(variableType(enumEntry.getSymbol(), clazz, ownerFallBack));
                }
            }

            if (!properties.isEmpty()) {
                if (fields == null) {
                    fields = new ArrayList<>(properties.size());
                }

                for (FirProperty property : properties) {
                    fields.add(variableType(property.getSymbol(), clazz, ownerFallBack));
                }
            }

            if (!javaFields.isEmpty()) {
                if (fields == null) {
                    fields = new ArrayList<>(javaFields.size());
                }

                for (FirJavaField field : javaFields) {
                    fields.add(variableType(field.getSymbol(), clazz, ownerFallBack));
                }
            }

            List<JavaType.Method> methods = null;
            if (!functions.isEmpty()) {
                methods = new ArrayList<>(functions.size());
                for (FirFunction function : functions) {
                    methods.add(methodDeclarationType(function, clazz, ownerFallBack));
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

    private JavaType classType(BinaryJavaClass classifier, String signature) {
        JavaType.Class clazz = typeCache.get(classifier.getFqName().asString());
        if (clazz == null) {
            clazz = new JavaType.Class(
                    null,
                    classifier.getAccess(),
                    classifier.getFqName().asString(),
                    convertToClassKind(classifier),
                    null, null, null, null, null, null, null
            );
            typeCache.put(classifier.getFqName().asString(), clazz);

            JavaType.FullyQualified supertype = null;
            List<JavaType.FullyQualified> interfaces = null;

            for (JavaClassifierType classifierSupertype : classifier.getSupertypes()) {
                if (classifierSupertype.getClassifier() instanceof JavaClass) {
                    if (((JavaClass) classifierSupertype.getClassifier()).isInterface()) {
                        if (interfaces == null) {
                            interfaces = new ArrayList<>();
                        }
                        interfaces.add((JavaType.FullyQualified) type(classifierSupertype));
                    } else if (!"java.lang.Object".equals(signature)) {
                        supertype = (JavaType.FullyQualified) type(classifierSupertype);
                    }
                }
            }

            JavaType.FullyQualified owner = null;
            if (classifier.getOuterClass() != null) {
                owner = TypeUtils.asFullyQualified(type(classifier.getOuterClass()));
            }

            List<JavaType.Variable> fields = null;
            if (!classifier.getFields().isEmpty()) {
                fields = new ArrayList<>(classifier.getFields().size());
                for (JavaField field : classifier.getFields()) {
                    fields.add(variableType(field, clazz));
                }
            }

            List<JavaType.Method> methods = null;
            if (!classifier.getMethods().isEmpty()) {
                methods = new ArrayList<>(classifier.getMethods().size());
                for (JavaMethod method : classifier.getMethods()) {
                    methods.add(methodDeclarationType(method, clazz));
                }
            }

            if (!classifier.getConstructors().isEmpty()) {
                for (JavaConstructor method : classifier.getConstructors()) {
                    if (method instanceof BinaryJavaConstructor) {
                        if (methods == null) {
                            methods = new ArrayList<>();
                        }
                        // Filter out the same methods as JavaTypeMapping: Flags.SYNTHETIC | Flags.BRIDGE | Flags.HYPOTHETICAL | Flags.ANONCONSTR
                        if ((((BinaryJavaConstructor) method).getAccess() & (1<<12 | 1L<<31 | 1L<<37 | 1<<29)) == 0) {
                            methods.add(methodConstructorSignature(method, clazz));
                        }
                    }
                }
            }
            List<JavaType> typeParameters = null;
            if (!classifier.getTypeParameters().isEmpty()) {
                typeParameters = new ArrayList<>(classifier.getTypeParameters().size());
                for (JavaTypeParameter typeArgument : classifier.getTypeParameters()) {
                    typeParameters.add(type(typeArgument));
                }
            }
            clazz.unsafeSet(typeParameters, supertype, owner, listAnnotations(classifier.getAnnotations()), interfaces, fields, methods);
        }

        if (!classifier.getTypeParameters().isEmpty()) {
            JavaType.Parameterized pt = typeCache.get(signature);
            if (pt == null) {
                pt = new JavaType.Parameterized(null, null, null);
                typeCache.put(signature, pt);

                List<JavaType> typeParameters = new ArrayList<>(classifier.getTypeParameters().size());
                for (JavaTypeParameter typeArgument : classifier.getTypeParameters()) {
                    typeParameters.add(type(typeArgument));
                }
                pt.unsafeSet(clazz, typeParameters);
            }
            return pt;
        }
        return clazz;
    }

    @Nullable
    public JavaType.Method methodDeclarationType(@Nullable FirFunction function, @Nullable JavaType.FullyQualified declaringType, @Nullable FirBasedSymbol<?> ownerFallBack) {
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

            JavaType.FullyQualified resolvedDeclaringType = declaringType;
            if (declaringType == null) {
                if (methodSymbol instanceof FirConstructorSymbol) {
                    resolvedDeclaringType = TypeUtils.asFullyQualified(type(methodSymbol.getResolvedReturnType()));
                } else if (methodSymbol.getDispatchReceiverType() != null) {
                    resolvedDeclaringType = TypeUtils.asFullyQualified(type(methodSymbol.getDispatchReceiverType()));
                } else if (ownerFallBack != null) {
                    resolvedDeclaringType = TypeUtils.asFullyQualified(type(ownerFallBack.getFir()));
                }
            }

            if (resolvedDeclaringType == null) {
                return null;
            }

            JavaType returnType = function instanceof FirJavaMethod ?
                    type(methodSymbol.getFir().getReturnTypeRef()) :
                    type(methodSymbol.getResolvedReturnTypeRef());

            List<JavaType> parameterTypes = null;
            if (!methodSymbol.getValueParameterSymbols().isEmpty()) {
                parameterTypes = new ArrayList<>(methodSymbol.getValueParameterSymbols().size());
                for (FirValueParameterSymbol parameterSymbol : methodSymbol.getValueParameterSymbols()) {
                    JavaType javaType;
                    if (parameterSymbol.getFir() instanceof FirJavaValueParameter) {
                        javaType = type(parameterSymbol.getFir().getReturnTypeRef());
                    } else {
                        javaType = type(parameterSymbol.getResolvedReturnTypeRef());
                    }
                    parameterTypes.add(javaType);
                }
            }

            method.unsafeSet(resolvedDeclaringType,
                    methodSymbol instanceof FirConstructorSymbol ? resolvedDeclaringType : returnType,
                    parameterTypes, null, listAnnotations(methodSymbol.getAnnotations()));
            return method;
        }

        return null;
    }

    @Nullable
    public JavaType.Method methodDeclarationType(@Nullable JavaMethod javaMethod, @Nullable JavaType.FullyQualified declaringType) {
        if (javaMethod != null) {
            String signature = signatureBuilder.methodDeclarationSignature(javaMethod);

            JavaType.Method existing = typeCache.get(signature);
            if (existing != null) {
                return existing;
            }

            List<String> paramNames = null;
            if (!javaMethod.getValueParameters().isEmpty()) {
                paramNames = new ArrayList<>(javaMethod.getValueParameters().size());
                List<JavaValueParameter> valueParameters = javaMethod.getValueParameters();
                // Generate names for parameters that match the output for the Java compiler.
                for (int i = 0; i < valueParameters.size(); i++) {
                    paramNames.add("arg" + i);
                }
            }

            List<String> defaultValues = null;
            if (javaMethod.getAnnotationParameterDefaultValue() != null) {
                if (javaMethod.getAnnotationParameterDefaultValue().getName() != null) {
                    defaultValues = new ArrayList<>();
                    defaultValues.add(javaMethod.getAnnotationParameterDefaultValue().getName().asString());
                }
            }

            JavaType.Method method = new JavaType.Method(
                    null,
                    javaMethod instanceof BinaryJavaMethod ? ((BinaryJavaMethod) javaMethod).getAccess() :
                            convertToFlagsBitMap(javaMethod.getVisibility(), javaMethod.isStatic(), javaMethod.isFinal(), javaMethod.isAbstract()),
                    null,
                    javaMethod.getName().asString(),
                    null,
                    paramNames,
                    null, null, null,
                    defaultValues
            );
            typeCache.put(signature, method);

            List<JavaType.FullyQualified> exceptionTypes = null;

            JavaType.FullyQualified resolvedDeclaringType = declaringType;
            if (declaringType == null) {
                resolvedDeclaringType = TypeUtils.asFullyQualified(type(javaMethod.getContainingClass()));
            }

            if (resolvedDeclaringType == null) {
                return null;
            }

            JavaType returnType = type(javaMethod.getReturnType());
            List<JavaType> parameterTypes = null;
            if (!javaMethod.getValueParameters().isEmpty()) {
                parameterTypes = new ArrayList<>(javaMethod.getValueParameters().size());
                for (JavaValueParameter parameterSymbol : javaMethod.getValueParameters()) {
                    JavaType javaType = type(parameterSymbol.getType());
                    parameterTypes.add(javaType);
                }
            }

            method.unsafeSet(resolvedDeclaringType,
                    returnType,
                    parameterTypes, exceptionTypes, listAnnotations(javaMethod.getAnnotations()));
            return method;
        }

        return null;
    }

    @Nullable
    public JavaType.Method methodConstructorSignature(@Nullable JavaConstructor constructor, @Nullable JavaType.FullyQualified declaringType) {
        if (constructor != null) {
            String signature = signatureBuilder.methodConstructorSignature(constructor);

            JavaType.Method existing = typeCache.get(signature);
            if (existing != null) {
                return existing;
            }

            List<String> paramNames = null;
            if (!constructor.getValueParameters().isEmpty()) {
                paramNames = new ArrayList<>(constructor.getValueParameters().size());
                List<JavaValueParameter> valueParameters = constructor.getValueParameters();
                for (int i = 0; i < valueParameters.size(); i++) {
                    paramNames.add("arg" + i);
                }
            }

            List<String> defaultValues = null;

            JavaType.Method method = new JavaType.Method(
                    null,
                    constructor instanceof BinaryJavaConstructor ? ((BinaryJavaConstructor) constructor).getAccess() :
                            convertToFlagsBitMap(constructor.getVisibility(), constructor.isStatic(), constructor.isFinal(), constructor.isAbstract()),
                    null,
                    "<constructor>",
                    null,
                    paramNames,
                    null, null, null,
                    defaultValues
            );
            typeCache.put(signature, method);

            List<JavaType.FullyQualified> exceptionTypes = null;

            JavaType.FullyQualified resolvedDeclaringType = declaringType;
            if (declaringType == null) {
                resolvedDeclaringType = TypeUtils.asFullyQualified(type(constructor.getContainingClass()));
            }

            if (resolvedDeclaringType == null) {
                return null;
            }

            List<JavaType> parameterTypes = null;
            if (!constructor.getValueParameters().isEmpty()) {
                parameterTypes = new ArrayList<>(constructor.getValueParameters().size());
                for (JavaValueParameter parameterSymbol : constructor.getValueParameters()) {
                    JavaType javaType = type(parameterSymbol.getType());
                    parameterTypes.add(javaType);
                }
            }

            method.unsafeSet(resolvedDeclaringType,
                    resolvedDeclaringType,
                    parameterTypes, exceptionTypes, listAnnotations(constructor.getAnnotations()));
            return method;
        }

        return null;
    }

    @Nullable
    public JavaType.Method methodInvocationType(@Nullable FirFunctionCall functionCall, @Nullable FirBasedSymbol<?> ownerSymbol) {
        if (functionCall == null || functionCall.getCalleeReference() instanceof FirErrorNamedReference) {
            return null;
        }

        String signature = signatureBuilder.methodSignature(functionCall, ownerSymbol);
        JavaType.Method existing = typeCache.get(signature);
        if (existing != null) {
            return existing;
        }

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

        List<JavaType> parameterTypes = null;
        if (constructor != null && !constructor.getValueParameters().isEmpty()) {
            parameterTypes = new ArrayList<>(constructor.getValueParameters().size());
            for (FirValueParameter argtype : constructor.getValueParameters()) {
                if (argtype != null) {
                    JavaType javaType = type(argtype);
                    parameterTypes.add(javaType);
                }
            }
        }

        JavaType.FullyQualified resolvedDeclaringType = null;
        if (functionCall.getCalleeReference() instanceof FirResolvedNamedReference) {
            if (((FirResolvedNamedReference) functionCall.getCalleeReference()).getResolvedSymbol() instanceof FirNamedFunctionSymbol) {
                FirNamedFunctionSymbol resolvedSymbol = (FirNamedFunctionSymbol) ((FirResolvedNamedReference) functionCall.getCalleeReference()).getResolvedSymbol();
                if (ClassMembersKt.containingClass(resolvedSymbol) != null) {
                    ConeClassLikeLookupTag lookupTag = ClassMembersKt.containingClass(resolvedSymbol);
                    if (lookupTag != null) {
                        FirRegularClassSymbol classSymbol = LookupTagUtilsKt.toFirRegularClassSymbol(lookupTag, firSession);
                        if (classSymbol != null) {
                            resolvedDeclaringType = TypeUtils.asFullyQualified(type(classSymbol.getFir()));
                        }
                    }
                } else if (resolvedSymbol.getOrigin() == FirDeclarationOrigin.Library.INSTANCE) {
                    if (resolvedSymbol.getFir().getContainerSource() instanceof JvmPackagePartSource) {
                        JvmPackagePartSource source = (JvmPackagePartSource) resolvedSymbol.getFir().getContainerSource();
                        if (source.getFacadeClassName() != null) {
                            resolvedDeclaringType = TypeUtils.asFullyQualified(type(convertKotlinFqToJavaFq(source.getFacadeClassName().toString())));
                        } else {
                            resolvedDeclaringType = TypeUtils.asFullyQualified(type(convertKotlinFqToJavaFq(source.getClassName().toString())));
                        }
                    } else if (!resolvedSymbol.getFir().getOrigin().getGenerated() &&
                            !resolvedSymbol.getFir().getOrigin().getFromSupertypes() &&
                            !resolvedSymbol.getFir().getOrigin().getFromSource()) {
                        resolvedDeclaringType = TypeUtils.asFullyQualified(type("kotlin.Library"));
                    }
                } else if (resolvedSymbol.getOrigin() == FirDeclarationOrigin.Source.INSTANCE && ownerSymbol != null) {
                    if (ownerSymbol instanceof FirFileSymbol) {
                        resolvedDeclaringType = TypeUtils.asFullyQualified(type(((FirFileSymbol) ownerSymbol).getFir()));
                    } else if (ownerSymbol instanceof FirNamedFunctionSymbol) {
                        resolvedDeclaringType = TypeUtils.asFullyQualified(type(((FirNamedFunctionSymbol) ownerSymbol).getFir()));
                    } else if (ownerSymbol instanceof FirRegularClassSymbol) {
                        resolvedDeclaringType = TypeUtils.asFullyQualified(type(((FirRegularClassSymbol) ownerSymbol).getFir()));
                    }
                }
            }
        }

        if (resolvedDeclaringType == null) {
            return null;
        }

        JavaType returnType = type(functionCall.getTypeRef(), ownerSymbol);

        method.unsafeSet(resolvedDeclaringType,
                constructor != null ? resolvedDeclaringType : returnType,
                parameterTypes, null, listAnnotations(constructor != null ? constructor.getAnnotations() : simpleFunction.getAnnotations()));
        return method;
    }

    @Nullable
    public JavaType.Variable variableType(@Nullable FirVariableSymbol<? extends FirVariable> symbol, @Nullable JavaType.FullyQualified owner, @Nullable FirBasedSymbol<?> ownerFallBack) {
        if (symbol == null) {
            return null;
        }

        String signature = signatureBuilder.variableSignature(symbol, ownerFallBack);
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
        if (owner == null && ownerFallBack != null) {
            // There isn't a way to link a Callable back to the owner unless it's a class member, but class members already set the owner.
            // The fallback isn't always safe and may result in type erasure.
            // We'll need to find the owner in the parser to set this on properties and variables in local scopes.
            resolvedOwner = type(ownerFallBack.getFir());
        }

        if (resolvedOwner == null) {
            resolvedOwner = JavaType.Unknown.getInstance();
        }

        FirTypeRef typeRef = symbol.getFir() instanceof FirJavaField || symbol.getFir() instanceof FirEnumEntry ? symbol.getFir().getReturnTypeRef() :
                symbol.getResolvedReturnTypeRef();
                variable.unsafeSet(resolvedOwner, type(typeRef), annotations);

        return variable;
    }

    @Nullable
    public JavaType.Variable variableType(JavaField javaField, @Nullable JavaType.FullyQualified owner) {
        String signature = signatureBuilder.variableSignature(javaField);
        JavaType.Variable existing = typeCache.get(signature);
        if (existing != null) {
            return existing;
        }

        JavaType.Variable variable = new JavaType.Variable(
                null,
                convertToFlagsBitMap(javaField.getVisibility(), javaField.isStatic(), javaField.isFinal(), javaField.isAbstract()),
                javaField.getName().asString(),
                null, null, null);

        typeCache.put(signature, variable);

        JavaType resolvedOwner = owner;
        if (owner == null) {
            resolvedOwner = TypeUtils.asFullyQualified(type(javaField.getContainingClass()));
            assert resolvedOwner != null;
        }

        variable.unsafeSet(resolvedOwner, type(javaField.getType()), listAnnotations(javaField.getAnnotations()));

        return variable;
    }

    public JavaType.Primitive primitive(ConeClassLikeType type) {
        // This may need to change in the future. The Kotlin primitives are converted to Java primitives, which is
        // correct for the resultant byte code that runs on the JVM, and helps to support `J`.
        // However, it is technically incorrect in terms of representing the types from the source code on the LST.
        // The transformation happens because `J.Literal` requires a `JavaType.Primitive`, and does not support
        // Kotlin's primitives. The result is Kotlin primitives are not represented in the type hierarchy, which may
        // cause issues as more Kotlin recipes are introduced.
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

        throw new IllegalArgumentException("Unsupported primitive type " + type);
    }

    private JavaType.Primitive primitive(@Nullable PrimitiveType primitiveType) {
        if (primitiveType == null) {
            return JavaType.Primitive.Void;
        }
        switch (primitiveType) {
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
            default:
                throw new IllegalArgumentException("Unsupported primitive type.");
        }
    }

    private JavaType resolveConeTypeProjection(ConeTypeProjection type, String signature, @Nullable FirBasedSymbol<?> ownerSymbol) {
        JavaType resolvedType = JavaType.Unknown.getInstance();

        // TODO: fix for multiple bounds.
        boolean isGeneric = type instanceof ConeKotlinTypeProjectionIn ||
                type instanceof ConeKotlinTypeProjectionOut ||
                type instanceof ConeStarProjection ||
                type instanceof ConeTypeParameterType;

        if (isGeneric) {
            String name;
            JavaType.GenericTypeVariable.Variance variance = INVARIANT;
            List<JavaType> bounds = null;

            if (type instanceof ConeKotlinTypeProjectionIn || type instanceof ConeKotlinTypeProjectionOut) {
                name = "";
            } else if (type instanceof ConeStarProjection) {
                name = "*";
            } else {
                name = type.toString();
            }

            JavaType.GenericTypeVariable gtv = new JavaType.GenericTypeVariable(null, name, INVARIANT, null);
            typeCache.put(signature, gtv);

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
            }

            gtv.unsafeSet(name, variance, bounds);
            resolvedType = gtv;
        } else {
            // The ConeTypeProjection is not a generic type, so it must be a class type.
            if (type instanceof ConeClassLikeType) {
                resolvedType = resolveConeLikeClassType((ConeClassLikeType) type, signature, ownerSymbol);
            } else if (type instanceof ConeFlexibleType) {
                resolvedType = type(((ConeFlexibleType) type).getLowerBound());
            }
        }

        return resolvedType;
    }

    private JavaType resolveConeLikeClassType(ConeClassLikeType coneClassLikeType, String signature, @Nullable FirBasedSymbol<?> ownerSymbol) {
        FirRegularClassSymbol classSymbol = TypeUtilsKt.toRegularClassSymbol(coneClassLikeType, firSession);
        if (classSymbol == null) {
            typeCache.put(signature, JavaType.Unknown.getInstance());
            return JavaType.Unknown.getInstance();
        }

        return type(classSymbol.getFir(), ownerSymbol);
    }

    private JavaType resolveConeTypeProjection(FirTypeParameter typeParameter, String signature) {
        JavaType.GenericTypeVariable gtv = new JavaType.GenericTypeVariable(null, typeParameter.getName().asString(), INVARIANT, null);
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

        gtv.unsafeSet(gtv.getName(), variance, bounds);
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
            bitMask += 1L << 3;
        }

        return bitMask;
    }

    private long convertToFlagsBitMap(Visibility visibility, boolean isStatic, boolean isFinal, boolean isAbstract) {
        long bitMask = 0;
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
                break;
        }

        if (isStatic) {
            bitMask += 1L << 3;
        }
        if (isFinal) {
            bitMask += 1L << 4;
        }
        if (isAbstract) {
            bitMask += 1L << 10;
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
            throw new IllegalArgumentException("Unsupported classKind: " + classKind.name());
        }

        return kind;
    }

    private JavaType.FullyQualified.Kind convertToClassKind(BinaryJavaClass clazz) {
        if (clazz.isEnum()) {
            return JavaType.FullyQualified.Kind.Enum;
        } else if (clazz.isInterface()) {
            return JavaType.FullyQualified.Kind.Interface;
        }
        return JavaType.FullyQualified.Kind.Class;
    }

    private JavaType mapJavaElementType(JavaElement type, String signature) {
        if (type instanceof BinaryJavaClass) {
            return classType((BinaryJavaClass) type, signature);
        } else if (type instanceof JavaTypeParameter) {
            return mapJavaTypeParameter((JavaTypeParameter) type, signature);
        } else if (type instanceof JavaValueParameter) {
            return mapJavaValueParameter((JavaValueParameter) type);
        } else if (type instanceof JavaAnnotation && ((JavaAnnotation) type).getClassId() != null) {
            JavaClass c = ((JavaAnnotation) type).resolve();
            if (c != null) {
                return type(c);
            }
        }
        return JavaType.Unknown.getInstance();
    }

    private JavaType mapJavaType(org.jetbrains.kotlin.load.java.structure.JavaType type, String signature) {
        if (type instanceof JavaPrimitiveType) {
            return primitive(((JavaPrimitiveType) type).getType());
        } else if (type instanceof JavaClassifierType) {
            return mapClassifierType((JavaClassifierType) type, signature);
        } else if (type instanceof JavaArrayType) {
            return array((JavaArrayType) type, signature);
        } else if (type instanceof JavaWildcardType) {
            return mapWildcardType((JavaWildcardType) type, signature);
        }
        return JavaType.Unknown.getInstance();
    }

    private JavaType mapClassifierType(JavaClassifierType type, String signature) {
        JavaType javaType = type(type.getClassifier());

        if (!type.getTypeArguments().isEmpty()) {
            JavaType.FullyQualified fq = TypeUtils.asFullyQualified(javaType);
            fq = fq instanceof JavaType.Parameterized ? ((JavaType.Parameterized) fq).getType() : fq;
            JavaType.Parameterized pt = typeCache.get(signature);
            if (pt == null) {
                pt = new JavaType.Parameterized(null, null, null);
                typeCache.put(signature, pt);

                List<JavaType> typeParameters = new ArrayList<>(type.getTypeArguments().size());
                for (org.jetbrains.kotlin.load.java.structure.JavaType typeArgument : type.getTypeArguments()) {
                    typeParameters.add(type(typeArgument));
                }
                pt.unsafeSet(fq, typeParameters);
            }
            return pt;
        }

        return javaType;
    }

    private JavaType mapJavaTypeParameter(JavaTypeParameter type, String signature) {
        String name = type.getName().asString();
        JavaType.GenericTypeVariable gtv = new JavaType.GenericTypeVariable(null,
                name, INVARIANT, null);
        typeCache.put(signature, gtv);

        List<JavaType> bounds = null;
        if (type.getUpperBounds().size() == 1) {
            JavaType mappedBound = type(type.getUpperBounds().toArray()[0]);
            if (!(mappedBound instanceof JavaType.FullyQualified) || !"java.lang.Object".equals(((JavaType.FullyQualified) mappedBound).getFullyQualifiedName())) {
                bounds = singletonList(mappedBound);
            }
        } else {
            bounds = new ArrayList<>(type.getUpperBounds().size());
            for (org.jetbrains.kotlin.load.java.structure.JavaType bound : type.getUpperBounds()) {
                bounds.add(type(bound));
            }
        }

        gtv.unsafeSet(gtv.getName(), bounds == null ? INVARIANT : COVARIANT, bounds);
        return gtv;
    }

    private JavaType mapJavaValueParameter(JavaValueParameter type) {
        return type(type.getType());
    }

    private JavaType mapWildcardType(JavaWildcardType wildcardType, String signature) {
        JavaType.GenericTypeVariable gtv = new JavaType.GenericTypeVariable(null, "?", INVARIANT, null);
        typeCache.put(signature, gtv);

        JavaType.GenericTypeVariable.Variance variance;
        List<JavaType> bounds;
        if (wildcardType.getBound() != null) {
            if (wildcardType.isExtends()) {
                variance = COVARIANT;
            } else {
                variance = CONTRAVARIANT;
            }
            bounds = singletonList(type(wildcardType.getBound()));
        } else {
            variance = INVARIANT;
            bounds = null;
        }

        if (bounds != null && bounds.get(0) instanceof JavaType.FullyQualified && "java.lang.Object".equals(((JavaType.FullyQualified) bounds.get(0))
                .getFullyQualifiedName())) {
            bounds = null;
        }

        gtv.unsafeSet(gtv.getName(), variance, bounds);
        return gtv;
    }

    private List<JavaType.FullyQualified> listAnnotations(List<FirAnnotation> firAnnotations) {
        List<JavaType.FullyQualified> annotations = new ArrayList<>(firAnnotations.size());
        for (FirAnnotation firAnnotation : firAnnotations) {
            FirRegularClassSymbol symbol = TypeUtilsKt.toRegularClassSymbol(FirTypeUtilsKt.getConeType(firAnnotation.getTypeRef()), firSession);
            if (skipAnnotation(symbol)) continue;
            JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type(firAnnotation.getTypeRef()));
            if (fq != null) {
                annotations.add(fq);
            }
        }

        return annotations;
    }

    private List<JavaType.FullyQualified> listAnnotations(Collection<JavaAnnotation> javaAnnotations) {
        List<JavaType.FullyQualified> annotations = new ArrayList<>(javaAnnotations.size());
        for (JavaAnnotation javaAnnotation : javaAnnotations) {
            JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type(javaAnnotation));
            if (fq != null) {
                annotations.add((JavaType.FullyQualified) type(javaAnnotation));
            }
        }

        return annotations;
    }

    private boolean skipAnnotation(@Nullable FirClassLikeSymbol<?> symbol) {
        if (symbol != null) {
            for (FirAnnotation annotation : symbol.getAnnotations()) {
                if (annotation instanceof FirAnnotationCall && !((FirAnnotationCall) annotation).getArgumentList().getArguments().isEmpty()) {
                    for (FirExpression argument : ((FirAnnotationCall) annotation).getArgumentList().getArguments()) {
                        if (argument instanceof FirPropertyAccessExpression) {
                            FirPropertyAccessExpression accessExpression = (FirPropertyAccessExpression) argument;
                            FirBasedSymbol<?> callRefSymbol = ((FirResolvedNamedReference) accessExpression.getCalleeReference()).getResolvedSymbol();
                            if (callRefSymbol instanceof FirEnumEntrySymbol) {
                                FirEnumEntrySymbol enumEntrySymbol = (FirEnumEntrySymbol) callRefSymbol;
                                if ("kotlin.annotation.AnnotationRetention$SOURCE".equals(convertKotlinFqToJavaFq(enumEntrySymbol.getCallableId().toString()))) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
