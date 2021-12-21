package org.openrewrite.java;

import org.junit.jupiter.api.Test;

public interface JavaTypeSignatureBuilderTest {

    void arraySignature();

    void classSignature();

    void primitiveSignature();

    void parameterizedSignature();

    void genericTypeVariable();

    void genericVariableContravariant();

    void traceySpecial();

    void genericVariableMultipleBounds();

    void genericTypeVariableUnbounded();
}
