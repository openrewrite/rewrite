/*
 * Copyright 2025 the original author or authors.
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
import {RecipeSpec} from "../../../src/test";
import {typescript} from "../../../src/javascript";

describe('mapped type mapping', () => {
    const spec = new RecipeSpec();

    test('simple', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type OptionsFlags<Type> = {
                     [Property in keyof Type]: /*a*/boolean/*b*/
                 };
             `)
        ));

    test('with readonly', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               type ReadonlyType<T> = {
                   /*a*/readonly/*b*/ [K in keyof T]: T[K];
               };
           `)
        ));

    test('with -readonly', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               type CreateMutable<Type> = {
                   /*a*/-/*b*/readonly/*c*/[Property in keyof Type]: Type[Property];
               };
           `)
        ));

    test('with suffix +?', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               type Concrete<Type> = {
                   [Property in keyof Type]+?: Type[Property];
               };
           `)
        ));

    test('with suffix -?', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type Concrete<Type> = {
                     [Property in keyof Type]/*a*/-/*b*/?/*c*/: Type[Property];
                 };
             `)
        ));

    test('record mapped type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               type Record<K extends keyof any, T> = {
                   [P in K]: T;
                   /*a*/}/*b*/
               ;
           `)
        ));

    test('record mapped type with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type Record<K extends keyof any, T> = /*a*/{ /*b*/
                     /*0*/[/*1*/P /*2*/in /*3*/K/*4*/]/*c*/:/*d*/ T/*e*/;/*f*/
                 }/*g*/;
             `)
        ));

    test('mapped type with "as" nameType', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               type ReadonlyRenamed<T> = {
                   + readonly [K in keyof T as \`readonly_\${string & K\}\`]: T[K];
               };
           `)
        ));

    test('recursive mapped types', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               type DeepPartial<T> = {
                   [K in keyof T]/*a*/?/*b*/: T[K] extends object /*c*/? DeepPartial<T[K]> /*d*/: T[K];
               };
           `)
        ));

    test('with function type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               type Getters<Type> = {
                   [Property in keyof Type as \`get\${Capitalize<string & Property>/*a*/}\`]: () => Type[Property] /*b*/
               };
           `)
        ));

    test('with repeating tokens', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type EventConfig<Events extends { kind: string }> = {
                     /*a*/[/*b*/E /*c*/in /*d*/Events as /*e*/E/*f*/[/*g*/"kind"/*h*/]/*i*/]/*j*/:/*k*/ (/*l*/event/*m*/: /*n*/E)/*o*/ => void;
                 }
           `)
        ));

    test('with conditional', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type ExtractPII<Type> = {
                     [Property in keyof Type]: Type[Property] extends { pii: true } ? true : false;
                 };
           `)
        ));

    test('mapped types intersection', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type MakeSpecificKeysReadonly<T, K extends keyof T> = {
                     readonly [P in K]: T[P];
                 } & {
                     [P in Exclude<keyof T, K>]: T[P];
                 };
           `)
        ));

    test('specific type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type UnionMapper<T> = {
                     [K in T]: { type: K };
                 }[T];
           `)
        ));

    test('complex key remapping', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type SnakeCaseKeys<T> = {
                     [K in keyof T as K extends string
                         ? \`\${K extends \`\${infer First}\${infer Rest}\`
                         ? \`\${Lowercase<First>}\${Rest extends Capitalize<Rest> ? \`_\${Lowercase<Rest>}\` : Rest}\`
                         : K}\`
                     : never]: T[K];
                 };
             `)
        ));

    test('no node type ', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 type Type = {
                     // comment
                     readonly [T in number] /*a*/
                 };
             `)
        ));

    test('no node type with ;', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                type Type = {
                    // comment
                    readonly [T in number] /*a*/;/*b*/
                };
            `)
        ));
});
