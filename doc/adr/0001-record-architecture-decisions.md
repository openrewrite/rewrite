# 1. Record architecture decisions

Date: 2021-04-05

## Status

Accepted

## Context

We need to record the architectural decisions made on this project.

## Decision

We will use Architecture Decision Records, as [described by Michael Nygard](http://thinkrelevance.com/blog/2011/11/15/documenting-architecture-decisions).

## Consequences

See Michael Nygard's article, linked above. For a lightweight ADR toolset, see Nat Pryce's [adr-tools](https://github.com/npryce/adr-tools).

Once installed: 

1. `export VISUAL=code` Define default visual editor to Visual Studio Code
1. `adr new <topic>` Create a new adr record and open it in the editor
1. `adr generate toc > doc/adr/README.md` Generate the table of contents.
