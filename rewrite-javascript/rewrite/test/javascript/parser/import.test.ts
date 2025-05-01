import {connect, disconnect, rewriteRun, typeScript} from '../testHarness';

describe('import mapping', () => {
    beforeAll(() => connect());
    afterAll(() => disconnect());

    test('simple', () => {
        rewriteRun(
          //language=typescript
          typeScript('import {foo} from "bar"')
        );
    });

    test('for side effect', () => {
        rewriteRun(
          //language=typescript
          typeScript('import "foo"')
        );
    });

    test('space', () => {
        rewriteRun(
          //language=typescript
          typeScript('import {foo} /*1*/ from /*2*/ "bar"/*3*/;')
        );
    });

    test('multiple', () => {
        rewriteRun(
          //language=typescript
          typeScript('import {foo, bar} from "baz"')
        );
    });

    test('trailing comma', () => {
        rewriteRun(
          //language=typescript
          typeScript('import {foo, } from "baz"')
        );
    });

    test('default', () => {
        rewriteRun(
          //language=typescript
          typeScript('import foo from "bar"')
        );
    });

    test('namespace', () => {
        rewriteRun(
          //language=typescript
          typeScript('import *  as foo  from "bar"')
        );
    });

    test('default and namespace', () => {
        rewriteRun(
          //language=typescript
          typeScript('import baz, * as foo from "bar"')
        );
    });

    test('default and others', () => {
        rewriteRun(
          //language=typescript
          typeScript('import baz, {foo1, } from "bar"')
        );
    });

    test('dynamic import', () => {
        rewriteRun(
          //language=typescript
          typeScript('export {};const module = await import("module-name");')
        )
    });

    test('type import and others', () => {
        rewriteRun(
            //language=typescript
            typeScript(`import {
                Client,
                defaultAxiosInstance,
                defaultHttpsAgent,
                type ElevationResponse,
                /*1*/ type  /*2*/ ElevationResponseSuper /*3*/ as   /*4*/ ERS   /*5*/ ,  /*6*/
            } from "../src";`)
        );
    });

    test('type imports only', () => {
        rewriteRun(
            //language=typescript
            typeScript(`import type { Component } from "react";`)
        );
    });

    test('experimental: import with import attributes', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                import Package from 'module-name' assert { type: "json" }
                import foo from 'module-name' with { type: "json" };
                /*{1}*/import/*{2}*/ foo /*{3}*/from /*{4}*/'module-name'/*{5}*/ with/*{6}*/ {/*{7}*/ type/*{8}*/: /*{9}*/"json", /*{10}*/ } /*{11}*/;
            `)
        );
    });

    test('import with import attributes', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                import type { SpyInstance } from 'jest';
                ///*{1}*/import /*{2}*/type /*{3}*/{ /*{4}*/ SpyInstance /*{5}*/} /*{6}*/ from /*{7}*/ 'jest' /*{8}*/;
                import SpyInstance = jest.SpyInstance;
            `)
        );
    });

    test('external module import', () => {
        rewriteRun(
            //language=typescript
            typeScript(`
                import mongodb = /*a*/require/*b*/(/*c*/'mongodb'/*d*/)/*e*/;
            `)
        );
    });
});
