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

package com.wooga.github.changelog

import com.wooga.github.changelog.changeSet.ChangeSet
import com.wooga.github.changelog.render.DefaultChangeRenderer
import groovy.transform.InheritConstructors
import org.kohsuke.github.GHCommit
import org.kohsuke.github.GHPullRequest

import static FilterUtil.filterPRCommits
import static FilterUtil.filterPRMergeCommits

@InheritConstructors
class DefaultGeneratorStrategy<E extends ChangeSet<GHCommit, GHPullRequest>> extends AbstractGeneratorStrategy<E, E> {

    boolean filterPRCommits = false
    boolean filterPRMergeCommits = false

    DefaultGeneratorStrategy() {
        super(new DefaultChangeRenderer())
    }

    E mapChangeSet(E changes) {
        if (filterPRCommits) {
            changes = filterPRCommits(changes)
        }

        if (filterPRMergeCommits) {
            changes = filterPRMergeCommits(changes)
        }

        changes
    }
}
