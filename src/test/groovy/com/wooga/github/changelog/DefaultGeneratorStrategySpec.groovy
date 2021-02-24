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

import com.wooga.github.changelog.changeSet.BaseChangeSet
import com.wooga.github.changelog.internal.RepoLayoutPresets
import com.wooga.spock.extensions.github.GithubRepository
import com.wooga.spock.extensions.github.Repository
import com.wooga.spock.extensions.github.api.RateLimitHandlerWait
import com.wooga.spock.extensions.github.api.TravisBuildNumberPostFix
import org.kohsuke.github.GHCommit
import org.kohsuke.github.GHPullRequest
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class DefaultGeneratorStrategySpec extends Specification {

    @Shared
    @GithubRepository(
            usernameEnv = "ATLAS_GITHUB_INTEGRATION_USER",
            tokenEnv = "ATLAS_GITHUB_INTEGRATION_PASSWORD",
            resetAfterTestCase = false,
            repositoryNamePrefix = "github-changelog-lib-integration-default-generator-spec",
            repositoryPostFixProvider = TravisBuildNumberPostFix.class,
            rateLimitHandler = RateLimitHandlerWait
    )
    Repository testRepo

    @Shared
    List<GHCommit> log

    @Shared
    ChangeDetector<BaseChangeSet<GHCommit,GHPullRequest>> changeDetector

    @Subject
    DefaultGeneratorStrategy<BaseChangeSet<GHCommit,GHPullRequest>> generatorStrategy

    def setupSpec() {
        /*
        NR
        30      *   Merge branch 'develop' (HEAD -> master, tag: v0.2.0, origin/master)
                |\
        29      | * commit 3 on develop (tag: v0.2.0-rc.2, origin/develop, develop)
        28      | * commit 2 on develop
        27      | * commit 1 on develop
        26      | *   Merge branch repo.defaultBranch.name into develop
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

    @Unroll("strategy generates changelog string #message")
    def "strategy generates changelog string based on default renderer"() {
        given: "a strategy with an default renderer"
        generatorStrategy = new DefaultGeneratorStrategy<BaseChangeSet<GHCommit, GHPullRequest>>()
        generatorStrategy.filterPRCommits = filterPRCommits
        generatorStrategy.filterPRMergeCommits = filterPRMergeCommits

        and: "some raw logs"
        def changes = changeDetector.detectChangesFromTag(from, to)
        changes.name = "Initial Release"
        when:
        def changeLog = generatorStrategy.render(changes)

        then:
        changeLog != null

        and: "contains all PR titles"
        changes.pullRequests.every { changeLog.contains(it.title) }

        and: "commit logs based on filter settings"
        List<String> prCommitsShaList = changes.pullRequests.collect {
            it.listCommits().collect { it.sha }
        }.flatten() as List<String>
        List<GHCommit> prCommits = changes.logs.findAll { prCommitsShaList.contains(it.getSHA1()) }

        List<String> prMergeCommitShaList = changes.pullRequests.collect { it.mergeCommitSha }
        List<GHCommit> prMergeCommits = changes.logs.findAll { prMergeCommitShaList.contains(it.getSHA1()) }
        List<GHCommit> filteredCommits = []

        if (filterPRCommits) {
            filteredCommits += prCommits
        }

        if (filterPRMergeCommits) {
            filteredCommits += prMergeCommits
        }

        def diffCommits = changes.logs - filteredCommits
        filteredCommits.every { !changeLog.contains(it.commitShortInfo.message.readLines().first()) }
        diffCommits.every { changeLog.contains(it.commitShortInfo.message.readLines().first()) }

        where:
        filterPRCommits | filterPRMergeCommits
        false           | true
        false           | false
        true            | false
        true            | true

        message = "${(filterPRMergeCommits || filterPRMergeCommits) ? "with " : "without filtering"}${(filterPRCommits) ? "PR commits filtered" : ""}${(filterPRCommits && filterPRMergeCommits) ? " and " : ""}${(filterPRMergeCommits) ? "PR merge commits filtered" : ""}"
        from = null
        to = null
    }
}
