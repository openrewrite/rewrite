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
import * as path from "path";
import {Recipe} from "../recipe";
import {Cursor, isSourceFile, SourceFile, Tree} from "../tree";
import {Result} from "../run";
import {TreePrinters} from "../print";
import {UUID} from "../uuid";
import {J} from "../java";
import {JavaScriptVisitor} from "../javascript";
import type {FormattingListener} from "../javascript/format";

interface Request {
    readonly tree: Tree
    readonly sourceFile: UUID
    readonly callSite: string
}

/**
 * Finds recipes that auto-format a whole source file or class that existed before the run. Test inputs are usually
 * formatted already, so the reformatting this causes on real code rarely shows in an expected output.
 */
export class FormattingScope implements FormattingListener {
    private readonly requests: Request[] = [];
    private readonly requested = new Set<Tree>();

    constructor(private readonly recipe: Recipe) {
    }

    onFormat(tree: Tree, stopAfter: Tree | undefined, cursor: Cursor | undefined): void {
        if (stopAfter || !isWhole(tree) || this.requested.has(tree)) {
            return;
        }
        this.requested.add(tree);
        const sourceFile = isSourceFile(tree) ? tree : cursor?.firstEnclosing(isSourceFile);
        const site = sourceFile && callSite(new Error().stack ?? "");
        if (sourceFile && site) {
            this.requests.push({tree, sourceFile: sourceFile.id, callSite: site});
        }
    }

    async assertFormattedOnlyWhatChanged(results: Result[]): Promise<void> {
        if (this.requests.length === 0 || await isFormatter(this.recipe)) {
            return;
        }
        const treesByFile = new Map<UUID, Map<UUID, Tree>>();
        const found = new Map<string, Set<string>>();
        for (const request of this.requests) {
            const result = results.find(r => r.before?.id === request.sourceFile && r.after && r.after !== r.before);
            if (!result || await onlyWhitespaceChanged(result)) {
                continue;
            }
            const before = result.before!;
            let trees = treesByFile.get(before.id);
            if (!trees) {
                treesByFile.set(before.id, trees = await treesById(before));
            }
            const original = trees.get(request.tree.id);
            if (original && !await holdsNoneOfItsChildren(original, request.tree)) {
                let targets = found.get(request.callSite);
                if (!targets) {
                    found.set(request.callSite, targets = new Set());
                }
                targets.add(`${describe(request.tree)} of ${before.sourcePath}`);
            }
        }
        if (found.size > 0) {
            throw new Error(this.message(found));
        }
    }

    private message(found: Map<string, Set<string>>): string {
        return [
            "Format only what the recipe changed, not the whole class or file around it. These calls of " +
            `${this.recipe.name} auto-formatted a whole file or class that existed before the run, with no stopAfter:`,
            ...[...found].flatMap(([site, targets]) => [`  ${site}`, ...[...targets].map(target => `    ${target}`)]),
            "That rewrites the whitespace of code the recipe did not change. Test inputs are usually formatted " +
            "already, so expected outputs rarely show it, but on real code the diff will carry unrelated " +
            "reformatting. Instead:",
            "  - format the element you created or replaced: maybeAutoFormat(original, changed, ctx) on that " +
            "element, or autoFormat(newElement, ctx, undefined, cursor) with a cursor on its parent",
            "  - when a new element only formats right inside its container, format a copy of the container that " +
            "holds just the new element, and add the formatted element to the original",
            "  - after changing a declaration's decorators or modifiers, stop after its header: " +
            "maybeAutoFormat(c, updated, ctx, updated.name, this.cursor.parent)",
            "  - when you remove an element, give its prefix to the element that takes its place",
            "  - re-indent statements you moved with autoIndent(..) rather than autoFormat",
            "  - a template already formats what it inserts, so drop the extra call",
            "Recipes named with a `format` segment, such as org.openrewrite.javascript.format.auto-format, are exempt. " +
            "Any other recipe whose purpose is formatting opts out with spec.checkMinimalFormatting = false."
        ].join("\n");
    }
}

/** A source file or a class. */
function isWhole(tree: Tree): boolean {
    return isSourceFile(tree) || tree.kind === J.Kind.ClassDeclaration;
}

interface Frame {
    readonly fn: string
    readonly location: string
}

/**
 * The code that asked for the format, from the stack of the request, or undefined when that is a formatter, a
 * template, or the scheduler running a recipe whose own visitor is a formatter.
 */
