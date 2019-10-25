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

package com.wooga.github.changelog.internal

import com.wooga.github.changelog.DefaultChangeDetector
import com.wooga.spock.extensions.github.GithubRepository
import com.wooga.spock.extensions.github.Repository
import com.wooga.spock.extensions.github.api.RateLimitHandlerWait
import com.wooga.spock.extensions.github.api.TravisBuildNumberPostFix
import org.kohsuke.github.GHCommit
import org.kohsuke.github.HttpException
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class ChangeDetectorSpec extends Specification {
    @Shared
    @GithubRepository(
            usernameEnv = "ATLAS_GITHUB_INTEGRATION_USER",
            tokenEnv = "ATLAS_GITHUB_INTEGRATION_PASSWORD",
            resetAfterTestCase = false,
            repositoryNamePrefix = "github-changelog-lib-integration-change-detector-spec",
            repositoryPostFixProvider = TravisBuildNumberPostFix.class,
            rateLimitHandler = RateLimitHandlerWait
    )
    Repository testRepo


    @Subject
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
    }

    @Unroll
    def "detect changes from tag #from to tag #to with #changesLogs commits and #changesPRs pull requests"() {
        given: "a detector"
        changeDetector = new DefaultChangeDetector(testRepo.client, testRepo.repository)

        when:
        def changes = changeDetector.detectChangesFromTag(from, to, branch)

        then:
        changes.logs.size() == changesLogs
        changes.pullRequests.size() == changesPRs

        where:
        from          | to            | changesLogs | changesPRs | branch
        null          | null          | 30          | 3          | testRepo.repository.defaultBranch
        null          | null          | 29          | 3          | "develop"
        "v0.1.0"      | null          | 24          | 3          | "develop"
        "v0.1.0"      | null          | 25          | 3          | testRepo.repository.defaultBranch
        "v0.1.0"      | "v0.2.0-rc.1" | 12          | 2          | "develop"
        "v0.2.0-rc.1" | "v0.2.0-rc.2" | 8           | 1          | testRepo.repository.defaultBranch
        "v0.2.0-rc.1" | "v0.2.0-rc.2" | 8           | 1          | "develop"
        "v0.1.0"      | "v0.1.1"      | 14          | 2          | testRepo.repository.defaultBranch
        "v0.1.0"      | "v0.1.1"      | 14          | 2          | "develop"
        "v0.2.0"      | null          | 0           | 0          | testRepo.repository.defaultBranch
    }

    String commitShaForCommit(int commitNumber) {
        if (commitNumber == 0) {
            return null
        } else if (commitNumber >= log.size()) {
            return "df83821329038123"
        }

        log.reverse().get(commitNumber - 1).SHA1.substring(0, 7)
    }

    @Unroll
    def "detect changes from sha from number #fromNumber to sha from number #toNumber with #changesLogs commits and #changesPRs pull requests"() {
        given: "a detector"
        changeDetector = new DefaultChangeDetector(testRepo.client, testRepo.repository)

        and: "a sha based on the from number"
        def from = commitShaForCommit(fromNumber)

        and: "a sha based on the to number"
        def to = commitShaForCommit(toNumber)

        when:
        def changes = changeDetector.detectChangesFromSha(from, to, branch)

        then:
        changes.logs.size() == changesLogs
        changes.pullRequests.size() == changesPRs

        where:
        fromNumber | toNumber | changesLogs | changesPRs | branch
        0          | 0        | 30          | 3          | testRepo.repository.defaultBranch
        0          | 0        | 29          | 3          | "develop"
        5          | 0        | 24          | 3          | "develop"
        5          | 0        | 25          | 3          | testRepo.repository.defaultBranch
        5          | 21       | 12          | 2          | "develop"
        21         | 29       | 8           | 1          | testRepo.repository.defaultBranch
        21         | 29       | 8           | 1          | "develop"
        5          | 25       | 14          | 2          | testRepo.repository.defaultBranch
        5          | 25       | 14          | 2          | "develop"
    }

    @Unroll
    def "mailformed tag parameter `#explaination` will fail with exception #type"() {
        given: "a detector"
        changeDetector = new DefaultChangeDetector(testRepo.client, testRepo.repository)

        when:
        changeDetector.detectChangesFromTag(from, to)

        then:
        def e = thrown(type)
        e.message.matches(messagePattern)

        where:
        from          | to            | explaination                            | type                          | messagePattern
        "v0.2.0-rc.1" | "v0.1.1"      | "from tag is not an ancestor of to tag" | ChangeDetectorException.class | /.* are not connected/
        "v0.1.1"      | "v0.2.0-rc.1" | "from tag is younger than to tag"       | ChangeDetectorException.class | /.* is newer than .*/
        "0.1.1"       | "v0.2.0-rc.1" | "from tag does no exist"                | ChangeDetectorException.class | /Tag 0.1.1 could no be found/
        "v0.1.1"      | "0.2.0-rc.1"  | "to tag does no exist"                  | ChangeDetectorException.class | /Tag 0.2.0-rc.1 could no be found/
    }

    @Unroll
    def "mailformed sha parameter `#explaination` will fail with exception #type"() {
        given: "a detector"
        changeDetector = new DefaultChangeDetector(testRepo.client, testRepo.repository)

        and: "a sha based on the from number"
        def from = commitShaForCommit(fromNumber)

        and: "a sha based on the to number"
        def to = commitShaForCommit(toNumber)

        when:
        changeDetector.detectChangesFromSha(from, to)

        then:
        def e = thrown(type)
        e.message.matches(messagePattern)

        where:
        fromNumber | toNumber | explaination                            | type                          | messagePattern
        21         | 25       | "from sha is not an ancestor of to sha" | ChangeDetectorException.class | /.* are not connected/
        25         | 21       | "from sha is younger than to sha"       | ChangeDetectorException.class | /.* is newer than .*/
        88         | 29       | "from sha does no exist"                | HttpException.class           | /.*No commit found for SHA: .*/
        21         | 90       | "to tag does no exist"                  | HttpException.class           | /.*No commit found for SHA: .*/
    }
}
