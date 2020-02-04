/*
 * Copyright 2020 Wooga GmbH
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
import groovy.transform.ThreadInterrupt
import groovy.transform.TimedInterrupt
import org.kohsuke.github.GHCommit
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.TimeUnit

class FilterPerformanceTest extends Specification {

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

        N      * Merge branch fix/(N-2)/3
            ..........
        8       *   Merge branch fix/2
                |\
        7       * |
        6       | * commit 1 on fix/one
                |/
        5       *   Merge branch fix/1
                |\
        4       * |
        3       | * commit 1 on fix/one
                |/
        2       * commit 1
        1       * Initial commit
        */
        RepoLayoutPresets.worstCaseRepoWithHighBranching(testRepo, 30)
        log = testRepo.queryCommits().list().collect()
        changeDetector = new DefaultChangeDetector(testRepo.client, testRepo.repository)
    }

    @ThreadInterrupt
    @TimedInterrupt(value = 20L)
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    def "detectChangesFromTag should return in under 20 s"() {
        given: "changes from HEAD to root"
        def changes = changeDetector.detectChangesFromTag(null, null)

        expect:
        changes.logs.size() == 92
    }

}
