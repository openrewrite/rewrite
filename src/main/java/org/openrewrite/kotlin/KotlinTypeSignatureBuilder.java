package org.openrewrite.kotlin;

import org.jetbrains.kotlin.fir.FirSession;
import org.jetbrains.kotlin.fir.declarations.*;
import org.jetbrains.kotlin.fir.expressions.*;
import org.jetbrains.kotlin.fir.references.FirNamedReference;
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference;
import org.jetbrains.kotlin.fir.resolve.LookupTagUtilsKt;
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol;
import org.jetbrains.kotlin.fir.symbols.impl.*;
import org.jetbrains.kotlin.fir.types.*;
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef;
import org.jetbrains.kotlin.name.ClassId;
import org.openrewrite.Incubating;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTypeSignatureBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

@Incubating(since = "0")
public class KotlinTypeSignatureBuilder implements JavaTypeSignatureBuilder {

    private final FirSession firSession;

    @Nullable
    Set<String> typeVariableNameStack;

    public KotlinTypeSignatureBuilder(FirSession firSession) {
        this.firSession = firSession;
    }

    public String signature(@Nullable Object type) {
        return signature(type, null);
    }

    @SuppressWarnings("ConstantConditions")
    public String signature(@Nullable Object type, @Nullable FirBasedSymbol<?> ownerSymbol) {
        if (type == null) {
            return "{undefined}";
        }

        if (type instanceof FirClass) {
            return ((FirClass) type).getTypeParameters().size() > 0 ? parameterizedSignature(type) : classSignature(type);
        } else if (type instanceof FirFunction) {
            return methodDeclarationSignature(((FirFunction) type).getSymbol());
        } else if (type instanceof FirVariable) {
            return variableSignature(((FirVariable) type).getSymbol(), ownerSymbol);
        } else if (type instanceof FirBasedSymbol<?>) {
            return signature(((FirBasedSymbol<?>) type).getFir(), ownerSymbol);
        } else if (type instanceof FirFile) {
            return ((FirFile) type).getName();
        }

        return resolveSignature(type, ownerSymbol);
    }

    /**
     * Interpret various parts of the Kotlin tree for type attribution.
     * This method should only be called by signature.
     */
    private String resolveSignature(Object type, @Nullable FirBasedSymbol<?> ownerSymbol) {
        if (type instanceof ConeTypeProjection) {
            return coneTypeProjectionSignature((ConeTypeProjection) type);
        } else if (type instanceof FirConstExpression) {
            return signature(((FirConstExpression<?>) type).getTypeRef(), ownerSymbol);
        } else if (type instanceof FirEqualityOperatorCall) {
            return signature(((FirEqualityOperatorCall) type).getTypeRef(), ownerSymbol);
        } else if (type instanceof FirFunctionTypeRef) {
            return signature(((FirFunctionTypeRef) type).getReturnTypeRef(), ownerSymbol);
        } else if (type instanceof FirNamedArgumentExpression) {
            return signature(((FirNamedArgumentExpression) type).getTypeRef(), ownerSymbol);
        } else if (type instanceof FirLambdaArgumentExpression) {
            return signature(((FirLambdaArgumentExpression) type).getTypeRef(), ownerSymbol);
        } else if (type instanceof FirResolvedNamedReference) {
            FirBasedSymbol<?> resolvedSymbol = ((FirResolvedNamedReference) type).getResolvedSymbol();
            if (resolvedSymbol instanceof FirConstructorSymbol) {
                return signature(((FirConstructorSymbol) resolvedSymbol).getResolvedReturnTypeRef(), ownerSymbol);
            } else if (resolvedSymbol instanceof FirEnumEntrySymbol) {
                return signature(((FirEnumEntrySymbol) resolvedSymbol).getResolvedReturnTypeRef(), ownerSymbol);
            } else if (resolvedSymbol instanceof FirNamedFunctionSymbol) {
                return signature(((FirNamedFunctionSymbol) resolvedSymbol).getResolvedReturnTypeRef(), ownerSymbol);
            } else if (resolvedSymbol instanceof FirPropertySymbol) {
                return signature(((FirPropertySymbol) resolvedSymbol).getResolvedReturnTypeRef(), ownerSymbol);
            } else if (resolvedSymbol instanceof FirValueParameterSymbol) {
                return signature(((FirValueParameterSymbol) resolvedSymbol).getResolvedReturnType(), ownerSymbol);
            } else {
                throw new UnsupportedOperationException("Unknown type " + type.getClass().getName());
            }
        } else if (type instanceof FirResolvedTypeRef) {
            ConeKotlinType coneKotlinType = ((FirResolvedTypeRef) type).getType();
            if (coneKotlinType instanceof ConeTypeParameterType) {
                FirClassifierSymbol<?> classifierSymbol = LookupTagUtilsKt.toSymbol(((ConeTypeParameterType) coneKotlinType).getLookupTag(), firSession);
                if (classifierSymbol != null && classifierSymbol.getFir() instanceof FirTypeParameter) {
                    return genericSignature(classifierSymbol.getFir());
                }
            }
            return coneKotlinType.getTypeArguments().length > 0 ? parameterizedTypeRef(coneKotlinType) : typeRefClassSignature(coneKotlinType);
        } else if (type instanceof FirResolvedQualifier) {
            return signature(((FirResolvedQualifier) type).getSymbol(), ownerSymbol);
        } else if (type instanceof FirStringConcatenationCall) {
            return signature(((FirStringConcatenationCall) type).getTypeRef(), ownerSymbol);
        } else if (type instanceof FirTypeParameter) {
            return genericSignature(type);
        } else if (type instanceof FirValueParameterSymbol) {
            return signature(((FirValueParameterSymbol) type).getResolvedReturnType(), ownerSymbol);
        } else if (type instanceof FirVariableAssignment) {
            return signature(((FirVariableAssignment) type).getCalleeReference(), ownerSymbol);
        } else if (type instanceof FirQualifiedAccessExpression) {
            return signature(((FirQualifiedAccessExpression) type).getTypeRef(), ownerSymbol);
        }

        throw new UnsupportedOperationException("Unexpected type " + type.getClass().getName());
    }

