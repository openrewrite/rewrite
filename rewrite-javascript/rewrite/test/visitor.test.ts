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
import {produceAsync} from "../src";

describe('produceAsync', () => {
    interface State {
        readonly a: number;
    }

    const before: State = {a: 1};

    test('async update', async () => {
        const updateA = async () => 2;
        const after = produceAsync<State>(before, async (draft) => {
            draft.a = await updateA();
            return draft;
        });
        await expect(after).resolves.toEqual({a: 2})
    });

    test('sync update', async () => {
        const after = produceAsync(before, (draft) => {
            draft.a = 2;
            return draft;
        });
        await expect(after).resolves.toEqual({a: 2})
    });
});
