/*
 * Copyright 2026 the original author or authors.
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
function sealed(target: Function) {
}

function logged(prefix: string) {
    return function (target: any, key: string) {
    };
}

@sealed
export abstract class Base<T, U extends object = {}> extends Object implements Iterable<T>, Disposable {
    static #count = 0;
    static readonly kind: string = "base";
    declare ambient: number;
    protected items: T[] = [];
    private optional?: U;
    #secret = 1;

    static {
        Base.#count++;
    }

    constructor(public readonly name: string, private age?: number, ...rest: T[]) {
        super();
    }

    @logged("method")
    method<V>(this: Base<T, U>, @logged("param") value: V): V {
        return value;
    }

    abstract describe(): string;

    get size(): number {
        return this.items.length;
    }

    set size(value: number) {
        this.items.length = value;
    }

    async* stream(): AsyncGenerator<T> {
        yield* this.items;
    }

    * [Symbol.iterator](): Iterator<T> {
        for (const item of this.items) {
            yield item;
        }
    }

    [Symbol.dispose](): void {
    }

    protected static async create?<V>(): Promise<V>;

    accessor counted = Base.#count;

    arrow = async <V, >(value: V): Promise<V> => value;
}

export default class extends Base<string> {
    override describe() {
        return `${this.name} has ${this.size} item${this.size === 1 ? "" : "s"}`;
    }
}

const anonymous = class Named<T> {
    constructor(readonly value: T) {
    }
};

export function overload(a: string): void;
export function overload(a: number, b?: string): void;
export function overload(a: any, b?: any): void {
}

export async function* generator<T>({ first, second: renamed = 1, ...others }: Record<string, T>, [head, , tail = head, ...more]: T[] = []): AsyncGenerator<T> {
    yield first;
    yield* more;
    return;
}

export
@sealed
class AfterExport {
}

@sealed export @logged("class") abstract class Between {
}

export /*1*/ @sealed /*2*/ @logged("kind") /*3*/ abstract /*4*/ class BeforeModifier {
}

export /*1*/ @sealed /*2*/ @logged("kind") /*3*/ class BeforeKind {
}

@sealed enum DecoratedEnum { A }

export @sealed interface DecoratedInterface { }

// @ts-ignore: decorator
@lazy export const lazy = "wasm";
export @lazy @inline const inlined = 1;
@lazy let first = 1, second = 2;

// @ts-ignore: decorator
@inline
export function inline(): void {
}

export @sealed function afterExport(): void {
}

@sealed export @logged("function") function between(): void {
}

export /*1*/ @sealed /*2*/ @logged("keyword") /*3*/ function /*4*/ beforeKeyword(): void {
}

export @sealed async function beforeModifier(): Promise<void> {
}

export default @sealed function defaulted(): void {
}

class Members {
    @sealed static [key: number]: any;
    /*1*/ @sealed /*2*/ @logged("index") /*3*/ static /*4*/ [key: symbol]: any;

    @sealed [Symbol.toPrimitive](): void {
    }

    decorated(@sealed { object }: any, @sealed [array]: any[], @sealed ...rest: any[]): void {
    }
}

const literal = { export @sealed method() {}, export @sealed get accessor() { return 1; } };

const decoratedLiteral = {
    export @sealed public property: 1,
    export @sealed get [Symbol.toStringTag]() { return ""; },
    /*1*/ export /*2*/ @sealed /*3*/ @logged("property") /*4*/ public /*5*/ commented /*6*/: 2,
};

function decoratedParameters(
    export @sealed identifier: string,
    @sealed export @logged("parameter") optional?: number,
    export @sealed { object }: any = {},
    /*1*/ @sealed /*2*/ export /*3*/ @logged("identifier") /*4*/ commented /*5*/: string,
    /*1*/ export /*2*/ @sealed /*3*/ @logged("object") /*4*/ { commentedObject } /*5*/: any,
): void {
}

const decoratedExpression = @sealed export @logged("expression") class {
};

const modifiedExpression = @sealed abstract class {
};

const commentedExpression = /*1*/ @sealed /*2*/ export /*3*/ @logged("expression") /*4*/ abstract /*5*/ class /*6*/ Named {
};