    /**
     * Kotlin does not support dimensioned arrays.
     */
    @Override
    public String arraySignature(Object type) {
        throw new UnsupportedOperationException("NA");
    }

    /**
     *  Build a class signature for a FirClass.
     */
    @Override
    public String classSignature(@Nullable Object type) {
        FirClass resolveType = null;
        if (type instanceof FirClass) {
            resolveType = (FirClass) type;
        } else if (type instanceof FirFunction) {
            if (type instanceof FirConstructor) {
                resolveType = convertToRegularClass(((FirResolvedTypeRef) ((FirConstructor) type).getReturnTypeRef()).getType());
            } else {
                FirFunction function = (FirFunction) type;
                resolveType = convertToRegularClass(function.getDispatchReceiverType() != null ? function.getDispatchReceiverType() :
                        ((FirResolvedTypeRef) function.getReturnTypeRef()).getType());
            }
        } else if (type instanceof FirResolvedTypeRef) {
            FirRegularClassSymbol symbol =  TypeUtilsKt.toRegularClassSymbol((((FirResolvedTypeRef)type).getType()), firSession);
            if (symbol != null) {
                resolveType = symbol.getFir();
            }
        } else if (type instanceof ConeClassLikeType) {
            FirRegularClassSymbol symbol =  TypeUtilsKt.toRegularClassSymbol(((ConeClassLikeType)type), firSession);
            if (symbol != null) {
                resolveType = symbol.getFir();
            }
        } else if (type instanceof FirFile) {
            return ((FirFile) type).getName();
        }

        if (resolveType == null) {
            return "{undefined}";
        }

        FirClassSymbol<? extends FirClass> symbol = resolveType.getSymbol();
        return convertClassIdToFqn(symbol.getClassId());
    }

    /**
     *  Build a class signature for a parameterized FirClass.
     */
    @Override
    public String parameterizedSignature(Object type) {
        StringBuilder s = new StringBuilder(classSignature(type));
        StringJoiner joiner = new StringJoiner(", ", "<", ">");
        for (FirTypeParameterRef tp : ((FirClass) type).getTypeParameters()) {
            String signature = signature(tp, ((FirClass) type).getSymbol());
            joiner.add(signature);
        }
        s.append(joiner);
        return s.toString();
    }

    /**
     *  Convert the ConeKotlinType to a {@link org.openrewrite.java.tree.JavaType} style FQN.
     */
    public String typeRefClassSignature(ConeKotlinType type) {
        ClassId classId = ConeTypeUtilsKt.getClassId(type);
        return classId == null ? "{undefined}" : convertClassIdToFqn(classId);
    }

