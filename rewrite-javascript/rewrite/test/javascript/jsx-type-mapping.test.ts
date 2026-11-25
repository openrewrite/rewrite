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
import * as fs from "node:fs";
import * as os from "node:os";
import * as path from "node:path";
import {JavaScriptParser, JavaScriptVisitor, JS, JSX} from "../../src/javascript";
import {J, Type} from "../../src/java";

function identity(method: Type.Method | undefined): string {
    return method ? `${Type.FullyQualified.getFullyQualifiedName(method.declaringType)}#${method.name}` : '∅';
}

function tagName(tag: JSX.Tag): string {
    const name = tag.openName.element;
    if (name.kind === J.Kind.Identifier) {
        return name.simpleName;
    }
    if (name.kind === J.Kind.FieldAccess) {
        return `${(name.target as J.Identifier).simpleName}.${name.name.element.simpleName}`;
    }
    if (name.kind === JS.Kind.JsxNamespacedName) {
        return `${name.namespace.simpleName}:${name.name.element.simpleName}`;
    }
    return '<>';
}

const project = {
    "tsconfig.json": JSON.stringify({compilerOptions: {jsx: "preserve", strict: true, module: "esnext", moduleResolution: "bundler"}}),
    "node_modules/@types/jsx/package.json": JSON.stringify({name: "@types/jsx", version: "1.0.0", types: "index.d.ts"}),
    "node_modules/@types/jsx/index.d.ts": `
        declare namespace JSX {
            interface Element {}
            interface ElementClass { render(): any }
            interface ElementAttributesProperty { props: {} }
            interface IntrinsicElements { [name: string]: any }
        }
    `,
    "node_modules/ui/package.json": JSON.stringify({name: "ui", version: "1.0.0", types: "index.d.ts"}),
    "node_modules/ui/index.d.ts": `
        export declare function Button(props: {label: string}): JSX.Element;
        export declare class Panel { props: {}; render(): JSX.Element }
    `,
};

async function tags(files: Record<string, string>): Promise<Record<string, Record<string, string>>> {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), "rewrite-jsx-type-mapping-"));
    const found: Record<string, Record<string, string>> = {};
    try {
        for (const [file, text] of Object.entries({...project, ...files})) {
            const target = path.join(dir, file);
            fs.mkdirSync(path.dirname(target), {recursive: true});
            fs.writeFileSync(target, text);
        }
        const parser = new JavaScriptParser({relativeTo: dir});
        for await (const sourceFile of parser.parse(...Object.keys(files).map(f => path.join(dir, f)))) {
            const inFile: Record<string, string> = found[sourceFile.sourcePath] = {};
            await new class extends JavaScriptVisitor<void> {
                protected override async visitJsxTag(tag: JSX.Tag, p: void) {
                    inFile[tagName(tag)] = identity(tag.methodType);
                    return super.visitJsxTag(tag, p);
                }

                protected override async visitMethodInvocation(method: J.MethodInvocation, p: void) {
                    const select = method.select?.element;
                    const receiver = select?.kind === J.Kind.Identifier ? `${(select as J.Identifier).simpleName}.` : '';
                    inFile[`${receiver}${method.name.simpleName}()`] = identity(method.methodType);
                    return super.visitMethodInvocation(method, p);
                }
            }().visit(sourceFile, undefined);
        }
    } finally {
        fs.rmSync(dir, {recursive: true, force: true});
    }
    return found;
}

describe('a JSX tag carries the method type a call of the component it renders would', () => {

    test('a function component, an arrow component and a default-exported component are named as their declarations', async () => {
        // when
        const found = await tags({
            "src/Counter.tsx": `
                export function Counter(props: {n: number}) { return <div>{props.n}</div>; }
                export const Badge = () => <span/>;
                export default function () { return <i/>; }
                function Local() { return <s/>; }
                export const local = <Local/>;
            `,
            "test/counter.test.tsx": `
                import Default, {Counter, Badge} from '../src/Counter';
                const a = <Counter n={1}/>;
                const b = <Badge/>;
                const c = <Default/>;
                Counter({n: 2});
            `,
        });

        // then
        expect(found["test/counter.test.tsx"]).toEqual({
            Counter: 'src/Counter#Counter',
            Badge: 'src/Counter#Badge',
            Default: 'src/Counter#<default>',
            'Counter()': 'src/Counter#Counter',
        });
        expect(found["src/Counter.tsx"]).toMatchObject({Local: 'src/Counter#Local'});
    });

    test('a class component is named as its constructor', async () => {
        // when
        const found = await tags({
            "src/Klass.tsx": `
                export class Klass { props!: {x?: number}; render() { return <b/>; } }
                export class Stateful { props: {}; constructor(props: {}) { this.props = props; } render() { return <b/>; } }
            `,
            "test/klass.test.tsx": `
                import {Klass, Stateful} from '../src/Klass';
                const a = <Klass x={1}/>;
                const b = <Stateful/>;
            `,
        });

        // then
        expect(found["test/klass.test.tsx"]).toEqual({
            Klass: 'src/Klass.Klass#<constructor>',
            Stateful: 'src/Klass.Stateful#<constructor>',
        });
    });

    test('a component of an installed package is named after the specifier, as a call into it is', async () => {
        // when
        const found = await tags({
            "test/ui.test.tsx": `
                import {Button, Panel} from 'ui';
                const a = <Button label="x"/>;
                const b = <Panel/>;
            `,
        });

        // then
        expect(found["test/ui.test.tsx"]).toEqual({
            Button: 'ui#Button',
            Panel: 'ui.Panel#<constructor>',
        });
    });

    test('a member tag is named as a call of the member', async () => {
        // when
        const found = await tags({
            "src/parts.tsx": `
                export const Foo = { Bar: (props: {}) => <p/> };
                export namespace NS { export function Inner() { return <u/>; } }
                export function Counter() { return <i/>; }
            `,
            "test/parts.test.tsx": `
                import * as parts from '../src/parts';
                import {Foo, NS} from '../src/parts';
                const a = <Foo.Bar/>;
                const b = <NS.Inner/>;
                const c = <parts.Counter/>;
                Foo.Bar({});
                NS.Inner();
                parts.Counter();
            `,
        });

        // then
        const parts = found["test/parts.test.tsx"];
        expect(parts['Foo.Bar']).toEqual('src/parts#Bar');
        expect(parts['NS.Inner']).toEqual('src/parts.NS#Inner');
        expect(parts['parts.Counter']).toEqual('src/parts#Counter');
        expect(parts['Foo.Bar']).toEqual(parts['Foo.Bar()']);
        expect(parts['NS.Inner']).toEqual(parts['NS.Inner()']);
        expect(parts['parts.Counter']).toEqual(parts['parts.Counter()']);
    });

    test('an intrinsic element, a fragment and an unresolved tag have no method type', async () => {
        // when
        const found = await tags({
            "src/plain.tsx": `
                export const a = <div><my-element/><svg:rect/></div>;
                export const b = <><span/></>;
                export const c = <Missing/>;
                const Tag = 'div' as const;
                export const d = <Tag/>;
                export function Heading({level}: {level: 1 | 2}) { const H = level === 1 ? 'h1' : 'h2'; return <H/>; }
                export function Dynamic({as: As}: {as: string}) { return <As/>; }
            `,
        });

        // then
        expect(found["src/plain.tsx"]).toEqual({
            div: '∅',
            'my-element': '∅',
            'svg:rect': '∅',
            '<>': '∅',
            span: '∅',
            Missing: '∅',
            Tag: '∅',
            H: '∅',
            As: '∅',
        });
    });
});
