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

import {ReplacedText} from "../fixtures/replaced-text";

describe("data tables", () => {

    test("data table descriptor", () => {
        const descriptor = ReplacedText.dataTable.descriptor;
        expect(descriptor).toEqual({
            name: "org.openrewrite.text.replaced-text",
            displayName: "Replaced text",
            description: "Text that was replaced.",
            columns: [
                {
                    name: "sourcePath",
                    displayName: "Source Path",
                    description: "The path of the file that was changed.",
                },
                {
                    name: "text",
                    displayName: "Text",
                    description: "The text that was replaced.",
                }
            ],
        });
    });
});
