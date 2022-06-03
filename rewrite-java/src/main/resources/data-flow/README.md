# Data Flow and Taint Tracking Model

The `model.csv` file contains a list of methods that transfer data flow and taint in some way.

This list of methods comes from CodeQL.

## Regenerating this CVS File

Periodically, it may be necessary to regenerate this file. This can be done by executing the following CodeQL query
against a CodeQL java database:

```ql
import java
import semmle.code.java.dataflow.ExternalFlow

from string namespace, string type, boolean subtypes, string name, string signature, string ext,
  string input, string output, string kind, boolean generated, string row
where summaryModel(namespace, type, subtypes, name, signature, ext, input, output, kind, generated, row)
select row
```

After regenerating the CSV, ensure you re-add the header to the file:
```csv
namespace; type; subtypes; name; signature; ext; input; output; kind
```
