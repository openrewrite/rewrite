import {J, JavaVisitor, Type} from "../../java";
import {ExecutionContext} from "../../execution";
import * as picomatch from "picomatch";
import {foundSearchResult} from "../../markers";

export class UsesType extends JavaVisitor<ExecutionContext> {
    private readonly matcher: picomatch.Matcher;

    constructor(typePattern: string) {
        super();
        // Create a picomatch matcher for the pattern
        this.matcher = picomatch.default ? picomatch.default(typePattern) : (picomatch as any)(typePattern);
    }

    protected async preVisit(tree: J, _: ExecutionContext): Promise<J | undefined> {
        if (J.hasType(tree)) {
            const type = tree.type;
            if (Type.isFullyQualified(type)) {
                const fullyQualifiedName = Type.FullyQualified.getFullyQualifiedName(type);
                if (this.matcher(fullyQualifiedName)) {
                    return foundSearchResult(tree);
                }
            }
        }
        return tree;
    }
}
