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
package model;

import generate.Skip;
import org.openrewrite.cobol.tree.CobolContainer;
import org.openrewrite.internal.lang.Nullable;

public interface Cobol {

    class TableCall implements Identifier {
        QualifiedDataName qualifiedDataName;
        CobolContainer<Parenthesized> subscripts;
        @Nullable
        ReferenceModifier referenceModifier;
    }

    class Parenthesized implements Cobol {
        String leftParen;
        CobolContainer<Cobol> contents;
        String rightParen;
    }

    class ReferenceModifier implements Cobol {
        String leftParen;
        ArithmeticExpression characterPosition;
        String colon;
        @Nullable
        ArithmeticExpression length;
        String rightParen;
    }

    class FunctionCall implements Identifier {
        String function;
        CobolWord functionName;
        CobolContainer<Parenthesized> arguments;
        @Nullable
        ReferenceModifier referenceModifier;
    }

    @Skip
    class ArithmeticExpression {}
    @Skip
    class Subscript {}
    @Skip
    class CobolWord {}
    @Skip
    class QualifiedDataName {}
    @Skip
    class StatementPhrase {}
    @Skip
    class ParenExpression {}
    @Skip
    class Condition {}
    @Skip
    class ProcedureName {}
}
