/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.style;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.JavaStyle;
import org.openrewrite.style.LineWrapSetting;

import java.util.List;

@Value
@With
public class WrappingAndBracesStyle implements JavaStyle {

    @Nullable
    Integer hardWrapAt;
    @Nullable
    ExtendsImplementsPermitsList extendsImplementsPermitsList;
    @Nullable
    ExtendsImplementsPermitsKeyword extendsImplementsPermitsKeyword;
    @Nullable
    ThrowsList throwsList; // Not yet implemented
    @Nullable
    ThrowsKeyword throwsKeyword; // Not yet implemented
    @Nullable
    MethodDeclarationParameters methodDeclarationParameters;
    @Nullable
    MethodCallArguments methodCallArguments;
    @Nullable
    MethodParentheses methodParentheses; // Not yet implemented
    @Nullable
    ChainedMethodCalls chainedMethodCalls;
    @Nullable
    IfStatement ifStatement;
    @Nullable
    ForStatement forStatement;
    @Nullable
    WhileStatement whileStatement;
    @Nullable
    DoWhileStatement doWhileStatement; // Not yet implemented
    @Nullable
    SwitchStatement switchStatement; // Not yet implemented
    @Nullable
    TryWithResources tryWithResources; // Not yet implemented
    @Nullable
    TryStatement tryStatement; // Not yet implemented
    @Nullable
    BinaryExpressions binaryExpressions; // Not yet implemented
    @Nullable
    AssignmentStatement assignmentStatement; // Not yet implemented
    @Nullable
    GroupDeclarations groupDeclarations; // Not yet implemented
    @Nullable
    TernaryOperation ternaryOperation; // Not yet implemented
    @Nullable
    ArrayInitializer arrayInitializer; // Not yet implemented
    @Nullable
    ModifierList modifierList; // Not yet implemented
    @Nullable
    AssertStatement assertStatement; // Not yet implemented
    @Nullable
    EnumConstants enumConstants; // Not yet implemented
    @Nullable
    Annotations classAnnotations; // Not yet implemented
    @Nullable
    Annotations methodAnnotations; // Not yet implemented
    @Nullable
    FieldAnnotations fieldAnnotations; // Not yet implemented
    @Nullable
    ParameterAnnotations parameterAnnotations; // Not yet implemented
    @Nullable
    Annotations localVariableAnnotations; // Not yet implemented
    @Nullable
    Annotations enumFieldAnnotations; // Not yet implemented
    @Nullable
    AnnotationParameters annotationParameters; // Not yet implemented
    @Nullable
    TextBlocks textBlocks; // Not yet implemented
    @Nullable
    RecordComponents recordComponents; // Not yet implemented
    @Nullable
    DeconstructionPatterns deconstructionPatterns; // Not yet implemented

    public enum ForceBraces {
        DoNotForce,
        WhenMultiline, // Not yet implemented
        Always
    }

    @Value
    @With
    public static class IfStatement {
        @Nullable
        ForceBraces forceBraces;
        @Nullable
        Boolean elseOnNewLine;
        @Nullable
        Boolean specialElseIfTreatment;
    }

    @Getter
    @FieldDefaults(makeFinal=true, level=AccessLevel.PRIVATE)
    @AllArgsConstructor
    @ToString
    @EqualsAndHashCode
    @With
    public static class Annotations {
        @Nullable
        LineWrapSetting wrap;
    }

    @Value
    @With
    public static class ExtendsImplementsPermitsList {
        @Nullable
        LineWrapSetting wrap;
        @Nullable
        Boolean alignWhenMultiline;
    }


    @Value
    @With
    public static class ExtendsImplementsPermitsKeyword {
        @Nullable
        LineWrapSetting wrap;
    }

    @Value
    @With
    public static class ThrowsList {
        @Nullable
        LineWrapSetting wrap;
        @Nullable
        Boolean alignWhenMultiline;
        @Nullable
        Boolean alignToMethodStart;
    }

