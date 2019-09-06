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

package com.wooga.github.changelog.strategies

import com.wooga.github.changelog.changeSet.LabelChangeSet
import com.wooga.github.changelog.DefaultChangeDetector
import com.wooga.github.changelog.FilterUtil
import com.wooga.github.changelog.internal.RepoLayoutPresets
import com.wooga.spock.extensions.github.GithubRepository
import com.wooga.spock.extensions.github.Repository
import com.wooga.spock.extensions.github.api.RateLimitHandlerWait
import com.wooga.spock.extensions.github.api.TravisBuildNumberPostFix
import org.kohsuke.github.GHCommit
import spock.lang.Shared
import spock.lang.Specification

class FilterUtilSpec extends Specification {

    @Shared
    @GithubRepository(
            usernameEnv = "ATLAS_GITHUB_INTEGRATION_USER",
            tokenEnv = "ATLAS_GITHUB_INTEGRATION_PASSWORD",
            resetAfterTestCase = false,
            repositoryNamePrefix = "github-changelog-lib-integration-filter-util",
            repositoryPostFixProvider = TravisBuildNumberPostFix.class,
            rateLimitHandler = RateLimitHandlerWait
    )
    Repository testRepo

    @Shared
    DefaultChangeDetector changeDetector

    @Shared
    List<GHCommit> log

    def setupSpec() {
        /*
        NR
        30      *   Merge branch 'develop' (HEAD -> master, tag: v0.2.0, origin/master)
                |\
        29      | * commit 3 on develop (tag: v0.2.0-rc.2, origin/develop, develop)
        28      | * commit 2 on develop
        27      | * commit 1 on develop
        26      | *   Merge branch 'master' into develop
                | |\
                | |/
                |/|
        25      * |   Merge pull request #3 from fix/two (tag: v0.1.1)
                |\ \
        24      | * | commit 3 on fix/two (origin/fix/two, fix/two)
        23      | * | commit 2 on fix/two
        22      | * | commit 1 on fix/two
                |/ /
        21      | *   Merge pull request #2 from feature/one (tag: v0.2.0-rc.1)
                | |\
                | | |
        20      * | | commit 9
        19      | | * commit 5 on feature/one (origin/feature/one, feature/one)
        18      * | | commit 8
        17      | | * commit 4 on feature/one
        16      * | | commit 7
        15      | | * commit 3 on feature/one
        14      * | | commit 6
        13      | | * commit 2 on feature/one
        12      | | * commit 1 on feature/one
                | |/
                |/
        11      *   Merge pull request #1 from fix/one
                |\
        10      | * commit 5 on fix/one (origin/fix/one, fix/one)
        9       | * commit 4 on fix/one
        8       | * commit 3 on fix/one
        7       | * commit 2 on fix/one
        6       | * commit 1 on fix/one
                |/
        5       * commit 5 (tag: v0.1.0)
        4       * commit 4
        3       * commit 3
        2       * commit 2
        1       * Initial commit
        */
        RepoLayoutPresets.parallelDevelopmentOnTwoBranchesWithPullRequests(testRepo)
        log = testRepo.queryCommits().list().collect()
        changeDetector = new DefaultChangeDetector(testRepo.client, testRepo.repository)
    }

    def "can filter out pull request merge commits"() {
        given: "changes from HEAD to root"
        def changes = changeDetector.detectChangesFromTag(null, null)
        assert changes.logs.size() == 30

        when:
        def r = FilterUtil.filterPRMergeCommits(changes)

        then:
        r.logs.size() == 27
    }

    def "can filter out pull request commits"() {
        given: "changes from HEAD to root"
        def changes = changeDetector.detectChangesFromTag(null, null)
        assert changes.logs.size() == 30

        when:
        def r = FilterUtil.filterPRCommits(changes)

        then:
        r.logs.size() == 17
    }

    def "can chain filters with matching types"(){
        given: "changes from HEAD to root"
        def changes = changeDetector.detectChangesFromTag(null, null)
        assert changes.logs.size() == 30

        when:
        def r = FilterUtil.filterPRMergeCommits(FilterUtil.filterPRCommits(changes))

        then:
        r.logs.size() == 14
    }

    def "can fetch labels for PRs"() {
        given: "changes from HEAD to root"
        def changes = changeDetector.detectChangesFromTag(null, null)
        assert changes.logs.size() == 30

        when:
        def r = FilterUtil.fetchLabels(changes)

        then:
        LabelChangeSet.isInstance(r)
        r.inner == changes
        r.labels.size() == 4
        r.labels.collect {it.name}.containsAll(["feature", "fix", "one", "two"])
    }

    def "can filter labels"() {
        given: "changes from HEAD to root"
        def changes = changeDetector.detectChangesFromTag(null, null)
        assert changes.logs.size() == 30

        and: "a changeset with PR labels"
        changes = FilterUtil.fetchLabels(changes)

        when:
        def r = FilterUtil.filterLabels(changes, ["fix","feature"])

        then:
        r.labels.size() == 2
        r.labels.collect {it.name}.containsAll(["feature", "fix"])
    }

    def "can mix labels filter with other filters"() {
        def changes = changeDetector.detectChangesFromTag(null, null)
        assert changes.logs.size() == 30

        when:
        def first = FilterUtil.fetchLabels(FilterUtil.filterPRCommits(changes))
        def second = FilterUtil.filterPRCommits(FilterUtil.fetchLabels(changes))

        then:
        def firstLogs = first.logs
        def seconfLogs = second.logs

        firstLogs == seconfLogs
        first.labels == second.labels
    }
}
