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
const numbers = [1, 0x1f, 0b11, 0o17, 1_000, 1e3, .5, 10n,];
const strings = ['single', "double", `template`, `multi
line ${numbers[0]} and ${`nested ${numbers.length}`}`];
const regex = /ab+c/gi, sparse = [, 1, , 2];
const key = "dynamic";
const object = {
    plain: 1,
    "quoted": 2,
    3: "numeric",
    [key]: 4,
    [`${key}2`]: 5,
    key,
    method() {
        return this.plain;
    },
    async asyncMethod() {
    },
    * generator() {
    },
    async* asyncGenerator() {
    },
    get accessor() {
        return 1;
    },
    set accessor(value) {
    },
    [key + "Method"]() {
    },
    arrow: (a, b = 1, ...rest) => a + b,
    ...numbers,
};

let a = 1, b = 2, c;
c = a + b - a * b / a % b ** 2;
c = a << 1 >> 2 >>> 3 & 4 | 5 ^ 6;
c = a < b || a > b && a <= b || a >= b;
c = a == b || a != b || a === b || a !== b;
c = "plain" in object && object instanceof Object;
c = a ?? b;
c = (a, b);
c = a ? b : a ? 1 : 2;
c = !a + -b + +a + ~b + typeof a + void 0;
c = a++ + ++a + b-- + --b;
a += 1, a -= 1, a *= 2, a /= 2, a %= 2, a **= 2;
a <<= 1, a >>= 1, a >>>= 1, a &= 1, a |= 1, a ^= 1;
a &&= b, a ||= b, a ??= b;
[a, b] = [b, a];
({ plain: a, ...c } = object);

const value = object?.plain;
const deep = object?.nested?.[key]?.(a);
const call = object.method?.();
const index = object?.[key];
const chained = object
    .method()
    .toString()
    .padStart(2, "0");

const instance = new Date;
const withArguments = new Date(2020, 0, 1);
const member = new object.constructor();
const target = function () {
    return new.target;
};
const meta = () => import.meta;
const tag = (parts, ...values) => parts.raw.join("");
const tagged = tag`a${a}b${b}c`;
const immediate = (function named() {
    return 1;
})();
const arrowImmediate = (() => ({}))();
const single = x => x * 2;
const asyncArrow = async x => await x;
const block = async (x, y) => {
    return x + y;
};
const spread = Math.max(...numbers, ...[1, 2]);
const grouped = ((a + b)) * (c);
const escaped = "tab\t unicode\u00e9 lone\uD800 pair\uD83D\uDE00";
delete object.plain;
