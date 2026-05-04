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

// Package main implements a JSON-RPC 2.0 server for OpenRewrite Go language support.
// It communicates over stdin/stdout using Content-Length framed messages (LSP protocol).
package main

import (
	"bufio"
	"encoding/csv"
	"encoding/json"
	"flag"
	"fmt"
	"io"
	"log"
	"os"
	"path/filepath"
	"reflect"
	"runtime"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/google/uuid"
	"github.com/grafana/pyroscope-go"

	goparser "github.com/openrewrite/rewrite/rewrite-go/pkg/parser"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/printer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/golang"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/recipe/installer"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/rpc"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/tree"
	"github.com/openrewrite/rewrite/rewrite-go/pkg/visitor"
)

// jsonRPCRequest represents an incoming JSON-RPC 2.0 message (request or response).
type jsonRPCRequest struct {
	JSONRPC string          `json:"jsonrpc"`
	ID      json.RawMessage `json:"id"`
	Method  string          `json:"method"`
	Params  json.RawMessage `json:"params"`
	Result  json.RawMessage `json:"result"` // present in responses
}

// jsonRPCResponse represents an outgoing JSON-RPC 2.0 response.
type jsonRPCResponse struct {
	JSONRPC string          `json:"jsonrpc"`
	ID      json.RawMessage `json:"id"`
	Result  any             `json:"result,omitempty"`
	Error   *rpcError       `json:"error,omitempty"`
}

// rpcError is the error object in a JSON-RPC response.
type rpcError struct {
	Code    int    `json:"code"`
	Message string `json:"message"`
	Data    string `json:"data,omitempty"`
}

// server holds the RPC state.
type server struct {
	localObjects  map[string]any
	remoteObjects map[string]any // forward direction: tracks what Java has from Go
	localRefs     map[uintptr]int
	remoteRefs    map[int]any
	batchSize     int

	// Separate state for reverse GetObject (Java→Go) to avoid conflating
	// with forward direction state
	reverseRemoteObjects map[string]any
	reverseRemoteRefs    map[int]any

	// Prepared recipe instances keyed by unique ID
	preparedRecipes map[string]recipe.Recipe

	// Per-prepared-recipe accumulator for ScanningRecipe. Lazily created on
	// the first scan Visit call. Lifetime = prepared recipe instance; freed
	// only on Reset (per the engineering review's D2 decision).
	preparedAccumulators map[string]any

	// ExecutionContext fetched from Java on first visit, cached for the
	// lifetime of the prepared recipe. Keyed by the ExecutionContext's
	// remote object id (the `p` value in Visit/Generate/BatchVisit).
	preparedContexts map[string]*recipe.ExecutionContext

	// Tracing toggles for GetObject
	traceReceive bool
	traceSend    bool

	// Server configuration from CLI flags (see serverConfig)
	metricsCsv        string
	dataTablesCsvDir  string

	// Per-RPC metrics writer. Lazily opened in newServer when metricsCsv
	// is set. Writes are guarded by metricsMu so concurrent dispatch
	// (e.g. parallel BatchVisit handlers) can't interleave rows.
	metricsFile   *os.File
	metricsWriter *csv.Writer
	metricsMu     sync.Mutex

	reader    *bufio.Reader
	writer    io.Writer
	logger    *log.Logger
	registry  *recipe.Registry
	installer *installer.Installer
}

// serverConfig holds CLI-driven configuration applied to the server at startup.
type serverConfig struct {
	logFile           string
	traceRpcMessages  bool
	metricsCsv        string
	recipeInstallDir  string
	dataTablesCsvDir  string
}

func newServer(cfg serverConfig) *server {
	var logOut io.Writer
	if cfg.logFile != "" {
		f, err := os.OpenFile(cfg.logFile, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0644)
		if err != nil {
			logOut = os.Stderr
		} else {
			logOut = f
		}
	} else {
		f, err := os.CreateTemp("", "go-rpc-*.log")
		if err != nil {
			logOut = os.Stderr
		} else {
			logOut = f
		}
	}

	// Register the empty-body codec for ExecutionContext under the FQN
	// the Java side uses. Matches JS execution.ts:25-35: rpcSend writes
	// nothing, rpcReceive returns a fresh ctx.
	rpc.RegisterFactory("org.openrewrite.InMemoryExecutionContext", func() any {
		return recipe.NewExecutionContext()
	})

	reg := recipe.NewRegistry()
	reg.Activate(golang.Activate)

	logger := log.New(logOut, "", log.LstdFlags)

	inst := installer.NewInstaller()
	inst.Logger = logger.Printf
	if cfg.recipeInstallDir != "" {
		inst.WorkspaceDir = cfg.recipeInstallDir
	}

	s := &server{
		localObjects:         make(map[string]any),
		remoteObjects:        make(map[string]any),
		localRefs:            make(map[uintptr]int),
		remoteRefs:           make(map[int]any),
		reverseRemoteObjects: make(map[string]any),
		reverseRemoteRefs:    make(map[int]any),
		preparedRecipes:      make(map[string]recipe.Recipe),
		preparedAccumulators: make(map[string]any),
		preparedContexts:     make(map[string]*recipe.ExecutionContext),
		batchSize:            1000,
		traceReceive:         cfg.traceRpcMessages,
		traceSend:            cfg.traceRpcMessages,
		metricsCsv:           cfg.metricsCsv,
		dataTablesCsvDir:     cfg.dataTablesCsvDir,
		reader:    bufio.NewReader(os.Stdin),
		writer:    os.Stdout,
		logger:    logger,
		registry:  reg,
		installer: inst,
	}

	if cfg.metricsCsv != "" {
		f, err := os.OpenFile(cfg.metricsCsv, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, 0644)
		if err != nil {
			logger.Printf("metrics-csv: cannot open %q: %v — metrics disabled", cfg.metricsCsv, err)
		} else {
			s.metricsFile = f
			s.metricsWriter = csv.NewWriter(f)
			if err := s.metricsWriter.Write([]string{"timestamp", "method", "duration_ms", "error"}); err != nil {
				logger.Printf("metrics-csv: cannot write header: %v", err)
			}
			s.metricsWriter.Flush()
		}
	}

	return s
}

// closeMetrics flushes and closes the metrics CSV writer if open. Idempotent.
func (s *server) closeMetrics() {
	s.metricsMu.Lock()
	defer s.metricsMu.Unlock()
	if s.metricsWriter != nil {
		s.metricsWriter.Flush()
		s.metricsWriter = nil
	}
	if s.metricsFile != nil {
		if err := s.metricsFile.Close(); err != nil {
			s.logger.Printf("metrics-csv: close failed: %v", err)
		}
		s.metricsFile = nil
	}
}

// recordMetric appends one row to the metrics CSV. Safe to call from
// concurrent goroutines; rows are written under metricsMu so they don't
// interleave. Errors are logged and dropped — metrics emission must never
// take down a request.
func (s *server) recordMetric(method string, duration time.Duration, rpcErr *rpcError) {
	s.metricsMu.Lock()
	defer s.metricsMu.Unlock()
	if s.metricsWriter == nil {
		return
	}
	errMsg := ""
	if rpcErr != nil {
		errMsg = rpcErr.Message
	}
	row := []string{
		time.Now().UTC().Format(time.RFC3339Nano),
		method,
		strconv.FormatInt(duration.Milliseconds(), 10),
		errMsg,
	}
	if err := s.metricsWriter.Write(row); err != nil {
		s.logger.Printf("metrics-csv: write row failed: %v", err)
		return
	}
	s.metricsWriter.Flush()
}

func parseFlags() serverConfig {
	var cfg serverConfig
	flag.StringVar(&cfg.logFile, "log-file", "", "path to write server log; empty = OS temp file")
	flag.BoolVar(&cfg.traceRpcMessages, "trace-rpc-messages", false, "log every GetObject batch send/receive")
	flag.StringVar(&cfg.metricsCsv, "metrics-csv", "", "path to write per-RPC metrics as CSV")
	flag.StringVar(&cfg.recipeInstallDir, "recipe-install-dir", "", "directory used as the installer workspace; defaults to ~/.rewrite/go-recipes")
	flag.StringVar(&cfg.dataTablesCsvDir, "data-tables-csv-dir", "", "directory where DataTable rows are written as CSV; empty = in-memory only")
	flag.Parse()
	return cfg
}

