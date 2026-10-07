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
import {JavaReceiver, JavaSender} from "../../src/java/rpc";
import {asRef, ReferenceMap, RpcReceiveQueue, RpcSendQueue} from "../../src/rpc";

const CLASS_FLAGS = 1 | 2 ** 4; // Public, Final
// Bit 20 is not a Java flag. C# sets it on extension methods.
const METHOD_FLAGS = 1 + 2 ** 43 + 2 ** 20; // Public, Default
const VARIABLE_FLAGS = 2 ** 1 | 2 ** 3 | 2 ** 4; // Private, Static, Final

test("flags of a class, its method and its member survive an RPC round trip", async () => {
    const cls = {
        kind: Type.Kind.Class,
        flags: CLASS_FLAGS,
        classKind: Type.Class.Kind.Class,
        fullyQualifiedName: "com.example.Foo",
        typeParameters: [],
        annotations: [],
        interfaces: [],
        members: [],
        methods: [],
    } as Type.Class;
    cls.methods.push({
        kind: Type.Kind.Method,
        flags: METHOD_FLAGS,
        declaringType: cls,
        name: "bar",
        returnType: Type.Primitive.Void,
        parameterNames: [],
        parameterTypes: [],
        thrownExceptions: [],
        annotations: [],
        declaredFormalTypeNames: [],
    } as Type.Method);
    cls.members.push({
        kind: Type.Kind.Variable,
        flags: VARIABLE_FLAGS,
        name: "baz",
        owner: cls,
        type: Type.Primitive.Int,
        annotations: [],
    } as Type.Variable);

    const sq = new RpcSendQueue(new ReferenceMap(), undefined, false);
    await sq.send(asRef(cls), undefined, () => new JavaSender().visitType(cls, sq));
    const batch = sq.finish();
    const rq = new RpcReceiveQueue(new Map(), undefined, async () => batch, undefined, false);
    const received = await rq.receive<Type>(undefined, t => new JavaReceiver().visitType(t, rq)) as Type.Class;

    expect(received.flags).toBe(CLASS_FLAGS);
    expect(received.methods[0].flags).toBe(METHOD_FLAGS);
    expect(received.members[0].flags).toBe(VARIABLE_FLAGS);
});
