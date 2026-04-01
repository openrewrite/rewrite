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
package org.openrewrite.test;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.function.Function;

/**
 * Controls the test framework's validation of invariants which are expected to hold true in an LST both before and
 * after the recipe run. Originally this applied only to validating the well-formedness of type metadata in Java LSTs
 * (hence the name TypeValidation), but has since expanded to include concepts relevant to other types of sources.
 * "InvariantValidation" would be a more accurate name, but "TypeValidation" is kept for backwards compatibility.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@Builder
public class TypeValidation {

    /**
     * Controls whether class declarations are validated to have valid type metadata.
     */
    @Builder.Default
    private boolean classDeclarations = true;

    /**
     * Controls whether identifiers declarations are validated to have valid type metadata.
     * Within even a well-formed Java LST not all identifiers typically have type metadata, so even when enabled some
     * identifiers are allowed to have null type.
     */
    @Builder.Default
    private boolean identifiers = true;

    /**
     * Controls whether method declarations are validated to have valid type metadata.
     */
    @Builder.Default
    private boolean methodDeclarations = true;

    /**
     * Controls whether field declarations are validated to have valid type metadata.
     */
    @Builder.Default
    private boolean variableDeclarations = true;

    /**
     * Controls whether method invocations are validated to have valid type metadata.
     */
    @Builder.Default
    private boolean methodInvocations = true;

    /**
     * Controls whether constructor invocations are validated to have valid type metadata.
     */
    @Builder.Default
    private boolean constructorInvocations = true;

    /**
     * Controls whether sources expected to have dependency resolution metadata in a model marker are validated to have
     * that model attached. For example, a Maven pom is expected to have a MavenResolutionResult model attached.
     */
    @Builder.Default
    private boolean dependencyModel = true;

    /**
     * Controls whether the recipe's usage of cursoring is validated to be acyclic.
     * The cursor indicates a position within a tree which has no cycles. So if a cursor ever shows that an element is
     * its own parent the recipe author has made a mistake. In some circumstances this mistake causes problems, in others
     * it is benign.
     */
    @Builder.Default
    private boolean cursorAcyclic = true;

    /**
     * Given finer control to client when they need to allow missing type metadata for a specific node.
     */
    @Builder.Default
    private Function<Object, Boolean> allowMissingType = o -> false;


    /**
     * Controls whether the LST is validated not to contain any `J.Erroneous` elements.
     */
    @Builder.Default
    private boolean erroneous = true;

    /**
     * Controls whether the LST is validated not to contain any `J.Unknown` elements.
     */
    @Builder.Default
    private boolean unknown = true;

    /**
     * Adding messages to execution context is a side effect which makes the recipe run itself stateful.
     * Potentially allows recipes to interfere with each other in surprising and hard to debug ways.
     * Problematic for all the same reasons mutable global variables or singletons are.
     */
    @Builder.Default
    private boolean immutableExecutionContext = true;

    /**
     * If ScanningRecipe.getScanner() attempts an edit during normal recipe execution it will be silently ignored.
     * This can be a source of confusion for new recipe authors that don't understand the difference between the
     * scanning and editing phases of the recipe lifecycle.
     * This validation raises an error if a scanner attempts an edit. You can disable this validation if your recipe
     * deliberately attempts an edit during scanning, and you don't care that this edit is ultimately ignored.
     */
    @Builder.Default
    private boolean immutableScanning = true;

    /**
     * It is almost always a parser bug if a non-whitespace, non-comment, characters shows up in the whitespace of an LST element.
     * In some edge cases with parsers this may be allowed.
     * If you find yourself using this setting in a normal recipe test consider reporting a bug on the parser instead.
     * If that parser bug were to be fixed it would change the behavior of recipes that expected non-whitespace in whitespace.
     */
    @Builder.Default
    private boolean allowNonWhitespaceInWhitespace = false;

    @Builder.Default
    private boolean parseAndPrintEquality = true;

    /**
     * Enable all invariant validation checks.
     */
    public static TypeValidation all() {
        return new TypeValidation();
    }

    /**
     * Skip all invariant validation checks.
     */
    public static TypeValidation none() {
        return new TypeValidation(false, false, false, false, false, false, false, false, o -> false, false, false, false, false, false, false);
    }

    static TypeValidation before(RecipeSpec testMethodSpec, RecipeSpec testClassSpec) {
        TypeValidation typeValidation = testMethodSpec.getTypeValidation() != null ?
                testMethodSpec.getTypeValidation() : testClassSpec.getTypeValidation();
        return typeValidation != null ? typeValidation : new TypeValidation();
    }

    static TypeValidation after(RecipeSpec testMethodSpec, RecipeSpec testClassSpec) {
        TypeValidation typeValidation = testMethodSpec.getAfterTypeValidation() != null ?
                testMethodSpec.getAfterTypeValidation() : testClassSpec.getAfterTypeValidation();
        // after type validation defaults to before type validation; possibly this will become stricter in the future
        return typeValidation != null ? typeValidation : before(testMethodSpec, testClassSpec);
    }
}