    /**
     *  Convert the ConeKotlinType to a {@link org.openrewrite.java.tree.JavaType} style FQN.
     */
    public String parameterizedTypeRef(ConeKotlinType type) {
        ClassId classId = ConeTypeUtilsKt.getClassId(type);
        String fq = classId == null ? "{undefined}" : convertClassIdToFqn(classId);

        StringBuilder s = new StringBuilder(fq);
        StringJoiner joiner = new StringJoiner(", ", "<", ">");
        for (ConeTypeProjection argument : type.getTypeArguments()) {
            String signature = coneTypeProjectionSignature(argument);
            joiner.add(signature);
        }

        s.append(joiner);
        return s.toString();
    }

    /**
     *  Generate a generic type signature from a FirElement.
     */
    @Override
    public String genericSignature(Object type) {
        FirTypeParameter typeParameter = (FirTypeParameter) type;
        String name = typeParameter.getName().asString();

        if (typeVariableNameStack == null) {
            typeVariableNameStack = new HashSet<>();
        }

        if (!typeVariableNameStack.add(name)) {
            return "Generic{" + name + "}";
        }

        StringBuilder s = new StringBuilder("Generic{").append(name);
        StringJoiner boundSigs = new StringJoiner(", ");
        for (FirTypeRef bound : typeParameter.getBounds()) {
            if (!(bound instanceof FirImplicitNullableAnyTypeRef)) {
                boundSigs.add(signature(bound));
            }
        }

        String boundSigStr = boundSigs.toString();
        if (!boundSigStr.isEmpty()) {
            s.append(": ").append(boundSigStr);
        }

        typeVariableNameStack.remove(name);
        return s.append("}").toString();
    }

    /**
     *  Generate a ConeTypeProject signature.
     */
    public String coneTypeProjectionSignature(ConeTypeProjection type) {
        // TODO: refactor to handle recursive generics.
        String typeSignature;
        StringBuilder s = new StringBuilder();
        if (type instanceof ConeKotlinTypeProjectionIn) {
            ConeKotlinTypeProjectionIn in = (ConeKotlinTypeProjectionIn) type;
            s.append("Generic{in ");
            s.append(signature(in.getType()));
            s.append("}");
        } else if (type instanceof ConeKotlinTypeProjectionOut) {
            ConeKotlinTypeProjectionOut out = (ConeKotlinTypeProjectionOut) type;
            s.append("Generic{out ");
            s.append(signature(out.getType()));
            s.append("}");
        } else if (type instanceof ConeStarProjection) {
            s.append("Generic{*}");
        } else if (type instanceof ConeClassLikeType) {
            ConeClassLikeType classLikeType = (ConeClassLikeType) type;
            s.append(convertClassIdToFqn(classLikeType.getLookupTag().getClassId()));
            if (classLikeType.getTypeArguments().length > 0) {
                s.append("<");
                ConeTypeProjection[] typeArguments = classLikeType.getTypeArguments();
                for (int i = 0; i < typeArguments.length; i++) {
                    ConeTypeProjection typeArgument = typeArguments[i];
                    s.append(signature(typeArgument));
                    if (i < typeArguments.length - 1) {
                        s.append(", ");
                    }
                }
                s.append(">");
            }
        } else if (type instanceof ConeTypeParameterType) {
            ConeTypeParameterType typeParameterType = (ConeTypeParameterType) type;
            s.append("Generic{");
            typeSignature = convertKotlinFqToJavaFq(typeParameterType.toString());
            s.append(typeSignature);
            s.append("}");
        } else {
            throw new IllegalStateException("Implement me.");
        }

        return s.toString();
    }

    /**
     * Kotlin does not support primitives.
     */
    @Override
    public String primitiveSignature(Object type) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     *  Generate a unique variable type signature.
     */
    public String variableSignature(FirVariableSymbol<? extends FirVariable> symbol, @Nullable FirBasedSymbol<?> ownerSymbol) {
        String owner = "{undefined}";
        ConeSimpleKotlinType kotlinType = symbol.getDispatchReceiverType();
        if (kotlinType instanceof ConeClassLikeType) {
            FirRegularClass regularClass = convertToRegularClass(kotlinType);
            if (regularClass != null) {
                owner = signature(regularClass);
                if (owner.contains("<")) {
                    owner = owner.substring(0, owner.indexOf('<'));
                }
            }
        } else if (symbol.getCallableId().getClassId() != null) {
            owner = convertClassIdToFqn(symbol.getCallableId().getClassId());
            if (owner.contains("<")) {
                owner = owner.substring(0, owner.indexOf('<'));
            }
        } else if (ownerSymbol != null) {
            owner = classSignature(ownerSymbol.getFir());
        }

        return owner + "{name=" + symbol.getName().asString() + ",type=" + signature(symbol.getResolvedReturnTypeRef()) + '}';
    }

