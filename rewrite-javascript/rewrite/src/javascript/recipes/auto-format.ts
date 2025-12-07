/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {ScanningRecipe} from "../../recipe";
import {TreeVisitor} from "../../visitor";
import {ExecutionContext} from "../../execution";
import {AutoformatVisitor} from "../format";
import {Autodetect, Detector} from "../autodetect";
import {JavaScriptVisitor} from "../visitor";
import {JS} from "../tree";
import {J} from "../../java";

/**
 * Accumulator for the AutoFormat scanning recipe.
 * Holds the Detector that collects formatting statistics during the scan phase.
 */
interface AutoFormatAccumulator {
    detector: Detector;
    detectedStyles?: Autodetect;
}

/**
 * Formats JavaScript/TypeScript code using a comprehensive set of formatting rules.
 *
 * This is a scanning recipe that:
 * 1. Scans all source files to detect the project's existing formatting style
 * 2. Applies consistent formatting based on the detected style
 *
 * The detected formatting includes:
 * - Tabs vs spaces preference
 * - Indent size (2, 4, etc.)
 * - ES6 import/export brace spacing
 *
 * If no clear style is detected, defaults to IntelliJ IDEA style.
 */
export class AutoFormat extends ScanningRecipe<AutoFormatAccumulator> {
    readonly name = "org.openrewrite.javascript.format.auto-format";
    readonly displayName = "Auto-format JavaScript/TypeScript code";
    readonly description = "Format JavaScript and TypeScript code using formatting rules auto-detected from the project's existing code style.";

    initialValue(_ctx: ExecutionContext): AutoFormatAccumulator {
        return {
            detector: Autodetect.detector()
        };
    }

    async scanner(acc: AutoFormatAccumulator): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends JavaScriptVisitor<ExecutionContext> {
            protected async visitJsCompilationUnit(cu: JS.CompilationUnit, ctx: ExecutionContext): Promise<J | undefined> {
                await acc.detector.sample(cu);
                return cu;
            }
        };
    }

    async editorWithData(acc: AutoFormatAccumulator): Promise<TreeVisitor<any, ExecutionContext>> {
        // Build detected styles once (lazily on first edit)
        if (!acc.detectedStyles) {
            acc.detectedStyles = acc.detector.build();
        }

        // Pass detected styles to the AutoformatVisitor
        // Autodetect is a NamedStyles, so pass it as an array
        return new AutoformatVisitor(undefined, [acc.detectedStyles]);
    }
}
