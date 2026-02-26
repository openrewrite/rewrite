from __future__ import annotations

from dataclasses import dataclass, replace
from enum import Enum, auto
from typing import Optional, List, Dict
from uuid import UUID

from rewrite import Marker


@dataclass(frozen=True, eq=False)
class KeywordArguments(Marker):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id_: UUID) -> 'KeywordArguments':
        return self if id_ is self._id else replace(self, _id=id_)


@dataclass(frozen=True, eq=False)
class KeywordOnlyArguments(Marker):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id_: UUID) -> 'KeywordOnlyArguments':
        return self if id_ is self._id else replace(self, _id=id_)


@dataclass(frozen=True, eq=False)
class Quoted(Marker):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id_: UUID) -> Quoted:
        return self if id_ is self._id else replace(self, _id=id_)

    _style: Style

    @property
    def style(self) -> Style:
        return self._style

    def with_style(self, style: Style) -> Quoted:
        return self if style is self._id else replace(self, _style=style)

    class Style(Enum):
        SINGLE = 0
        DOUBLE = 1
        TRIPLE_SINGLE = 2
        TRIPLE_DOUBLE = 3
        BACKTICK = 4  # Python 2 repr: `x`

        @property
        def quote(self) -> str:
            """Return the quote character(s) for this style."""
            if self == Quoted.Style.SINGLE:
                return "'"
            elif self == Quoted.Style.DOUBLE:
                return '"'
            elif self == Quoted.Style.TRIPLE_SINGLE:
                return "'''"
            elif self == Quoted.Style.TRIPLE_DOUBLE:
                return '"""'
            elif self == Quoted.Style.BACKTICK:
                return "`"
            return ""


@dataclass(frozen=True, eq=False)
class SuppressNewline(Marker):
    """Marker to suppress trailing newline in compilation units."""
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id_: UUID) -> 'SuppressNewline':
        return self if id_ is self._id else replace(self, _id=id_)


@dataclass(frozen=True, eq=False)
class PrintSyntax(Marker):
    """Marker indicating a J.MethodInvocation represents a Python 2 print statement.

    In Python 2, print is a statement:
        print "hello"           # simple print
        print >> stderr, "err"  # print to file (has_destination=True)
        print x,                # trailing comma suppresses newline
    """
    _id: UUID
    _has_destination: bool
    _trailing_comma: bool

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id_: UUID) -> 'PrintSyntax':
        return self if id_ is self._id else replace(self, _id=id_)

    @property
    def has_destination(self) -> bool:
        return self._has_destination

    def with_has_destination(self, has_destination: bool) -> 'PrintSyntax':
        return self if has_destination is self._has_destination else replace(self, _has_destination=has_destination)

    @property
    def trailing_comma(self) -> bool:
        return self._trailing_comma

    def with_trailing_comma(self, trailing_comma: bool) -> 'PrintSyntax':
        return self if trailing_comma is self._trailing_comma else replace(self, _trailing_comma=trailing_comma)


@dataclass(frozen=True, eq=False)
class ExecSyntax(Marker):
    """Marker indicating a J.MethodInvocation represents a Python 2 exec statement.

    In Python 2, exec is a statement:
        exec code                    # simple form
        exec code in globals         # with globals dict
        exec code in globals, locals # with globals and locals dicts
    """
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id_: UUID) -> 'ExecSyntax':
        return self if id_ is self._id else replace(self, _id=id_)


