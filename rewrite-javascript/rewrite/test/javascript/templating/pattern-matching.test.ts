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
import {capture, pattern, TemplateProcessor} from "../../../src/javascript";

describe('Pattern Matching', () => {
    describe('TemplateProcessor', () => {
        test('buildTemplateString creates correct string with captures', () => {
            const templateParts = ['', ' + ', ''];
            const captures = [
                capture('left'),
                capture('right')
            ];

            const processor = new TemplateProcessor(templateParts as unknown as TemplateStringsArray, captures);

            // Access the private method using type assertion
            const buildTemplateString = (processor as any).buildTemplateString.bind(processor);

            const result = buildTemplateString();
            expect(result).toBe('__capture_left__ + __capture_right__');
        });
    });

    describe('Pattern', () => {
        test('creates a pattern with correct template parts and captures', () => {
            const p = pattern`${capture('left')} + ${capture('right')}`;
            
            expect(p.templateParts).toBeDefined();
            expect(p.captures).toBeDefined();
            expect(p.captures.length).toBe(2);
            expect(p.captures[0].name).toBe('left');
            expect(p.captures[1].name).toBe('right');
        });
    });
});
