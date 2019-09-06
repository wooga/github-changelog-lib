github-changelog-lib
====================

A groovy library with utility classes to fetch and generate changelogs directly from github.
This library provides only a minimum set of functionality to generate change logs. The main responsibility of this library is to provide a function to fetch changes between versions directly from github.
There is no need for a local git checkout. The default implementations of this library use `org.kohsuke:github-api` API to access github.
At the moment the types for fields like `logs` (_commits_) and `pullrequests` use types from the underlying library (eg `GHCommit`, `GHPullRequest`).

Usage
-----

The library consists of three main pieces:

 - a `ChangeDetector<B>`: detects changes between tags or commits
 - a `ChangeRenderer<C>`: renders the changelog to a desired output format
 - a `ChangesetMapper<C,B>`: maps the _base_ change-set and retrieves more data or transforms data
 
The whole library works with generics and provides a default implementation with basic data-types. The main idea is that the `detector` returns a set of changes. The `mapper` transforms these and the `renderer` renders the result.
This whole flow can be wrapped into a `strategy`. The library provides a abstract base class `AbstractGeneratorStrategy` for this purpose.

```groovy

// create a change detector object.
// you need to provide a github api client and the repository

def changeDetector = new DefaultChangeDetector(testRepo.client, testRepo.repository)

// create a generator strategy object.
// the base strategy has some fields to customize the internal mapper function.

def generatorStrategy = new DefaultGeneratorStrategy<BaseChangeSet<GHCommmu>>()
generatorStrategy.filterPRCommits = true
generatorStrategy.filterPRMergeCommits = true

//fetch changes
def changes = changeDetector.detectChangesFromTagToHead("1.0.0")
changes.name = "1.1.0"
def changeLog = generatorStrategy.render(changes)

``` 

LICENSE
=======

Copyright 2018 Wooga GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.