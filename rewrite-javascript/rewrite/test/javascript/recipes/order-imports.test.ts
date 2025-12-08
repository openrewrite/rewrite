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
import {describe} from "@jest/globals";
import {RecipeSpec} from "../../../src/test";
import {OrderImports} from "../../../src/javascript/recipes/order-imports";
import {typescript} from "../../../src/javascript";

describe("order-imports", () => {
    const spec = new RecipeSpec()
    spec.recipe = new OrderImports();

    test("sorts imports by module path", () => {
        return spec.rewriteRun(
            typescript(
                `
import {z} from 'zebra';
import {a} from 'alpha';
import {m} from 'middle';
`,
                `
import {a} from 'alpha';
import {m} from 'middle';
import {z} from 'zebra';
`))
    });

    test("sorts named specifiers within each import alphabetically", () => {
        return spec.rewriteRun(
            typescript(
                `
import {gamma, delta, alpha, beta} from 'module';
`,
                `
import {alpha, beta, delta, gamma} from 'module';
`))
    });

    test("preserves trailing comma when sorting specifiers", () => {
        return spec.rewriteRun(
            typescript(
                `
import {zebra, alpha,} from 'module';
`,
                `
import {alpha, zebra,} from 'module';
`))
    });

    test("handles aliased imports", () => {
        return spec.rewriteRun(
            typescript(
                `
import {beta as b, alpha as a} from 'module';
`,
                `
import {alpha as a, beta as b} from 'module';
`))
    });

    test("side-effect imports come first", () => {
        return spec.rewriteRun(
            typescript(
                `
import {a} from 'alpha';
import 'side-effect';
import {b} from 'beta';
`,
                `
import 'side-effect';
import {a} from 'alpha';
import {b} from 'beta';
`))
    });

    test("multiple side-effect imports sorted by module path", () => {
        return spec.rewriteRun(
            typescript(
                `
import 'zebra-side-effect';
import 'alpha-side-effect';
`,
                `
import 'alpha-side-effect';
import 'zebra-side-effect';
`))
    });

    test("namespace imports sorted by module path", () => {
        return spec.rewriteRun(
            typescript(
                `
import * as z from 'zebra';
import * as a from 'alpha';
`,
                `
import * as a from 'alpha';
import * as z from 'zebra';
`))
    });

    test("default imports sorted by module path", () => {
        return spec.rewriteRun(
            typescript(
                `
import zebra from 'zebra';
import alpha from 'alpha';
`,
                `
import alpha from 'alpha';
import zebra from 'zebra';
`))
    });

    test("mixed import types sorted by module path within their groups", () => {
        return spec.rewriteRun(
            typescript(
                `
import {z} from 'zebra';
import {a} from 'alpha';
import * as zns from 'zebra-ns';
import * as ans from 'alpha-ns';
import zdef from 'zebra-def';
import adef from 'alpha-def';
import 'zebra-side';
import 'alpha-side';
`,
                `
import 'alpha-side';
import 'zebra-side';
import * as ans from 'alpha-ns';
import * as zns from 'zebra-ns';
import adef from 'alpha-def';
import zdef from 'zebra-def';
import {a} from 'alpha';
import {z} from 'zebra';
`))
    });

    test("type imports come after regular imports from same module", () => {
        return spec.rewriteRun(
            typescript(
                `
import type {TypeA} from 'module';
import {a} from 'module';
`,
                `
import {a} from 'module';
import type {TypeA} from 'module';
`))
    });

    test("type imports sorted by module path", () => {
        return spec.rewriteRun(
            typescript(
                `
import type {Z} from 'zebra';
import type {A} from 'alpha';
`,
                `
import type {A} from 'alpha';
import type {Z} from 'zebra';
`))
    });

    test("default with named imports", () => {
        return spec.rewriteRun(
            typescript(
                `
import React, {useState, useEffect} from 'react';
`,
                `
import React, {useEffect, useState} from 'react';
`))
    });

    test("case-insensitive module path sorting", () => {
        return spec.rewriteRun(
            typescript(
                `
import {a} from 'Zebra';
import {b} from 'alpha';
import {c} from 'Beta';
`,
                `
import {b} from 'alpha';
import {c} from 'Beta';
import {a} from 'Zebra';
`))
    });

    test("no change when already sorted", () => {
        return spec.rewriteRun(
            typescript(
                `
import {a} from 'alpha';
import {b} from 'beta';
`))
    });

    test("handles empty import list", () => {
        return spec.rewriteRun(
            typescript(
                `
const x = 1;
`))
    });

    test("comprehensive: basic example from original test", () => {
        return spec.rewriteRun(
            typescript(
                `
import {gamma, delta} from 'delta.js';
import {beta as bet, alpha,} from 'alpha.js';
import {b} from 'qux.js';
import * as foo from 'foo.js';
import * as bar from 'bar.js';
import a from 'baz.js';
import 'module-without-export.js';
`,
                `
import 'module-without-export.js';
import * as bar from 'bar.js';
import * as foo from 'foo.js';
import a from 'baz.js';
import {alpha, beta as bet,} from 'alpha.js';
import {delta, gamma} from 'delta.js';
import {b} from 'qux.js';
`))
    });
});