// initPyroscope starts continuous profiling when PYROSCOPE_SERVER_ADDRESS is
// set. Tags inherited via PYROSCOPE_TAGS (k=v,k=v) are forwarded verbatim; a
// runtime=go tag is added so flame graphs in the shared modcli application
// can be sliced by which RPC subprocess produced them.
func initPyroscope() {
	server := os.Getenv("PYROSCOPE_SERVER_ADDRESS")
	if server == "" {
		return
	}
	appName := os.Getenv("PYROSCOPE_APPLICATION_NAME")
	if appName == "" {
		appName = "modcli"
	}
	tags := map[string]string{"runtime": "go"}
	for _, pair := range strings.Split(os.Getenv("PYROSCOPE_TAGS"), ",") {
		if i := strings.Index(pair, "="); i > 0 {
			tags[strings.TrimSpace(pair[:i])] = strings.TrimSpace(pair[i+1:])
		}
	}
	_, _ = pyroscope.Start(pyroscope.Config{
		ApplicationName: appName,
		ServerAddress:   server,
		Tags:            tags,
	})
}

func main() {
	initPyroscope()
	cfg := parseFlags()
	s := newServer(cfg)
	s.logger.Println("Go RPC server starting...")

	for {
		req, err := s.readMessage()
		if err != nil {
			if err == io.EOF {
				break
			}
			s.logger.Printf("Error reading message: %v", err)
			break
		}

		resp := s.safeHandleRequest(req)
		if err := s.writeMessage(resp); err != nil {
			s.logger.Printf("Error writing response: %v", err)
			break
		}
	}

	s.logger.Println("Go RPC server shutting down...")
	s.closeMetrics()
}

// readMessage reads a Content-Length framed JSON-RPC message from stdin.
func (s *server) readMessage() (*jsonRPCRequest, error) {
	// Read Content-Length header
	headerLine, err := s.reader.ReadString('\n')
	if err != nil {
		return nil, err
	}
	headerLine = strings.TrimSpace(headerLine)
	if !strings.HasPrefix(headerLine, "Content-Length:") {
		return nil, fmt.Errorf("invalid header: %s", headerLine)
	}
	lengthStr := strings.TrimSpace(strings.TrimPrefix(headerLine, "Content-Length:"))
	contentLength, err := strconv.Atoi(lengthStr)
	if err != nil {
		return nil, fmt.Errorf("invalid content length: %s", lengthStr)
	}

	// Read empty separator line
	if _, err := s.reader.ReadString('\n'); err != nil {
		return nil, err
	}

	// Read content body
	body := make([]byte, contentLength)
	if _, err := io.ReadFull(s.reader, body); err != nil {
		return nil, err
	}

	var req jsonRPCRequest
	if err := json.Unmarshal(body, &req); err != nil {
		return nil, fmt.Errorf("invalid JSON: %w", err)
	}
	return &req, nil
}

// writeMessage writes a Content-Length framed JSON-RPC message to stdout.
func (s *server) writeMessage(resp *jsonRPCResponse) error {
	body, err := json.Marshal(resp)
	if err != nil {
		return err
	}
	header := fmt.Sprintf("Content-Length: %d\r\n\r\n", len(body))
	_, err = s.writer.Write(append([]byte(header), body...))
	return err
}

// safeHandleRequest wraps handleRequest with panic recovery and per-RPC
// metrics capture. The metric row is written exactly once per request,
// after the response is determined (panic-recovered or not).
func (s *server) safeHandleRequest(req *jsonRPCRequest) (resp *jsonRPCResponse) {
	start := time.Now()
	defer func() {
		if r := recover(); r != nil {
			buf := make([]byte, 4096)
			n := runtime.Stack(buf, false)
			s.logger.Printf("PANIC in %s: %v\n%s", req.Method, r, buf[:n])
			resp = &jsonRPCResponse{
				JSONRPC: "2.0",
				ID:      req.ID,
				Error:   &rpcError{Code: -32603, Message: fmt.Sprintf("Internal error: %v", r)},
			}
		}
		s.recordMetric(req.Method, time.Since(start), resp.Error)
	}()
	return s.handleRequest(req)
}

// handleRequest dispatches to the appropriate handler.
func (s *server) handleRequest(req *jsonRPCRequest) *jsonRPCResponse {
	s.logger.Printf("Handling: %s", req.Method)

	var result any
	var rpcErr *rpcError

	switch req.Method {
	case "GetLanguages":
		result = s.handleGetLanguages()
	case "Parse":
		result, rpcErr = s.handleParse(req.Params)
	case "GetObject":
		result, rpcErr = s.handleGetObject(req.Params)
	case "Print":
		result, rpcErr = s.handlePrint(req.Params)
	case "InstallRecipes":
		result, rpcErr = s.handleInstallRecipes(req.Params)
	case "Reset":
		result = s.handleReset()
	case "GetMarketplace":
		result, rpcErr = s.handleGetMarketplace(req.Params)
	case "PrepareRecipe":
		result, rpcErr = s.handlePrepareRecipe(req.Params)
	case "Visit":
		result, rpcErr = s.handleVisit(req.Params)
	case "BatchVisit":
		result, rpcErr = s.handleBatchVisit(req.Params)
	case "Generate":
		result, rpcErr = s.handleGenerate(req.Params)
	case "TraceGetObject":
		result, rpcErr = s.handleTraceGetObject(req.Params)
	case "ParseProject":
		result, rpcErr = s.handleParseProject(req.Params)
	default:
		rpcErr = &rpcError{
			Code:    -32601,
			Message: fmt.Sprintf("Unknown method: %s", req.Method),
		}
	}

	return &jsonRPCResponse{
		JSONRPC: "2.0",
		ID:      req.ID,
		Result:  result,
		Error:   rpcErr,
	}
}

// handleGetLanguages returns the supported language types.
func (s *server) handleGetLanguages() []string {
	return []string{"org.openrewrite.golang.tree.Go$CompilationUnit"}
}

// parseRequest is the parameter type for Parse.
//
// `Module` and `GoModContent` are optional and let callers establish a
// project context for the batch: when present, the server parses the
// go.mod content into a GoResolutionResult, builds a ProjectImporter
// with the module's `require` entries plus all sibling .go inputs as
// known sources, and uses that importer for type attribution. Without
// them, the server falls back to per-file parsing with the stdlib
// importer (today's behavior).
type parseRequest struct {
	Inputs       []parseInput `json:"inputs"`
	RelativeTo   *string      `json:"relativeTo"`
	Module       string       `json:"module,omitempty"`
	GoModContent string       `json:"goModContent,omitempty"`
}

// parseInput can be a path-based or text-based input.
// It supports two JSON forms:
//   - A bare string (treated as a file path)
//   - A structured object with path, text, and sourcePath fields
type parseInput struct {
	Path       string `json:"path"`
	Text       string `json:"text"`
	SourcePath string `json:"sourcePath"`
}

func (p *parseInput) UnmarshalJSON(data []byte) error {
	// Try bare string first (Java PathInput serializes as @JsonValue string)
	var s string
	if err := json.Unmarshal(data, &s); err == nil {
		p.Path = s
		return nil
	}
	// Otherwise unmarshal as a structured object
	type alias parseInput
	var a alias
	if err := json.Unmarshal(data, &a); err != nil {
		return err
	}
	*p = parseInput(a)
	return nil
}

