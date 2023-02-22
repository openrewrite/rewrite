# Data Flow and Taint Tracking Model

The `model.csv` file contains a list of methods that transfer data flow and taint in some way.

This list of methods comes from CodeQL.

## Regenerating the `model.csv` File

Periodically, it may be necessary to regenerate this file. This can be done by executing the following CodeQL query
against a CodeQL java database (using `codeql query run`):

```ql
import java
import semmle.code.java.dataflow.ExternalFlowExtensions

from string package, string type, boolean subtypes, string name, string signature, string ext, string input, string output, string kind, string provenance
where summaryModel(package, type, subtypes, name, signature, ext, input, output, kind, provenance)
select package, type, subtypes, name, signature, ext, input, output, kind, provenance
```

The result can then be transformed to CSV using `codeql bqrs decode --format=csv` and replace the existing `model.csv` file.

## Regenerating the `sinks.csv` File

Similarly for the `sinks.csv` file, the following query can be used:

```ql
import java
import semmle.code.java.dataflow.ExternalFlowExtensions

from string package, string type, boolean subtypes, string name, string signature, string ext, string input, string kind, string provenance
where sinkModel(package, type, subtypes, name, signature, ext, input, kind, provenance)
select package, type, subtypes, name, signature, ext, input, kind, provenance
```

Again, the result can then be transformed to CSV using `codeql bqrs decode --format=csv` and replace the existing `sinks.csv` file.