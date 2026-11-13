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

import {Column, DataTable, Option, Recipe} from "../src";

class Occurrence {
    @Column({
        displayName: "Symbol",
        description: "The symbol that was found."
    })
    readonly symbol!: string;

    @Column({
        displayName: "Time Saved",
        description: "Estimated time saved, in seconds.",
        type: "Long"
    })
    readonly timeSaved!: number;
}

const occurrences = new DataTable<Occurrence>(
    "org.openrewrite.test.occurrences", "Occurrences", "Symbols that were found.", Occurrence);

function columnsByName(dataTable: DataTable<any>) {
    return Object.fromEntries(dataTable.descriptor.columns.map(c => [c.name, c] as const));
}

describe("data table column types", () => {

    test("every column reports a type", () => {
        expect(occurrences.descriptor.columns.filter(c => c.type === undefined)).toEqual([]);
    });

    test("a declared type overrides the default", () => {
        expect(columnsByName(occurrences)["timeSaved"].type).toBe("Long");
    });

    test("an omitted type defaults to String without disturbing the other metadata", () => {
        expect(columnsByName(occurrences)["symbol"]).toEqual({
            name: "symbol",
            type: "String",
            displayName: "Symbol",
            description: "The symbol that was found."
        });
    });
});

class RecipeWithOptions extends Recipe {
    name = "org.openrewrite.test.recipe-with-options";
    displayName = "Recipe with options";
    description = "A recipe declaring options.";

    @Option({
        displayName: "Symbol",
        description: "The symbol to find."
    })
    symbol!: string;

    @Option({
        displayName: "Retries",
        description: "How many times to retry.",
        type: "Long"
    })
    retries!: number;
}

describe("recipe option types", () => {

    test("every option reports a type", async () => {
        const {options} = await new RecipeWithOptions().descriptor();
        expect(options.filter(o => o.type === undefined)).toEqual([]);
    });

    test("an omitted type falls back to String", async () => {
        const {options} = await new RecipeWithOptions().descriptor();
        expect(options.find(o => o.name === "symbol")!.type).toBe("String");
    });

    test("a declared type overrides the default", async () => {
        const {options} = await new RecipeWithOptions().descriptor();
        expect(options.find(o => o.name === "retries")!.type).toBe("Long");
    });
});