// handleParse parses Go source files and returns their IDs.
//
// When req.Module + req.GoModContent are set, the handler builds a
// ProjectImporter from the parsed go.mod (requires) plus every .go input
// in the batch (siblings) and uses it for type attribution. Inputs in the
// same package directory are parsed together so cross-file references
// resolve. Without module context the handler parses each input in
// isolation with the stdlib-only importer.
func (s *server) handleParse(params json.RawMessage) (any, *rpcError) {
	var req parseRequest
	if err := json.Unmarshal(params, &req); err != nil {
		return nil, &rpcError{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
	}

	// Resolve every input to (sourcePath, source) before deciding how to
	// parse. This lets us build the ProjectImporter with knowledge of all
	// siblings and lets us group by package directory.
	type resolved struct {
		idx        int
		sourcePath string
		source     string
	}
	resolvedInputs := make([]resolved, 0, len(req.Inputs))
	for i, input := range req.Inputs {
		var sourcePath, source string
		if input.Text != "" {
			source = input.Text
			sourcePath = input.SourcePath
			if sourcePath == "" {
				sourcePath = "<unknown>"
			}
		} else {
			filePath := input.Path
			if filePath == "" {
				filePath = input.SourcePath
			}
			if filePath == "" {
				continue
			}
			absPath := filePath
			data, err := os.ReadFile(absPath)
			if err != nil {
				return nil, &rpcError{Code: -32603, Message: fmt.Sprintf("Failed to read file: %v", err)}
			}
			source = string(data)
			if req.RelativeTo != nil && *req.RelativeTo != "" {
				if rel, err := filepath.Rel(*req.RelativeTo, absPath); err == nil {
					sourcePath = rel
				} else {
					sourcePath = absPath
				}
			} else {
				sourcePath = absPath
			}
		}
		resolvedInputs = append(resolvedInputs, resolved{idx: i, sourcePath: sourcePath, source: source})
	}

	p := goparser.NewGoParser()

	// Build a ProjectImporter when module context is provided. Recognize
	// the requires from go.mod content; register every .go input as a
	// sibling source so intra-project imports type-check against real
	// sources, and third-party imports declared in `require` resolve to
	// stub *types.Package objects. When req.RelativeTo is set, the vendor
	// walker scans `<RelativeTo>/vendor/<importPath>/` for real
	// resolution — replace directives in the go.mod redirect that walk.
	if req.Module != "" {
		pi := goparser.NewProjectImporter(req.Module, nil)
		if req.RelativeTo != nil && *req.RelativeTo != "" {
			pi.SetProjectRoot(*req.RelativeTo)
		}
		if req.GoModContent != "" {
			if mrr, err := goparser.ParseGoMod("go.mod", req.GoModContent); err == nil && mrr != nil {
				for _, r := range mrr.Requires {
					pi.AddRequire(r.ModulePath)
				}
				for _, r := range mrr.Replaces {
					pi.AddReplace(r.OldPath, r.NewPath, r.NewVersion)
				}
			}
		}
		for _, r := range resolvedInputs {
			if strings.HasSuffix(r.sourcePath, ".go") {
				pi.AddSource(r.sourcePath, r.source)
			}
		}
		p.Importer = pi
	}

	// Group .go inputs by package directory. Each group parses together
	// via parser.ParsePackage so file-A-references-file-B resolves.
	type fileEntry struct {
		idx   int
		input goparser.FileInput
	}
	groups := map[string][]fileEntry{}
	for _, r := range resolvedInputs {
		if !strings.HasSuffix(r.sourcePath, ".go") {
			continue
		}
		dir := filepath.Dir(r.sourcePath)
		groups[dir] = append(groups[dir], fileEntry{idx: r.idx, input: goparser.FileInput{Path: r.sourcePath, Content: r.source}})
	}

	// Parse each group; collect CUs by their original input index so the
	// returned IDs land in input-order. Pre-filter against the parser's
	// BuildContext so the post-parse `cus` slice aligns 1:1 with the
	// `included` subset of the group.
	cuByIdx := make(map[int]*tree.CompilationUnit, len(resolvedInputs))
	parseErrByIdx := make(map[int]error)
	for _, group := range groups {
		included := make([]fileEntry, 0, len(group))
		files := make([]goparser.FileInput, 0, len(group))
		for _, g := range group {
			if !goparser.MatchBuildContext(p.BuildContext, filepath.Base(g.input.Path), g.input.Content) {
				continue
			}
			included = append(included, g)
			files = append(files, g.input)
		}
		if len(files) == 0 {
			continue
		}
		cus, err := func() (out []*tree.CompilationUnit, err error) {
			defer func() {
				if r := recover(); r != nil {
					err = fmt.Errorf("panic: %v", r)
				}
			}()
			return p.ParsePackage(files)
		}()
		if err != nil {
			// Whole-package parse failure — record per-file ParseErrors
			// for every file the build context didn't exclude.
			for _, g := range included {
				parseErrByIdx[g.idx] = err
			}
			continue
		}
		for i, cu := range cus {
			cuByIdx[included[i].idx] = cu
		}
	}

	// Emit results in input order.
	ids := make([]string, 0, len(req.Inputs))
	for _, r := range resolvedInputs {
		if cu, ok := cuByIdx[r.idx]; ok && cu != nil {
			id := cu.ID.String()
			s.localObjects[id] = cu
			ids = append(ids, id)
			continue
		}
		err := parseErrByIdx[r.idx]
		if err == nil {
			err = fmt.Errorf("no compilation unit produced")
		}
		s.logger.Printf("Parse error for %s: %v", r.sourcePath, err)
		pe := tree.NewParseError(r.sourcePath, r.source, err)
		id := pe.Ident.String()
		s.localObjects[id] = pe
		ids = append(ids, id)
	}

	return ids, nil
}

// getObjectRequest is the parameter type for GetObject.
type getObjectRequest struct {
	ID             string `json:"id"`
	SourceFileType string `json:"sourceFileType"`
}

// handleGetObject serializes a local object for transfer to Java.
func (s *server) handleGetObject(params json.RawMessage) (any, *rpcError) {
	var req getObjectRequest
	if err := json.Unmarshal(params, &req); err != nil {
		return nil, &rpcError{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
	}

	obj := s.localObjects[req.ID]
	if obj == nil {
		return []rpc.RpcObjectData{
			{State: rpc.Delete},
			{State: rpc.EndOfObject},
		}, nil
	}

	before := s.remoteObjects[req.ID]
	// Use a fresh ref map for each GetObject to avoid ref ID collisions
	// between the reverse direction (Java→Go) and forward direction (Go→Java).
	localRefs := make(map[uintptr]int)

	// Collect all batches into a single result
	var result []rpc.RpcObjectData
	q := rpc.NewSendQueue(s.batchSize, func(batch []rpc.RpcObjectData) {
		result = append(result, batch...)
	}, localRefs)

	sender := rpc.NewGoSender()
	q.Send(obj, before, func(v any) {
		if t, ok := v.(tree.Tree); ok {
			sender.Visit(t, q)
		}
	})
	q.Put(rpc.RpcObjectData{State: rpc.EndOfObject})
	q.Flush()

	// Update remote tracking
	s.remoteObjects[req.ID] = obj

	return result, nil
}

// printRequest is the parameter type for Print.
type printRequest struct {
	TreeID         string  `json:"treeId"`
	SourcePath     string  `json:"sourcePath"`
	SourceFileType string  `json:"sourceFileType"`
	MarkerPrinter  *string `json:"markerPrinter"`
}

// handlePrint retrieves a tree from Java and prints it to source.
func (s *server) handlePrint(params json.RawMessage) (any, *rpcError) {
	var req printRequest
	if err := json.Unmarshal(params, &req); err != nil {
		return nil, &rpcError{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
	}

	// Get the object from Java via bidirectional RPC
	obj := s.getObjectFromJava(req.TreeID, req.SourceFileType)
	if obj == nil {
		return "", nil
	}

	// Map markerPrinter from the request to the Go MarkerPrinter
	mp := mapMarkerPrinter(req.MarkerPrinter)

	if cu, ok := obj.(*tree.CompilationUnit); ok {
		if mp != nil {
			return printer.PrintWithMarkers(cu, mp), nil
		}
		return printer.Print(cu), nil
	}
	if t, ok := obj.(tree.Tree); ok {
		if mp != nil {
			return printer.PrintWithMarkers(t, mp), nil
		}
		return printer.Print(t), nil
	}

	return "", &rpcError{Code: -32603, Message: "Object is not a Tree"}
}

// mapMarkerPrinter maps the RPC marker printer string to the Go MarkerPrinter.
func mapMarkerPrinter(mp *string) printer.MarkerPrinter {
	if mp == nil {
		return nil
	}
	switch *mp {
	case "DEFAULT":
		return printer.DefaultMarkerPrinter
	case "SEARCH_MARKERS_ONLY":
		return printer.SearchOnlyMarkerPrinter
	case "FENCED":
		return printer.FencedMarkerPrinter
	case "SANITIZED":
		return printer.SanitizedMarkerPrinter
	default:
		return nil
	}
}

// getObjectFromJava fetches an object from the Java side via bidirectional RPC.
// For Print, Java holds the (potentially modified) tree. We need to request it back.
// Supports multi-batch transfers: each GetObject call returns one batch, and
// subsequent calls are made until the END_OF_OBJECT marker is received.
func (s *server) getObjectFromJava(id string, sourceFileType string) any {
	// Use reverse-direction tracking if available; otherwise fall back to
	// localObjects (the tree Go originally parsed and sent to Java via forward
	// GetObject). This matches Java's remoteObjects baseline.
	before := s.reverseRemoteObjects[id]
	if before == nil {
		before = s.localObjects[id]
	}

	// fetchBatch sends a GetObject request to Java and reads one batch of RpcObjectData.
	fetchBatch := func() []rpc.RpcObjectData {
		reqParams := getObjectRequest{ID: id, SourceFileType: sourceFileType}
		paramsJSON, _ := json.Marshal(reqParams)
		rpcReq := map[string]any{
			"jsonrpc": "2.0",
			"id":      "go-GetObject",
			"method":  "GetObject",
			"params":  json.RawMessage(paramsJSON),
		}
		body, _ := json.Marshal(rpcReq)
		header := fmt.Sprintf("Content-Length: %d\r\n\r\n", len(body))
		s.writer.Write(append([]byte(header), body...))

		resp, err := s.readMessage()
		if err != nil {
			s.logger.Printf("Error reading bidirectional response: %v", err)
			return nil
		}

		resultData := resp.Result
		if resultData == nil {
			resultData = resp.Params
		}
		if resultData == nil {
			s.logger.Printf("No result data in bidirectional response")
			return nil
		}
		var respResult any
		if err := json.Unmarshal(resultData, &respResult); err != nil {
			s.logger.Printf("Error parsing response result: %v", err)
			return nil
		}

		batchData, ok := respResult.([]any)
		if !ok || len(batchData) == 0 {
			return nil
		}

		batch := make([]rpc.RpcObjectData, 0, len(batchData))
		for _, item := range batchData {
			if m, ok := item.(map[string]any); ok {
				batch = append(batch, rpc.ParseObjectData(m))
			}
		}
		return batch
	}

	q := rpc.NewReceiveQueue(s.reverseRemoteRefs, fetchBatch)

	receiver := rpc.NewGoReceiver()
	obj := q.Receive(before, func(v any) any {
		// ExecutionContext uses an empty-body codec (matches JS execution.ts):
		// the type tag arrives via the queue envelope; no field messages follow.
		if ctx, ok := v.(*recipe.ExecutionContext); ok {
			return ctx
		}
		if t, ok := v.(tree.Tree); ok {
			return receiver.Visit(t, q)
		}
		return v
	})

	// Consume the END_OF_OBJECT sentinel if present
	if len(q.PeekBatch()) > 0 && q.PeekBatch()[0].State == rpc.EndOfObject {
		q.Take()
	}

	if obj != nil {
		s.reverseRemoteObjects[id] = obj
		s.localObjects[id] = obj
	}

	return obj
}

// installRecipesResponse is the response type for InstallRecipes.
type installRecipesResponse struct {
	RecipesInstalled int     `json:"recipesInstalled"`
	Version          *string `json:"version"`
}

// handleInstallRecipes handles recipe installation requests.
// The request params contain a "recipes" field that is either:
//   - a string (local file path)
//   - an object with "packageName" and optional "version" fields
func (s *server) handleInstallRecipes(params json.RawMessage) (any, *rpcError) {
	var raw map[string]json.RawMessage
	if err := json.Unmarshal(params, &raw); err != nil {
		return nil, &rpcError{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
	}

	recipesRaw, ok := raw["recipes"]
	if !ok {
		return nil, &rpcError{Code: -32602, Message: "Missing 'recipes' parameter"}
	}

	beforeCount := len(s.registry.AllRecipes())

	// Try to parse as a string (local path) first
	var localPath string
	if err := json.Unmarshal(recipesRaw, &localPath); err == nil {
		s.logger.Printf("InstallRecipes from local path: %s", localPath)
		_, err := s.installer.InstallFromPath(localPath, s.registry)
		if err != nil {
			return nil, &rpcError{Code: -32603, Message: fmt.Sprintf("Install from path failed: %v", err)}
		}
		afterCount := len(s.registry.AllRecipes())
		return &installRecipesResponse{
			RecipesInstalled: afterCount - beforeCount,
			Version:          nil,
		}, nil
	}

	// Otherwise parse as a package spec
	var pkg struct {
		PackageName string  `json:"packageName"`
		Version     *string `json:"version"`
	}
	if err := json.Unmarshal(recipesRaw, &pkg); err != nil {
		return nil, &rpcError{Code: -32602, Message: fmt.Sprintf("Invalid recipes parameter: %v", err)}
	}

	s.logger.Printf("InstallRecipes package: %s version: %v", pkg.PackageName, pkg.Version)
	info, err := s.installer.InstallFromPackage(pkg.PackageName, pkg.Version, s.registry)
	if err != nil {
		return nil, &rpcError{Code: -32603, Message: fmt.Sprintf("Install package failed: %v", err)}
	}

	afterCount := len(s.registry.AllRecipes())
	var version *string
	if info.Version != "" {
		version = &info.Version
	}
	return &installRecipesResponse{
		RecipesInstalled: afterCount - beforeCount,
		Version:          version,
	}, nil
}

// handleReset clears all cached state.
func (s *server) handleReset() bool {
	s.localObjects = make(map[string]any)
	s.remoteObjects = make(map[string]any)
	s.localRefs = make(map[uintptr]int)
	s.remoteRefs = make(map[int]any)
	s.reverseRemoteObjects = make(map[string]any)
	s.reverseRemoteRefs = make(map[int]any)
	s.preparedRecipes = make(map[string]recipe.Recipe)
	s.preparedAccumulators = make(map[string]any)
	s.preparedContexts = make(map[string]*recipe.ExecutionContext)
	return true
}

// resolveExecutionContext returns a usable ExecutionContext for a Visit /
// Generate / BatchVisit call. If pid is nil or empty, a fresh local ctx is
// returned. Otherwise the ctx is fetched from Java once (via the empty-body
// codec) and cached under the pid for subsequent calls in the same recipe run.
//
// When --data-tables-csv-dir is set, a CsvDataTableStore is installed into
// the ctx so any recipe that emits data-table rows writes them to that
// directory. Otherwise an InMemoryDataTableStore is created lazily on first
// InsertRow.
func (s *server) resolveExecutionContext(pid *string) *recipe.ExecutionContext {
	var ctx *recipe.ExecutionContext
	if pid == nil || *pid == "" {
		ctx = recipe.NewExecutionContext()
	} else if cached, ok := s.preparedContexts[*pid]; ok {
		return cached
	} else {
		obj := s.getObjectFromJava(*pid, "org.openrewrite.InMemoryExecutionContext")
		var ok bool
		ctx, ok = obj.(*recipe.ExecutionContext)
		if !ok || ctx == nil {
			ctx = recipe.NewExecutionContext()
		}
		s.preparedContexts[*pid] = ctx
	}
	s.installDataTableStore(ctx)
	return ctx
}

// getOrCreateAccumulator returns the accumulator for a ScanningRecipe,
// creating it lazily on the first call. The accumulator's lifetime is
// tied to the prepared recipe instance — freed only on Reset.
func (s *server) getOrCreateAccumulator(recipeID string, sr recipe.ScanningRecipe, ctx *recipe.ExecutionContext) any {
	if acc, ok := s.preparedAccumulators[recipeID]; ok {
		return acc
	}
	acc := sr.InitialValue(ctx)
	s.preparedAccumulators[recipeID] = acc
	return acc
}

// seedCursor reconstructs the cursor chain from RPC cursor IDs (root
// first) and seeds it onto the visitor via SetCursor. Visitors that
// don't expose SetCursor (e.g., aren't GoVisitor-derived) silently
// skip. Each cursor ID points to a tree node Java has; fetched via
// the existing reverse-RPC GetObject path. Mirrors how Java's RpcRecipe
// seeds the JavaVisitor cursor before traversal.
func (s *server) seedCursor(v recipe.TreeVisitor, ids []string) {
	type cursorAware interface {
		SetCursor(c *visitor.Cursor)
	}
	ca, ok := v.(cursorAware)
	if !ok || len(ids) == 0 {
		return
	}
	values := make([]tree.Tree, 0, len(ids))
	for _, id := range ids {
		obj := s.getObjectFromJava(id, "")
		if t, ok := obj.(tree.Tree); ok {
			values = append(values, t)
		}
	}
	if len(values) > 0 {
		ca.SetCursor(visitor.BuildChain(values))
	}
}

// installDataTableStore puts a DataTableStore into the ctx if one isn't
// already present. Choice driven by --data-tables-csv-dir.
func (s *server) installDataTableStore(ctx *recipe.ExecutionContext) {
	if _, ok := ctx.GetMessage(recipe.DataTableStoreKey); ok {
		return
	}
	if s.dataTablesCsvDir != "" {
		store, err := recipe.NewCsvDataTableStore(s.dataTablesCsvDir)
		if err != nil {
			s.logger.Printf("CsvDataTableStore unavailable, falling back to in-memory: %v", err)
		} else {
			ctx.PutMessage(recipe.DataTableStoreKey, store)
			return
		}
	}
	ctx.PutMessage(recipe.DataTableStoreKey, recipe.NewInMemoryDataTableStore())
}

// marketplaceRow matches Java's GetMarketplaceResponse.Row.
type marketplaceRow struct {
	Descriptor    marketplaceDescriptor     `json:"descriptor"`
	CategoryPaths [][]marketplaceCategory   `json:"categoryPaths"`
}

// marketplaceDescriptor matches Java's RecipeDescriptor (minimal fields).
type marketplaceDescriptor struct {
	Name                         string                    `json:"name"`
	DisplayName                  string                    `json:"displayName"`
	InstanceName                 string                    `json:"instanceName"`
	Description                  string                    `json:"description"`
	Tags                         []string                  `json:"tags"`
	EstimatedEffortPerOccurrence *string                   `json:"estimatedEffortPerOccurrence"`
	Options                      []marketplaceOption       `json:"options"`
	Preconditions                []marketplaceDescriptor   `json:"preconditions"`
	RecipeList                   []marketplaceDescriptor   `json:"recipeList"`
	DataTables                   []any                     `json:"dataTables"`
	Maintainers                  []any                     `json:"maintainers"`
	Contributors                 []any                     `json:"contributors"`
	Examples                     []any                     `json:"examples"`
	Source                       *string                   `json:"source"`
}

type marketplaceOption struct {
	Name        string  `json:"name"`
	DisplayName string  `json:"displayName"`
	Description string  `json:"description"`
	Example     *string `json:"example"`
	Required    bool    `json:"required"`
	Type        string  `json:"type"`
	Value       any     `json:"value"`
	Valid       []any   `json:"valid"`
}

// marketplaceCategory matches Java's CategoryDescriptor.
type marketplaceCategory struct {
	DisplayName string   `json:"displayName"`
	PackageName string   `json:"packageName"`
	Description string   `json:"description"`
	Tags        []string `json:"tags"`
	Root        bool     `json:"root"`
	Priority    int      `json:"priority"`
}

// handleGetMarketplace returns available recipes from the Go registry.
// The Java side expects a JSON array of Row{descriptor, categoryPaths}.
func (s *server) handleGetMarketplace(params json.RawMessage) (any, *rpcError) {
	var rows []marketplaceRow
	for _, reg := range s.registry.AllRegistrations() {
		desc := reg.Descriptor

		var categoryPath []marketplaceCategory
		for _, cat := range reg.Categories {
			categoryPath = append(categoryPath, marketplaceCategory{
				DisplayName: cat.DisplayName,
				PackageName: "",
				Description: cat.Description,
				Tags:        []string{},
				Root:        false,
				Priority:    0,
			})
		}

		rows = append(rows, marketplaceRow{
			Descriptor:    marketplaceDescriptorFromRecipe(desc),
			CategoryPaths: [][]marketplaceCategory{categoryPath},
		})
	}
	if rows == nil {
		rows = []marketplaceRow{}
	}
	return rows, nil
}

// marketplaceDescriptorFromRecipe converts a recipe.RecipeDescriptor to the
// wire-format marketplaceDescriptor expected by Java. Recursive fields
// (recipeList, preconditions) are populated. Cycle protection is handled
// upstream by recipe.Describe.
func marketplaceDescriptorFromRecipe(desc recipe.RecipeDescriptor) marketplaceDescriptor {
	options := make([]marketplaceOption, 0, len(desc.Options))
	for _, opt := range desc.Options {
		var example *string
		if opt.Example != "" {
			example = &opt.Example
		}
		valid := make([]any, 0, len(opt.Valid))
		for _, v := range opt.Valid {
			valid = append(valid, v)
		}
		options = append(options, marketplaceOption{
			Name:        opt.Name,
			DisplayName: opt.DisplayName,
			Description: opt.Description,
			Example:     example,
			Required:    opt.Required,
			Type:        opt.TypeName(),
			Value:       opt.Value,
			Valid:       valid,
		})
	}

	recipeList := make([]marketplaceDescriptor, 0, len(desc.RecipeList))
	for _, sub := range desc.RecipeList {
		recipeList = append(recipeList, marketplaceDescriptorFromRecipe(sub))
	}

	preconditions := make([]marketplaceDescriptor, 0, len(desc.Preconditions))
	for _, pre := range desc.Preconditions {
		preconditions = append(preconditions, marketplaceDescriptorFromRecipe(pre))
	}

	dataTables := make([]any, 0, len(desc.DataTables))
	for _, dt := range desc.DataTables {
		dataTables = append(dataTables, marketplaceDataTable{
			Name:        dt.Name,
			DisplayName: dt.DisplayName,
			Description: dt.Description,
			Columns:     marketplaceColumns(dt.Columns),
		})
	}

	maintainers := make([]any, 0, len(desc.Maintainers))
	for _, m := range desc.Maintainers {
		maintainers = append(maintainers, marketplaceMaintainer{
			Name: m.Name, Email: m.Email, Logo: m.Logo,
		})
	}

	contributors := make([]any, 0, len(desc.Contributors))
	for _, c := range desc.Contributors {
		contributors = append(contributors, marketplaceContributor{
			Name: c.Name, Email: c.Email, LineCount: c.LineCount,
		})
	}

	examples := make([]any, 0, len(desc.Examples))
	for _, ex := range desc.Examples {
		sources := make([]any, 0, len(ex.Sources))
		for _, src := range ex.Sources {
			sources = append(sources, marketplaceExampleSource{
				Before: src.Before, After: src.After,
				Path: src.Path, Language: src.Language,
			})
		}
		examples = append(examples, marketplaceExample{
			Description: ex.Description,
			Sources:     sources,
			Parameters:  nonNil(ex.Parameters),
		})
	}

	return marketplaceDescriptor{
		Name:          desc.Name,
		DisplayName:   desc.DisplayName,
		InstanceName:  desc.DisplayName,
		Description:   desc.Description,
		Tags:          nonNil(desc.Tags),
		Options:       options,
		Preconditions: preconditions,
		RecipeList:    recipeList,
		DataTables:    dataTables,
		Maintainers:   maintainers,
		Contributors:  contributors,
		Examples:      examples,
	}
}

func marketplaceColumns(cols []recipe.ColumnDescriptor) []marketplaceColumn {
	out := make([]marketplaceColumn, 0, len(cols))
	for _, c := range cols {
		out = append(out, marketplaceColumn{
			Name: c.Name, DisplayName: c.DisplayName,
			Description: c.Description, Type: c.Type,
		})
	}
	return out
}

type marketplaceDataTable struct {
	Name        string              `json:"name"`
	DisplayName string              `json:"displayName"`
	Description string              `json:"description"`
	Columns     []marketplaceColumn `json:"columns"`
}

type marketplaceColumn struct {
	Name        string `json:"name"`
	DisplayName string `json:"displayName"`
	Description string `json:"description"`
	Type        string `json:"type"`
}

type marketplaceMaintainer struct {
	Name  string `json:"name"`
	Email string `json:"email,omitempty"`
	Logo  string `json:"logo,omitempty"`
}

type marketplaceContributor struct {
	Name      string `json:"name"`
	Email     string `json:"email,omitempty"`
	LineCount int    `json:"lineCount"`
}

type marketplaceExample struct {
	Description string   `json:"description"`
	Sources     []any    `json:"sources"`
	Parameters  []string `json:"parameters"`
}

type marketplaceExampleSource struct {
	Before   string `json:"before"`
	After    string `json:"after"`
	Path     string `json:"path,omitempty"`
	Language string `json:"language,omitempty"`
}

func nonNil(s []string) []string {
	if s == nil {
		return []string{}
	}
	return s
}

// prepareRecipeRequest is the parameter type for PrepareRecipe.
type prepareRecipeRequest struct {
	ID      string         `json:"id"`
	Options map[string]any `json:"options"`
}

// prepareRecipeResponse contains the prepared recipe info.
type prepareRecipeResponse struct {
	ID                string                `json:"id"`
	Descriptor        marketplaceDescriptor `json:"descriptor"`
	EditVisitor       string                `json:"editVisitor"`
	EditPreconditions []any                 `json:"editPreconditions"`
	ScanVisitor       *string               `json:"scanVisitor,omitempty"`
	ScanPreconditions []any                 `json:"scanPreconditions"`
	DelegatesTo       *delegatesToResponse  `json:"delegatesTo,omitempty"`
}

type delegatesToResponse struct {
	RecipeName string         `json:"recipeName"`
	Options    map[string]any `json:"options"`
}

// handlePrepareRecipe instantiates a recipe by name with options.
func (s *server) handlePrepareRecipe(params json.RawMessage) (any, *rpcError) {
	var req prepareRecipeRequest
	if err := json.Unmarshal(params, &req); err != nil {
		return nil, &rpcError{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
	}

	reg, ok := s.registry.FindRecipe(req.ID)
	if !ok {
		return nil, &rpcError{Code: -32602, Message: fmt.Sprintf("Unknown recipe: %s", req.ID)}
	}

	// Create recipe instance with options
	instance := reg.Constructor(req.Options)

	var desc recipe.RecipeDescriptor
	if instance != nil {
		desc = recipe.Describe(instance)
	} else {
		// Installer-loaded recipes have no constructor; use stored descriptor
		desc = reg.Descriptor
	}

	// response.ID must be the per-instance UUID, not the recipe name —
	// callers echo it back to identify the prepared instance.
	recipeID := uuid.New().String()
	s.preparedRecipes[recipeID] = instance

	resp := prepareRecipeResponse{
		ID:                recipeID,
		Descriptor:        marketplaceDescriptorFromRecipe(desc),
		EditVisitor:       "edit:" + recipeID,
		EditPreconditions: []any{},
		ScanPreconditions: []any{},
	}

	// Check if this is a scanning recipe
	if instance != nil {
		if _, isScan := instance.(recipe.ScanningRecipe); isScan {
			scanVis := "scan:" + recipeID
			resp.ScanVisitor = &scanVis
		}
	}

	// Check for delegation
	if instance != nil {
		if del, ok := instance.(recipe.DelegatesTo); ok {
			resp.DelegatesTo = &delegatesToResponse{
				RecipeName: del.JavaRecipeName(),
				Options:    del.JavaOptions(),
			}
		}
	}

	return resp, nil
}

// visitRequest is the parameter type for Visit.
type visitRequest struct {
	Visitor        string   `json:"visitor"`
	TreeID         string   `json:"treeId"`
	SourceFileType string   `json:"sourceFileType"`
	PID            *string  `json:"p"`
	Cursor         []string `json:"cursor"`
}

// visitResponse is the response type for Visit.
type visitResponse struct {
	Modified bool `json:"modified"`
}

// handleVisit applies a prepared recipe's visitor to a tree.
func (s *server) handleVisit(params json.RawMessage) (any, *rpcError) {
	var req visitRequest
	if err := json.Unmarshal(params, &req); err != nil {
		return nil, &rpcError{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
	}

	// Parse visitor name: "edit:<recipeId>" or "scan:<recipeId>"
	parts := strings.SplitN(req.Visitor, ":", 2)
	if len(parts) != 2 {
		return nil, &rpcError{Code: -32602, Message: "Invalid visitor name: " + req.Visitor}
	}
	phase := parts[0]
	recipeID := parts[1]

	// Look up the prepared recipe
	r, ok := s.preparedRecipes[recipeID]
	if !ok {
		return nil, &rpcError{Code: -32602, Message: "Unknown recipe: " + recipeID}
	}
	if r == nil {
		// Installer-loaded recipes have no implementation in Go
		return &visitResponse{Modified: false}, nil
	}

	// Get the tree from Java via bidirectional RPC
	treeObj := s.getObjectFromJava(req.TreeID, req.SourceFileType)
	if treeObj == nil {
		return &visitResponse{Modified: false}, nil
	}

	// Resolve ctx first because ScanningRecipe.InitialValue may need it.
	ctx := s.resolveExecutionContext(req.PID)

	// Get the visitor based on phase. For ScanningRecipe, both scan and edit
	// phases need access to the accumulator; the accumulator is created
	// lazily on the first scan visit.
	var v recipe.TreeVisitor
	switch phase {
	case "edit":
		if sr, ok := r.(recipe.ScanningRecipe); ok {
			acc := s.getOrCreateAccumulator(recipeID, sr, ctx)
			v = sr.EditorWithData(acc)
		} else {
			v = r.Editor()
		}
	case "scan":
		sr, ok := r.(recipe.ScanningRecipe)
		if !ok {
			// scan visitor for a non-scanning recipe is a no-op
			return &visitResponse{Modified: false}, nil
		}
		acc := s.getOrCreateAccumulator(recipeID, sr, ctx)
		v = sr.Scanner(acc)
	default:
		return nil, &rpcError{Code: -32602, Message: "Unknown phase: " + phase}
	}

	if v == nil {
		return &visitResponse{Modified: false}, nil
	}

	// Apply the visitor
	treeNode, ok := treeObj.(tree.Tree)
	if !ok {
		return &visitResponse{Modified: false}, nil
	}
	s.seedCursor(v, req.Cursor)
	before := treeNode
	after := v.Visit(treeNode, ctx)
	if after == nil {
		after = before
	}
	// Drain any after-visits queued via GoVisitor.DoAfterVisit during
	// the main visit. Mirrors JavaVisitor's afterVisit drain — recipes
	// use this to compose follow-ups (e.g. AddImport as a side-effect).
	after = visitor.DrainAfterVisits(v, after, ctx)

	// Check if modified by pointer identity (not value equality,
	// since tree nodes contain slices which are not comparable).
	modified := !treeIdentical(before, after)

	// Store the result — update both localObjects (for forward GetObject)
	// and reverseRemoteObjects (baseline for reverse getObjectFromJava in Print)
	if after != nil {
		s.localObjects[req.TreeID] = after
		s.reverseRemoteObjects[req.TreeID] = after
	} else {
		delete(s.localObjects, req.TreeID)
	}

	return &visitResponse{Modified: modified}, nil
}

// generateRequest is the parameter type for Generate.
type generateRequest struct {
	ID  string  `json:"id"`
	PID *string `json:"p"`
}

// generateResponse is the response type for Generate.
type generateResponse struct {
	IDs             []string `json:"ids"`
	SourceFileTypes []string `json:"sourceFileTypes"`
}

// stringSet builds a set from a list of strings; used as a snapshot
// helper for hasNewMessages tracking in BatchVisit.
func stringSet(xs []string) map[string]struct{} {
	out := make(map[string]struct{}, len(xs))
	for _, x := range xs {
		out[x] = struct{}{}
	}
	return out
}

// instantiateVisitor parses a visitor name like "edit:UUID" or "scan:UUID"
// and returns the configured visitor for that prepared recipe. Returns nil
// for installer-loaded recipes (no Go-side implementation) or unknown phases.
func (s *server) instantiateVisitor(visitorName string, ctx *recipe.ExecutionContext) recipe.TreeVisitor {
	parts := strings.SplitN(visitorName, ":", 2)
	if len(parts) != 2 {
		return nil
	}
	phase := parts[0]
	recipeID := parts[1]

	r, ok := s.preparedRecipes[recipeID]
	if !ok || r == nil {
		return nil
	}

	switch phase {
	case "edit":
		if sr, ok := r.(recipe.ScanningRecipe); ok {
			acc := s.getOrCreateAccumulator(recipeID, sr, ctx)
			return sr.EditorWithData(acc)
		}
		return r.Editor()
	case "scan":
		if sr, ok := r.(recipe.ScanningRecipe); ok {
			acc := s.getOrCreateAccumulator(recipeID, sr, ctx)
			return sr.Scanner(acc)
		}
		return nil
	}
	return nil
}

// batchVisitRequest is the parameter type for BatchVisit.
// Wire shape mirrors JS rewrite-javascript/rewrite/src/rpc/request/batch-visit.ts.
type batchVisitRequest struct {
	SourceFileType string            `json:"sourceFileType"`
	TreeID         string            `json:"treeId"`
	PID            *string           `json:"p"`
	Cursor         []string          `json:"cursor"`
	Visitors       []batchVisitItem  `json:"visitors"`
}

type batchVisitItem struct {
	Visitor        string         `json:"visitor"`
	VisitorOptions map[string]any `json:"visitorOptions"`
}

// batchVisitResponse must use the four-field per-result shape Java expects:
// {modified, deleted, hasNewMessages, searchResultIds}.
type batchVisitResponse struct {
	Results []batchVisitResult `json:"results"`
}

type batchVisitResult struct {
	Modified        bool     `json:"modified"`
	Deleted         bool     `json:"deleted"`
	HasNewMessages  bool     `json:"hasNewMessages"`
	SearchResultIDs []string `json:"searchResultIds"`
}

// handleBatchVisit runs N visitors sequentially against a single tree,
// piping the output of visitor N into visitor N+1. On deletion, the
// pipeline stops and remaining visitors are not run.
func (s *server) handleBatchVisit(params json.RawMessage) (any, *rpcError) {
	var req batchVisitRequest
	if err := json.Unmarshal(params, &req); err != nil {
		return nil, &rpcError{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
	}

	ctx := s.resolveExecutionContext(req.PID)

	treeObj := s.getObjectFromJava(req.TreeID, req.SourceFileType)
	current, _ := treeObj.(tree.Tree)
	if current == nil {
		return &batchVisitResponse{Results: []batchVisitResult{}}, nil
	}

	// Track search-result marker IDs already visible on the tree before any
	// visitor runs. Per-visitor, we diff against this set so each result
	// only carries the IDs *newly added* by that visitor (matches
	// JS batch-visit.ts:46+).
	knownIDs := map[string]struct{}{}
	for _, id := range tree.CollectSearchResultIDs(current) {
		knownIDs[id.String()] = struct{}{}
	}

	results := make([]batchVisitResult, 0, len(req.Visitors))
	for _, item := range req.Visitors {
		v := s.instantiateVisitor(item.Visitor, ctx)
		if v == nil {
			results = append(results, batchVisitResult{SearchResultIDs: []string{}})
			continue
		}
		s.seedCursor(v, req.Cursor)
		before := current
		// Snapshot the ctx message keys so we can detect whether the
		// visitor added any new ones (`hasNewMessages`).
		preKeys := stringSet(ctx.MessageKeys())
		after := v.Visit(current, ctx)

		deleted := after == nil
		modified := !deleted && !treeIdentical(before, after)

		hasNewMessages := false
		for _, k := range ctx.MessageKeys() {
			if _, ok := preKeys[k]; !ok {
				hasNewMessages = true
				break
			}
		}

		var newSearchResultIDs []string
		if !deleted {
			afterTree, _ := after.(tree.Tree)
			if afterTree != nil {
				for _, id := range tree.CollectSearchResultIDs(afterTree) {
					sid := id.String()
					if _, seen := knownIDs[sid]; seen {
						continue
					}
					knownIDs[sid] = struct{}{}
					newSearchResultIDs = append(newSearchResultIDs, sid)
				}
			}
		}
		if newSearchResultIDs == nil {
			newSearchResultIDs = []string{}
		}

		results = append(results, batchVisitResult{
			Modified:        modified,
			Deleted:         deleted,
			HasNewMessages:  hasNewMessages,
			SearchResultIDs: newSearchResultIDs,
		})

		if deleted {
			delete(s.localObjects, req.TreeID)
			current = nil
			break
		}
		if modified {
			if t, ok := after.(tree.Tree); ok {
				current = t
			}
		}
	}

	// Store the final tree under both req.treeId and its own id (if different),
	// matching the JS pattern.
	if current != nil {
		s.localObjects[req.TreeID] = current
		s.reverseRemoteObjects[req.TreeID] = current
	}

	return &batchVisitResponse{Results: results}, nil
}

// handleGenerate returns any new source files generated by a scanning recipe.
// req.ID is the per-instance UUID returned by PrepareRecipe.
func (s *server) handleGenerate(params json.RawMessage) (any, *rpcError) {
	var req generateRequest
	if err := json.Unmarshal(params, &req); err != nil {
		return nil, &rpcError{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
	}

	r, ok := s.preparedRecipes[req.ID]
	if !ok || r == nil {
		return &generateResponse{IDs: []string{}, SourceFileTypes: []string{}}, nil
	}
	sr, ok := r.(recipe.ScanningRecipe)
	if !ok {
		return &generateResponse{IDs: []string{}, SourceFileTypes: []string{}}, nil
	}

	ctx := s.resolveExecutionContext(req.PID)
	acc := s.getOrCreateAccumulator(req.ID, sr, ctx)

	generated := sr.Generate(acc, ctx)
	resp := &generateResponse{
		IDs:             make([]string, 0, len(generated)),
		SourceFileTypes: make([]string, 0, len(generated)),
	}
	for _, t := range generated {
		if t == nil {
			continue
		}
		// New trees get a fresh UUID; Java fetches them via GetObject.
		newID := uuid.New().String()
		s.localObjects[newID] = t
		resp.IDs = append(resp.IDs, newID)
		resp.SourceFileTypes = append(resp.SourceFileTypes, sourceFileTypeFor(t))
	}
	return resp, nil
}

// sourceFileTypeFor returns the FQN Java expects for a Go-side tree.
// Currently every Go source file is a CompilationUnit; expand if other
// SourceFile types are added.
func sourceFileTypeFor(t tree.Tree) string {
	if _, ok := t.(*tree.CompilationUnit); ok {
		return "org.openrewrite.golang.tree.Go$CompilationUnit"
	}
	return ""
}

// traceGetObjectRequest is the parameter type for TraceGetObject.
type traceGetObjectRequest struct {
	Receive bool `json:"receive"`
	Send    bool `json:"send"`
}

// handleTraceGetObject toggles tracing for GetObject calls.
func (s *server) handleTraceGetObject(params json.RawMessage) (any, *rpcError) {
	var req traceGetObjectRequest
	if err := json.Unmarshal(params, &req); err != nil {
		return nil, &rpcError{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
	}

	s.traceReceive = req.Receive
	s.traceSend = req.Send
	s.logger.Printf("TraceGetObject: receive=%v send=%v", req.Receive, req.Send)

	return true, nil
}

// parseProjectRequest is the parameter type for ParseProject.
type parseProjectRequest struct {
	ProjectPath string   `json:"projectPath"`
	Exclusions  []string `json:"exclusions"`
	RelativeTo  *string  `json:"relativeTo"`
}

// parseProjectResponseItem describes a parsed source file.
type parseProjectResponseItem struct {
	ID             string `json:"id"`
	SourceFileType string `json:"sourceFileType"`
	SourcePath     string `json:"sourcePath"`
}

// handleParseProject discovers and parses all Go files in a project
// directory. When a sibling go.mod exists, files are grouped by their
// closest-ancestor module and parsed together so cross-file references
// inside a package resolve. Each parsed compilation unit gets the owning
// module's GoResolutionResult attached as a marker so Java-side recipes
// can read module dependency info without re-parsing go.mod themselves.
//
// Multi-module repos (root go.mod plus nested submodules) are honored:
// each .go file resolves against its closest-ancestor go.mod, not the
// project root's.
func (s *server) handleParseProject(params json.RawMessage) (any, *rpcError) {
	var req parseProjectRequest
	if err := json.Unmarshal(params, &req); err != nil {
		return nil, &rpcError{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}
	}

	s.logger.Printf("ParseProject: path=%s", req.ProjectPath)

	// Discover all .go files AND every go.mod in the project tree.
	type discovered struct {
		goFiles  []string
		goMods   []string
	}
	var disc discovered
	err := filepath.Walk(req.ProjectPath, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if info.IsDir() {
			base := filepath.Base(path)
			// Skip common non-source directories. vendor/ is handled by
			// the (3-tier) ProjectImporter for symbol resolution; we
			// don't want to parse vendored code as project sources.
			if base == "vendor" || base == "node_modules" || base == ".git" || base == "testdata" {
				return filepath.SkipDir
			}
			for _, excl := range req.Exclusions {
				if matched, _ := filepath.Match(excl, base); matched {
					return filepath.SkipDir
				}
			}
			return nil
		}
		switch {
		case filepath.Base(path) == "go.mod":
			disc.goMods = append(disc.goMods, path)
		case strings.HasSuffix(path, ".go") && !strings.HasSuffix(path, "_test.go"):
			disc.goFiles = append(disc.goFiles, path)
		}
		return nil
	})
	if err != nil {
		return nil, &rpcError{Code: -32603, Message: fmt.Sprintf("Walk error: %v", err)}
	}

	// Parse every go.mod once; index by directory so we can find the
	// closest ancestor for each .go file. Failing to parse a go.mod is
	// non-fatal — the affected files just lose module context and fall
	// back to stdlib-only attribution.
	type modCtx struct {
		dir string                       // absolute directory containing go.mod
		mrr *tree.GoResolutionResult
	}
	mods := make(map[string]*modCtx, len(disc.goMods))
	for _, modPath := range disc.goMods {
		data, err := os.ReadFile(modPath)
		if err != nil {
			s.logger.Printf("ParseProject: skip go.mod %s: %v", modPath, err)
			continue
		}
		mrr, err := goparser.ParseGoMod(modPath, string(data))
		if err != nil || mrr == nil {
			s.logger.Printf("ParseProject: skip malformed go.mod %s: %v", modPath, err)
			continue
		}
		// If a sibling go.sum exists, populate ResolvedDependencies too.
		sumPath := filepath.Join(filepath.Dir(modPath), "go.sum")
		if sumData, err := os.ReadFile(sumPath); err == nil {
			mrr.ResolvedDependencies = goparser.ParseGoSum(string(sumData))
		}
		mods[filepath.Dir(modPath)] = &modCtx{dir: filepath.Dir(modPath), mrr: mrr}
	}

	// closestModule walks up `dir` looking for the deepest known go.mod
	// directory. Returns nil when no ancestor module exists (tree-relative
	// stdlib-only parse).
	closestModule := func(dir string) *modCtx {
		for cur := dir; ; {
			if m, ok := mods[cur]; ok {
				return m
			}
			parent := filepath.Dir(cur)
			if parent == cur {
				return nil
			}
			cur = parent
		}
	}

	// Pre-read every .go file so each is touched once even if used both
	// as a ProjectImporter source and as a parse input.
	contents := make(map[string]string, len(disc.goFiles))
	for _, goFile := range disc.goFiles {
		data, err := os.ReadFile(goFile)
		if err != nil {
			s.logger.Printf("ParseProject: skip %s: %v", goFile, err)
			continue
		}
		contents[goFile] = string(data)
	}

	// Build a ProjectImporter per module, populated with every .go file
	// that belongs to that module so cross-package resolution works.
	// Project root is the module's go.mod dir so the vendor walker
	// scans the right tree; replace directives are forwarded too.
	piByModule := make(map[string]*goparser.ProjectImporter, len(mods))
	for _, m := range mods {
		pi := goparser.NewProjectImporter(m.mrr.ModulePath, nil)
		pi.SetProjectRoot(m.dir)
		for _, r := range m.mrr.Requires {
			pi.AddRequire(r.ModulePath)
		}
		for _, r := range m.mrr.Replaces {
			pi.AddReplace(r.OldPath, r.NewPath, r.NewVersion)
		}
		piByModule[m.dir] = pi
	}
	for _, goFile := range disc.goFiles {
		src, ok := contents[goFile]
		if !ok {
			continue
		}
		m := closestModule(filepath.Dir(goFile))
		if m == nil {
			continue
		}
		piByModule[m.dir].AddSource(goFile, src)
	}

	// Group files by (owning module, package directory). Each group
	// parses together via ParsePackage so file-A-references-file-B
	// resolves within a package.
	type groupKey struct{ moduleDir, pkgDir string }
	type fileEntry struct {
		idx        int
		path       string
		sourcePath string
		content    string
	}
	groups := make(map[groupKey][]fileEntry)
	type ordered struct {
		idx        int
		sourcePath string
		modCtx     *modCtx
	}
	order := make([]ordered, 0, len(disc.goFiles))
	for i, goFile := range disc.goFiles {
		src, ok := contents[goFile]
		if !ok {
			continue
		}
		sourcePath := goFile
		if req.RelativeTo != nil && *req.RelativeTo != "" {
			if rel, err := filepath.Rel(*req.RelativeTo, goFile); err == nil {
				sourcePath = rel
			}
		}
		m := closestModule(filepath.Dir(goFile))
		moduleDir := ""
		if m != nil {
			moduleDir = m.dir
		}
		key := groupKey{moduleDir: moduleDir, pkgDir: filepath.Dir(goFile)}
		groups[key] = append(groups[key], fileEntry{
			idx:        i,
			path:       goFile,
			sourcePath: sourcePath,
			content:    src,
		})
		order = append(order, ordered{idx: i, sourcePath: sourcePath, modCtx: m})
	}

	// Parse each group; collect CUs by original input index so the
	// returned IDs land in input-order. Files filtered out by the
	// parser's BuildContext (`//go:build` / `_GOOS_GOARCH.go` suffixes)
	// don't appear in the response — handled here so the post-parse
	// `cus` slice aligns with the `included` subset of entries.
	cuByIdx := make(map[int]*tree.CompilationUnit, len(disc.goFiles))
	for key, entries := range groups {
		p := goparser.NewGoParser()
		if pi, ok := piByModule[key.moduleDir]; ok {
			p.Importer = pi
		}
		included := make([]fileEntry, 0, len(entries))
		inputs := make([]goparser.FileInput, 0, len(entries))
		for _, e := range entries {
			if !goparser.MatchBuildContext(p.BuildContext, filepath.Base(e.sourcePath), e.content) {
				continue
			}
			included = append(included, e)
			inputs = append(inputs, goparser.FileInput{Path: e.sourcePath, Content: e.content})
		}
		if len(inputs) == 0 {
			continue
		}
		cus, err := func() (out []*tree.CompilationUnit, err error) {
			defer func() {
				if r := recover(); r != nil {
					err = fmt.Errorf("panic: %v", r)
				}
			}()
			return p.ParsePackage(inputs)
		}()
		if err != nil {
			for _, e := range included {
				s.logger.Printf("ParseProject: parse error in %s: %v", e.path, err)
			}
			continue
		}
		for i, cu := range cus {
			cuByIdx[included[i].idx] = cu
		}
	}

	// Emit results in input order, attaching the owning module's marker
	// to each cu so Java-side recipes can read module dependency info.
	items := make([]parseProjectResponseItem, 0, len(disc.goFiles))
	for _, o := range order {
		cu, ok := cuByIdx[o.idx]
		if !ok || cu == nil {
			continue
		}
		if o.modCtx != nil {
			cu.Markers = tree.AddMarker(cu.Markers, *o.modCtx.mrr)
		}
		id := cu.ID.String()
		s.localObjects[id] = cu
		items = append(items, parseProjectResponseItem{
			ID:             id,
			SourceFileType: "org.openrewrite.golang.tree.Go$CompilationUnit",
			SourcePath:     o.sourcePath,
		})
	}

	s.logger.Printf("ParseProject: parsed %d files across %d module(s)", len(items), len(mods))
	return items, nil
}

// treeIdentical compares two tree nodes by pointer identity.
// Tree nodes contain slices (uncomparable in Go), so we compare
// the underlying pointer addresses via reflect.
func treeIdentical(a, b any) bool {
	if a == nil && b == nil {
		return true
	}
	if a == nil || b == nil {
		return false
	}
	va := reflect.ValueOf(a)
	vb := reflect.ValueOf(b)
	if va.Kind() == reflect.Ptr && vb.Kind() == reflect.Ptr {
		return va.Pointer() == vb.Pointer()
	}
	return false
}
