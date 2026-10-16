import {J, JavaVisitor, Type} from "../../java";
import {JS} from "../tree";
import {ExecutionContext} from "../../execution";
import * as picomatch from "picomatch";
import {foundSearchResult} from "../../markers";

/**
 * Marks each tree whose type matches a glob pattern.
 *
 * With `assignable`, a type also matches through a class or interface it extends. A primitive then
 * matches by its keyword. With `includeImplicit`, a call also matches through its method's declaring,
 * return and parameter types.
 */
export class UsesType extends JavaVisitor<ExecutionContext> {
    private readonly matcher: picomatch.Matcher;
    private readonly assignable: boolean;
    private readonly includeImplicit: boolean;

    constructor(typePattern: string, {assignable = false, includeImplicit = false}: {
        assignable?: boolean,
        includeImplicit?: boolean
    } = {}) {
        super();
        this.matcher = picomatch.default ? picomatch.default(typePattern) : (picomatch as any)(typePattern);
        this.assignable = assignable;
        this.includeImplicit = includeImplicit;
    }

    protected async preVisit(tree: J, _: ExecutionContext): Promise<J | undefined> {
        if (J.hasType(tree) && this.matches(tree.type)) {
            return foundSearchResult(tree);
        }
        if (this.includeImplicit && JS.isMethodCall(tree) && tree.methodType) {
            const method = tree.methodType;
            if ([method.declaringType, method.returnType, ...method.parameterTypes].some(t => this.matches(t))) {
                return foundSearchResult(tree);
            }
        }
        return tree;
    }

    private matches(type: Type | undefined): boolean {
        if (Type.isPrimitive(type)) {
            return this.assignable && type.keyword !== '' && this.matcher(type.keyword);
        }
        return Type.isOfTypeWithName(type, this.assignable, this.matcher);
    }
}
