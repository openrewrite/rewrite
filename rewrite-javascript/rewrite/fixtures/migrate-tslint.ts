/*
 * Copyright 2026 the original author or authors.
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
import {ExecutionContext, ScanningRecipe, SourceFile, TreeVisitor} from "@openrewrite/rewrite";
import {JavaScriptParser} from "@openrewrite/rewrite/javascript";
import {Json, JsonVisitor} from "@openrewrite/rewrite/json";

const TSLINT = "tslint.json";
const ESLINT = "eslint.config.mjs";

interface Accumulator {
    rules?: string[]
    eslintConfigExists: boolean
}

function ruleNames(document: Json.Document): string[] {
    if (document.value.kind !== Json.Kind.Object) {
        return [];
    }
    for (const member of (document.value as Json.Object).members) {
        const m = member.element as Json.Member;
        const key = m.key.element;
        if (key.kind === Json.Kind.Literal && (key as Json.Literal).value === "rules" &&
            m.value.kind === Json.Kind.Object) {
            return (m.value as Json.Object).members
                .map(r => (r.element as Json.Member).key?.element)
                .filter(k => k?.kind === Json.Kind.Literal)
                .map(k => (k as Json.Literal).value as string);
        }
    }
    return [];
}

/**
 * The shape of a TSLint to ESLint migration: reads `tslint.json` while scanning,
 * generates `eslint.config.mjs` from what it read, and deletes `tslint.json`.
 */
export class MigrateTslint extends ScanningRecipe<Accumulator> {
    name = "org.openrewrite.example.javascript.migrate-tslint";
    displayName = "Migrate `tslint.json` to `eslint.config.mjs`";
    description = "Replace `tslint.json` with an `eslint.config.mjs` enabling the same rules.";

    initialValue(_ctx: ExecutionContext): Accumulator {
        return {eslintConfigExists: false};
    }

    async scanner(acc: Accumulator): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends TreeVisitor<any, ExecutionContext> {
            protected async preVisit(tree: any, _ctx: ExecutionContext): Promise<any> {
                if (tree.sourcePath === ESLINT) {
                    acc.eslintConfigExists = true;
                } else if (tree.sourcePath === TSLINT && tree.kind === Json.Kind.Document) {
                    acc.rules = ruleNames(tree);
                }
                this.stopAfterPreVisit();
                return tree;
            }
        };
    }

    async generate(acc: Accumulator, _ctx: ExecutionContext): Promise<SourceFile[]> {
        if (!acc.rules || acc.eslintConfigExists) {
            return [];
        }
        const rules = acc.rules.map(rule => `      "${rule}": "error",\n`).join("");
        const text = `export default [\n  {\n    rules: {\n${rules}    },\n  },\n];\n`;
        return [await new JavaScriptParser().parseOnly({text, sourcePath: ESLINT})];
    }

    async editorWithData(_acc: Accumulator): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends JsonVisitor<ExecutionContext> {
            protected async visitDocument(document: Json.Document, _ctx: ExecutionContext): Promise<Json | undefined> {
                return document.sourcePath === TSLINT ? undefined : document;
            }
        };
    }
}
