package org.openrewrite.java;

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
