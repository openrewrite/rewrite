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
// The JavaScript built-ins EcmaScript, UriComponent and V8RegExp port, asked of the Node that runs this, as rows.
// Usage: node node-builtins.js > ../upstream/node-24.19.0-builtins.jsonl
const rows = []

function encode(v) {
  if (v === undefined) {
    return {$: 'undefined'}
  }
  if (typeof v === 'number' && (!Number.isFinite(v) || Object.is(v, -0))) {
    return {$: 'number', value: Object.is(v, -0) ? '-0' : String(v)}
  }
  if (Array.isArray(v)) {
    return v.map(encode)
  }
  return v
}

function ask(test, fn, args, call) {
  const row = {test, fn, args: args.map(encode)}
  try {
    row.out = encode(call(...args))
  } catch (e) {
    row.threw = {$: 'error', name: e.name, message: e.message}
  }
  rows.push(JSON.stringify(row))
}

const integers = ['', ' 12', '\u3000\u2028-12', '\ufeff42', '0x1F', '-0x1F', '+0x10', '0X1a', '0x', '0xg', '-0',
  '  -0  ', '1e3', '12abc', '077', '0b101', '0o17', '1_000', '99999999999999999999999', '9007199254740993',
  '123456789012345678901234567890', '\u0661\u0662', '\uff14\uff12', 'z', 'Z9', '-', '+', '+-1', 'Infinity', 'NaN',
  '.5', '-.5', '00012', 'ffffffffffffffffffffffff', '0'.repeat(320) + '7', '1' + '0'.repeat(320), '9'.repeat(400),
  '1'.repeat(30), 'zzzzzzzzzzzzzzzzzzzzzzzzz']
for (const radix of [undefined, 2, 8, 10, 16, 36, 1, 37]) {
  for (const s of integers) {
    ask('parseInt', 'parseInt', [s, radix], (x, r) => parseInt(x, r))
  }
}

for (const [s, start, end] of [['abcdef', 2, -1], ['abcdef', -3, undefined], ['abcdef', -10, 2], ['abcdef', 4, 2],
  ['abcdef', 0, -0], ['', 0, 1], ['abc', 1, 100], ['abc', 3, undefined], ['abc', 0, -3]]) {
  ask('String.prototype.slice', 'slice', end === undefined ? [s, start] : [s, start, end],
    (x, a, b) => x.slice(a, b))
}

for (const [s, count] of [['ab', 0], ['ab', 3], ['', 5], ['0', 1]]) {
  ask('String.prototype.repeat', 'repeat', [s, count], (x, n) => x.repeat(n))
}

for (const [s, separator] of [['a/b//c', '/'], ['abc', ''], ['', '/'], ['', ''], ['a..b..', '..'], ['/a/', '/']]) {
  ask('String.prototype.split', 'split', [s, separator], (x, sep) => x.split(sep))
}

const doubles = [0, -0, 1.5, -1.5, 2147483647, 2147483648, -2147483649, 4294967296.5, 1e21, -1e21, 2 ** 53 + 2,
  2 ** 63 + 2048, 1e300, NaN, Infinity, -Infinity, 9007199254740991]
for (const d of doubles) {
  ask('ToInt32', 'toInt32', [d], x => x >> 0)
  ask('Number.isInteger', 'isInteger', [d], x => Number.isInteger(x))
}

for (const s of ['0x10', ' 12 ', '1e3', '', '0b2', '0b11', '0o8', 'Infinity', '-Infinity', '+0x10', '1_0',
  '\u0663', '.5e1', '5.', '\u00a0 7 \ufeff', '0X1F', '-0', 'infinity']) {
  ask('Number', 'toNumber', [s], x => Number(x))
}

for (const d of [1e21, 1e-7, 123e-20, -0, 0.1 + 0.2, 2 ** 53 + 2, 100, 1.5e300, 5e-324, 123456789012345680000]) {
  ask('Number.prototype.toString', 'numberToString', [d], x => String(x))
}

for (const s of ['abc', '\t a b \n', '\u3000\u2028x\u2029\ufeff', '\u180ex\u180e', '\u0085x\u200b']) {
  ask('String.prototype.trim', 'trim', [s], x => x.trim())
  ask('String.prototype.trimStart', 'trimStart', [s], x => x.trimStart())
}

