// noinspection JSUnusedLocalSymbols,TypeScriptUnresolvedReference,JSDuplicatedDeclaration

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
import {RecipeSpec} from "../../../src/test";
import {JS, typescript} from "../../../src/javascript";
import {J, JavaType} from "../../../src/java";
import {tap} from "../../test-util";

describe('interface mapping', () => {
    const spec = new RecipeSpec();

    test('empty interface', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 interface Empty {}
             `)
        ));

    test('interface with export modifier', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               export interface Empty {
                   greet(name: string, surname: string): void;
               }
           `)
        ));

    test('interface with declare modifier', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               declare interface Empty {
                   greet(name: string, surname: string): void;
               }
           `)
        ));

    test('interface with extends', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               interface Animal {
                   name: string;
               }
 
               interface Dog extends Animal {
                   breed: string;
               }
           `)
        ));

    test('interface with extending multiple interfaces', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               interface HasLegs {
                   count: string;
               }
 
               interface Animal {
                   name: string;
               }
 
               interface Dog extends Animal, HasLegs {
                   breed: string;
               }
           `)
        ));

    test('interface with properties', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 interface Person {
                   name: string
                   age: number
                 }
             `)
        ));

    test('interface with properties with semicolons', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               interface Person {
                   name: string;
                   age: number;
               }
           `)
        ));

    test('interface with properties with comma', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               interface Person {
                   name: string,
                   age: number,
               }
           `)
        ));

    test('interface with properties with semicolons and comma', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               interface Person {
                   name: string,
                   age: number;
               }
           `)
        ));

    test('interface with properties with semicolons, comma and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               interface Person {
                   /*a*/ name /*b*/: /*c*/ string /*d*/ ; /*e*/
                   age: number /*f*/
               }
           `)
        ));

    test('interface with methods', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               interface Person {
                   greet(): void;
                   age(): number;
                   name(): string,
                   greet_name: (name: string) => string;
               }
           `)
        ));

    test('interface with methods and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               interface Person {
                   /*a*/ greet() /*b*/ :  /*c*/ void /*d*/;
                   age(): number; /*e*/
                   name(): string /*f*/
                   greet_name/*g*/: /*h*/(/*i*/name/*j*/: /*k*/string/*l*/) /*m*/=> /*n*/string  /*o*/;
 
               }
           `)
        ));

    test('interface with properties and methods', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               interface Person {
                   greet(name: string): void
                   name: string
                   name(): string;
                   age: number
                   age(): number;
               }
             `)
        ));

    test('interface with get/set methods', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               interface Person {
                   name: string;
                   get age() : number ; // Getter for age
                   set age(a: number) ;  // Setter for age
               }
           `)
        ));

    test('interface with constructor signature', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 interface Constructible {
                     new (name: string, age: number): Person; // Interface that defines a constructor signature
                 }
             `)
        ));

    test('interface with constructor signature with type parameters', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 interface GenericConstructor {
                     new<R, T> (value: R, value: T): GenericClass;
                 }
             `)
        ));

    test('interface with constructor signature with type parameters and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 interface GenericConstructor {
                     /*a*/new /*b*/</*c*/R/*d*/,/*e*/ T/*f*/>/*g*/ (value1: R, value2: T)/*e*/: GenericClass/*j*/;
                 }
             `)
        ));

    test('interface with properties and methods with modifiers ', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               interface Person {
                   greet(name: string): void
                   readonly name: string
               }
           `)
        ));

    test('interface with optional property signature', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                 interface Person {
                     surname?: string;
                     readonly name ?: string
                 }
             `)
        ));

    test('interface with optional properties and methods ', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               interface Person {
                   greet ?(name: string): void
                   add ?(): (x: number, y?: number) => number;
                   readonly name?: string
               }
           `)
        ));

    test('interface with properties, methods and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               interface Person {
                   /*a*/ greet(/*1*/name/*2*/: /*3*/string/*4*/, /*5*/ surname: string/*6*/): /*b*/ void /*c*/
                   name /*d*/ : string
                   name(/*11*/name/*22*/: /*33*/string/*44*/): string;
                   age: /*e*/ number /*f*/
                   age(): number;
               }
           `)
        ));

    test('interface with function type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               interface Add {
                   add(): (x: number, y: number) => number;
               }
           `)
        ));

    test('interface with function type and zero param', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               interface Add {
                   produce(): () => number;
               }
           `)
        ));

    test('interface with function type and several return types', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               interface Add {
                   consume(): () => number /*a*/ | /*b*/ void;
               }
           `)
        ));

    test('interface with function type and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               interface Add {
                   /*a*/ add/*b*/(/*c*/)/*d*/ : /*e*/ (/*f*/ x/*g */:/*h*/ number /*i*/, /*j*/y /*k*/: /*l*/ number/*m*/)/*n*/ => /*o*/ number /*p*/;
               }
           `)
        ));

    test('interface with call signature', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               interface Add {
                   greet: (name: string) => string;
                   (x: number, y: number): number,
               }
             `)
        ));

    test('interface with call signature and comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               interface Add {
                   greet: (name: string) => string;
                   (x: number /*abc*/): number /*bcd*/,
                   (/*none*/) /*a*/:/*b*/ number /*c*/
               }
           `)
        ));

    test('interface with indexable type', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               interface Add {
                   [index: number]: string
               }
             `)
        ));

    test('interface with hybrid types', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               interface Counter {
                   (start: number): string;   // Call signature
                   interval: number;          // Property
                   reset(): void;             // Method
                   [index: number]: string    // Indexable
                   add(): (x: number, y: number) => number; //Function signature
               }
           `)
        ));

    test('interface with generics', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               interface GenericIdentityFn<   T >   {
                     /*1231*/   < Type    /*1*/ >      (   arg   :    Type    )  :    T ;
                     /*1231*/ /*1231*/ add   < Type    /*1*/ , R >   (arg: Type): (x: T, y: Type) => R; //Function signature
               }
           `)
        ));

    test('interface with generics', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               interface X {
                     find ? <R, T> (v1: R, v2: T): string;
                 }
           `)
        ));

    test('function type with empty args', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                export interface ProxyCursorHooks {
                    getValue?: (/*a*/) => any;
                }
            `)
        ));

    test('interface type mapping', async () => {
        const spec = new RecipeSpec();
        //language=typescript
        const source = typescript(`
                interface ColumnDescriptor {
                    displayName: string,
                    number: number,
                    print(prefix: string): string
                }
                let columnDescriptor: ColumnDescriptor;
                `)
        source.afterRecipe = tree => {
            const varDecl = tree.statements[1].element as J.VariableDeclarations;
            const ident = varDecl.variables[0].element.name as J.Identifier;
            expect(ident.simpleName).toEqual("columnDescriptor");
            expect(ident.type!.kind).toEqual(JavaType.Kind.Class);
            expect((ident.type! as JavaType.Class).classKind).toEqual(JavaType.Class.Kind.Interface);
            const type = ident.type! as JavaType.Class
            expect(type.members).toHaveLength(2);
            tap(type.members[0], mem => {
                expect(mem.kind).toEqual(JavaType.Kind.Variable);
                expect((mem as JavaType.Variable).name).toEqual("displayName");
                expect(mem.type.kind).toEqual(JavaType.Kind.Primitive);
            });
            tap(type.members[1], mem => {
                expect(mem.kind).toEqual(JavaType.Kind.Variable);
                expect((mem as JavaType.Variable).name).toEqual("number");
                expect(mem.type.kind).toEqual(JavaType.Kind.Primitive);
            });
            expect(type.methods).toHaveLength(1);
            tap(type.methods[0], method => {
                expect(method.kind).toEqual(JavaType.Kind.Method);
                expect((method as JavaType.Method).name).toEqual("print");
                expect(method.returnType.kind).toEqual(JavaType.Kind.Primitive);
                expect(method.parameterNames[0]).toEqual("prefix");
                expect(method.parameterTypes[0].kind).toEqual(JavaType.Kind.Primitive);
            });
        }
        await spec.rewriteRun(source);
    })
});
