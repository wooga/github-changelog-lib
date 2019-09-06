/*
 * Copyright 2019 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.wooga.github.changelog.changeSet

class BaseChangeSet<C,P> implements ChangeSet<C, P> {
    final String from
    final String to

    final String repoName
    final List<C> logs
    final List<P> pullRequests

    String name
    Date date

    BaseChangeSet(String repoName, String from, String to, List<C> logs, List<P> pullRequests) {
        this.repoName = repoName
        this.from = from
        this.to = to
        this.logs = logs ?: new ArrayList<C>()
        this.pullRequests = pullRequests ?: new ArrayList<P>()

        this.name = from
        this.date = new Date()

        if (this.repoName == null) {
            throw new IllegalArgumentException("repoName can not be null")
        }
    }

    @Override
    void setLogs(Iterable<C> logs) {
        this.logs.clear()
        this.logs.addAll(logs)
    }

    @Override
    void setPullRequests(Iterable<P> pullRequests) {
        this.pullRequests.clear()
        this.pullRequests.addAll(pullRequests)
    }

    @Override
    void mutate(Closure mutator) {
        this.with mutator
    }

    @Override
    <M> M map(Closure<M> map) {
        map.delegate = this
        map.resolveStrategy = Closure.DELEGATE_ONLY
        map.call(this)
    }
}
