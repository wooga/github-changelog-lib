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
import com.wooga.github.changelog.changeSet.Labels
import com.wooga.github.changelog.changeSet.Logs
import com.wooga.github.changelog.changeSet.PullRequests
import com.wooga.github.changelog.changeSet.LabelChangeSet
import org.kohsuke.github.GHLabel

class FilterUtil {

    static FILTER_PR_COMMITS = {c ->
        List<String> prCommitsShaList = c.pullRequests.collect {it.listCommits().collect {it.sha}}.flatten() as List<String>
        c.logs = c.logs.findAll {!prCommitsShaList.contains(it.getSHA1())}
    }

    static FILTER_PR_MERGE_COMMITS = {c ->
        List<String> prMergeCommitShaList = c.pullRequests.collect {it.mergeCommitSha}
        c.logs = c.logs.findAll {!prMergeCommitShaList.contains(it.getSHA1())}
    }

    static FETCH_PR_LABELS = {c ->
        List<GHLabel> labels = c.pullRequests.collect {
            it.labels
        }.flatten().unique { GHLabel l1, GHLabel l2 -> l1.name <=> l2.name } as List<GHLabel>
        new LabelChangeSet(c, labels)
    }


    static <C extends PullRequests & Logs> C filterPRCommits(C changes) {
        changes.mutate (FILTER_PR_COMMITS)
        changes
    }

    static <C extends PullRequests & Logs> C filterPRMergeCommits(C changes) {
        changes.mutate (FILTER_PR_MERGE_COMMITS)
        changes
    }

    static <C extends ChangeSet> LabelChangeSet<C, GHLabel> fetchLabels(C changes) {
        changes.map(FETCH_PR_LABELS)
    }

    static <C extends Labels<GHLabel>> C filterLabels(C changes, Iterable<String> labels) {
        changes.mutate {C c->
            def l = c.labels.findAll {labels.contains(it.name)}
            c.labels = l
        }
        changes
    }
}
