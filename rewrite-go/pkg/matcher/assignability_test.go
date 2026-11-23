/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://docs.moderne.io/licensing/moderne-source-available-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package matcher

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

func method(name string, returns java.JavaType, params ...java.JavaType) *java.JavaTypeMethod {
	return &java.JavaTypeMethod{Name: name, ReturnType: returns, ParameterTypes: params}
}

func goInterface(fqn string, methods ...*java.JavaTypeMethod) *java.JavaTypeClass {
	return &java.JavaTypeClass{FullyQualifiedName: fqn, Kind: "Interface", Methods: methods}
}

func goClass(fqn string, methods ...*java.JavaTypeMethod) *java.JavaTypeClass {
	return &java.JavaTypeClass{FullyQualifiedName: fqn, Kind: "Class", Methods: methods}
}

// errorType is how the type mapper attributes Go's predeclared `error`.
func errorType() *java.JavaTypeClass {
	return goInterface("error", method("Error", goType("string")))
}

func TestIsAssignableToTypeAcceptsTheSameType(t *testing.T) {
	assert.True(t, IsAssignableToType(errorType(), errorType()))
}

func TestIsAssignableToTypeFoldsSpellingsOfOneType(t *testing.T) {
	assert.True(t, IsAssignableToType(goType("byte"), goType("uint8")))
	assert.True(t, IsAssignableToType(goType("uint8"), goType("byte")))
}

func TestIsAssignableToTypeAcceptsALiteralKeyword(t *testing.T) {
	assert.True(t, IsAssignableToType(litType("int"), goType("int")))
	assert.False(t, IsAssignableToType(litType("int"), goType("string")))
}

func TestIsAssignableToTypeAcceptsAStructuralImplementation(t *testing.T) {
	myErr := goClass("a.MyErr", method("Error", goType("string")))
	assert.True(t, IsAssignableToType(myErr, errorType()))
}

func TestIsAssignableToTypeRefusesAMissingMethod(t *testing.T) {
	assert.False(t, IsAssignableToType(goClass("a.Plain"), errorType()))
}

func TestIsAssignableToTypeRefusesAMismatchedSignature(t *testing.T) {
	assert.False(t, IsAssignableToType(goClass("a.WrongErr", method("Error", goType("int"))), errorType()))
	assert.False(t, IsAssignableToType(
		goClass("a.ArgErr", method("Error", goType("string"), goType("string"))), errorType()))
}

func TestIsAssignableToTypeAcceptsAnythingAsAny(t *testing.T) {
	assert.True(t, IsAssignableToType(goType("string"), goInterface("any")))
}

func TestIsAssignableToTypeRefusesAnUnrelatedClass(t *testing.T) {
	assert.False(t, IsAssignableToType(goType("string"), goType("time.Duration")))
}

func TestIsAssignableToTypeRefusesAnAbsentType(t *testing.T) {
	assert.False(t, IsAssignableToType(nil, errorType()))
	assert.False(t, IsAssignableToType(goType("string"), nil))
	assert.False(t, IsAssignableToType(&java.JavaTypeUnknown{}, errorType()))
	assert.False(t, IsAssignableToType(goType("string"), &java.JavaTypeUnknown{}))
}

func TestIsAssignableToTypeRefusesAShallowClassWithNoMethods(t *testing.T) {
	shallow := &java.JavaTypeShallowClass{JavaTypeClass: *goClass("x.Err")}
	assert.False(t, IsAssignableToType(shallow, errorType()))
}

// An embedded interface arrives flattened into the embedding one's methods.
func TestIsAssignableToTypeAcceptsAnInterfaceSatisfyingAnother(t *testing.T) {
	stringer := goInterface("fmt.Stringer", method("String", goType("string")))
	both := goInterface("a.Both", method("Error", goType("string")), method("String", goType("string")))
	assert.True(t, IsAssignableToType(both, stringer))
	assert.False(t, IsAssignableToType(stringer, errorType()))
}

// Go declares no interface relation, but a peer model over RPC records one.
func TestIsAssignableToTypeFollowsARecordedInterface(t *testing.T) {
	recorded := goClass("a.Recorded")
	recorded.Interfaces = []java.FullyQualified{errorType()}
	assert.True(t, IsAssignableToType(recorded, errorType()))
}

func TestIsAssignableToTypeComparesTypeArguments(t *testing.T) {
	mapOf := func(k, v java.JavaType) java.JavaType {
		return &java.JavaTypeParameterized{
			Type:           goClass("map"),
			TypeParameters: []java.JavaType{k, v},
		}
	}
	assert.True(t, IsAssignableToType(mapOf(goType("string"), goType("int")), mapOf(goType("string"), goType("int"))))
	assert.False(t, IsAssignableToType(mapOf(goType("int"), goType("string")), mapOf(goType("string"), goType("int"))))
}

func TestIsAssignableToTypeAcceptsALiteralAsAny(t *testing.T) {
	assert.True(t, IsAssignableToType(litType("int"), goInterface("any")))
	assert.False(t, IsAssignableToType(litType("int"), errorType()))
}
