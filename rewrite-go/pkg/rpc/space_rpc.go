/*
 * Copyright 2025 the original author or authors.
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
	"github.com/google/uuid"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree/java"
)

// nilableString converts a *string to an any, returning nil if the pointer is nil.
func nilableString(s *string) any {
	if s == nil {
		return nil
	}
	return *s
}

// emptyAsNil converts an empty Go string to wire-null, mirroring how Java
// serializes a {@code @Nullable String} field whose value is null. Use this
// for marker fields that are typed `string` on the Go side but `@Nullable`
// on the Java side (where the Java-side empty case is null, not "").
func emptyAsNil(s string) any {
	if s == "" {
		return nil
	}
	return s
}

// sendSpace serializes a Space to the send queue.
// Matches JavaSender.visitSpace field order: comments (list), whitespace.
func sendSpace(s java.Space, q *SendQueue) {
	// Comments list: id = text + suffix
	q.GetAndSendList(s,
		func(v any) []any {
			sp := v.(java.Space)
			if sp.Comments == nil {
				return []any{} // must be empty slice, not nil, so Java gets ADD with empty list
			}
			result := make([]any, len(sp.Comments))
			for i, c := range sp.Comments {
				result[i] = c
			}
			return result
		},
		func(v any) any {
			c := v.(java.Comment)
			return c.Text + c.Suffix
		},
		func(v any) {
			c := v.(java.Comment)
			// multiline, text, suffix, markers (matching JavaSender.visitSpace comment fields)
			q.GetAndSend(c, func(x any) any { return x.(java.Comment).Multiline }, nil)
			q.GetAndSend(c, func(x any) any { return x.(java.Comment).Text }, nil)
			q.GetAndSend(c, func(x any) any { return x.(java.Comment).Suffix }, nil)
			// Markers - use codec callback to match Java's Markers codec behavior
			q.GetAndSend(c, func(x any) any { return x.(java.Comment).Markers },
				func(v any) { SendMarkersCodec(v.(java.Markers), q) })
		})
	// Whitespace
	q.GetAndSend(s, func(v any) any { return v.(java.Space).Whitespace }, nil)
}

// receiveSpace deserializes a Space from the receive queue.
func receiveSpace(before java.Space, q *ReceiveQueue) java.Space {
	// Comments list
	commentsAny := make([]any, len(before.Comments))
	for i, c := range before.Comments {
		commentsAny[i] = c
	}
	afterComments := q.ReceiveList(commentsAny, func(v any) any {
		if v == nil {
			return nil
		}
		c := v.(java.Comment)
		c.Multiline = receiveScalar[bool](q, c.Multiline)
		c.Text = receiveScalar[string](q, c.Text)
		c.Suffix = receiveScalar[string](q, c.Suffix)
		// Markers is sent by Java as a codec-wrapped field (state + sub-fields).
		// Use q.Receive to consume the state message first.
		if result := q.Receive(c.Markers, func(v any) any {
			return receiveMarkersCodec(q, v.(java.Markers))
		}); result != nil {
			c.Markers = result.(java.Markers)
		}
		return c
	})

	var comments []java.Comment
	if afterComments != nil {
		comments = make([]java.Comment, len(afterComments))
		for i, v := range afterComments {
			comments[i] = v.(java.Comment)
		}
	}

	whitespace := receiveScalar[string](q, before.Whitespace)
	return java.Space{Comments: comments, Whitespace: whitespace}
}

// SendMarkersCodec serializes Markers fields like Java's Markers.rpcSend codec.
// Sends: ID (uuid string), then marker entries list as ref.
func SendMarkersCodec(m java.Markers, q *SendQueue) {
	// ID
	q.GetAndSend(m, func(v any) any { return v.(java.Markers).ID.String() }, nil)
	// Entries list (as ref) — matches Java's Markers.rpcSend protocol.
	q.GetAndSendListAsRef(m,
		func(v any) []any {
			markers := v.(java.Markers)
			if markers.Entries == nil {
				return []any{}
			}
			result := make([]any, len(markers.Entries))
			for i, e := range markers.Entries {
				result[i] = e
			}
			return result
		},
		func(v any) any {
			return v.(java.Marker).ID().String()
		},
		func(v any) { sendMarkerCodecFields(v, q) })
}

// hasGenericMarkerCodec reports whether sendMarkerCodecFields will dispatch
// sub-field messages for a java.GenericMarker with the given Java FQN.
// Markers not listed here have no RpcCodec on either side and must travel
// inline (as the ADD message's Value) so the receiver does not desync waiting
// for sub-fields that never arrive.
func hasGenericMarkerCodec(javaType string) bool {
	switch javaType {
	case "org.openrewrite.Checksum",
		"org.openrewrite.FileAttributes",
		"org.openrewrite.marker.Markup$Error",
		"org.openrewrite.marker.Markup$Warn",
		"org.openrewrite.marker.Markup$Info",
		"org.openrewrite.marker.Markup$Debug",
		"org.openrewrite.java.marker.OmitBraces",
		"org.openrewrite.java.marker.OmitParentheses",
		"org.openrewrite.java.marker.Semicolon",
		"org.openrewrite.java.marker.NullSafe",
		"org.openrewrite.java.marker.TrailingComma":
		return true
	}
	return false
}

// sendMarkerCodecFields sends the sub-fields for markers that implement RpcCodec on the Java side.
// This must match the field order in each marker's rpcSend method.
func sendMarkerCodecFields(v any, q *SendQueue) {
	switch m := v.(type) {
	case java.ParseExceptionResult:
		// ParseExceptionResult.rpcSend sends: id, parserType, exceptionType, message, treeType
		q.GetAndSend(m, func(x any) any { return x.(java.ParseExceptionResult).Ident.String() }, nil)
		q.GetAndSend(m, func(x any) any { return x.(java.ParseExceptionResult).ParserType }, nil)
		q.GetAndSend(m, func(x any) any { return x.(java.ParseExceptionResult).ExceptionType }, nil)
		q.GetAndSend(m, func(x any) any { return x.(java.ParseExceptionResult).Message }, nil)
		q.GetAndSend(m, func(x any) any { return nilableString(x.(java.ParseExceptionResult).TreeType) }, nil)
	case java.SearchResult:
		// SearchResult.rpcSend sends: id (UUID string), description (nullable string)
		q.GetAndSend(m, func(x any) any { return x.(java.SearchResult).Ident.String() }, nil)
		q.GetAndSend(m, func(x any) any { return x.(java.SearchResult).Description }, nil)
	case golang.GroupedImport:
		// GroupedImport.rpcSend sends: id (UUID string), before whitespace (string)
		q.GetAndSend(m, func(x any) any { return x.(golang.GroupedImport).Ident.String() }, nil)
		q.GetAndSend(m, func(x any) any { return x.(golang.GroupedImport).Before.Whitespace }, nil)
	case golang.ImportBlock:
		// ImportBlock.rpcSend sends: id, closePrevious, before, grouped, groupedBefore
		q.GetAndSend(m, func(x any) any { return x.(golang.ImportBlock).Ident.String() }, nil)
		q.GetAndSend(m, func(x any) any { return x.(golang.ImportBlock).ClosePrevious }, nil)
		q.GetAndSend(m, func(x any) any { return x.(golang.ImportBlock).Before.Whitespace }, nil)
		q.GetAndSend(m, func(x any) any { return x.(golang.ImportBlock).Grouped }, nil)
		q.GetAndSend(m, func(x any) any { return x.(golang.ImportBlock).GroupedBefore.Whitespace }, nil)
	case golang.ShortVarDecl:
		q.GetAndSend(m, func(x any) any { return x.(golang.ShortVarDecl).Ident.String() }, nil)
	case golang.VarKeyword:
		q.GetAndSend(m, func(x any) any { return x.(golang.VarKeyword).Ident.String() }, nil)
	case golang.ConstDecl:
		q.GetAndSend(m, func(x any) any { return x.(golang.ConstDecl).Ident.String() }, nil)
	case golang.GroupedSpec:
		q.GetAndSend(m, func(x any) any { return x.(golang.GroupedSpec).Ident.String() }, nil)
	case golang.InterfaceMethod:
		q.GetAndSend(m, func(x any) any { return x.(golang.InterfaceMethod).Ident.String() }, nil)
	case golang.SelectStmt:
		q.GetAndSend(m, func(x any) any { return x.(golang.SelectStmt).Ident.String() }, nil)
	case golang.TypeSwitchGuard:
		q.GetAndSend(m, func(x any) any { return x.(golang.TypeSwitchGuard).Ident.String() }, nil)
	case golang.StructTag:
		// StructTag.rpcSend sends: id (UUID string), tag valueSource (string)
		q.GetAndSend(m, func(x any) any { return x.(golang.StructTag).Ident.String() }, nil)
		q.GetAndSend(m, func(x any) any { return x.(golang.StructTag).Tag.Source }, nil)
	case golang.TrailingComma:
		// TrailingComma.rpcSend sends: id (UUID string), before whitespace, after whitespace
		q.GetAndSend(m, func(x any) any { return x.(golang.TrailingComma).Ident.String() }, nil)
		q.GetAndSend(m, func(x any) any { return x.(golang.TrailingComma).Before.Whitespace }, nil)
		q.GetAndSend(m, func(x any) any { return x.(golang.TrailingComma).After.Whitespace }, nil)
	case golang.Semicolon:
		// Semicolon.rpcSend sends: id (UUID string)
		q.GetAndSend(m, func(x any) any { return x.(golang.Semicolon).Ident.String() }, nil)
	case golang.GoProject:
		// GoProject.rpcSend sends: id (UUID string), projectName (string)
		q.GetAndSend(m, func(x any) any { return x.(golang.GoProject).Ident.String() }, nil)
		q.GetAndSend(m, func(x any) any { return x.(golang.GoProject).ProjectName }, nil)
	case golang.GoResolutionResult:
		// Field order mirrors Java's GoResolutionResult#rpcSend exactly;
		// see go_resolution_result_codec.go for the per-field commentary.
		sendGoResolutionResult(m, q)
	case java.GenericMarker:
		// Send codec sub-fields matching what Java expects
		d := m.Data
		switch m.JavaType {
		case "org.openrewrite.Checksum":
			q.GetAndSend(m, func(_ any) any {
				if d != nil {
					return d["algorithm"]
				}
				return ""
			}, nil)
			q.GetAndSend(m, func(_ any) any {
				if d != nil {
					return d["value"]
				}
				return nil
			}, nil)
		case "org.openrewrite.FileAttributes":
			for _, key := range []string{"creationTime", "lastModifiedTime", "lastAccessTime", "isReadable", "isWritable", "isExecutable", "size"} {
				k := key
				q.GetAndSend(m, func(_ any) any {
					if d != nil {
						return d[k]
					}
					return nil
				}, nil)
			}
		case "org.openrewrite.marker.Markup$Error",
			"org.openrewrite.marker.Markup$Warn",
			"org.openrewrite.marker.Markup$Info",
			"org.openrewrite.marker.Markup$Debug":
			// Markup inner classes implement RpcCodec: id, message, detail
			q.GetAndSend(m, func(_ any) any {
				if d != nil {
					return d["id"]
				}
				return ""
			}, nil)
			q.GetAndSend(m, func(_ any) any {
				if d != nil {
					return d["message"]
				}
				return ""
			}, nil)
			q.GetAndSend(m, func(_ any) any {
				if d != nil {
					return d["detail"]
				}
				return nil
			}, nil)
		case "org.openrewrite.java.marker.OmitBraces",
			"org.openrewrite.java.marker.OmitParentheses",
			"org.openrewrite.java.marker.Semicolon":
			q.GetAndSend(m, func(_ any) any {
				if d != nil {
					return d["id"]
				}
				return ""
			}, nil)
		case "org.openrewrite.java.marker.NullSafe",
			"org.openrewrite.java.marker.TrailingComma":
			q.GetAndSend(m, func(_ any) any {
				if d != nil {
					return d["id"]
				}
				return ""
			}, nil)
			q.GetAndSend(m, func(_ any) any { return nil }, nil) // Space
		default:
			// Unknown marker type — no sub-fields to send
		}
	default:
		// Non-Go, non-Generic markers have no sub-fields
	}
}

// receiveMarkersCodec deserializes Markers fields like Java's Markers.rpcReceive codec.
func receiveMarkersCodec(q *ReceiveQueue, before java.Markers) java.Markers {
	// ID
	idStr := receiveScalar[string](q, before.ID.String())
	id := before.ID
	if idStr != "" && idStr != before.ID.String() {
		if parsed, err := uuid.Parse(idStr); err == nil {
			id = parsed
		}
	}
	// Entries list
	var beforeAny []any
	if before.Entries != nil {
		beforeAny = make([]any, len(before.Entries))
		for i, e := range before.Entries {
			beforeAny[i] = e
		}
	}
	afterAny := q.ReceiveList(beforeAny, func(v any) any {
		// Markers that implement RpcCodec on the Java side send sub-fields.
		// We dispatch based on the concrete Go type created by the factory.
		switch m := v.(type) {
		case java.ParseExceptionResult:
			// ParseExceptionResult.rpcReceive: id, parserType, exceptionType, message, treeType
			idStr := receiveScalar[string](q, m.Ident.String())
			if idStr != "" {
				if parsed, err := uuid.Parse(idStr); err == nil {
					m.Ident = parsed
				}
			}
			m.ParserType = receiveScalar[string](q, m.ParserType)
			m.ExceptionType = receiveScalar[string](q, m.ExceptionType)
			m.Message = receiveScalar[string](q, m.Message)
			treeTypeVal := q.Receive(nilableString(m.TreeType), nil)
			if treeTypeVal != nil {
				s := treeTypeVal.(string)
				m.TreeType = &s
			} else {
				m.TreeType = nil
			}
			return m
		case java.SearchResult:
			// SearchResult.rpcSend sends: id (UUID string), description (nullable string)
			idStr := receiveScalar[string](q, m.Ident.String())
			if idStr != "" {
				if parsed, err := uuid.Parse(idStr); err == nil {
					m.Ident = parsed
				}
			}
			desc := q.Receive(m.Description, nil)
			if desc != nil {
				m.Description = desc.(string)
			}
			return m
		case golang.GroupedImport:
			// GroupedImport.rpcSend sends: id (UUID string), before whitespace (string)
			idStr := receiveScalar[string](q, m.Ident.String())
			if idStr != "" {
				if parsed, err := uuid.Parse(idStr); err == nil {
					m.Ident = parsed
				}
			}
			ws := receiveScalar[string](q, m.Before.Whitespace)
			m.Before = java.Space{Whitespace: ws}
			return m
		case golang.ImportBlock:
			// ImportBlock.rpcReceive: id, closePrevious, before, grouped, groupedBefore
			idStr := receiveScalar[string](q, m.Ident.String())
			if idStr != "" {
				if parsed, err := uuid.Parse(idStr); err == nil {
					m.Ident = parsed
				}
			}
			m.ClosePrevious = receiveScalar[bool](q, m.ClosePrevious)
			ws := receiveScalar[string](q, m.Before.Whitespace)
			m.Before = java.Space{Whitespace: ws}
			m.Grouped = receiveScalar[bool](q, m.Grouped)
			gbWs := receiveScalar[string](q, m.GroupedBefore.Whitespace)
			m.GroupedBefore = java.Space{Whitespace: gbWs}
			return m
		case golang.ShortVarDecl:
			idStr := receiveScalar[string](q, m.Ident.String())
			if idStr != "" {
				if parsed, err := uuid.Parse(idStr); err == nil {
					m.Ident = parsed
				}
			}
			return m
		case golang.VarKeyword:
			idStr := receiveScalar[string](q, m.Ident.String())
			if idStr != "" {
				if parsed, err := uuid.Parse(idStr); err == nil {
					m.Ident = parsed
				}
			}
			return m
		case golang.ConstDecl:
			idStr := receiveScalar[string](q, m.Ident.String())
			if idStr != "" {
				if parsed, err := uuid.Parse(idStr); err == nil {
					m.Ident = parsed
				}
			}
			return m
		case golang.GroupedSpec:
			idStr := receiveScalar[string](q, m.Ident.String())
			if idStr != "" {
				if parsed, err := uuid.Parse(idStr); err == nil {
					m.Ident = parsed
				}
			}
			return m
		case golang.InterfaceMethod:
			idStr := receiveScalar[string](q, m.Ident.String())
			if idStr != "" {
				if parsed, err := uuid.Parse(idStr); err == nil {
					m.Ident = parsed
				}
			}
			return m
		case golang.SelectStmt:
			idStr := receiveScalar[string](q, m.Ident.String())
			if idStr != "" {
				if parsed, err := uuid.Parse(idStr); err == nil {
					m.Ident = parsed
				}
			}
			return m
		case golang.TypeSwitchGuard:
			idStr := receiveScalar[string](q, m.Ident.String())
			if idStr != "" {
				if parsed, err := uuid.Parse(idStr); err == nil {
					m.Ident = parsed
				}
			}
			return m
		case golang.StructTag:
			idStr := receiveScalar[string](q, m.Ident.String())
			if idStr != "" {
				if parsed, err := uuid.Parse(idStr); err == nil {
					m.Ident = parsed
				}
			}
			// Java now sends just the valueSource string instead of the full Literal.
			// Receive it and reconstruct the Literal with the updated source.
			var beforeSource string
			if m.Tag != nil {
				beforeSource = m.Tag.Source
			}
			valueSource := receiveScalar[string](q, beforeSource)
			if m.Tag != nil {
				tag := *m.Tag
				tag.Source = valueSource
				m.Tag = &tag
			} else {
				m.Tag = &java.Literal{
					ID:     uuid.New(),
					Source: valueSource,
				}
			}
			return m
		case golang.TrailingComma:
			idStr := receiveScalar[string](q, m.Ident.String())
			if idStr != "" {
				if parsed, err := uuid.Parse(idStr); err == nil {
					m.Ident = parsed
				}
			}
			beforeWs := receiveScalar[string](q, m.Before.Whitespace)
			m.Before = java.Space{Whitespace: beforeWs}
			afterWs := receiveScalar[string](q, m.After.Whitespace)
			m.After = java.Space{Whitespace: afterWs}
			return m
		case golang.Semicolon:
			idStr := receiveScalar[string](q, m.Ident.String())
			if idStr != "" {
				if parsed, err := uuid.Parse(idStr); err == nil {
					m.Ident = parsed
				}
			}
			return m
		case golang.GoProject:
			idStr := receiveScalar[string](q, m.Ident.String())
			if idStr != "" {
				if parsed, err := uuid.Parse(idStr); err == nil {
					m.Ident = parsed
				}
			}
			m.ProjectName = receiveScalar[string](q, m.ProjectName)
			return m
		case golang.GoResolutionResult:
			return receiveGoResolutionResult(m, q)
		case java.GenericMarker:
			// Read codec sub-fields based on the original Java marker type.
			// Each Java marker's rpcSend sends specific sub-fields that we must consume.
			switch m.JavaType {
			case "org.openrewrite.Checksum":
				m.Data = map[string]any{
					"algorithm": receiveScalar[string](q, ""),
					"value":     q.Receive(nil, nil),
				}
			case "org.openrewrite.FileAttributes":
				m.Data = map[string]any{}
				for _, key := range []string{"creationTime", "lastModifiedTime", "lastAccessTime", "isReadable", "isWritable", "isExecutable", "size"} {
					m.Data[key] = q.Receive(nil, nil)
				}
			case "org.openrewrite.marker.Markup$Error",
				"org.openrewrite.marker.Markup$Warn",
				"org.openrewrite.marker.Markup$Info",
				"org.openrewrite.marker.Markup$Debug":
				// Markup inner classes implement RpcCodec: id, message, detail
				m.Data = map[string]any{
					"id":      receiveScalar[string](q, ""),
					"message": receiveScalar[string](q, ""),
					"detail":  q.Receive(nil, nil),
				}
			case "org.openrewrite.java.marker.OmitBraces",
				"org.openrewrite.java.marker.OmitParentheses",
				"org.openrewrite.java.marker.Semicolon":
				// These markers send only id
				m.Data = map[string]any{
					"id": receiveScalar[string](q, ""),
				}
			case "org.openrewrite.java.marker.NullSafe",
				"org.openrewrite.java.marker.TrailingComma":
				// These send id + a Space
				m.Data = map[string]any{
					"id": receiveScalar[string](q, ""),
				}
				q.Receive(nil, func(v any) any { return receiveSpace(v.(java.Space), q) })
			default:
				// Unknown marker type — no sub-fields to read.
				// If this causes queue misalignment, the marker type needs
				// to be added to the cases above.
			}
			return m
		default:
			return v
		}
	})
	var entries []java.Marker
	for _, v := range afterAny {
		// Skip a nil/non-Marker entry — an RPC NO_CHANGE delta resolved against
		// a diverged baseline — instead of a panicking type assertion.
		if m, ok := v.(java.Marker); ok {
			entries = append(entries, m)
		}
	}
	return java.Markers{ID: id, Entries: entries}
}