    @Value
    @With
    public static class ThrowsKeyword {
        @Nullable
        LineWrapSetting wrap;
    }

    @Value
    @With
    public static class MethodParentheses {
        @Nullable
        Boolean alignWhenMultiline;
        @Nullable
        Boolean newLineWhenBodyIsPresented;
    }

    @Value
    @With
    public static class ChainedMethodCalls {
        @Nullable
        LineWrapSetting wrap;
        @Nullable
        Boolean wrapFirstCall;
        @Nullable
        Boolean alignWhenMultiline;
        @Nullable
        List<String> builderMethods;
        @Nullable
        Boolean keepBuilderMethodsIndents; // Not yet implemented
        @Nullable
        Boolean moveSemicolonToNewLine; // Not yet implemented
    }

    @Value
    @With
    public static class MethodDeclarationParameters {
        @Nullable
        LineWrapSetting wrap;
        @Nullable
        Boolean alignWhenMultiline;
        @Nullable
        Boolean openNewLine;
        @Nullable
        Boolean closeNewLine;
    }

    @Value
    @With
    public static class MethodCallArguments {
        @Nullable
        LineWrapSetting wrap;
        @Nullable
        Boolean alignWhenMultiline;
        @Nullable
        Boolean takePriorityOverCallChainWrapping; // Not yet implemented
        @Nullable
        Boolean openNewLine;
        @Nullable
        Boolean closeNewLine;
    }

    @Value
    @With
    public static class ForStatement {
        @Nullable
        LineWrapSetting wrap;
        @Nullable
        Boolean alignWhenMultiline;
        @Nullable
        Boolean openNewLine;
        @Nullable
        Boolean closeNewLine;
        @Nullable
        ForceBraces forceBraces;
    }

    @Value
    @With
    public static class WhileStatement {
        @Nullable
        ForceBraces forceBraces;
    }

    @Value
    @With
    public static class DoWhileStatement {
        @Nullable
        ForceBraces forceBraces;
        @Nullable
        Boolean whileOnNewLine;
    }

    @Value
    @With
    public static class SwitchStatement {
        @Nullable
        LineWrapSetting wrap;
        @Nullable
        Boolean indentCaseBranches;
        @Nullable
        Boolean eachCaseOnSeparateLine;
    }

    @Value
    @With
    public static class TryWithResources {
        @Nullable
        LineWrapSetting wrap;
        @Nullable
        Boolean alignWhenMultiline;
        @Nullable
        Boolean openNewLine;
    }

    @Value
    @With
    public static class TryStatement {
        @Nullable
        Boolean catchOnNewLine;
        @Nullable
        Boolean finallyOnNewLine;
        @Nullable
        LineWrapSetting typesInMulticatch;
        @Nullable
        Boolean alignTypesInMultiCatch;
    }

    @Value
    @With
    public static class BinaryExpressions {
        @Nullable
        LineWrapSetting wrap;
        @Nullable
        Boolean alignWhenMultiline;
        @Nullable
        Boolean operationSignOnNextLine;
        @Nullable
        Boolean alignParenthesizedWhenMultiline;
        @Nullable
        Boolean openNewLine;
        @Nullable
        Boolean closeNewLine;
    }

    @Value
    @With
    public static class AssignmentStatement {
        @Nullable
        LineWrapSetting wrap;
        @Nullable
        Boolean assignmentSignOnNextLine;
    }

    @Value
    @With
    public static class GroupDeclarations {
        @Nullable
        Boolean alignFieldsInColumns;
        @Nullable
        Boolean alignVariablesInColumns;
        @Nullable
        Boolean alignAssignmentsInColumns;
        @Nullable
        Boolean alignSimpleMethodsInColumns;
    }

    @Value
    @With
    public static class TernaryOperation {
        @Nullable
        LineWrapSetting wrap;
        @Nullable
        Boolean alignWhenMultiline;
        @Nullable
        Boolean questionMarkAndColonOnNextLine;
    }

