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
	"github.com/google/uuid"

	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
)

// sendGoResolutionResult mirrors Java's
// org.openrewrite.golang.marker.GoResolutionResult#rpcSend.
//
// Field order MUST match the Java side exactly, otherwise the cross-
// language round-trip queue desyncs:
//
//  1. id (UUID string)
//  2. modulePath (String)
//  3. goVersion (String, nullable)
//  4. toolchain (String, nullable)
//  5. path (String)
//  6. requires (List<Require>, ref-by-key)
//  7. replaces (List<Replace>, ref-by-key)
//  8. excludes (List<Exclude>, ref-by-key)
//  9. retracts (List<Retract>, ref-by-key)
// 10. resolvedDependencies (List<ResolvedDependency>, ref-by-key)
//
// Each list element invokes its own rpcSend on the Java side; we mirror
// the same field order in the per-element onChange callback.
func sendGoResolutionResult(m tree.GoResolutionResult, q *SendQueue) {
	q.GetAndSend(m, func(x any) any { return x.(tree.GoResolutionResult).Ident.String() }, nil)
	q.GetAndSend(m, func(x any) any { return x.(tree.GoResolutionResult).ModulePath }, nil)
	q.GetAndSend(m, func(x any) any { return emptyAsNil(x.(tree.GoResolutionResult).GoVersion) }, nil)
	q.GetAndSend(m, func(x any) any { return emptyAsNil(x.(tree.GoResolutionResult).Toolchain) }, nil)
	q.GetAndSend(m, func(x any) any { return x.(tree.GoResolutionResult).Path }, nil)

	q.GetAndSendListAsRef(m,
		func(x any) []any { return requireSlice(x.(tree.GoResolutionResult).Requires) },
		func(x any) any {
			r := x.(tree.GoRequire)
			return r.ModulePath + "@" + r.Version
		},
		func(x any) {
			r := x.(tree.GoRequire)
			q.GetAndSend(r, func(y any) any { return y.(tree.GoRequire).ModulePath }, nil)
			q.GetAndSend(r, func(y any) any { return y.(tree.GoRequire).Version }, nil)
			q.GetAndSend(r, func(y any) any { return y.(tree.GoRequire).Indirect }, nil)
		})

	q.GetAndSendListAsRef(m,
		func(x any) []any { return replaceSlice(x.(tree.GoResolutionResult).Replaces) },
		func(x any) any {
			r := x.(tree.GoReplace)
			return r.OldPath + "@" + r.OldVersion + "=>" + r.NewPath + "@" + r.NewVersion
		},
		func(x any) {
			r := x.(tree.GoReplace)
			q.GetAndSend(r, func(y any) any { return y.(tree.GoReplace).OldPath }, nil)
			q.GetAndSend(r, func(y any) any { return emptyAsNil(y.(tree.GoReplace).OldVersion) }, nil)
			q.GetAndSend(r, func(y any) any { return y.(tree.GoReplace).NewPath }, nil)
			q.GetAndSend(r, func(y any) any { return emptyAsNil(y.(tree.GoReplace).NewVersion) }, nil)
		})

	q.GetAndSendListAsRef(m,
		func(x any) []any { return excludeSlice(x.(tree.GoResolutionResult).Excludes) },
		func(x any) any {
			e := x.(tree.GoExclude)
			return e.ModulePath + "@" + e.Version
		},
		func(x any) {
			e := x.(tree.GoExclude)
			q.GetAndSend(e, func(y any) any { return y.(tree.GoExclude).ModulePath }, nil)
			q.GetAndSend(e, func(y any) any { return y.(tree.GoExclude).Version }, nil)
		})

	q.GetAndSendListAsRef(m,
		func(x any) []any { return retractSlice(x.(tree.GoResolutionResult).Retracts) },
		func(x any) any { return x.(tree.GoRetract).VersionRange },
		func(x any) {
			r := x.(tree.GoRetract)
			q.GetAndSend(r, func(y any) any { return y.(tree.GoRetract).VersionRange }, nil)
			q.GetAndSend(r, func(y any) any { return emptyAsNil(y.(tree.GoRetract).Rationale) }, nil)
		})

	q.GetAndSendListAsRef(m,
		func(x any) []any { return resolvedSlice(x.(tree.GoResolutionResult).ResolvedDependencies) },
		func(x any) any {
			d := x.(tree.GoResolvedDependency)
			return d.ModulePath + "@" + d.Version
		},
		func(x any) {
			d := x.(tree.GoResolvedDependency)
			q.GetAndSend(d, func(y any) any { return y.(tree.GoResolvedDependency).ModulePath }, nil)
			q.GetAndSend(d, func(y any) any { return y.(tree.GoResolvedDependency).Version }, nil)
			q.GetAndSend(d, func(y any) any { return emptyAsNil(y.(tree.GoResolvedDependency).ModuleHash) }, nil)
			q.GetAndSend(d, func(y any) any { return emptyAsNil(y.(tree.GoResolvedDependency).GoModHash) }, nil)
		})
}