// Object.entries of an object made from [key, value] pairs in their order, or of the value itself
const emoji = 'a' + String.fromCharCode(0xd83d, 0xde00)
for (const [kind, value] of [['object', [['b', 1], ['2', true], ['a', 'x'], ['1', null], ['01', 0], ['4294967295', 2],
  ['4294967294', [1]]]], ['object', []], ['value', ['x', null, ['y']]], ['value', emoji], ['value', ''], ['value', 5],
  ['value', true], ['value', null], ['value', undefined]]) {
  ask('Object.entries', 'entries', [kind, value], (k, x) => Object.entries(k === 'object' ? Object.fromEntries(x) : x))
}

// JSON.stringify(value, replacer, space): a value as the rows hold it, an object from [key, value] pairs, an array with
// holes from [index, value] pairs, or a chain of containers along keys whose last member is the one at index back
const replacers = {
  none: undefined,
  dropB: (k, v) => k === 'b' ? undefined : v,
  wrapRoot: (k, v) => k === '' ? {wrapped: v} : v,
  numbersAsStrings: (k, v) => typeof v === 'number' ? String(v) : v,
  keyed: (k, v) => typeof v === 'string' ? k + '=' + v : v,
  undefinedAsNull: (k, v) => v === undefined ? null : v,
}
function build(kind, value, extra) {
  if (kind === 'object') {
    return Object.fromEntries(value)
  }
  if (kind === 'sparse') {
    const array = new Array(value)
    for (const [i, v] of extra) {
      array[i] = v
    }
    return array
  }
  if (kind === 'cycle') {
    const chain = [typeof value[0] === 'number' ? [] : {}]
    for (let i = 0; i < value.length - 1; i++) {
      const next = typeof value[i + 1] === 'number' ? [] : {}
      chain[i][value[i]] = next
      chain.push(next)
    }
    chain[chain.length - 1][value[value.length - 1]] = chain[extra]
    return chain[0]
  }
  return value
}
const stringified = [['value', 'x'], ['value', 1e21], ['value', -0], ['value', NaN], ['value', Infinity],
  ['value', null], ['value', true], ['value', undefined], ['value', [1, undefined, null, 'a\n\u2028\ud800']],
  ['value', []], ['value', {}], ['value', {a: [], b: {}, c: [{}, []]}],
  ['value', {b: 1, a: {c: 'x', d: [1, {e: null}]}}],
  ['object', [['b', 1], ['2', true], ['a', [1, 2]], ['u', undefined], ['1', {x: 'y'}]]], ['sparse', 3, [[1, 'x']]],
  ['cycle', ['a'], 0], ['cycle', ['a', 'b', 'c'], 1], ['cycle', [0, 0, 0, 0], 0], ['cycle', ['q', 0], 1],
  ['cycle', ['k', 'k', 'k', 'z'], 0], ['cycle', ['k', 'k', 'k', 'k', 'z'], 0], ['cycle', ['2', 'x'], 0],
  ['cycle', ['b', '', 'c'], 1], ['cycle', ['k0', 'k1', 'k2', 'k3', 'k4', 'k5', ''], 2], ['cycle', ['01', 1], 0]]
const spaces = [undefined, 2, 0, -1, 1.5, 11, Infinity, NaN, '\t', '', ' ^_^ ', '12345678901', true, null]
for (const [kind, value, extra] of stringified) {
  for (const replacer of Object.keys(replacers)) {
    for (const space of kind === 'cycle' ? [undefined] : replacer === 'none' ? spaces : [undefined, 2]) {
      ask('JSON.stringify', 'jsonStringify', [kind, value, extra, replacer, space],
        (k, v, x, r, s) => JSON.stringify(build(k, v, x), replacers[r], s))
    }
  }
}

