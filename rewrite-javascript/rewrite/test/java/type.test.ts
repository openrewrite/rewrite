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
import {Type} from "../../src/java";

describe('Type.isFullyQualified', () => {
    test('guards getFullyQualifiedName, so an array is not fully qualified', () => {
        const array: Type.Array = {kind: Type.Kind.Array, elemType: Type.Primitive.String, annotations: []};
        expect(Type.isFullyQualified(array)).toBe(false);
    });
});

describe('Type.isOfTypeWithName', () => {
    function classType(fullyQualifiedName: string): Type.Class {
        return {
            kind: Type.Kind.Class,
            flags: 0,
            classKind: Type.Class.Kind.Interface,
            fullyQualifiedName,
            typeParameters: [],
            annotations: [],
            interfaces: [],
            members: [],
            methods: []
        } as Type.Class;
    }

    test('finds a name its heritage records', () => {
        const request = classType('express-serve-static-core.Request');
        const incomingMessage = classType('http.IncomingMessage');
        const readable = classType('stream.Readable');
        request.interfaces.push(incomingMessage);
        incomingMessage.supertype = readable;

        expect(Type.isOfTypeWithName(request, true, name => name === 'stream.Readable')).toBe(true);
        expect(Type.isOfTypeWithName(request, false, name => name === 'stream.Readable')).toBe(false);
    });

    test('ends where the heritage cycles back through an interface', () => {
        const request = classType('express.Request');
        const coreRequest = classType('express-serve-static-core.Request');
        const incomingMessage = classType('http.IncomingMessage');
        request.interfaces.push(coreRequest);
        coreRequest.interfaces.push(request, incomingMessage);

        expect(Type.isOfTypeWithName(request, true, () => false)).toBe(false);
        expect(Type.isOfTypeWithName(request, true, name => name === 'http.IncomingMessage')).toBe(true);
    });

    test('ends where the heritage cycles back through a parameterized supertype', () => {
        const application = classType('express-serve-static-core.Application');
        const parameterized: Type.Parameterized = {
            kind: Type.Kind.Parameterized,
            type: application,
            typeParameters: [Type.Primitive.String]
        } as Type.Parameterized;
        application.classKind = Type.Class.Kind.Class;
        application.supertype = parameterized as unknown as Type.Class;

        expect(Type.isOfTypeWithName(parameterized, true, () => false)).toBe(false);
    });
});
