# 3. OSS contributor guidelines

Date: 2021-05-05

## Status

Accepted

## Context

Provide guidelines for open source contributors to Rewrite projects.

## Decisions

### Backlog project

OSS maintainers should regularly assign new issues to the [Backlog](https://github.com/orgs/openrewrite/projects/4/views/10) project. By default, issues begin in the icebox and are pulled forward to the backlog when work is anticipated soon.

### Milestones

* OSS maintainers should assign issues to the active milestone when the issue is closed or is deemed to be a necessity for the next release. If the active milestone is a patch release and the issue being merged is a new feature, it is the responsibility of the person closing the issue to edit the milestone to be the next minor/major version in sequence.
* When performing a release, use the active milestone as a record for what to write in the release notes and close it. Open a new milestone with the next patch version in sequence.
* PRs should also be attached to milestones when merged whenever they are not associated with an issue.

## Consequences

Best communicate the progress of individual issues and the work we are planning on doing to the community.
