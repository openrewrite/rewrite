// noinspection TypeScriptUnresolvedReference,JSUnusedLocalSymbols

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
import {RecipeSpec} from "../../../../src/test";
import {DateFnsToTemporal} from "../../../../src/javascript/migrate/date-fns/date-fns-to-temporal";
import {typescript} from "../../../../src/javascript";

describe("date-fns-to-temporal", () => {
    const spec = new RecipeSpec();
    spec.recipe = new DateFnsToTemporal();

    // --- Date Arithmetic ---

    test("addDays", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import { addDays } from 'date-fns';\nconst result = addDays(date, 5);`,
                `const result = Temporal.PlainDate.from(date).add({days: 5});`
            )
        );
    }, 60000);

    test("addMonths", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import { addMonths } from 'date-fns';\nconst result = addMonths(date, 3);`,
                `const result = Temporal.PlainDate.from(date).add({months: 3});`
            )
        );
    }, 60000);

    test("addYears", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import { addYears } from 'date-fns';\nconst result = addYears(date, 1);`,
                `const result = Temporal.PlainDate.from(date).add({years: 1});`
            )
        );
    }, 60000);

    test("subDays", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import { subDays } from 'date-fns';\nconst result = subDays(date, 5);`,
                `const result = Temporal.PlainDate.from(date).subtract({days: 5});`
            )
        );
    }, 60000);

    test("subMonths", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import { subMonths } from 'date-fns';\nconst result = subMonths(date, 2);`,
                `const result = Temporal.PlainDate.from(date).subtract({months: 2});`
            )
        );
    }, 60000);

    test("subYears", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import { subYears } from 'date-fns';\nconst result = subYears(date, 1);`,
                `const result = Temporal.PlainDate.from(date).subtract({years: 1});`
            )
        );
    }, 60000);

    // --- Comparison ---

    test("isAfter", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import { isAfter } from 'date-fns';\nconst result = isAfter(a, b);`,
                `const result = Temporal.PlainDateTime.compare(a, b) > 0;`
            )
        );
    }, 60000);

    test("isBefore", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import { isBefore } from 'date-fns';\nconst result = isBefore(a, b);`,
                `const result = Temporal.PlainDateTime.compare(a, b) < 0;`
            )
        );
    }, 60000);

    test("isEqual", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import { isEqual } from 'date-fns';\nconst result = isEqual(a, b);`,
                `const result = Temporal.PlainDateTime.compare(a, b) === 0;`
            )
        );
    }, 60000);

    // --- Difference ---

    test("differenceInDays", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import { differenceInDays } from 'date-fns';\nconst result = differenceInDays(a, b);`,
                `const result = a.since(b, {largestUnit: "day"}).days;`
            )
        );
    }, 60000);

    test("differenceInHours", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import { differenceInHours } from 'date-fns';\nconst result = differenceInHours(a, b);`,
                `const result = a.since(b, {largestUnit: "hour"}).hours;`
            )
        );
    }, 60000);

    test("differenceInMinutes", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import { differenceInMinutes } from 'date-fns';\nconst result = differenceInMinutes(a, b);`,
                `const result = a.since(b, {largestUnit: "minute"}).minutes;`
            )
        );
    }, 60000);

    // --- Start of period ---

    test("startOfDay", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import { startOfDay } from 'date-fns';\nconst result = startOfDay(date);`,
                `const result = Temporal.PlainDate.from(date).toPlainDateTime();`
            )
        );
    }, 60000);

    test("startOfMonth", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import { startOfMonth } from 'date-fns';\nconst result = startOfMonth(date);`,
                `const result = Temporal.PlainDate.from(date).with({day: 1}).toPlainDateTime();`
            )
        );
    }, 60000);

    test("startOfYear", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import { startOfYear } from 'date-fns';\nconst result = startOfYear(date);`,
                `const result = Temporal.PlainDate.from(date).with({month: 1, day: 1}).toPlainDateTime();`
            )
        );
    }, 60000);

    // --- End of period ---

    test("endOfDay", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import { endOfDay } from 'date-fns';\nconst result = endOfDay(date);`,
                `const result = Temporal.PlainDate.from(date).toPlainDateTime({hour: 23, minute: 59, second: 59, millisecond: 999});`
            )
        );
    }, 60000);

    test("endOfMonth", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import { endOfMonth } from 'date-fns';\nconst result = endOfMonth(date);`,
                `const result = ((d) => d.with({day: d.daysInMonth}))(Temporal.PlainDate.from(date));`
            )
        );
    }, 60000);

    test("endOfYear", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import { endOfYear } from 'date-fns';\nconst result = endOfYear(date);`,
                `const result = Temporal.PlainDate.from(date).with({month: 12, day: 31}).toPlainDateTime({hour: 23, minute: 59, second: 59, millisecond: 999});`
            )
        );
    }, 60000);

    // --- Guard: no date-fns import ---

    test("no changes without date-fns import", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `const result = addDays(date, 5);`
            )
        );
    }, 60000);

    // --- Multiple imports ---

    test("multiple date-fns imports", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import { addDays, subMonths } from 'date-fns';\nconst a = addDays(d, 1);\nconst b = subMonths(d, 2);`,
                `const a = Temporal.PlainDate.from(d).add({days: 1});\nconst b = Temporal.PlainDate.from(d).subtract({months: 2});`
            )
        );
    }, 60000);

    // --- Aliased imports ---

    test("aliased import: addDays as plusDays", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import { addDays as plusDays } from 'date-fns';\nconst result = plusDays(date, 5);`,
                `const result = Temporal.PlainDate.from(date).add({days: 5});`
            )
        );
    }, 60000);
});
