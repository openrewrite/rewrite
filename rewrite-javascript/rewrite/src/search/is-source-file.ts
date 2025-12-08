import {TreeVisitor} from "../visitor";
import {ExecutionContext} from "../execution";
import {isSourceFile} from "../tree";
import {foundSearchResult} from "../markers";
import * as picomatch from "picomatch";

export class IsSourceFile extends TreeVisitor<any, ExecutionContext> {
    private readonly matcher: picomatch.Matcher;

    constructor(filePattern: string) {
        super();
        // Create a picomatch matcher for the pattern
        this.matcher = picomatch.default ? picomatch.default(filePattern) : (picomatch as any)(filePattern);
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
