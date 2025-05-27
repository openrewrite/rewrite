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
import {mapAsync} from "../src";

describe("mapAsync", () => {
    test("sequential execution", async () => {
        // given
        let global = "";
        const arr = ["four", "three", "six"];
        function task(item: string, idx: number) {
            return new Promise(resolve => {
                setTimeout(() => {
                    global = global + item;
                    resolve("irrelevant");
                }, item.length);
            });
        }

        // when
        await mapAsync(arr, task);

        // test
        expect(global).toEqual("fourthreesix");
    });
});
