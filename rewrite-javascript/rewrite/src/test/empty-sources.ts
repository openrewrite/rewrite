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
import type {SourceSpec} from "./rewrite-test";
import {Recipe} from "../recipe";
import {ExecutionContext} from "../execution";
import {SourceFile} from "../tree";
import {Result} from "../run";
import {TreePrinters} from "../print";
import {describeThrown, probeOnce, thrownBy, typeName, withMarkersOf} from "./probes";

/** Runs the recipe on an empty file of each kind it changed, and fails the test if the recipe throws there. */
export class EmptySources {
    private readonly changed: Set<SourceFile>;

    constructor(private readonly recipe: Recipe, results: Result[]) {
        this.changed = new Set(results.filter(r => r.before && r.after !== r.before).map(r => r.before!));
    }

    async assertNoneThrow(parsed: [SourceSpec<any>, SourceFile][], parseCtx: ExecutionContext,
                          recipeCtx: ExecutionContext, cycles: number): Promise<void> {
        const kinds = new Set<string>();
        for (const [spec, before] of parsed) {
            const probe = `empty ${before.kind} ${spec.ext}`;
            if (!this.changed.has(before) || kinds.has(probe)) {
                continue;
            }
            kinds.add(probe);
            await probeOnce(this.recipe, probe, async () => {
                const empty = await parseEmpty(before, spec, parseCtx);
                const thrown = empty ? await thrownBy(this.recipe, [empty], recipeCtx, cycles) : undefined;
                if (empty && thrown) {
                    throw new Error(this.message(empty, thrown.error));
                }
            });
        }
    }

    private message(empty: SourceFile, thrown: unknown): string {
        return [
            "Check for the structure the recipe edits before using it, so that it handles an empty file or leaves " +
            "it unchanged instead of throwing.",
            `${this.recipe.name} threw when run on an empty ${typeName(empty.kind)} at ${empty.sourcePath}` +
            `${withMarkersOf(empty.markers.markers)}:`,
            describeThrown(thrown, "    "),
            "The test gives the recipe only files that contain what it edits, but repositories also hold empty " +
            "files and files without that block, such as an empty index.ts, a placeholder tsconfig.json or an " +
            "empty .eslintrc.yml.",
            "  - return the tree unchanged when what the visitor assumes is absent: check the kind before a cast " +
            "(`document.value.kind === Json.Kind.Object`), the length before an index (`cu.statements.length > 0`) " +
            "and undefined before a dereference (`documents.documents[0]?.block`)",
            "  - if the recipe adds what is missing, build that content correctly for an empty file too",
            "  - don't catch the exception to make this pass",
            "Then add a test with an empty source, such as typescript(\"\"), json(\"\") or yaml(\"\"), that states the " +
            "intended result."
        ].join("\n");
    }
}

/**
 * An empty file at the source's path, with the markers that describe the source's project, or undefined for a kind of
 * file that can't be empty.
 */
async function parseEmpty(before: SourceFile, spec: SourceSpec<any>,
                          parseCtx: ExecutionContext): Promise<SourceFile | undefined> {
    // a file its parser resolves a project from, such as a package.json, can't be empty
    if (before.markers.markers.some(m => m.kind.endsWith("ResolutionResult"))) {
        return undefined;
    }
    let empty: SourceFile;
    try {
        const parsed: SourceFile[] = [];
        for await (const sourceFile of spec.parser(parseCtx).parse({text: "", sourcePath: before.sourcePath})) {
            parsed.push(sourceFile);
        }
        // some kinds of file can't be empty
        if (parsed.length !== 1 || parsed[0].kind !== before.kind || await TreePrinters.print(parsed[0]) !== "") {
            return undefined;
        }
        empty = parsed[0];
    } catch {
        return undefined;
    }
    const present = new Set(empty.markers.markers.map(m => m.kind));
    const added = before.markers.markers.filter(m => !present.has(m.kind));
    return {...empty, markers: {...empty.markers, markers: [...empty.markers.markers, ...added]}};
}
