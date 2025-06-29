// noinspection TypeScriptCheckImport,TypeScriptUnresolvedReference,ES6UnusedImports

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

describe('import mapping', () => {
    const spec = new RecipeSpec();

    test('simple', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('import {foo} from "bar"')
        ));

    test('for side effect', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('import "foo"')
        ));

    test('space', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('import {foo} /*1*/ from /*2*/ "bar"/*3*/;')
        ));

    test('multiple', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('import {foo, bar} from "baz"')
        ));

    test('trailing comma', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('import {foo, } from "baz"')
        ));

    test('default', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('import foo from "bar"')
        ));

    test('namespace', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('import *  as foo  from "bar"')
        ));

    test('default and namespace', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('import baz, * as foo from "bar"')
        ));

    test('default and others', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('import baz, {foo1, } from "bar"')
        ));

    test('dynamic import', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('export {};const module = await import("module-name");')
        ));

    test('type import and others', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`import {
                Client,
                defaultAxiosInstance,
                defaultHttpsAgent,
                type ElevationResponse,
                /*1*/ type  /*2*/ ElevationResponseSuper /*3*/ as   /*4*/ ERS   /*5*/,  /*6*/
            } from "../src";`)
        ));

    test('type imports only', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`import type {Component} from "react";`)
        ));

    // noinspection JSDuplicatedDeclaration
    test('experimental: import with import attributes', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                import Package from 'module-name' assert {type: "json"}
                import foo from 'module-name' with {type: "json"};
                /*{1}*/
                import/*{2}*/ foo /*{3}*/
                    from /*{4}*/'module-name'/*{5}*/ with/*{6}*/ {/*{7}*/ type/*{8}*/: /*{9}*/"json", /*{10}*/} /*{11}*/;
            `)
        ));

    test('import with import attributes', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                /*{1}*/import /*{2}*/type /*{3}*/{ /*{4}*/ SpyInstance /*{5}*/} /*{6}*/ from /*{7}*/ 'jest' /*{8}*/;
                import SpyInstance = jest.SpyInstance;
            `)
        ));

    test('import equals with require', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                import mongodb = /*a*/require/*b*/(/*c*/'mongodb'/*d*/)/*e*/;
            `)
        ));

    test('import equals with qualified name', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                namespace MyLib {
                    export function hello() {}
                }
                import my = MyLib.hello;
            `)
        ));

    test('import type equals', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                import   type my = require('my-library');
                `)
        ));

    test('export import', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                import * as i from "immer";
                /*1*/ export /*2*/ import /*3*/ cd /*4*/ = i.createDraft;
            `)
        ));
});
