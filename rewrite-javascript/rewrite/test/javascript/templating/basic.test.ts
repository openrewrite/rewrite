/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {capture, Matcher, Pattern, pattern} from "../../../src/javascript";

describe('templating basics', () => {
    describe('match', () => {
        test('creates a pattern with template parts and captures', () => {
            const p = pattern`${capture()} + ${capture()}`;
            expect(p).toBeInstanceOf(Pattern);

            const parts = p.templateParts;
            expect(parts.length).toBe(3);
            expect(parts[0]).toBe('');
            expect(parts[1]).toBe(' + ');
            expect(parts[2]).toBe('');
        });
    });

    describe('Pattern', () => {
        test('against returns a Matcher', () => {
            const p = pattern`${capture()} + ${capture()}`;
            const matcher = p.against({} as any);
            expect(matcher).toBeInstanceOf(Matcher);
        });
    });

    describe('Matcher', () => {
        test('matches returns false in initial implementation', async () => {
            const p = pattern`${capture()} + ${capture()}`;
            const matcher = p.against({} as any);
            expect(await matcher.matches()).toBe(false);
        });

        test('getAll returns empty map initially', () => {
            const p = pattern`${capture()} + ${capture()}`;
            const matcher = p.against({} as any);
            expect(matcher.getAll().size).toBe(0);
        });
    });
});