for (const t of [0, -0, -1, 1.5, -1.5, 1791462896789, 253402300799999, 253402300800000, -62167219200000,
  -62167219200001, 8.64e15, -8.64e15, 8.64e15 + 1, NaN, Infinity]) {
  ask('Date.prototype.toISOString', 'toISOString', [t], x => new Date(x).toISOString())
  ask('TimeClip', 'timeClip', [t], x => new Date(x).getTime())
}
for (const t of [0.999, -0.5, 1e15 + 0.5, -8.64e15 - 1, 1791462896789.75]) {
  ask('TimeClip', 'timeClip', [t], x => new Date(x).getTime())
}
for (const fields of [[2020, 0, 1, 0, 0, 0, 0], [99, 11, 31, 23, 59, 59, 999], [0, 0, 1, 0, 0, 0, 0],
  [100, 0, 1, 0, 0, 0, 0], [2020, 1, 30, 0, 0, 0, 0], [2020, 13, 1, 0, 0, 0, 0], [2020, -1, 1, 0, 0, 0, 0],
  [275760, 8, 13, 0, 0, 0, 0], [275760, 8, 13, 0, 0, 0, 1], [-271821, 3, 20, 0, 0, 0, 0],
  [-271821, 3, 19, 23, 59, 59, 999], [1970, 0, 0, 24, 0, 0, 0]]) {
  ask('Date.UTC', 'utc', fields, (...f) => Date.UTC(...f))
}

for (const s of ['a b', "-_.!~*'()", '\u00e9', '\ud83d\ude00', '\ud83d', '\ude00x', '#?&=/:@+$,;', '%']) {
  ask('encodeURIComponent', 'encodeURIComponent', [s], x => encodeURIComponent(x))
}
for (const s of ['%20', '%E9', '%C3%A9', '%', '%zz', '%C3', '%F0%9F%98%80', '%ED%A0%80', 'a%2fb', '%3', '%c3%a9']) {
  ask('decodeURIComponent', 'decodeURIComponent', [s], x => decodeURIComponent(x))
}
for (const s of ['%2F%41', '/a%20b/%C3%A9', '%E2%82%AC%23x', '%25', 'a%2Fb%3Fc%40d', '%F0%9F%98%80', '%', '%C3',
  '%C3%28', '%ED%A0%80', '%80', '%41%2', '%G1', '%3B%2C%26%3D%2B%24']) {
  ask('decodeURI', 'decodeURI', [s], x => decodeURI(x))
}

// String.prototype.replace with a pattern and a replacement string (GetSubstitution)
const replacements = [['abcabc', 'b', false, '[$&]'], ['abcabc', 'b', true, '[$&]'], ['abc', '(b)', false, '$1$1'],
  ['abc', '(b)', false, '$$'], ['abc', '(b)', false, '$`'], ['abc', '(b)', false, "$'"], ['abc', '(b)', false, '$0'],
  ['abc', '(b)', false, '$2'], ['abc', '(b)', false, '$01'], ['abc', '(b)', false, '$10'], ['abc', '(b)', false, '$'],
  ['abc', '(b)(c)?', false, '<$2>'], ['abcdefghijk', '(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)(k)', false, '$11-$10-$1'],
  ['abc', '(?<n>b)', false, '$<n>'], ['abc', 'b', false, '$<n>'], ['aaa', 'a*?', true, '-'], ['abc', '', true, '-'],
  ['abc', '(b)', false, '$1\u0663']]
for (const [s, source, global, replacement] of replacements) {
  ask('String.prototype.replace', 'replace', [s, source, global, replacement],
    (x, src, g, r) => x.replace(new RegExp(src, g ? 'g' : ''), r))
}

// new RegExp(source) without flags: the match of each subject, or V8's SyntaxError
const patterns = [['a(', 'a'], ['[', ''], ['(?<n>a)\\k<n>', 'aa'], ['\\k<n>', 'k<n>'], ['a{2,1}', 'aa'],
  ['a{,2}', 'a{,2}'], ['\\p{L}', 'p{L}'], ['(?=a)*b', 'b'], ['[\\d-z]', '-'], ['.', '\u2028'], ['.', '\u0085'],
  ['\\s', '\ufeff'], ['\\s', '\u180e'], ['\\bx', '\u00e9x'], ['\\w', '\u00e9'], ['[^]', '\n'], ['[]', 'a'],
  ['\\cJ', '\n'], ['\\c1', '\\c1'], ['\\8', '8'], ['(a)\\1', 'aa'], ['\\1(a)', 'a'], ['\\0', '\0'],
  ['\\u{41}', 'u'.repeat(41)], ['\\x4', 'x4'], ['$^', ''], ['a|', 'b'], ['(?:)', 'x'], ['\\/', '/'], ['[\\b]', '\b'],
  ['^\\S+$', 'a\u00a0b'], ['a{1}?', 'a'], ['(?!a)?', 'a'], ['\\D\\W', '-.'], ['x*', 'xxx'], ['[a-\\d]', '-'],
  ['\\-', '-'], ['(?<a>.)(?<b>.)', 'xy'], ['(?<a>x)|(?<a>y)', 'y'], ['{2,1}', ''], ['(?=a){2,1}', ''],
  ['(?<=a){1}', ''], ['(?=a){', 'a{'], ['(?<=a){', 'a{'], ['x{1}{2}', ''], ['(?i:a)', 'A'], ['(?-:a)', ''],
  ['(?ii:a)', ''], ['(?--:a)', ''], ['(?i', ''], ['(?I:a)', ''], ['(?i-m)', ''], ['(?<m>a)\\k<n>', ''],
  ['(?<m>a)\\k', ''], ['(?<m>a)\\k<', ''], ['(?<1a>x)', ''], ['(?<>x)', ''], ['(?<a-b>x)', ''],
  ['(?<a>x)\\k<b>(', ''], ['[\\k<a>](?<a>x)', ''], ['[\\k]', 'k'], ['(?<a>.)\\k<a>', 'xx'], ['\\k<a>(?<a>x)', 'x'],
  ['(?<a>x\\k<a>)', 'x']]
