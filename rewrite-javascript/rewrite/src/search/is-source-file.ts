import {TreeVisitor} from "../visitor";
import {ExecutionContext} from "../execution";
import {isSourceFile} from "../tree";
import {foundSearchResult} from "../markers";
import * as picomatch from "picomatch";

export class IsSourceFile extends TreeVisitor<any, ExecutionContext> {
    private readonly matcher: picomatch.Matcher;

    constructor(filePattern: string) {
        super();
        // Wildcards match dot-segments too, as Java's PathUtils.matchesGlob does for the same precondition.
        const options = {dot: true};
        this.matcher = picomatch.default ? picomatch.default(filePattern, options) : (picomatch as any)(filePattern, options);
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
