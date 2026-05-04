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

package recipe

import (
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"os"
	"path/filepath"
	"reflect"
	"regexp"
	"strings"
	"sync"
)

// DataTableStoreKey is the ExecutionContext message key under which a
// DataTableStore is installed. Mirrors JS DATA_TABLE_STORE.
const DataTableStoreKey = "org.openrewrite.dataTables.store"

// DataTableLike is the type-erased view of a DataTable that DataTableStore
// implementations work against. Generic DataTable[Row] satisfies this.
type DataTableLike interface {
	Descriptor() DataTableDescriptor
	InstanceName() string
	Group() string
}

// DataTableStore is where rows emitted by a recipe end up. The default
// in-memory store is created lazily on first InsertRow when no store has
// been installed in the ExecutionContext.
type DataTableStore interface {
	InsertRow(dt DataTableLike, ctx *ExecutionContext, row any)
	GetRows(dataTableName, group string) []any
	GetDataTables() []DataTableLike
}

// DataTable is a typed, strongly-bound handle a recipe holds onto and writes
// rows into. Row is the row type; recipes typically declare:
//
//	var Findings = recipe.NewDataTable[FindingsRow](
//	    "org.example.MyRecipe.Findings",
//	    "My findings", "What MyRecipe found",
//	    []recipe.ColumnDescriptor{...})
type DataTable[Row any] struct {
	descriptor   DataTableDescriptor
	instanceName string
	group        string
}

// NewDataTable creates a DataTable handle. The instance name defaults to
// the display name; recipes may override it via SetInstanceName /
// SetGroup if multiple buckets within one recipe run are needed.
func NewDataTable[Row any](name, displayName, description string, columns []ColumnDescriptor) *DataTable[Row] {
	return &DataTable[Row]{
		descriptor: DataTableDescriptor{
			Name:        name,
			DisplayName: displayName,
			Description: description,
			Columns:     columns,
		},
		instanceName: displayName,
	}
}

func (dt *DataTable[Row]) Descriptor() DataTableDescriptor { return dt.descriptor }
func (dt *DataTable[Row]) InstanceName() string            { return dt.instanceName }
func (dt *DataTable[Row]) Group() string                   { return dt.group }
func (dt *DataTable[Row]) SetInstanceName(name string)     { dt.instanceName = name }
func (dt *DataTable[Row]) SetGroup(group string)           { dt.group = group }

// InsertRow appends a row to the data table. If no DataTableStore has been
// installed in ctx, an InMemoryDataTableStore is created lazily.
func (dt *DataTable[Row]) InsertRow(ctx *ExecutionContext, row Row) {
	store, ok := ctx.GetMessage(DataTableStoreKey)
	if !ok {
		store = NewInMemoryDataTableStore()
		ctx.PutMessage(DataTableStoreKey, store)
	}
	if s, ok := store.(DataTableStore); ok {
		s.InsertRow(dt, ctx, row)
	}
}

// --- InMemoryDataTableStore ---

type bucket struct {
	dt   DataTableLike
	rows []any
}

// InMemoryDataTableStore holds rows in memory keyed by (dataTableName, group).
// Rows can be read back via GetRows. Default for tests and recipes that
// don't need disk-backed output.
type InMemoryDataTableStore struct {
	mu      sync.Mutex
	buckets map[string]*bucket
}

func NewInMemoryDataTableStore() *InMemoryDataTableStore {
	return &InMemoryDataTableStore{buckets: map[string]*bucket{}}
}

func bucketKey(dt DataTableLike) string {
	suffix := dt.Group()
	if suffix == "" {
		suffix = dt.InstanceName()
	}
	return dt.Descriptor().Name + "\x00" + suffix
}

func (s *InMemoryDataTableStore) InsertRow(dt DataTableLike, _ *ExecutionContext, row any) {
	s.mu.Lock()
	defer s.mu.Unlock()
	key := bucketKey(dt)
	b, ok := s.buckets[key]
	if !ok {
		b = &bucket{dt: dt}
		s.buckets[key] = b
	}
	b.rows = append(b.rows, row)
}

func (s *InMemoryDataTableStore) GetRows(dataTableName, group string) []any {
	s.mu.Lock()
	defer s.mu.Unlock()
	if group != "" {
		if b, ok := s.buckets[dataTableName+"\x00"+group]; ok {
			return append([]any{}, b.rows...)
		}
		return nil
	}
	for _, b := range s.buckets {
		if b.dt.Descriptor().Name == dataTableName && b.dt.Group() == "" {
			return append([]any{}, b.rows...)
		}
	}
	return nil
}

func (s *InMemoryDataTableStore) GetDataTables() []DataTableLike {
	s.mu.Lock()
	defer s.mu.Unlock()
	out := make([]DataTableLike, 0, len(s.buckets))
	for _, b := range s.buckets {
		out = append(out, b.dt)
	}
	return out
}

// --- CsvDataTableStore ---

