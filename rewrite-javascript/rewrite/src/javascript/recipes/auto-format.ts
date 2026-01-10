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

import {Recipe, RecipeVisitor} from "../../recipe";
import {ExecutionContext} from "../../execution";
import {AutoformatVisitor} from "../format";

/**
 * Formats JavaScript/TypeScript code using a comprehensive set of formatting rules.
 *
 * This recipe applies formatting based on styles detected at parse time:
 * - If PrettierStyle marker is present, uses Prettier formatting
 * - If Autodetect marker is present, uses auto-detected project style
 * - Otherwise, defaults to IntelliJ IDEA style
 *
 * The detected formatting includes:
 * - Tabs vs spaces preference
 * - Indent size (2, 4, etc.)
 * - ES6 import/export brace spacing
 */
export class AutoFormat extends Recipe {
    readonly name = "org.openrewrite.javascript.format.auto-format";
    readonly displayName = "Auto-format JavaScript/TypeScript code";
    readonly description = "Format JavaScript and TypeScript code using formatting rules auto-detected from the project's existing code style.";

    async editor(): Promise<RecipeVisitor> {
        // AutoformatVisitor looks up styles from source file markers
        return new AutoformatVisitor();
    }
}