    public String methodSignature(FirFunctionCall functionCall, @Nullable FirBasedSymbol<?> ownerSymbol) {
        String owner = "{undefined}";
        if (functionCall.getExplicitReceiver() != null) {
            // Update to class sig once ref resolution is implemented.
            owner = signature(functionCall.getExplicitReceiver().getTypeRef());
        } else if (ownerSymbol != null) {
            if (ownerSymbol instanceof FirFileSymbol) {
                owner = ((FirFileSymbol) ownerSymbol).getFir().getName();
            } else if (ownerSymbol instanceof FirNamedFunctionSymbol) {
                owner = signature(((FirNamedFunctionSymbol) ownerSymbol).getFir());
            } else if (ownerSymbol instanceof FirRegularClassSymbol) {
                owner = signature(((FirRegularClassSymbol) ownerSymbol).getFir());
            }
        }
        String s = owner;

        FirNamedReference namedReference = functionCall.getCalleeReference();
        if (namedReference instanceof FirResolvedNamedReference &&
                ((FirResolvedNamedReference) namedReference).getResolvedSymbol() instanceof FirConstructorSymbol) {
            s += "{name=<constructor>,return=" + s;
        } else {
            s += "{name=" + functionCall.getCalleeReference().getName().asString() +
                    ",return=" + signature(functionCall.getTypeRef());
        }

        return s + ",parameters=" + methodArgumentSignature(functionCall.getArgumentList().getArguments()) + '}';
    }

    /**
     *  Generate the method declaration signature.
     */
    public String methodDeclarationSignature(FirFunctionSymbol<? extends FirFunction> symbol) {
        String s = symbol instanceof FirConstructorSymbol ? classSignature(symbol.getResolvedReturnTypeRef()) : classSignature(symbol.getDispatchReceiverType());

        if (symbol instanceof FirConstructorSymbol) {
            s += "{name=<constructor>,return=" + s;
        } else {
            s += "{name=" + symbol.getName().asString() +
                    ",return=" + signature(symbol.getResolvedReturnTypeRef());
        }
        return s + ",parameters=" + methodArgumentSignature(symbol) + '}';
    }

    /**
     *  Generate the method argument signature.
     */
    private String methodArgumentSignature(List<FirExpression> argumentsList) {
        StringJoiner genericArgumentTypes = new StringJoiner(",", "[", "]");
        if (argumentsList.size() == 1 && argumentsList.get(0) instanceof FirVarargArgumentsExpression) {
                FirVarargArgumentsExpression varargArgumentsExpression = (FirVarargArgumentsExpression) argumentsList.get(0);
            for (FirExpression argument : varargArgumentsExpression.getArguments()) {
                genericArgumentTypes.add(signature(argument));
            }
        } else {
            for (FirExpression firExpression : argumentsList) {
                genericArgumentTypes.add(signature(firExpression));
            }
        }
        return genericArgumentTypes.toString();
    }

    /**
     *  Generate the method argument signature.
     */
    private String methodArgumentSignature(FirFunctionSymbol<? extends FirFunction> sym) {
        StringJoiner genericArgumentTypes = new StringJoiner(",", "[", "]");
        for (FirValueParameterSymbol parameterSymbol : sym.getValueParameterSymbols()) {
                genericArgumentTypes.add(signature(parameterSymbol.getResolvedReturnType(), sym));
        }
        return genericArgumentTypes.toString();
    }

    /**
     *  Converts the ConeKotlinType to it's FirRegularClass.
     */
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

    /**
     *  Converts the Kotlin ClassId to a {@link org.openrewrite.java.tree.J} style FQN.
     */
    public static String convertClassIdToFqn(ClassId classId) {
        return convertKotlinFqToJavaFq(classId.toString());
    }

    /**
     *  Converts the Kotlin FQN to a {@link org.openrewrite.java.tree.J} style FQN.
     */
    public static String convertKotlinFqToJavaFq(String kotlinFqn) {
        String cleanedFqn = kotlinFqn
                .replace(".", "$")
                .replace("/", ".")
                .replace("?", "");
        return cleanedFqn.startsWith(".") ? cleanedFqn.replaceFirst(".", "") : cleanedFqn;
    }
}
