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
/* leading block comment */
// leading line comment
var legacy = 1, second /* inline */ = 2, third
let counter = 0;
const { a, b: { c = legacy } = {}, ...rest } = {}, [x, , y = 1, ...zs] = [];

if (a) counter++; else if (c) counter--; else {
    counter = 0;
}

for (var i = 0, j = 10; i < j; i++, j--) {
    if (i === 3) continue;
    if (i === 5) break;
}

for (; ;) break;

for (const key in rest) delete rest[key];

for (const value of zs) {
    void value;
}

for (x of zs) ;

for ({ a } of zs) ;

for ([x, y] of zs) ;

for (zs[0] of zs) ;

for ([x] in rest) ;

async function drain(source) {
    for await (const chunk of source) console.log(chunk);
}

outer: while (true) {
    inner: do {
        if (counter > 3) break outer;
        counter += 1;
        continue inner;
    } while (counter < 10)
}

switch (typeof x) {
    case "number":
    case "bigint": {
        counter **= 2;
        break;
    }
    case "string":
        counter = x.length
    default:
        counter >>>= 1;
}

try {
    throw new Error("boom");
} catch ({ message }) {
    console.error(message);
} finally {
    counter = -counter;
}

try {
    JSON.parse("{");
} catch {
}

with (Math) {
    counter = max(counter, PI);
}

function* numbers(limit = 3) {
    let received = yield limit;
    yield* [1, 2, 3];
    return received;
}

function legacyArguments() {
    return arguments.length;
}

class Point {
    static origin = new Point;
    #x = 0;
    y;

    constructor(x, y) {
        this.#x = x;
        this.y = y;
    }

    get x() {
        return this.#x;
    }

    set x(value) {
        this.#x = value;
    }

    static [`computed${counter}`]() {
    }

    * [Symbol.iterator]() {
        yield this.#x;
        yield this.y;
    }

    async load() {
        return await Promise.resolve(this);
    }
}

debugger;
;
module.exports = { Point, numbers, drain, legacyArguments };