var nonAlnum = regexp.MustCompile(`[^a-z0-9]`)
var dashRun = regexp.MustCompile(`-+`)
var leadingTrailingDash = regexp.MustCompile(`^-|-$`)

// SanitizeScope produces a filesystem-safe identifier from a scope string,
// matching JS data-table.ts:85-103. Lowercase, non-alnum→dash, collapse
// dashes, truncate to 30 at a word boundary, suffix with a 4-char sha256.
func SanitizeScope(scope string) string {
	s := strings.ToLower(scope)
	s = nonAlnum.ReplaceAllString(s, "-")
	s = dashRun.ReplaceAllString(s, "-")
	s = leadingTrailingDash.ReplaceAllString(s, "")
	if len(s) > 30 {
		s = s[:30]
		if i := strings.LastIndex(s, "-"); i > 0 {
			s = s[:i]
		}
	}
	sum := sha256.Sum256([]byte(scope))
	return s + "-" + hex.EncodeToString(sum[:2])
}

// CsvDataTableStore writes rows directly to CSV files as they are inserted.
// One file per (recipe, dataTable, group) tuple, opened append-mode and
// kept open for the store's lifetime.
type CsvDataTableStore struct {
	outputDir string
	mu        sync.Mutex
	files     map[string]*os.File
	dataTables map[string]DataTableLike
}

func NewCsvDataTableStore(outputDir string) (*CsvDataTableStore, error) {
	if err := os.MkdirAll(outputDir, 0755); err != nil {
		return nil, fmt.Errorf("create dataTables output dir: %w", err)
	}
	return &CsvDataTableStore{
		outputDir:  outputDir,
		files:      map[string]*os.File{},
		dataTables: map[string]DataTableLike{},
	}, nil
}

// Close flushes and closes all CSV files. Call on server Reset / shutdown.
func (s *CsvDataTableStore) Close() {
	s.mu.Lock()
	defer s.mu.Unlock()
	for _, f := range s.files {
		_ = f.Close()
	}
	s.files = map[string]*os.File{}
}

func fileKey(dt DataTableLike) string {
	scope := dt.InstanceName()
	if dt.Group() != "" {
		scope = scope + "-" + dt.Group()
	}
	return SanitizeScope(dt.Descriptor().Name + "-" + scope)
}

func (s *CsvDataTableStore) InsertRow(dt DataTableLike, _ *ExecutionContext, row any) {
	s.mu.Lock()
	defer s.mu.Unlock()

	key := fileKey(dt)
	f, ok := s.files[key]
	if !ok {
		csvPath := filepath.Join(s.outputDir, key+".csv")
		newFile, err := os.OpenFile(csvPath, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0644)
		if err != nil {
			return // best-effort; fail silently rather than crash a recipe
		}
		// Header preamble + column header row, written once when the file is created.
		desc := dt.Descriptor()
		preamble := fmt.Sprintf("# @name %s\n# @instanceName %s\n# @group %s\n",
			desc.Name, dt.InstanceName(), dt.Group())
		header := make([]string, 0, len(desc.Columns))
		for _, c := range desc.Columns {
			header = append(header, csvEscape(c.DisplayName))
		}
		_, _ = newFile.WriteString(preamble + strings.Join(header, ",") + "\n")
		f = newFile
		s.files[key] = f
		s.dataTables[key] = dt
	}

	// Reflect the row by column name. Rows are typically structs with field
	// names matching column names (case-insensitive).
	desc := dt.Descriptor()
	values := make([]string, 0, len(desc.Columns))
	rv := reflect.ValueOf(row)
	if rv.Kind() == reflect.Ptr {
		rv = rv.Elem()
	}
	for _, col := range desc.Columns {
		var v any
		if rv.Kind() == reflect.Struct {
			fv := rv.FieldByNameFunc(func(name string) bool {
				return strings.EqualFold(name, col.Name)
			})
			if fv.IsValid() {
				v = fv.Interface()
			}
		} else if rv.Kind() == reflect.Map {
			mv := rv.MapIndex(reflect.ValueOf(col.Name))
			if mv.IsValid() {
				v = mv.Interface()
			}
		}
		values = append(values, csvEscape(v))
	}
	_, _ = f.WriteString(strings.Join(values, ",") + "\n")
}

func (s *CsvDataTableStore) GetRows(_, _ string) []any {
	// CSV store is write-only; reads happen by parsing the CSV files
	// from outside the process (the saas mounts the dir).
	return nil
}

func (s *CsvDataTableStore) GetDataTables() []DataTableLike {
	s.mu.Lock()
	defer s.mu.Unlock()
	out := make([]DataTableLike, 0, len(s.dataTables))
	for _, dt := range s.dataTables {
		out = append(out, dt)
	}
	return out
}

// csvEscape formats a value following RFC 4180.
func csvEscape(v any) string {
	if v == nil {
		return `""`
	}
	s := fmt.Sprintf("%v", v)
	if strings.ContainsAny(s, ",\"\n\r") {
		return `"` + strings.ReplaceAll(s, `"`, `""`) + `"`
	}
	return s
}
