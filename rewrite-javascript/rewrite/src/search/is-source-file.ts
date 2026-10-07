import {TreeVisitor} from "../visitor";
import {ExecutionContext} from "../execution";
import {isSourceFile} from "../tree";
import {foundSearchResult} from "../markers";
import * as picomatch from "picomatch";

export class IsSourceFile extends TreeVisitor<any, ExecutionContext> {
    private readonly matcher: (path: string) => boolean;

    /** Reads `filePattern` the way Java's FindSourceFiles does, so the precondition agrees on both hosts. */
    constructor(filePattern: string) {
        super();
        const patterns = filePattern.split(";")
            .map(p => p.trim().replace(/^\.?[/\\]/, ""))
            .filter(p => p.length > 0);
        // Wildcards match dot-segments too, as PathUtils.matchesGlob does.
        const options = {dot: true};
        this.matcher = patterns.length === 0 ? () => true :
            picomatch.default ? picomatch.default(patterns, options) : (picomatch as any)(patterns, options);
    }

    protected async preVisit(tree: any, _: ExecutionContext): Promise<any> {
        this.stopAfterPreVisit();
        if (isSourceFile(tree) && tree.sourcePath) {
            const path = tree.sourcePath.replace(/\\/g, '/'); // Normalize to Unix separators
            if (this.matcher(path)) {
                return foundSearchResult(tree);
            }
        }
        return tree;
    }
}
