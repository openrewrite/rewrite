/*
 * Copyright 2026 the original author or authors.
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
import * as path from "path";
import {describeJavaRpc, testJavaRpc} from "../../src/test/java-rpc";
import {TreePrinters} from "../../src/print";
import {moduleDir, parse} from "./printer-parity";

// The slowest of the Java printer parity tests, in a file of its own so that vitest runs it beside the others.
describeJavaRpc("Java printer parity", () => {

    testJavaRpc("the sources of this module", async ({javaRpc}) => {
        for (const {cu, text} of await parse(path.join(moduleDir, "src"))) {
            const printed = await TreePrinters.print(cu);
            expect(printed, cu.sourcePath).toEqual(text);
            expect(await javaRpc.rpc.print(cu), cu.sourcePath).toEqual(printed);
        }
    }, 300_000);
});
