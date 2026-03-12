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
import {MomentToTemporal} from "../../../../src/javascript/migrate/moment/moment-to-temporal";
import {typescript} from "../../../../src/javascript";

describe("moment-to-temporal", () => {
    const spec = new RecipeSpec();
    spec.recipe = new MomentToTemporal();

    // --- Creation ---

    test("moment() with no args", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import moment from 'moment';\nconst now = moment();`,
                `const now = Temporal.Now.plainDateTimeISO();`
            )
        );
    }, 60000);

    test("moment(string)", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import moment from 'moment';\nconst d = moment('2024-01-15');`,
                `const d = Temporal.PlainDateTime.from('2024-01-15');`
            )
        );
    }, 60000);

    test("moment(variable)", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import moment from 'moment';\nconst d = moment(dateStr);`,
                `const d = Temporal.PlainDateTime.from(dateStr);`
            )
        );
    }, 60000);

    test("moment.utc() with no args", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import moment from 'moment';\nconst now = moment.utc();`,
                `const now = Temporal.Now.instant();`
            )
        );
    }, 60000);

    test("moment.utc(string)", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import moment from 'moment';\nconst d = moment.utc('2024-01-15');`,
                `const d = Temporal.Instant.from('2024-01-15');`
            )
        );
    }, 60000);

    // --- Guard ---

    test("no changes without moment import", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `const d = moment();`
            )
        );
    }, 60000);

    // --- Arithmetic ---

    test(".add(n, 'days')", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import moment from 'moment';\nconst d = moment(date).add(5, 'days');`,
                `const d = Temporal.PlainDateTime.from(date).add({days: 5});`
            )
        );
    }, 60000);

    test(".add(n, 'd') shorthand", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import moment from 'moment';\nconst d = moment(date).add(5, 'd');`,
                `const d = Temporal.PlainDateTime.from(date).add({days: 5});`
            )
        );
    }, 60000);

    test(".add(n, 'months')", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import moment from 'moment';\nconst d = moment(date).add(3, 'months');`,
                `const d = Temporal.PlainDateTime.from(date).add({months: 3});`
            )
        );
    }, 60000);

    test(".add(n, 'day') singular", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import moment from 'moment';\nconst d = moment(date).add(1, 'day');`,
                `const d = Temporal.PlainDateTime.from(date).add({days: 1});`
            )
        );
    }, 60000);

    test(".subtract(n, 'hours')", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import moment from 'moment';\nconst d = moment(date).subtract(2, 'hours');`,
                `const d = Temporal.PlainDateTime.from(date).subtract({hours: 2});`
            )
        );
    }, 60000);

    test(".subtract(n, 's') shorthand", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import moment from 'moment';\nconst d = moment(date).subtract(30, 's');`,
                `const d = Temporal.PlainDateTime.from(date).subtract({seconds: 30});`
            )
        );
    }, 60000);

    // --- Comparison ---

    test(".isBefore(other)", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import moment from 'moment';\nconst result = moment(a).isBefore(moment(b));`,
                `const result = Temporal.PlainDateTime.compare(Temporal.PlainDateTime.from(a), Temporal.PlainDateTime.from(b)) < 0;`
            )
        );
    }, 60000);

    test(".isAfter(other)", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import moment from 'moment';\nconst result = moment(a).isAfter(moment(b));`,
                `const result = Temporal.PlainDateTime.compare(Temporal.PlainDateTime.from(a), Temporal.PlainDateTime.from(b)) > 0;`
            )
        );
    }, 60000);

    test(".isSame(other)", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import moment from 'moment';\nconst result = moment(a).isSame(moment(b));`,
                `const result = Temporal.PlainDateTime.compare(Temporal.PlainDateTime.from(a), Temporal.PlainDateTime.from(b)) === 0;`
            )
        );
    }, 60000);

    // --- Clone ---

    test(".clone() removed", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import moment from 'moment';\nconst copy = moment(date).clone();`,
                `const copy = Temporal.PlainDateTime.from(date);`
            )
        );
    }, 60000);

    // --- startOf ---

    test(".startOf('day')", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import moment from 'moment';\nconst d = moment(date).startOf('day');`,
                `const d = Temporal.PlainDateTime.from(date).toPlainDate().toPlainDateTime();`
            )
        );
    }, 60000);

    test(".startOf('month')", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import moment from 'moment';\nconst d = moment(date).startOf('month');`,
                `const d = Temporal.PlainDateTime.from(date).with({day: 1}).toPlainDate().toPlainDateTime();`
            )
        );
    }, 60000);

    test(".startOf('year')", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import moment from 'moment';\nconst d = moment(date).startOf('year');`,
                `const d = Temporal.PlainDateTime.from(date).with({month: 1, day: 1}).toPlainDate().toPlainDateTime();`
            )
        );
    }, 60000);

    test(".add(n, 'ms') milliseconds", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import moment from 'moment';\nconst d = moment(date).add(100, 'ms');`,
                `const d = Temporal.PlainDateTime.from(date).add({milliseconds: 100});`
            )
        );
    }, 60000);

    // --- Chain ---

    test("chained: moment().add().startOf()", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `import moment from 'moment';\nconst d = moment().add(1, 'months').startOf('day');`,
                `const d = Temporal.Now.plainDateTimeISO().add({months: 1}).toPlainDate().toPlainDateTime();`
            )
        );
    }, 60000);
});
