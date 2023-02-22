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

import org.openrewrite.kotlin.tree.K;

public class KotlinIsoVisitor<P> extends KotlinVisitor<P> {

    @Override
    public K.CompilationUnit visitCompilationUnit(K.CompilationUnit cu, P p) {
        return (K.CompilationUnit) super.visitCompilationUnit(cu, p);
    }

    @Override
    public K.Binary visitBinary(K.Binary binary, P p) {
        return (K.Binary) super.visitBinary(binary, p);
    }

    @Override
    public K.FunctionType visitFunctionType(K.FunctionType functionType, P p) {
        return (K.FunctionType) super.visitFunctionType(functionType, p);
    }

    @Override
    public K.KReturn visitKReturn(K.KReturn kReturn, P p) {
        return (K.KReturn) super.visitKReturn(kReturn, p);
    }

    @Override
    public K.KString visitKString(K.KString kString, P p) {
        return (K.KString) super.visitKString(kString, p);
    }

    @Override
    public K.KString.Value visitKStringValue(K.KString.Value value, P p) {
        return (K.KString.Value) super.visitKStringValue(value, p);
    }

    @Override
    public K.ListLiteral visitListLiteral(K.ListLiteral listLiteral, P p) {
        return (K.ListLiteral) super.visitListLiteral(listLiteral, p);
    }

    @Override
    public K.When visitWhen(K.When when, P p) {
        return (K.When) super.visitWhen(when, p);
    }

    @Override
    public K.WhenBranch visitWhenBranch(K.WhenBranch whenBranch, P p) {
        return (K.WhenBranch) super.visitWhenBranch(whenBranch, p);
    }
}
