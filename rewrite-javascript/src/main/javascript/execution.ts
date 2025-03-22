export type ExecutionContext = Map<string | symbol, any> & { __brand: "ExecutionContext" }

export function createExecutionContext(): ExecutionContext {
    return new Map<string | symbol, any>() as ExecutionContext;
}
