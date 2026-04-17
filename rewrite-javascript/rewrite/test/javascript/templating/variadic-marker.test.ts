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

describe('variadic marker attachment', () => {
    test('regular capture does not have variadic marker', () => {
        const arg = capture('arg');
        const pat = pattern`foo(${arg})`;

        // Verify the capture object itself
        expect(arg.isVariadic()).toBe(false);
        expect(arg.getVariadicOptions()).toBeUndefined();
    });

    test('variadic capture stores options in capture object', () => {
        const args = capture({ variadic: true });
        const pat = pattern`foo(${args})`;

        // Verify the capture object itself
        expect(args.isVariadic()).toBe(true);
        expect(args.getVariadicOptions()).toBeDefined();
    });

    test('variadic capture with custom options stores them correctly', () => {
        const args = capture({
            variadic: {
                min: 1,
                max: 3
            }
        });
        const pat = pattern`foo(${args})`;

        const options = args.getVariadicOptions();
        expect(options?.min).toBe(1);
        expect(options?.max).toBe(3);
    });

    test('pattern captures array includes variadic capture', () => {
        const args = capture({ name: 'args', variadic: true });
        const pat = pattern`foo(${args})`;

        // Pattern should have the captures array
        expect(pat.captures).toHaveLength(1);
        const firstCapture = pat.captures[0] as typeof args;
        expect(firstCapture.getName()).toBe('args');
        expect(firstCapture.isVariadic()).toBe(true);
    });
});
