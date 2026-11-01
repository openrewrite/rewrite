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
import {isParseError, ParseError, ParseErrorKind} from "../parse-error";
import {Markers, MarkersKind, ParseExceptionResult} from "../markers";
import {PlainText} from "../text";
import {randomId} from "../uuid";
import {describeThrown, passed, probedBefore, thrownBy, typeName, withMarkersOf} from "./probes";

/** The kinds of source a repository holds at a path its parser didn't make a tree of, as JavaScript reads them. */
const unparsedKinds = [
    {kind: ParseErrorKind, description: "a file its parser failed on"},
    {kind: PlainText.Kind.PlainText, description: "a file read as plain text"}
];

/**
 * Runs the recipe with a source it changed in place of the file as a parse error and as plain text, and fails the test
 * if the recipe throws there.
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
            for (const {kind, description} of unparsedKinds) {
                const probe = `unparsed ${before.kind} ${kind}`;
                if (before.kind === kind || await probedBefore(this.recipe, probe)) {
                    continue;
                }
                const unparsed = await asUnparsed(before, kind);
                const thrown = await thrownBy(this.recipe, sources.map(s => s === before ? unparsed : s), recipeCtx,
                    cycles);
                if (thrown) {
                    throw new Error(this.message(unparsed, description, thrown.error));
                }
                await passed(this.recipe, probe);
            }
        }
    }

    private message(unparsed: SourceFile, description: string, thrown: unknown): string {
        const markers = unparsed.markers.markers.filter(m => m.kind !== MarkersKind.ParseExceptionResult);
        return [
            "Check the kind of source file before casting it, so that the recipe leaves a file it can't edit " +
            "unchanged instead of throwing.",
            `${this.recipe.name} threw when ${unparsed.sourcePath} was a ${typeName(unparsed.kind)} (${description})` +
            `${withMarkersOf(markers)}:`,
            describeThrown(thrown, "    "),
            "Besides the files the test gives it, a repository holds files their parser failed on and files read as " +
            "plain text, at the same paths and with the same markers as files the recipe edits.",
            "  - visit with the visitor of the language the recipe edits, such as JavaScriptVisitor, JsonVisitor or " +
            "YamlVisitor, whose isAcceptable skips other kinds of source file",
            "  - in a TreeVisitor<any, ExecutionContext>, check the kind before a cast " +
            "(`tree.kind === JS.Kind.CompilationUnit`), not the file's path or markers",
            "  - when the scanner found no file of the kind the recipe edits, generate and change nothing, instead " +
            "of reading an empty accumulator",
            "  - don't catch the exception to make this pass"
        ].join("\n");
    }
}

/**
 * The file `before` was parsed from, as the kind of source a repository holds when its parser fails on the file or
 * reads it as plain text.
 */
async function asUnparsed(before: SourceFile, kind: string): Promise<SourceFile> {
    // these come from parsing the file itself
    const kept = before.markers.markers.filter(m => m.kind !== MarkersKind.ParseExceptionResult &&
        !m.kind.endsWith("ResolutionResult"));
    const markers: Markers = {...before.markers, markers: kept};
    const file = {
        id: randomId(),
        sourcePath: before.sourcePath,
        charsetName: before.charsetName,
        charsetBomMarked: before.charsetBomMarked,
        checksum: before.checksum,
        fileAttributes: before.fileAttributes,
        text: await TreePrinters.print(before)
    };
    if (kind === ParseErrorKind) {
        const exception: ParseExceptionResult = {
            kind: MarkersKind.ParseExceptionResult,
            id: randomId(),
            parserType: "Parser",
            exceptionType: "Error",
            message: "The file could not be parsed"
        };
        return {...file, kind: ParseErrorKind, markers: {...markers, markers: [...kept, exception]}} as ParseError;
    }
    return {...file, kind: PlainText.Kind.PlainText, markers, snippets: []} as PlainText;
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
