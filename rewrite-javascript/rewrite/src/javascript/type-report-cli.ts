#!/usr/bin/env node
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
import * as fs from "fs";
import * as path from "path";
import {Command, CommanderError} from "commander";
import {JavaScriptParser} from "./parser";
import {JS} from "./tree";
import {LstDebugPrinter} from "./tree-debug";
import {printTypes} from "./type-report";

/**
 * `rewrite-javascript-types <file>`: print a file's type attribution. The in-process parse
 * behind it is for diagnostics only.
 */
export async function main(argv: string[]): Promise<number> {
    const program = new Command()
        .name("rewrite-javascript-types")
        .description("Print a parsed JavaScript/TypeScript LST annotated with its type attribution.")
        .argument("<path>", "the file to parse")
        .option("--project-root <dir>",
            "directory node_modules and tsconfig resolve against (default: the nearest one with a package.json)")
        .option("--only-missing", "list only the nodes whose type is missing")
        .option("--all", "list every node with a type slot, not just calls and declarations")
        .option("--supertypes", "show each declaring type's ancestry, which bounds how general a MethodMatcher pattern can be")
        .option("--tree", "print the nested structure with each node's type instead of the listing")
        .option("--json", "print the listing as JSON")
        .exitOverride();

    try {
        await program.parseAsync(argv, {from: "user"});
        const opts = program.opts();
        if (opts.tree) {
            // The tree lists every node already, so a flag that narrows or reshapes the
            // listing is a mistake worth naming rather than silently ignoring.
            for (const flag of ["onlyMissing", "all", "supertypes", "json"]) {
                if (opts[flag]) {
                    program.error(`--tree does not take --${flag.replace(/[A-Z]/g, c => "-" + c.toLowerCase())}`);
                }
            }
        }

        const file = path.resolve(program.args[0]);
        const root = path.resolve(opts.projectRoot ?? projectRootOf(file));
        const parsed = (await new JavaScriptParser({relativeTo: root}).parse(file).next()).value;
        if (parsed?.kind !== JS.Kind.CompilationUnit) {
            process.stderr.write(`could not parse ${file}\n`);
            return 1;
        }

        if (opts.tree) {
            new LstDebugPrinter({includeCursorMessages: false, includeTypes: true}).print(parsed);
        } else {
            await printTypes(parsed, {
                onlyMissing: opts.onlyMissing,
                all: opts.all,
                supertypes: opts.supertypes,
                json: opts.json,
            });
        }
        return 0;
    } catch (e) {
        if (e instanceof CommanderError) {
            return e.exitCode;
        }
        throw e;
    }
}

function projectRootOf(file: string): string {
    for (let dir = path.dirname(file); ; dir = path.dirname(dir)) {
        if (fs.existsSync(path.join(dir, "package.json"))) {
            return dir;
        }
        if (path.dirname(dir) === dir) {
            return path.dirname(file);
        }
    }
}

if (typeof require !== "undefined" && typeof module !== "undefined" && require.main === module) {
    main(process.argv.slice(2)).then(code => process.exitCode = code);
}