for (const [source, subject] of patterns) {
  ask('RegExp', 'match', [source, subject], (src, x) => {
    const m = x.match(new RegExp(src))
    return m === null ? null : Array.from(m)
  })
}

// new RegExp(source, 'u'): the match of each subject, or V8's SyntaxError; a surrogate escape is spelled in two parts
const bs = '\\'
const cp = String.fromCodePoint
const ch = String.fromCharCode
const pair = bs + 'uD83D' + bs + 'uDE00'
const unicodePatterns = [['\\-', '-'], ['\\,', ','], ['\\#', '#'], ['\\ ', ' '], ['\\!', '!'], ['\\/', '/'],
  ['\\.', 'a.b'], ['\\{', '{'], ['[\\-]', '-'], ['[\\,]', ','], ['[\\!]', '!'], ['\\c1', ''], ['\\c', ''],
  ['\\x4', ''], ['\\x', ''], ['[\\x]', ''], ['\\u12', ''], ['[\\u12]', ''], ['\\u{1F600}', cp(0x1F600)],
  ['\\u{110000}', ''], ['\\u{}', ''], ['\\u{1F600', ''], ['\\u{0}', ch(0)], ['\\1', ''], ['\\8', ''], ['\\9', ''],
  ['(a)\\1', 'aa'], ['(a)\\2', ''], ['\\0', ch(0)], ['\\00', ''], ['\\07', ''], ['[\\0]', ch(0)], ['[\\00]', ''],
  ['[\\1]', ''], ['[\\8]', ''], ['{', ''], ['}', ''], [']', ''], ['a{1', ''], ['a{,2}', ''], ['a{', ''],
  ['a{1,', ''], ['a{1,2', ''], ['{1}', ''], ['x{1}', 'xx'], ['a{2,1}', ''], ['*', ''], ['a|*', ''], ['^*', ''],
  ['$*', ''], ['\\b*', ''], ['\\b+', ''], ['(*)', ''], ['a**', ''], ['(?=a)*', ''], ['(?!a)?', ''], ['(?<=a)*', ''],
  ['(?<!a)?', ''], ['[\\d-z]', ''], ['[a-\\d]', ''], ['[\\p{L}-z]', ''], ['[\\w-]', '-'], ['[-\\w]', 'w'],
  ['[a-]', '-'], ['[b-a]', ''], ['[', ''], ['[a', ''], ['[\\]', ''], ['a)', ''], ['(a', ''], ['(?:a', ''],
  ['(?<n>a', ''], ['(?=a', ''], ['\\', ''], ['\\k<n>', ''], ['\\k', ''], ['(?<n>a)\\k<n>', 'aa'], ['(?x)', ''],
  ['(?<a>x)|(?<a>y)', 'y'], ['\\p{L}', ch(0xe9)], ['\\P{L}', 'a1'], ['\\p{Lu}', 'aB'], ['\\p{gc=Lu}', 'aB'],
  ['\\p{General_Category=Lu}', 'aB'], ['\\p{Lu}*', 'AB'], ['\\p{Letter}', 'a'], ['\\p{Script=Greek}', 'a'],
  ['\\p{ASCII}', 'a'], ['\\p{Foo}', ''], ['\\p', ''], ['\\P', ''], ['\\p{', ''], ['\\p{L', ''], ['[\\p]', ''],
  ['[\\p{L}\\p{Nl}]', '1' + ch(0x2160)], ['[^\\p{Z}\\p{C}]', ' x'], ['[\\x00-\\x7f]', ch(0xe9) + 'a'],
  ['[\\t\\r\\n\\v\\f]', 'a' + ch(11)], ['\\b', 'a'], ['[\\b]', ch(8)], ['\\B', 'ab'], ['[\\B]', ''],
  ['[\\c_]', ''], ['[\\c]', ''], ['.', cp(0x1F600)], ['^.$', cp(0x1F600)], ['^[\\p{L}]$', cp(0x1F600)],
  ['[^/]*?', 'ab'], ['(?:a|b)', 'b'], ['\\d\\w\\s', '1a '], ['[\\s]', ch(0xa0)], ['\\a', ''], ['\\e', ''],
  ['\\_', ''], ['\\$', '$'], ['\\^', '^'], ['\\|', '|'], ['\\(', '('], ['\\)', ')'], ['\\[', '['], ['\\]', ']'],
  ['\\}', '}'], ['\\*', '*'], ['\\+', '+'], ['\\?', '?'], ['\\\\', '\\'], ['[\\]]', ']'], ['[\\[]', '['],
  ['[\\\\]', '\\'], ['[\\^]', '^'], ['[\\$]', '$'], ['[\\/]', '/'], ['[\\.]', '.'], ['[\\{]', '{'], ['[\\|]', '|'],
  ['[\\-a]', '-'], ['\\-x', ''], ['[\\u{1F600}-\\u{1F602}]', cp(0x1F601)],
  ['[' + pair + '-' + bs + 'uD83D' + bs + 'uDE02]', cp(0x1F601)], [pair + '*', cp(0x1F600) + cp(0x1F600)],
  ['^(?!\\.)[\\p{L}\\p{Nl}][^/]*?x$', 'ab' + cp(0x1F600) + 'x'], ['^(?!\\.)[\\p{L}\\p{Nl}][^/]*?\\-x$', 'ab-x'],
  ['^(?!\\.)[^/]*?\\.js$', 'a.js'], ['^(?!\\.)([\\p{L}]|[^\\p{Z}\\p{C}])$', '.'], ['{2,1}', ''], ['(?=a){2,1}', ''],
  ['(?<=a){2,1}', ''], ['(?=a){1}', ''], ['(?=){', ''], ['(?<=){ ', ''], ['^{2,1}', ''], ['({1})', ''], ['\\b{1}', ''],
  ['\\b{1', ''], ['^{', ''], ['x{1}{2}', ''], ['(?=a)?', ''], ['\\k<n>(', ''], ['\\k<n>{', ''], ['\\k<n>(?<m>a)', ''],
  ['(?<m>a)\\k<n>', ''], ['\\k<n>*', ''], ['\\k<1>', ''], ['\\k<n', ''], ['\\k<n>)', ''], ['\\k<n>[', ''],
  ['(?<a>x)\\k<b>(', ''], ['(?<a>.)\\k<a>', 'xx'], ['(?-i-s:a)', ''], ['(?i-:a)', 'a'], ['(?i', ''], ['(?-)', ''],
  ['(?<1a>x)', ''], ['(?<a', ''], ['[\\S]', cp(0x1F600)], ['[\\s\\S]', cp(0x1F600) + 'b'], ['[^\\S]', cp(0x1F600)],
  ['\\S', cp(0x1F600)], ['[^\\s]', cp(0x1F600)], ['[\\S\\d]', cp(0x1F600)], ['\\S+', 'a' + cp(0x1F600) + ' ']]
for (const [source, subject] of unicodePatterns) {
  ask('RegExp', 'match', [source, subject, 'u'], (src, x, flags) => {
    const m = x.match(new RegExp(src, flags))
    return m === null ? null : Array.from(m)
  })
}

process.stdout.write([JSON.stringify({header: 'Node ' + process.versions.node + ' (V8 ' + process.versions.v8 +
  '): the built-ins the lock package ports, asked; generate/node-builtins.js'}), ...rows].join('\n') + '\n')
