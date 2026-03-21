import {RecipeSpec} from "../../../src/test";
import {JavaScriptVisitor, typescript} from "../../../src/javascript";
import {J, Type} from "../../../src/java";

describe("function type mapping", () => {
    const spec = new RecipeSpec();

    function assertFunctionType(
        funcType: Type,
        expectedParamCount: number,
        expectedParamNames: string[],
        returnTypeKeyword?: string
    ) {
        expect(Type.isFunctionType(funcType)).toBeTruthy();
        const classType = funcType as Type.Class;
        expect(classType.fullyQualifiedName).toBe('𝑓');

        // Type parameters: [R (covariant), P1 (contravariant), P2 (contravariant), ...]
        expect(classType.typeParameters.length).toBe(expectedParamCount + 1);

        // Check return type parameter (R) - should be covariant
        const returnTypeParam = classType.typeParameters[0] as Type.GenericTypeVariable;
        expect(returnTypeParam.kind).toBe(Type.Kind.GenericTypeVariable);
        expect(returnTypeParam.name).toBe('R');
        expect(returnTypeParam.variance).toBe(Type.GenericTypeVariable.Variance.Covariant);

        if (returnTypeKeyword) {
            expect(returnTypeParam.bounds.length).toBe(1);
            expect(Type.isPrimitive(returnTypeParam.bounds[0])).toBeTruthy();
            expect((returnTypeParam.bounds[0] as Type.Primitive).keyword).toBe(returnTypeKeyword);
        }

        // Check parameter type variables - should be contravariant
        for (let i = 0; i < expectedParamCount; i++) {
            const paramTypeVar = classType.typeParameters[i + 1] as Type.GenericTypeVariable;
            expect(paramTypeVar.kind).toBe(Type.Kind.GenericTypeVariable);
            expect(paramTypeVar.name).toBe(`P${i + 1}`);
            expect(paramTypeVar.variance).toBe(Type.GenericTypeVariable.Variance.Contravariant);
            expect(paramTypeVar.bounds.length).toBe(1);
        }

        // Apply method
        expect(classType.methods.length).toBe(1);
        const applyMethod = classType.methods[0];
        expect(applyMethod.name).toBe('apply');
        expect(applyMethod.parameterTypes.length).toBe(expectedParamCount);
        expect(applyMethod.parameterNames).toEqual(expectedParamNames);
    }

    test("arrow function assigned to variable", async () => {
        await spec.rewriteRun(
            //language=ts
            {
                ...typescript(`
                    const add = (a: number, b: number): number => a + b;
                    const greet = (name: string): string => \`Hello, \${name}\`;
                `),
                afterRecipe: async cu => {
                    let foundAdd = false;
                    let foundGreet = false;

                    await (new class extends JavaScriptVisitor<any> {
                        async visitIdentifier(identifier: J.Identifier, _: any): Promise<J | undefined> {
                            if (identifier.simpleName === 'add' && identifier.type) {
                                foundAdd = true;
                                assertFunctionType(identifier.type, 2, ['a', 'b'], 'double');
                            }

                            if (identifier.simpleName === 'greet' && identifier.type) {
                                foundGreet = true;
                                assertFunctionType(identifier.type, 1, ['name'], 'String');
                            }

                            return identifier;
                        }
                    }).visit(cu, 0);

                    expect(foundAdd).toBeTruthy();
                    expect(foundGreet).toBeTruthy();
                }
            }
        );
    });

    test("function declaration", async () => {
        await spec.rewriteRun(
            //language=ts
            {
                ...typescript(`
                    function multiply(x: number, y: number): number {
                        return x * y;
                    }
                `),
                afterRecipe: async cu => {
                    let foundMultiply = false;

                    await (new class extends JavaScriptVisitor<any> {
                        async visitIdentifier(identifier: J.Identifier, _: any): Promise<J | undefined> {
                            if (identifier.simpleName === 'multiply' && identifier.type) {
                                foundMultiply = true;
                                assertFunctionType(identifier.type, 2, ['x', 'y'], 'double');
                            }
                            return identifier;
                        }
                    }).visit(cu, 0);

                    expect(foundMultiply).toBeTruthy();
                }
            }
        );
    });

    test("function expression", async () => {
        await spec.rewriteRun(
            //language=ts
            {
                ...typescript(`
                    const divide = function(a: number, b: number): number {
                        return a / b;
                    };
                `),
                afterRecipe: async cu => {
                    let foundDivide = false;

                    await (new class extends JavaScriptVisitor<any> {
                        async visitIdentifier(identifier: J.Identifier, _: any): Promise<J | undefined> {
                            if (identifier.simpleName === 'divide' && identifier.type) {
                                foundDivide = true;
                                assertFunctionType(identifier.type, 2, ['a', 'b'], 'double');
                            }
                            return identifier;
                        }
                    }).visit(cu, 0);

                    expect(foundDivide).toBeTruthy();
                }
            }
        );
    });
});
