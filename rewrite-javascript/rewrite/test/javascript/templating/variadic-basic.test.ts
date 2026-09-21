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
import {capture, pattern} from "../../../src/javascript";

describe('variadic capture basic functionality', () => {
    test('regular capture is not variadic', () => {
        const arg = capture('arg');
        expect(arg.isVariadic()).toBe(false);
        expect(arg.getVariadicOptions()).toBeUndefined();
    });

    test('variadic: true creates variadic capture with defaults', () => {
        const args = capture({ variadic: true });
        expect(args.isVariadic()).toBe(true);

        const options = args.getVariadicOptions();
        expect(options).toBeDefined();
        expect(options?.min).toBeUndefined();
        expect(options?.max).toBeUndefined();
    });

    test('variadic bounds are carried through, named or not', () => {
        const args = capture({ variadic: { min: 1, max: 3 } });
        expect(args.isVariadic()).toBe(true);
        expect(args.getVariadicOptions()?.min).toBe(1);
        expect(args.getVariadicOptions()?.max).toBe(3);

        const named = capture({ name: 'args', variadic: { min: 2, max: 5 } });
        expect(named.getVariadicOptions()?.min).toBe(2);
        expect(named.getVariadicOptions()?.max).toBe(5);
    });

    test('unnamed variadic capture', () => {
        const args = capture({ variadic: true });
        expect(args.isVariadic()).toBe(true);
        expect(args.getName()).toMatch(/^unnamed_\d+$/);
    });

    test('a pattern collects the variadic capture it interpolates', () => {
        const args = capture({ name: 'args', variadic: true });
        const pat = pattern`foo(${args})`;

        expect(pat.captures).toHaveLength(1);
        const firstCapture = pat.captures[0] as typeof args;
        expect(firstCapture.getName()).toBe('args');
        expect(firstCapture.isVariadic()).toBe(true);
    });
});