    @Value
    @With
    public static class ArrayInitializer {
        @Nullable
        LineWrapSetting wrap;
        @Nullable
        Boolean alignWhenMultiline;
        @Nullable
        Boolean newLineAfterOpeningCurly;
        @Nullable
        Boolean placeClosingCurlyOnNewLine;
    }

    @Value
    @With
    public static class ModifierList {
        @Nullable
        Boolean wrapAfterModifierList;
    }

    @Value
    @With
    public static class AssertStatement {
        @Nullable
        LineWrapSetting wrap;
        @Nullable
        Boolean openNewLine;
        @Nullable
        Boolean colonSignsOnNextLine;
    }

    @Value
    @With
    public static class EnumConstants {
        @Nullable
        LineWrapSetting wrap;
    }

    @Getter
    @FieldDefaults(makeFinal=true, level=AccessLevel.PRIVATE)
    @ToString
    @EqualsAndHashCode(callSuper = true)
    public static class FieldAnnotations extends Annotations {
        @Nullable
        Boolean doNotWrapAfterSingleAnnotation;

        public FieldAnnotations(@Nullable LineWrapSetting wrap, @Nullable Boolean doNotWrapAfterSingleAnnotation) {
            super(wrap);
            this.doNotWrapAfterSingleAnnotation = doNotWrapAfterSingleAnnotation;
        }

        public FieldAnnotations withDoNotWrapAfterSingleAnnotation(Boolean doNotWrapAfterSingleAnnotation) {
            return new FieldAnnotations(this.getWrap(), doNotWrapAfterSingleAnnotation);
        }

        public FieldAnnotations withWrap(LineWrapSetting wrap) {
            return new FieldAnnotations(wrap, this.doNotWrapAfterSingleAnnotation);
        }
    }

    @Getter
    @FieldDefaults(makeFinal=true, level=AccessLevel.PRIVATE)
    @ToString
    @EqualsAndHashCode(callSuper = true)
    public static class ParameterAnnotations extends Annotations {
        @Nullable
        Boolean doNotWrapAfterSingleAnnotation;

        public ParameterAnnotations(@Nullable LineWrapSetting wrap, @Nullable Boolean doNotWrapAfterSingleAnnotation) {
            super(wrap);
            this.doNotWrapAfterSingleAnnotation = doNotWrapAfterSingleAnnotation;
        }

        public ParameterAnnotations withDoNotWrapAfterSingleAnnotation(Boolean doNotWrapAfterSingleAnnotation) {
            return new ParameterAnnotations(this.getWrap(), doNotWrapAfterSingleAnnotation);
        }

        public ParameterAnnotations withWrap(LineWrapSetting wrap) {
            return new ParameterAnnotations(wrap, this.doNotWrapAfterSingleAnnotation);
        }
    }

    @Value
    @With
    public static class AnnotationParameters {
        @Nullable
        LineWrapSetting wrap;
        @Nullable
        Boolean alignWhenMultiline;
        @Nullable
        Boolean openNewLine;
        @Nullable
        Boolean closeNewLine;
    }

    @Value
    @With
    public static class TextBlocks {
        @Nullable
        Boolean alignWhenMultiline;
    }

    @Value
    @With
    public static class RecordComponents {
        @Nullable
        LineWrapSetting wrap;
        @Nullable
        Boolean alignWhenMultiline;
        @Nullable
        Boolean openNewLine;
        @Nullable
        Boolean closeNewLine;
        @Nullable
        Boolean newLineForAnnotations;
    }

    @Value
    @With
    public static class DeconstructionPatterns {
        @Nullable
        LineWrapSetting wrap;
        @Nullable
        Boolean alignWhenMultiline;
        @Nullable
        Boolean openNewLine;
        @Nullable
        Boolean closeNewLine;
    }
}
