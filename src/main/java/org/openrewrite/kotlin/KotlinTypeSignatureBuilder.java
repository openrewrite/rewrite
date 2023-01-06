package org.openrewrite.kotlin;

import org.jetbrains.kotlin.fir.FirElement;
import org.jetbrains.kotlin.fir.declarations.FirClass;
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol;
import org.jetbrains.kotlin.js.common.Symbol;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTypeSignatureBuilder;

import java.util.Set;

public class KotlinTypeSignatureBuilder implements JavaTypeSignatureBuilder {

    @Nullable
    Set<String> typeVariableNameStack;

    @Override
    public String signature(@Nullable Object t) {
        return signature((FirElement) t);
    }

    private String signature(@Nullable FirElement type) {
        if (type == null) {
            return "{undefined}";
        } else if (type instanceof FirClass) {
            return ((FirClass) type).getTypeParameters().size() > 0 ? parameterizedSignature(type) : classSignature(type);
        }

        throw new IllegalStateException("Unexpected type " + type.getClass().getName());
    }

    @Override
    public String arraySignature(Object type) {
        return null;
    }

    @Override
    public String classSignature(Object type) {
        FirClassSymbol<? extends FirClass> symbol = ((FirClass) type).getSymbol();
        return symbol.getClassId().asFqNameString();
    }

    @Override
    public String genericSignature(Object type) {
        return null;
    }

    @Override
    public String parameterizedSignature(Object type) {
        return null;
    }

    @Override
    public String primitiveSignature(Object type) {
        return null;
    }
}