// receiveGoResolutionResult mirrors Java's
// org.openrewrite.golang.marker.GoResolutionResult#rpcReceive.
func receiveGoResolutionResult(before tree.GoResolutionResult, q *ReceiveQueue) tree.GoResolutionResult {
	idStr := receiveScalar[string](q, before.Ident.String())
	if idStr != "" {
		if parsed, err := uuid.Parse(idStr); err == nil {
			before.Ident = parsed
		}
	}
	before.ModulePath = receiveScalar[string](q, before.ModulePath)
	before.GoVersion = receiveNullableString(q, before.GoVersion)
	before.Toolchain = receiveNullableString(q, before.Toolchain)
	before.Path = receiveScalar[string](q, before.Path)

	before.Requires = recvRequires(q, before.Requires)
	before.Replaces = recvReplaces(q, before.Replaces)
	before.Excludes = recvExcludes(q, before.Excludes)
	before.Retracts = recvRetracts(q, before.Retracts)
	before.ResolvedDependencies = recvResolvedDeps(q, before.ResolvedDependencies)
	return before
}

func recvRequires(q *ReceiveQueue, before []tree.GoRequire) []tree.GoRequire {
	beforeAny := requireSlice(before)
	afterAny := q.ReceiveList(beforeAny, func(v any) any {
		r := v.(tree.GoRequire)
		r.ModulePath = receiveScalar[string](q, r.ModulePath)
		r.Version = receiveScalar[string](q, r.Version)
		r.Indirect = receiveScalar[bool](q, r.Indirect)
		return r
	})
	if afterAny == nil {
		return nil
	}
	out := make([]tree.GoRequire, len(afterAny))
	for i, v := range afterAny {
		out[i] = v.(tree.GoRequire)
	}
	return out
}

func recvReplaces(q *ReceiveQueue, before []tree.GoReplace) []tree.GoReplace {
	beforeAny := replaceSlice(before)
	afterAny := q.ReceiveList(beforeAny, func(v any) any {
		r := v.(tree.GoReplace)
		r.OldPath = receiveScalar[string](q, r.OldPath)
		r.OldVersion = receiveNullableString(q, r.OldVersion)
		r.NewPath = receiveScalar[string](q, r.NewPath)
		r.NewVersion = receiveNullableString(q, r.NewVersion)
		return r
	})
	if afterAny == nil {
		return nil
	}
	out := make([]tree.GoReplace, len(afterAny))
	for i, v := range afterAny {
		out[i] = v.(tree.GoReplace)
	}
	return out
}

func recvExcludes(q *ReceiveQueue, before []tree.GoExclude) []tree.GoExclude {
	beforeAny := excludeSlice(before)
	afterAny := q.ReceiveList(beforeAny, func(v any) any {
		e := v.(tree.GoExclude)
		e.ModulePath = receiveScalar[string](q, e.ModulePath)
		e.Version = receiveScalar[string](q, e.Version)
		return e
	})
	if afterAny == nil {
		return nil
	}
	out := make([]tree.GoExclude, len(afterAny))
	for i, v := range afterAny {
		out[i] = v.(tree.GoExclude)
	}
	return out
}

func recvRetracts(q *ReceiveQueue, before []tree.GoRetract) []tree.GoRetract {
	beforeAny := retractSlice(before)
	afterAny := q.ReceiveList(beforeAny, func(v any) any {
		r := v.(tree.GoRetract)
		r.VersionRange = receiveScalar[string](q, r.VersionRange)
		r.Rationale = receiveNullableString(q, r.Rationale)
		return r
	})
	if afterAny == nil {
		return nil
	}
	out := make([]tree.GoRetract, len(afterAny))
	for i, v := range afterAny {
		out[i] = v.(tree.GoRetract)
	}
	return out
}

func recvResolvedDeps(q *ReceiveQueue, before []tree.GoResolvedDependency) []tree.GoResolvedDependency {
	beforeAny := resolvedSlice(before)
	afterAny := q.ReceiveList(beforeAny, func(v any) any {
		d := v.(tree.GoResolvedDependency)
		d.ModulePath = receiveScalar[string](q, d.ModulePath)
		d.Version = receiveScalar[string](q, d.Version)
		d.ModuleHash = receiveNullableString(q, d.ModuleHash)
		d.GoModHash = receiveNullableString(q, d.GoModHash)
		return d
	})
	if afterAny == nil {
		return nil
	}
	out := make([]tree.GoResolvedDependency, len(afterAny))
	for i, v := range afterAny {
		out[i] = v.(tree.GoResolvedDependency)
	}
	return out
}

func requireSlice(s []tree.GoRequire) []any {
	if s == nil {
		return nil
	}
	out := make([]any, len(s))
	for i, v := range s {
		out[i] = v
	}
	return out
}

func replaceSlice(s []tree.GoReplace) []any {
	if s == nil {
		return nil
	}
	out := make([]any, len(s))
	for i, v := range s {
		out[i] = v
	}
	return out
}

func excludeSlice(s []tree.GoExclude) []any {
	if s == nil {
		return nil
	}
	out := make([]any, len(s))
	for i, v := range s {
		out[i] = v
	}
	return out
}

func retractSlice(s []tree.GoRetract) []any {
	if s == nil {
		return nil
	}
	out := make([]any, len(s))
	for i, v := range s {
		out[i] = v
	}
	return out
}

func resolvedSlice(s []tree.GoResolvedDependency) []any {
	if s == nil {
		return nil
	}
	out := make([]any, len(s))
	for i, v := range s {
		out[i] = v
	}
	return out
}

// receiveNullableString reads a value that may be null on the wire and
// returns "" if so. Mirrors how emptyAsNil is sent on the send side.
func receiveNullableString(q *ReceiveQueue, before string) string {
	v := q.Receive(emptyAsNil(before), nil)
	if v == nil {
		return ""
	}
	return v.(string)
}
