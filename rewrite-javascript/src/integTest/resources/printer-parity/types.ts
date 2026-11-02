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
type Optional<T extends object = {}> = { readonly [K in keyof T]?: T[K] };
type Mutable<T> = { -readonly [K in keyof T]-?: T[K]; };
type Getters<T> = { +readonly [K in keyof T as `get${Capitalize<string & K>}`]+?: () => T[K] };
type Bare<T> = { [K in keyof T] };
type Element<T> = T extends (infer U)[] ? U : never;
type Constructor = new (...args: any[]) => object;
type AbstractConstructor = abstract new () => object;
type Callback = <T, >(value: T, index?: number) => void;
type Tuple = [first: string, second?: number, ...rest: boolean[]];
type Literal = "a" | 'b' | 1 | -1 | true | null | undefined | `prefix-${string}`;
type Leading =
    | string
    | number;
type Intersection = { a: string } & { b: number, };
type Indexed = Optional<{ a: string }>["a"];
type Query = typeof globalThis.console;
type Instantiated = typeof Array<string>;
type Keys = keyof Intersection;
type Frozen = readonly string[];
type Matrix = number[][];
type Grouped = (string | number)[];
type Nullable = string | null;

declare const brand: unique symbol;
declare function overloaded(value: string): string;
declare function overloaded(value: number): number;

interface Shape<T = unknown> extends Base<string>, Other {
    readonly name: string;
    optional?: number;
    [key: string]: unknown;
    method<U>(arg: U, ...rest: T[]): U;
    new(x: number): Shape;
    (y: string): void;
    get accessor(): string;
    set accessor(value: string);
    [brand]: true;
}

function isString(x: unknown): x is string {
    return typeof x === "string";
}

function assertIsDefined<T>(x: T): asserts x is NonNullable<T> {
}

function assertTrue(x: unknown): asserts x {
}

const enum Direction {
    Up = 1,
    Down,
    Left = "LEFT".length,
}

enum Empty {}

enum Computed {
    [ 'first' ],
    ['second'] = 2,
}

type Unsupported = string?;

let cast = <unknown>brand;
let chained = cast as string as unknown;
let checked = { a: 1 } satisfies Record<string, number>;
let definite!: string;
let asserted = definite!.length;
let optional = checked?.a ?? asserted;
let element = (checked as any)?.[0]?.name!;
let called = (checked as any)?.(1, 2)?.then<string>();
let generic = new Map<string, Array<number>>();
let tagged = String.raw<string>`a${1}b`;
let escaped = "a\uD83D\uDE00b\uDC00";
let nested = String.raw/*a*/<<T>(first: T) => T>`a`;
let nestedCall = generic.get /*a*/ <<T>(first: T) => T, number>(1);
let nestedOptional = checked?.then<<T>() => void>();
let nestedNew = new Map /*a*/ <<T>(first: T) => T, Array<<T>() => void>>();
let nestedType: Array /*a*/ <<T>(first: T) => T>;
declare function either(): { a: number } | { b: string };
let spread = { ...either(), method(value: string) { return value; } };

interface Modified {
    public visible: number;
    static shared(): void;
    /*1*/ public /*2*/ static /*3*/ commented?(): void;
    static [brand](): void;
}

type ModifiedLiteral = { readonly fixed: number; public visible(): void };
