const EXECUTION_CONTEXT_KEY = Symbol("org.openrewrite.ExecutionContext");

export interface ExecutionContext {
    [EXECUTION_CONTEXT_KEY]: true

    [key: string | symbol]: any
}

export function isExecutionContext(obj?: any): obj is ExecutionContext {
    return obj !== undefined && obj[EXECUTION_CONTEXT_KEY] === true;
}

export function createExecutionContext(): ExecutionContext {
    return {[EXECUTION_CONTEXT_KEY]: true};
}
