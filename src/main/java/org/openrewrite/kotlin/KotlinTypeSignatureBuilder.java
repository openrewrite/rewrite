package org.openrewrite.kotlin;

import org.jetbrains.kotlin.fir.FirSession;
import org.jetbrains.kotlin.fir.declarations.*;
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall;
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall;
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression;
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment;
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference;
import org.jetbrains.kotlin.fir.resolve.LookupTagUtilsKt;
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol;
import org.jetbrains.kotlin.fir.symbols.impl.*;
import org.jetbrains.kotlin.fir.types.*;
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef;
import org.jetbrains.kotlin.name.ClassId;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTypeSignatureBuilder;

import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

public class KotlinTypeSignatureBuilder implements JavaTypeSignatureBuilder {

    private final FirSession firSession;

    @Nullable
    Set<String> typeVariableNameStack;

    public KotlinTypeSignatureBuilder(FirSession firSession) {
        this.firSession = firSession;
    }

    @SuppressWarnings("ConstantConditions")
    public String signature(@Nullable Object type) {
        if (type == null) {
            return "{undefined}";
        }

        if (type instanceof FirClass) {
            return ((FirClass) type).getTypeParameters().size() > 0 ? parameterizedSignature(type) : classSignature(type);
        } else if (type instanceof FirFunction) {
            return methodDeclarationSignature(((FirFunction) type).getSymbol());
        }  else if (type instanceof FirFunctionCall) {
            return methodInvocationSignature((FirFunctionCall) type);
        } else if (type instanceof FirVariable) {
            return variableSignature(((FirVariable) type).getSymbol());
        }

        return resolveSignature(type);
    }

    private String resolveSignature(Object type) {
        if (type instanceof ConeTypeProjection) {
            return coneTypeProjectionSignature((ConeTypeProjection) type);
        } else if (type instanceof FirEqualityOperatorCall) {
            return signature(((FirEqualityOperatorCall) type).getTypeRef());
        } else if (type instanceof FirResolvedNamedReference) {
            FirBasedSymbol<?> resolvedSymbol = ((FirResolvedNamedReference) type).getResolvedSymbol();
            if (resolvedSymbol instanceof FirConstructorSymbol) {
                return signature(((FirConstructorSymbol) resolvedSymbol).getResolvedReturnTypeRef());
            } else if (resolvedSymbol instanceof FirEnumEntrySymbol) {
                return signature(((FirEnumEntrySymbol) resolvedSymbol).getResolvedReturnTypeRef());
            } else if (resolvedSymbol instanceof FirNamedFunctionSymbol) {
                return signature(((FirNamedFunctionSymbol) resolvedSymbol).getResolvedReturnTypeRef());
            } else if (resolvedSymbol instanceof FirPropertySymbol) {
                return signature(((FirPropertySymbol) resolvedSymbol).getResolvedReturnTypeRef());
            } else if (resolvedSymbol instanceof FirValueParameterSymbol) {
                return signature(((FirValueParameterSymbol) resolvedSymbol).getResolvedReturnType());
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
        } else if (type instanceof FirTypeParameter) {
            return genericSignature(type);
        } else if (type instanceof FirValueParameter) {
            return signature(((FirValueParameter) type).getReturnTypeRef());
        } else if (type instanceof FirValueParameterSymbol) {
            return signature(((FirValueParameterSymbol) type).getResolvedReturnType());
        } else if (type instanceof FirVariableAssignment) {
            return signature(((FirVariableAssignment) type).getCalleeReference());
        } else if (type instanceof FirQualifiedAccessExpression) {
            return signature(((FirQualifiedAccessExpression) type).getTypeRef());
        }

        throw new IllegalStateException("Unexpected type " + type.getClass().getName());
    }

    @Nullable
    public String methodInvocationSignature(FirFunctionCall functionCall) {
        throw new UnsupportedOperationException("Implement function call signatures.");
    }

    @Override
    public String arraySignature(Object type) {
        throw new UnsupportedOperationException("NA");
    }

    @Override
    public String classSignature(@Nullable Object type) {
        if (type == null) {
            return "{undefined}";
        }

        FirClassSymbol<? extends FirClass> symbol = ((FirClass) type).getSymbol();
        return convertClassIdToFqn(symbol.getClassId());
    }

    @Override
    public String parameterizedSignature(Object type) {
        StringBuilder s = new StringBuilder(classSignature(type));
        StringJoiner joiner = new StringJoiner(", ", "<", ">");
        for (FirTypeParameterRef tp : ((FirClass) type).getTypeParameters()) {
            String signature = signature(tp);
            joiner.add(signature);
        }
        s.append(joiner);
        return s.toString();
    }

    public String typeRefClassSignature(ConeKotlinType type) {
        ClassId classId = ConeTypeUtilsKt.getClassId(type);
        return classId == null ? "{undefined}" : convertClassIdToFqn(classId);
    }

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
            s.append(classLikeType.getLookupTag().getClassId().asFqNameString());
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
            typeSignature = typeParameterType.toString().replace("/", ".").replace("?", "");
            s.append(typeSignature);
            s.append("}");
        } else {
            throw new IllegalStateException("Implement me.");
        }

        return s.toString();
    }

    @Override
    public String primitiveSignature(Object type) {
        throw new UnsupportedOperationException("TODO");
    }

    public String variableSignature(FirVariableSymbol<? extends FirVariable> symbol) {
        String owner = "{undefined}";
        ConeSimpleKotlinType kotlinType = symbol.getDispatchReceiverType();

        if (kotlinType instanceof ConeClassLikeType) {
            FirRegularClass regularClass = convertToRegularClass(kotlinType);
            if (regularClass != null) {
                owner = signature(regularClass);
            }
            if (owner.contains("<")) {
                owner = owner.substring(0, owner.indexOf('<'));
            }
        } else if (symbol.getCallableId().getClassId() != null) {
            owner = convertClassIdToFqn(symbol.getCallableId().getClassId());
            if (owner.contains("<")) {
                owner = owner.substring(0, owner.indexOf('<'));
            }
        }

        return owner + "{name=" + symbol.getName().asString() + ",type=" + signature(symbol.getResolvedReturnTypeRef()) + '}';
    }

    public String methodDeclarationSignature(FirFunctionSymbol<? extends FirFunction> symbol) {
        String s = symbol instanceof FirConstructorSymbol ? signature(symbol.getResolvedReturnTypeRef()) : signature(symbol.getDispatchReceiverType());

        if (symbol instanceof FirConstructorSymbol) {
            s += "{name=<constructor>,return=" + s;
        } else {
            s += "{name=" + symbol.getName().asString() +
                    ",return=" + signature(symbol.getResolvedReturnTypeRef());
        }
        return s + ",parameters=" + methodArgumentSignature(symbol) + '}';
    }

    private String methodArgumentSignature(FirFunctionSymbol<? extends FirFunction> sym) {
        StringJoiner genericArgumentTypes = new StringJoiner(",", "[", "]");
        for (FirValueParameterSymbol parameterSymbol : sym.getValueParameterSymbols()) {
                genericArgumentTypes.add(signature(parameterSymbol));
        }
        return genericArgumentTypes.toString();
    }

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

    public static String convertClassIdToFqn(ClassId classId) {
        return convertKotlinFqToJavaFq(classId.toString());
    }

    public static String convertKotlinFqToJavaFq(String kotlinFqn) {
        String cleanedFqn = kotlinFqn
                .replace(".", "$")
                .replace("/", ".")
                .replace("?", "");
        return cleanedFqn.startsWith(".") ? cleanedFqn.replaceFirst(".", "") : cleanedFqn;
    }
}
