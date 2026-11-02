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
package rpc

import (
	"go/ast"
	goparser "go/parser"
	gotoken "go/token"
	"os"
	"path/filepath"
	"reflect"
	"sort"
	"strings"
	"testing"

	"github.com/google/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// declaredMarkers reads back every marker the tree packages declare, as
// package-qualified names. A marker is a struct with an ID method.
func declaredMarkers(t *testing.T) []string {
	t.Helper()
	var names []string
	for _, pkg := range []string{"golang", "java"} {
		pkgs, err := goparser.ParseDir(gotoken.NewFileSet(), filepath.Join("..", "tree", pkg), nil, 0)
		require.NoError(t, err)
		for _, parsed := range pkgs {
			for _, file := range parsed.Files {
				for _, decl := range file.Decls {
					fn, ok := decl.(*ast.FuncDecl)
					if !ok || fn.Recv == nil || fn.Name.Name != "ID" || len(fn.Recv.List) != 1 {
						continue
					}
					if recv, ok := fn.Recv.List[0].Type.(*ast.Ident); ok {
						names = append(names, pkg+"."+recv.Name)
					}
				}
			}
		}
	}
	sort.Strings(names)
	return names
}

// markerSamples returns a value of the named marker with every field it can
// fill set, or nothing when the marker has no way onto the wire.
func markerSamples(name string) []java.Marker {
	switch name {
	case "java.Markup":
		// One Java class per level.
		var samples []java.Marker
		for level := range markupJavaTypes {
			samples = append(samples, java.Markup{Ident: uuid.New(), Level: level, Message: "message", Detail: "detail"})
		}
		return samples
	case "java.GenericMarker":
		// Names the Java class it stands in for, and travels as that class's data.
		id := uuid.New()
		return []java.Marker{java.GenericMarker{
			Ident:    id,
			JavaType: "org.openrewrite.marker.BuildTool",
			Data:     map[string]any{"id": id.String(), "version": "1"},
		}}
	}
	for typ := range valueTypeMap {
		if typ.Kind() != reflect.Struct || filepath.Base(typ.PkgPath())+"."+typ.Name() != name {
			continue
		}
		sample := reflect.New(typ).Elem()
		for i := 0; i < sample.NumField(); i++ {
			field := sample.Field(i)
			switch field.Interface().(type) {
			case uuid.UUID:
				field.Set(reflect.ValueOf(uuid.New()))
			case string:
				field.SetString(typ.Field(i).Name)
			case bool:
				field.SetBool(true)
			case java.Space:
				field.Set(reflect.ValueOf(java.MakeSpace([]java.Comment{{Multiline: true, Text: "c", Suffix: " "}}, " ")))
			case *java.Literal:
				field.Set(reflect.ValueOf(&java.Literal{ID: uuid.New(), Source: "`tag`"}))
			}
		}
		return []java.Marker{sample.Interface().(java.Marker)}
	}
	return nil
}

// javaSource finds the source of the Java class a marker arrives as.
func javaSource(t *testing.T, javaType string) string {
	t.Helper()
	outer := strings.SplitN(javaType, "$", 2)[0]
	path := strings.ReplaceAll(outer, ".", string(filepath.Separator)) + ".java"
	for _, module := range []string{"rewrite-go", "rewrite-core", "rewrite-java"} {
		source, err := os.ReadFile(filepath.Join("..", "..", "..", module, "src", "main", "java", path))
		if err == nil {
			return string(source)
		}
	}
	require.Failf(t, "no Java class", "%s is not in rewrite-go, rewrite-core or rewrite-java", javaType)
	return ""
}

// A marker with no value type reaches Java as null, and one whose fields the
// codec skips arrives hollow. Every marker the engine can attach therefore has
// to name a Java class that has a codec, and has to come back from the wire
// as it went out.
func TestEveryMarkerCrossesRpc(t *testing.T) {
	for _, name := range declaredMarkers(t) {
		t.Run(name, func(t *testing.T) {
			samples := markerSamples(name)
			require.NotEmptyf(t, samples, "%s has no RPC value type, so Java receives null in its place", name)

			for _, sample := range samples {
				javaType := getValueType(sample)
				require.NotNil(t, javaType)

				received, known := newObjIfKnown(*javaType)
				require.Truef(t, known, "no factory for %s", *javaType)
				assert.IsTypef(t, sample, received, "the factory for %s", *javaType)

				after := roundTripMarkers(t, java.MakeMarkers(uuid.New(), []java.Marker{sample}))
				require.Len(t, after.Entries(), 1)
				assertSameMarker(t, sample, after.Entries()[0])

				if _, generic := sample.(java.GenericMarker); !generic {
					simpleName := (*javaType)[strings.LastIndexAny(*javaType, ".$")+1:]
					assert.Containsf(t, javaSource(t, *javaType), "RpcCodec<"+simpleName+">",
						"%s needs an RpcCodec to receive what the engine sends", *javaType)
				}
			}
		})
	}
}

// assertSameMarker compares field by field, since a Space that crossed the
// wire is equal to the one sent without being identical to it.
func assertSameMarker(t *testing.T, want, got java.Marker) {
	t.Helper()
	require.IsType(t, want, got)
	w, g := reflect.ValueOf(want), reflect.ValueOf(got)
	for i := 0; i < w.NumField(); i++ {
		field := w.Type().Field(i).Name
		switch expected := w.Field(i).Interface().(type) {
		case java.Space:
			actual := g.Field(i).Interface().(java.Space)
			assert.Equalf(t, expected.Whitespace(), actual.Whitespace(), "%s", field)
			require.Lenf(t, actual.Comments(), len(expected.Comments()), "%s", field)
			for c, comment := range expected.Comments() {
				assert.Equalf(t, comment.Text, actual.Comments()[c].Text, "%s", field)
				assert.Equalf(t, comment.Suffix, actual.Comments()[c].Suffix, "%s", field)
			}
		case *java.Literal:
			assert.Equalf(t, expected.Source, g.Field(i).Interface().(*java.Literal).Source, "%s", field)
		default:
			if w.Field(i).Kind() == reflect.Slice {
				assert.Equalf(t, w.Field(i).Len(), g.Field(i).Len(), "%s", field)
			} else {
				assert.Equalf(t, expected, g.Field(i).Interface(), "%s", field)
			}
		}
	}
}

func sentMarkers(markers ...java.Marker) []RpcObjectData {
	var messages []RpcObjectData
	sendQ := NewSendQueue(1000, func(batch []RpcObjectData) {
		messages = append(messages, batch...)
	}, NewReferenceMap())
	SendMarkersCodec(java.MakeMarkers(uuid.New(), markers), sendQ)
	sendQ.Flush()
	return messages
}

// A marker Java has no codec for arrives as the map Jackson wrote, `@c` and
// `@ref` included, and goes back as it came. Java sets both again on the
// marker itself and resolves the refs nested below it against each other.
func TestMarkerWithoutACodecGoesBackAsItArrived(t *testing.T) {
	// a marker is one by where it arrives, not by what its class is called
	for _, javaType := range []string{"org.openrewrite.marker.BuildTool", "org.openrewrite.rpc.RpcMarker", "org.openrewrite.style.NamedStyles"} {
		arrived := map[string]any{
			"@c":   javaType,
			"@ref": float64(1),
			"id":   uuid.New().String(),
			"tool": "example",
			"styles": []any{
				map[string]any{"@c": "org.openrewrite.style.Example", "@ref": float64(2)},
			},
		}
		markersID := uuid.New().String()
		messages := []RpcObjectData{
			{State: Add, Value: markersID},
			{State: Add},
			{State: Change, Value: []any{float64(AddedListItem)}},
			{State: Add, ValueType: &javaType, Value: arrived},
		}
		recvQ := NewReceiveQueue(make(map[int]any), func() []RpcObjectData {
			batch := messages
			messages = nil
			return batch
		})
		received := receiveMarkersCodec(recvQ, java.EmptyMarkers)
		require.Len(t, received.Entries(), 1)
		held, isMarker := received.Entries()[0].(java.GenericMarker)
		require.Truef(t, isMarker, "%s arrived as %T", javaType, received.Entries()[0])

		sent := sentMarkers(held)
		last := sent[len(sent)-1]

		require.NotNil(t, last.ValueType)
		assert.Equal(t, javaType, *last.ValueType)
		assert.Equal(t, arrived, last.Value)
	}
}

type markerOfARecipe struct {
	Ident uuid.UUID
	Note  string
	found int // what the recipe keeps to itself
}

func (m markerOfARecipe) ID() uuid.UUID { return m.Ident }

// A marker type a recipe declares has no Java class, so Java holds it as an
// RpcMarker with its id and what JSON carries of its fields.
func TestMarkerOfARecipeIsSentAsAnRpcMarker(t *testing.T) {
	marker := markerOfARecipe{Ident: uuid.New(), Note: "kept by the recipe", found: 3}

	messages := sentMarkers(marker)
	last := messages[len(messages)-1]

	assert.Equal(t, Add, last.State)
	require.NotNil(t, last.ValueType)
	assert.Equal(t, "org.openrewrite.rpc.RpcMarker", *last.ValueType)
	assert.Equal(t, map[string]any{"id": marker.Ident.String(), "Note": "kept by the recipe"}, last.Value)
}

// Java returns only what it was sent, which is not the marker the recipe
// attached.
func TestMarkerOfARecipeIsKeptWhenJavaReturnsItsStandIn(t *testing.T) {
	marker := markerOfARecipe{Ident: uuid.New(), Note: "kept by the recipe", found: 3}
	held := java.MakeMarkers(uuid.New(), []java.Marker{marker})
	returned := func(before java.Markers) java.Marker {
		var messages []RpcObjectData
		sendQ := NewSendQueue(1000, func(batch []RpcObjectData) {
			messages = append(messages, batch...)
		}, NewReferenceMap())
		SendMarkersCodec(held, sendQ)
		sendQ.Flush()
		delivered := false
		recvQ := NewReceiveQueue(make(map[int]any), func() []RpcObjectData {
			if delivered {
				return nil
			}
			delivered = true
			return messages
		})
		after := receiveMarkersCodec(recvQ, before)
		require.Len(t, after.Entries(), 1)
		return after.Entries()[0]
	}

	assert.Equal(t, marker, returned(held))

	// with the original gone, what Java returned is all there is
	standIn, ok := returned(java.EmptyMarkers).(java.GenericMarker)
	require.True(t, ok)
	assert.Equal(t, marker.Ident, standIn.Ident)
	assert.Equal(t, "org.openrewrite.rpc.RpcMarker", standIn.JavaType)
	assert.Equal(t, "kept by the recipe", standIn.Data["Note"])
}