@dataclass(frozen=True, eq=False)
class PythonResolutionResult(Marker):
    """Contains metadata about a Python project, parsed from pyproject.toml and uv.lock."""

    class PackageManager(Enum):
        Uv = auto()
        Pip = auto()
        Pipenv = auto()
        Poetry = auto()
        Pdm = auto()

    @dataclass(frozen=True, eq=False)
    class SourceIndex:
        _name: str
        _url: str
        _default_index: bool

        @property
        def name(self) -> str:
            return self._name

        def with_name(self, name: str) -> PythonResolutionResult.SourceIndex:
            return self if name is self._name else replace(self, _name=name)

        @property
        def url(self) -> str:
            return self._url

        def with_url(self, url: str) -> PythonResolutionResult.SourceIndex:
            return self if url is self._url else replace(self, _url=url)

        @property
        def default_index(self) -> bool:
            return self._default_index

        def with_default_index(self, default_index: bool) -> PythonResolutionResult.SourceIndex:
            return self if default_index is self._default_index else replace(self, _default_index=default_index)

    @dataclass(frozen=True, eq=False)
    class ResolvedDependency:
        _name: str
        _version: str
        _source: Optional[str]
        _dependencies: Optional[List[PythonResolutionResult.ResolvedDependency]]

        @property
        def name(self) -> str:
            return self._name

        def with_name(self, name: str) -> PythonResolutionResult.ResolvedDependency:
            return self if name is self._name else replace(self, _name=name)

        @property
        def version(self) -> str:
            return self._version

        def with_version(self, version: str) -> PythonResolutionResult.ResolvedDependency:
            return self if version is self._version else replace(self, _version=version)

        @property
        def source(self) -> Optional[str]:
            return self._source

        def with_source(self, source: Optional[str]) -> PythonResolutionResult.ResolvedDependency:
            return self if source is self._source else replace(self, _source=source)

        @property
        def dependencies(self) -> Optional[List[PythonResolutionResult.ResolvedDependency]]:
            return self._dependencies

        def with_dependencies(self, dependencies: Optional[List[PythonResolutionResult.ResolvedDependency]]) -> PythonResolutionResult.ResolvedDependency:
            return self if dependencies is self._dependencies else replace(self, _dependencies=dependencies)

    @dataclass(frozen=True, eq=False)
    class Dependency:
        _name: str
        _version_constraint: Optional[str]
        _extras: Optional[List[str]]
        _marker: Optional[str]
        _resolved: Optional[PythonResolutionResult.ResolvedDependency]

        @property
        def name(self) -> str:
            return self._name

        def with_name(self, name: str) -> PythonResolutionResult.Dependency:
            return self if name is self._name else replace(self, _name=name)

        @property
        def version_constraint(self) -> Optional[str]:
            return self._version_constraint

        def with_version_constraint(self, version_constraint: Optional[str]) -> PythonResolutionResult.Dependency:
            return self if version_constraint is self._version_constraint else replace(self, _version_constraint=version_constraint)

        @property
        def extras(self) -> Optional[List[str]]:
            return self._extras

        def with_extras(self, extras: Optional[List[str]]) -> PythonResolutionResult.Dependency:
            return self if extras is self._extras else replace(self, _extras=extras)

        @property
        def marker(self) -> Optional[str]:
            return self._marker

        def with_marker(self, marker: Optional[str]) -> PythonResolutionResult.Dependency:
            return self if marker is self._marker else replace(self, _marker=marker)

        @property
        def resolved(self) -> Optional[PythonResolutionResult.ResolvedDependency]:
            return self._resolved

        def with_resolved(self, resolved: Optional[PythonResolutionResult.ResolvedDependency]) -> PythonResolutionResult.Dependency:
            return self if resolved is self._resolved else replace(self, _resolved=resolved)

    _id: UUID
    _name: Optional[str]
    _version: Optional[str]
    _description: Optional[str]
    _license: Optional[str]
    _path: str
    _requires_python: Optional[str]
    _build_backend: Optional[str]
    _build_requires: List[Dependency]
    _dependencies: List[Dependency]
    _optional_dependencies: Dict[str, List]
    _dependency_groups: Dict[str, List]
    _constraint_dependencies: List[Dependency]
    _override_dependencies: List[Dependency]
    _resolved_dependencies: List[ResolvedDependency]
    _package_manager: Optional[PackageManager]
    _source_indexes: Optional[List[SourceIndex]]

    @property
    def id(self) -> UUID:
        return self._id

    def with_id(self, id_: UUID) -> PythonResolutionResult:
        return self if id_ is self._id else replace(self, _id=id_)

    @property
    def name(self) -> Optional[str]:
        return self._name

    def with_name(self, name: Optional[str]) -> PythonResolutionResult:
        return self if name is self._name else replace(self, _name=name)

    @property
    def version(self) -> Optional[str]:
        return self._version

    def with_version(self, version: Optional[str]) -> PythonResolutionResult:
        return self if version is self._version else replace(self, _version=version)

    @property
    def description(self) -> Optional[str]:
        return self._description

    def with_description(self, description: Optional[str]) -> PythonResolutionResult:
        return self if description is self._description else replace(self, _description=description)

    @property
    def license(self) -> Optional[str]:
        return self._license

    def with_license(self, license: Optional[str]) -> PythonResolutionResult:
        return self if license is self._license else replace(self, _license=license)

    @property
    def path(self) -> str:
        return self._path

    def with_path(self, path: str) -> PythonResolutionResult:
        return self if path is self._path else replace(self, _path=path)

    @property
    def requires_python(self) -> Optional[str]:
        return self._requires_python

    def with_requires_python(self, requires_python: Optional[str]) -> PythonResolutionResult:
        return self if requires_python is self._requires_python else replace(self, _requires_python=requires_python)

    @property
    def build_backend(self) -> Optional[str]:
        return self._build_backend

    def with_build_backend(self, build_backend: Optional[str]) -> PythonResolutionResult:
        return self if build_backend is self._build_backend else replace(self, _build_backend=build_backend)

    @property
    def build_requires(self) -> List[Dependency]:
        return self._build_requires

    def with_build_requires(self, build_requires: List[Dependency]) -> PythonResolutionResult:
        return self if build_requires is self._build_requires else replace(self, _build_requires=build_requires)

    @property
    def dependencies(self) -> List[Dependency]:
        return self._dependencies

    def with_dependencies(self, dependencies: List[Dependency]) -> PythonResolutionResult:
        return self if dependencies is self._dependencies else replace(self, _dependencies=dependencies)

    @property
    def optional_dependencies(self) -> Dict[str, List]:
        return self._optional_dependencies

    def with_optional_dependencies(self, optional_dependencies: Dict[str, List]) -> PythonResolutionResult:
        return self if optional_dependencies is self._optional_dependencies else replace(self, _optional_dependencies=optional_dependencies)

    @property
    def dependency_groups(self) -> Dict[str, List]:
        return self._dependency_groups

    def with_dependency_groups(self, dependency_groups: Dict[str, List]) -> PythonResolutionResult:
        return self if dependency_groups is self._dependency_groups else replace(self, _dependency_groups=dependency_groups)

    @property
    def constraint_dependencies(self) -> List[Dependency]:
        return self._constraint_dependencies

    def with_constraint_dependencies(self, constraint_dependencies: List[Dependency]) -> PythonResolutionResult:
        return self if constraint_dependencies is self._constraint_dependencies else replace(self, _constraint_dependencies=constraint_dependencies)

    @property
    def override_dependencies(self) -> List[Dependency]:
        return self._override_dependencies

    def with_override_dependencies(self, override_dependencies: List[Dependency]) -> PythonResolutionResult:
        return self if override_dependencies is self._override_dependencies else replace(self, _override_dependencies=override_dependencies)

    @property
    def resolved_dependencies(self) -> List[ResolvedDependency]:
        return self._resolved_dependencies

    def with_resolved_dependencies(self, resolved_dependencies: List[ResolvedDependency]) -> PythonResolutionResult:
        return self if resolved_dependencies is self._resolved_dependencies else replace(self, _resolved_dependencies=resolved_dependencies)

    @property
    def package_manager(self) -> Optional[PackageManager]:
        return self._package_manager

    def with_package_manager(self, package_manager: Optional[PackageManager]) -> PythonResolutionResult:
        return self if package_manager is self._package_manager else replace(self, _package_manager=package_manager)

    @property
    def source_indexes(self) -> Optional[List[SourceIndex]]:
        return self._source_indexes

    def with_source_indexes(self, source_indexes: Optional[List[SourceIndex]]) -> PythonResolutionResult:
        return self if source_indexes is self._source_indexes else replace(self, _source_indexes=source_indexes)
