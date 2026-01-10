import {Expression, J, JavaVisitor} from "../../java";
import {ExecutionContext} from "../../execution";
import {MethodMatcher} from "../method-matcher";
import {JS} from "../tree";
import {foundSearchResult} from "../../markers";

export class UsesMethod extends JavaVisitor<ExecutionContext> {
    private readonly matcher;

    constructor(pattern: string) {
        super();
        this.matcher = new MethodMatcher(pattern);
    }

    protected visitExpression(expression: Expression, p: ExecutionContext): J | undefined {
        if (JS.isMethodCall(expression) && this.matcher.matches(expression.methodType)) {
            return foundSearchResult(expression);
        }
        return super.visitExpression(expression, p);
    }
}