function callSite(stack: string): string | undefined {
    const frames: Frame[] = stack.split("\n").map(line => line.trim()).filter(line => line.startsWith("at "))
        .map(line => {
            const m = /^at (?:async )?(?:(.*?) \((.*)\)|(.*))$/.exec(line)!;
            return {fn: m[1] ?? "", location: (m[2] ?? m[3]).split(process.cwd() + path.sep).join("")};
        });
    // the listener, the formatter and the helpers that run it
    let i = frames.findIndex(f => /\.visit$/.test(f.fn) && inFormat(f));
    if (i < 0) {
        return undefined;
    }
    let helper: string | undefined;
    for (i++; i < frames.length && inFormat(frames[i]) && /^(maybeAutoFormat|autoFormat)$/.test(frames[i].fn); i++) {
        helper = frames[i].fn;
    }
    // a precondition and a scanning recipe's editor hand the tree on to the visitor they wrap, after an await
    while (i < frames.length && (isWrapper(frames[i]) || frames[i].location.startsWith("node:"))) {
        i++;
    }
    const caller = frames[i];
    if (!caller) {
        return undefined;
    } else if (/(^|[\\/])(src|dist)[\\/]visitor\.[jt]s:/.test(caller.location)) {
        // the visitor that asked is gone from the stack by the time its after-visitors run
        return "A visitor's afterVisit.push(new AutoformatVisitor()) ran on";
    } else if (inFormat(caller) || /[\\/]templating[\\/]/.test(caller.location) ||
        /(^|[\\/])(src|dist)[\\/]run\.[jt]s:/.test(caller.location)) {
        return undefined;
    }
    return `${caller.fn || "<anonymous>"} (${caller.location}) ${helper ? `called ${helper} on` : "ran an AutoformatVisitor on"}`;
}

function inFormat(frame: Frame): boolean {
    return /[\\/]format[\\/]/.test(frame.location);
}

/** The visit of a precondition or of a scanning recipe's editor, which runs the visitor it wraps. */
function isWrapper(frame: Frame): boolean {
    return /\.visit$/.test(frame.fn) && /(^|[\\/])(src|dist)[\\/](preconditions|recipe)\.[jt]s:/.test(frame.location);
}

/** A formatting recipe may format whole files, and so may the recipes run alongside it. */
async function isFormatter(recipe: Recipe): Promise<boolean> {
    if (/(^|\.)format\./.test(recipe.name)) {
        return true;
    }
    for (const child of await recipe.recipeList()) {
        if (await isFormatter(child)) {
            return true;
        }
    }
    return false;
}

async function onlyWhitespaceChanged(result: Result): Promise<boolean> {
    const [before, after] = [await TreePrinters.print(result.before!), await TreePrinters.print(result.after!)];
    return before.replace(/\s+/g, "") === after.replace(/\s+/g, "");
}

async function treesById(sourceFile: SourceFile): Promise<Map<UUID, Tree>> {
    const trees = new Map<UUID, Tree>();
    await new class extends JavaScriptVisitor<undefined> {
        protected override async preVisit(tree: J): Promise<J | undefined> {
            trees.set(tree.id, tree);
            return tree;
        }
    }().visit(sourceFile, undefined);
    return trees;
}

/**
 * A tree formatted with none of the children it had reformats no existing code below it: a class with its body
 * emptied to be put back afterwards, or a copy of a container holding only a new element. A tree that had no children
 * to begin with would format them on real code.
 */
async function holdsNoneOfItsChildren(original: Tree, formatted: Tree): Promise<boolean> {
    const children = await childrenOf(original);
    if (children.size === 0) {
        return false;
    }
    for (const child of await childrenOf(formatted)) {
        if (children.has(child)) {
            return false;
        }
    }
    return true;
}

/** The members of a class's body, or the trees directly below any other tree. */
async function childrenOf(tree: Tree): Promise<Set<UUID>> {
    if (tree.kind === J.Kind.ClassDeclaration) {
        return new Set((tree as J.ClassDeclaration).body.statements.map(s => s.element.id));
    }
    const children = new Set<UUID>();
    await new class extends JavaScriptVisitor<undefined> {
        protected override async preVisit(t: J): Promise<J | undefined> {
            if (t !== tree) {
                children.add(t.id);
                this.stopAfterPreVisit();
            }
            return t;
        }
    }().visit(tree, undefined);
    return children;
}

/** Reads as "all" or "class `A`". */
function describe(tree: Tree): string {
    return isSourceFile(tree) ? "all" : `class \`${(tree as J.ClassDeclaration).name.simpleName}\``;
}
