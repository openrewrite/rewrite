namespace Rewrite.Core;

/// <summary>
/// Controls how markers are printed in output. Called at three points during printing:
/// before prefix, before syntax, and after syntax.
/// </summary>
public interface IMarkerPrinter
{
    string BeforePrefix(Marker marker, Cursor cursor, Func<string, string> commentWrapper) => "";

    string BeforeSyntax(Marker marker, Cursor cursor, Func<string, string> commentWrapper) => "";

    string AfterSyntax(Marker marker, Cursor cursor, Func<string, string> commentWrapper) => "";
}

public static class MarkerPrinter
{
    /// <summary>
    /// Default marker printer that prints SearchResult and Markup markers before syntax.
    /// </summary>
    public static readonly IMarkerPrinter Default = new DefaultMarkerPrinter();

    /// <summary>
    /// Only prints SearchResult markers, ignoring Markup and other markers.
    /// </summary>
    public static readonly IMarkerPrinter SearchMarkersOnly = new SearchMarkersOnlyPrinter();

    /// <summary>
    /// Wraps SearchResult and Markup markers with fenced {{id}} delimiters.
    /// </summary>
    public static readonly IMarkerPrinter Fenced = new FencedMarkerPrinter();

    /// <summary>
    /// Suppresses all marker output.
    /// </summary>
    public static readonly IMarkerPrinter Sanitized = new SanitizedMarkerPrinter();

    private class DefaultMarkerPrinter : IMarkerPrinter
    {
        public string BeforeSyntax(Marker marker, Cursor cursor, Func<string, string> commentWrapper)
        {
            return marker switch
            {
                SearchResult sr => commentWrapper(sr.Description == null ? "" : $"({sr.Description})"),
                Markup m => commentWrapper(m.Detail != null ? $"({m.Message}: {m.Detail})" : $"({m.Message})"),
                _ => ""
            };
        }
    }

    private class SearchMarkersOnlyPrinter : IMarkerPrinter
    {
        public string BeforeSyntax(Marker marker, Cursor cursor, Func<string, string> commentWrapper)
        {
            return marker is SearchResult sr
                ? commentWrapper(sr.Description == null ? "" : $"({sr.Description})")
                : "";
        }
    }

    private class FencedMarkerPrinter : IMarkerPrinter
    {
        public string BeforeSyntax(Marker marker, Cursor cursor, Func<string, string> commentWrapper)
        {
            return marker is SearchResult or Markup ? $"{{{{{marker.Id}}}}}" : "";
        }

        public string AfterSyntax(Marker marker, Cursor cursor, Func<string, string> commentWrapper)
        {
            return marker is SearchResult or Markup ? $"{{{{{marker.Id}}}}}" : "";
        }
    }

    private class SanitizedMarkerPrinter : IMarkerPrinter
    {
    }
}
