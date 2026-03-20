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
import {
    capture, expr, ident, typeRef, stmt, any,
    Pattern, pattern
} from "../../../src/javascript";
import {
    CAPTURE_KIND_SYMBOL
} from "../../../src/javascript/templating/capture";
import { CaptureKind } from "../../../src/javascript/templating/types";

describe('capture kinds', () => {
    describe('CaptureKind enum', () => {
        test('has expected values', () => {
            expect(CaptureKind.Expression).toBe('expression');
            expect(CaptureKind.Identifier).toBe('identifier');
            expect(CaptureKind.TypeReference).toBe('type-reference');
            expect(CaptureKind.Statement).toBe('statement');
        });
    });

    describe('factory functions', () => {
        test('expr() creates capture with Expression kind', () => {
            const c = expr('x');
            expect(c.getName()).toBe('x');
            expect((c as any)[CAPTURE_KIND_SYMBOL]).toBe(CaptureKind.Expression);
        });

        test('ident() creates capture with Identifier kind', () => {
            const c = ident('name');
            expect(c.getName()).toBe('name');
            expect((c as any)[CAPTURE_KIND_SYMBOL]).toBe(CaptureKind.Identifier);
        });

        test('typeRef() creates capture with TypeReference kind', () => {
            const c = typeRef('t');
            expect(c.getName()).toBe('t');
            expect((c as any)[CAPTURE_KIND_SYMBOL]).toBe(CaptureKind.TypeReference);
        });

        test('stmt() creates capture with Statement kind', () => {
            const c = stmt('s');
            expect(c.getName()).toBe('s');
            expect((c as any)[CAPTURE_KIND_SYMBOL]).toBe(CaptureKind.Statement);
        });

        test('factory functions work without a name', () => {
            const e = expr();
            const i = ident();
            const t = typeRef();
            const s = stmt();
            expect(e.getName()).toMatch(/^unnamed_/);
            expect(i.getName()).toMatch(/^unnamed_/);
            expect(t.getName()).toMatch(/^unnamed_/);
            expect(s.getName()).toMatch(/^unnamed_/);
        });

        test('factory functions accept options', () => {
            const c = expr({
                name: 'x',
                constraint: (node) => true
            });
            expect(c.getName()).toBe('x');
            expect((c as any)[CAPTURE_KIND_SYMBOL]).toBe(CaptureKind.Expression);
            expect(c.getConstraint()).toBeDefined();
        });

        test('factory functions accept variadic options', () => {
            const c = expr({
                name: 'args',
                variadic: true
            });
            expect(c.getName()).toBe('args');
            expect(c.isVariadic()).toBe(true);
            expect((c as any)[CAPTURE_KIND_SYMBOL]).toBe(CaptureKind.Expression);
        });
    });

    describe('backwards compatibility', () => {
        test('capture() defaults to Expression kind', () => {
            const c = capture('x');
            expect((c as any)[CAPTURE_KIND_SYMBOL]).toBe(CaptureKind.Expression);
        });

        test('capture() with options defaults to Expression kind', () => {
            const c = capture({ name: 'x' });
            expect((c as any)[CAPTURE_KIND_SYMBOL]).toBe(CaptureKind.Expression);
        });
    });

    describe('namespace access on capture', () => {
        test('capture.expr() works', () => {
            const c = capture.expr('x');
            expect(c.getName()).toBe('x');
            expect((c as any)[CAPTURE_KIND_SYMBOL]).toBe(CaptureKind.Expression);
        });

        test('capture.ident() works', () => {
            const c = capture.ident('n');
            expect(c.getName()).toBe('n');
            expect((c as any)[CAPTURE_KIND_SYMBOL]).toBe(CaptureKind.Identifier);
        });

        test('capture.typeRef() works', () => {
            const c = capture.typeRef('t');
            expect(c.getName()).toBe('t');
            expect((c as any)[CAPTURE_KIND_SYMBOL]).toBe(CaptureKind.TypeReference);
        });

        test('capture.stmt() works', () => {
            const c = capture.stmt('s');
            expect(c.getName()).toBe('s');
            expect((c as any)[CAPTURE_KIND_SYMBOL]).toBe(CaptureKind.Statement);
        });
    });

    describe('any() defaults', () => {
        test('any() defaults to Expression kind', () => {
            const a = any();
            expect((a as any)[CAPTURE_KIND_SYMBOL]).toBe(CaptureKind.Expression);
        });
    });

    describe('usage in patterns', () => {
        test('factory captures work in pattern template literals', () => {
            const e = expr('x');
            const n = ident('method');
            const p = pattern`${n}(${e})`;
            expect(p).toBeInstanceOf(Pattern);
            expect(p.captures.length).toBe(2);
        });
    });
});
