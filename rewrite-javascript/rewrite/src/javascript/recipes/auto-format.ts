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

import {Recipe} from "../../recipe";
import {TreeVisitor} from "../../visitor";
import {ExecutionContext} from "../../execution";
import {AutoformatVisitor} from "../format";

/**
 * Formats JavaScript/TypeScript code using a comprehensive set of formatting rules.
 *
 * This recipe applies the following formatting:
 * - Normalizes whitespace
 * - Ensures minimum viable spacing
 * - Applies blank line rules
 * - Applies wrapping and braces rules
 * - Applies spacing rules
 * - Applies tabs and indentation rules
 *
 * The formatting rules are determined by the style settings attached to the source file,
 * or defaults to IntelliJ IDEA style if no custom style is specified.
 */
export class AutoFormat extends Recipe {
    readonly name = "org.openrewrite.javascript.format.auto-format";
    readonly displayName = "Auto-format JavaScript/TypeScript code";
    readonly description = "Format JavaScript and TypeScript code using a comprehensive set of formatting rules based on the project's style settings.";

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new AutoformatVisitor();
    }
}
