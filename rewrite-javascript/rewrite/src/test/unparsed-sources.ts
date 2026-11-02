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
import {Recipe} from "../recipe";
import {ExecutionContext} from "../execution";
import {SourceFile} from "../tree";
import {Result} from "../run";
import {TreePrinters} from "../print";
import {isParseError} from "../parse-error";
import {MarkersKind} from "../markers";
import {PlainText} from "../text";
import {randomId} from "../uuid";
import {describeThrown, probeOnce, thrownBy, typeName, withMarkersOf} from "./probes";

/**
 * Runs the recipe without a source it changed, as it meets a file its parser failed on, and with that source as plain
 * text, and fails the test if the recipe throws there.
 */
export class UnparsedSources {
    private readonly changed: Set<SourceFile>;

    constructor(private readonly recipe: Recipe, results: Result[]) {
        this.changed = new Set(results.filter(r => r.before && r.after !== r.before).map(r => r.before!));
    }

    async assertNoneThrow(sources: SourceFile[], recipeCtx: ExecutionContext, cycles: number): Promise<void> {
        if (await declaredInline(this.recipe)) {
            return;
        }
        const probedKinds = new Set<string>();
        for (const before of sources) {
            if (!this.changed.has(before) || isParseError(before) || probedKinds.has(before.kind)) {
                continue;
            }
            probedKinds.add(before.kind);
            await probeOnce(this.recipe, `unparsed ${before.kind} missing`, async () => {
                const thrown = await thrownBy(this.recipe, sources.filter(s => s !== before), recipeCtx, cycles);
                if (thrown) {
                    throw new Error(this.missingMessage(before, thrown.error));
                }
            });
            if (before.kind !== PlainText.Kind.PlainText) {
                await probeOnce(this.recipe, `unparsed ${before.kind} ${PlainText.Kind.PlainText}`, async () => {
                    const unparsed = await asPlainText(before);
                    const thrown = await thrownBy(this.recipe, sources.map(s => s === before ? unparsed : s),
                        recipeCtx, cycles);
                    if (thrown) {
                        throw new Error(this.plainTextMessage(unparsed, thrown.error));
                    }
                });
            }
        }
    }

    private missingMessage(missing: SourceFile, thrown: unknown): string {
        return [
            "Check that the scanner found the files the recipe needs before using what it keeps of them, so that " +
            "the recipe changes nothing instead of throwing when one is missing.",
            `${this.recipe.name} threw when ${missing.sourcePath} was missing, which is how a JavaScript recipe sees ` +
            "a file its parser failed on or left unparsed:",
            describeThrown(thrown, "    "),
            "Besides the files the test gives it, a repository holds files their parser failed on or left " +
            "unparsed. The host sends a JavaScript recipe only the JS/TS, JSON, YAML and plain-text files it " +
            "parsed, so such a file is simply not there for the recipe.",
            "  - when the scanner found no file of the kind the recipe needs, generate and change nothing, instead " +
            "of reading an empty accumulator",
            "  - check for undefined before using what the scanner keeps of a file it may not find " +
            "(`acc.get(\"package.json\")?.version`)",
            "  - don't catch the exception to make this pass"
        ].join("\n");
    }

    private plainTextMessage(unparsed: SourceFile, thrown: unknown): string {
        return [
            "Check the kind of source file before casting it, so that the recipe leaves a file it can't edit " +
            "unchanged instead of throwing.",
            `${this.recipe.name} threw when ${unparsed.sourcePath} was a ${typeName(unparsed.kind)} (a file read ` +
            `as plain text)${withMarkersOf(unparsed.markers.markers)}:`,
            describeThrown(thrown, "    "),
            "Besides the files the test gives it, a repository holds files read as plain text, at the same paths " +
            "and with the same markers as files the recipe edits.",
            "  - visit with the visitor of the language the recipe edits, such as JavaScriptVisitor, JsonVisitor or " +
            "YamlVisitor, whose isAcceptable skips other kinds of source file",
            "  - in a TreeVisitor<any, ExecutionContext>, check the kind before a cast " +
            "(`tree.kind === JS.Kind.CompilationUnit`), not the file's path or markers",
            "  - don't catch the exception to make this pass"
        ].join("\n");
    }
}

/** The file `before` was parsed from, as the plain text a repository holds when no parser claims the file. */
async function asPlainText(before: SourceFile): Promise<PlainText> {
    return {
        kind: PlainText.Kind.PlainText,
        id: randomId(),
        // these come from parsing the file itself
        markers: {
            ...before.markers,
            markers: before.markers.markers.filter(m => m.kind !== MarkersKind.ParseExceptionResult &&
                !m.kind.endsWith("ResolutionResult"))
        },
        sourcePath: before.sourcePath,
        charsetName: before.charsetName,
        charsetBomMarked: before.charsetBomMarked,
        checksum: before.checksum,
        fileAttributes: before.fileAttributes,
        text: await TreePrinters.print(before),
        snippets: []
    };
}

/**
 * Whether the recipe, or one in its recipe list, is an anonymous class, which a test declares inline for itself as Java
 * tests declare their scaffolding.
 */
async function declaredInline(recipe: Recipe): Promise<boolean> {
    if (recipe.constructor.name === "") {
        return true;
    }
    for (const child of await recipe.recipeList()) {
        if (await declaredInline(child)) {
            return true;
        }
    }
    return false;
}
