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

package com.wooga.github.changelog.render.internal

import com.wooga.github.changelog.changeSet.BaseChangeSet
import com.wooga.github.changelog.render.DefaultChangeRenderer
import com.wooga.github.changelog.render.markdown.HeadlineType
import com.wooga.github.changelog.render.markdown.Link
import org.kohsuke.github.GHCommit
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GHUser
import org.kohsuke.github.GitUser
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.text.SimpleDateFormat

class DefaultChangeRendererSpec extends Specification {

    @Subject
    DefaultChangeRenderer<BaseChangeSet> renderer

    @Shared
    BaseChangeSet testChangeset

    @Shared
    List<String> commitMessages

    @Shared
    List<String> pullRequestTitles

    @Shared
    List<String> commitSha

    @Shared
    List<Integer> pullRequestNumbers

    private String repoBaseUrl = "https://github.com/test/"

    def setup() {
        commitMessages = [
                "Setup custom test runner",
                "Improve overall Architecture",
                "Fix new awesome test",
                "Add awesome test",
                "Fix tests in integration spec"
        ]

        pullRequestTitles = [
                "Add custom integration tests",
                "Bugfix in test runner"
        ]
        pullRequestNumbers = [1, 2]
        commitSha = (0..10).collect { "123456${it.toString().padLeft(2, "0")}".padRight(40, "28791583").toString() }


        List<GHCommit> logs = generateMockLog(commitMessages.size())
        List<GHPullRequest> pullRequests = generateMockPR(pullRequestTitles.size())
        testChangeset = new BaseChangeSet("test", null, null, logs, pullRequests)
    }

    GHCommit mockCommit(String commitMessage, String sha1, boolean isGithubUser = true) {
        def user = null
        if (isGithubUser) {
            user = Mock(GHUser)
            user.getLogin() >> "TestUser"
            user.getEmail() >> "test@user.com"
        }

        def gitUser = Mock(GHCommit.GHAuthor)
        gitUser.email >> "test@user.com"
        gitUser.name >> "GitTestUser"

        mockCommit(commitMessage, sha1, user, gitUser)
    }

    GHCommit mockCommit(String commitMessage, String sha1, GHUser user, GHCommit.GHAuthor gitUser) {
        def shortInfo = Mock(GHCommit.ShortInfo)
        shortInfo.getMessage() >> commitMessage
        shortInfo.committer >> gitUser
        shortInfo.author >> gitUser

        def commit = Mock(GHCommit)

        commit.getSHA1() >> sha1
        commit.getAuthor() >> user
        commit.getCommitter() >> user
        commit.getCommitShortInfo() >> shortInfo
        commit.getHtmlUrl() >> new URL(repoBaseUrl + "commit/" + sha1)
        commit
    }

    List<GHCommit> generateMockLog(int count) {
        def list = []
        for (int i = 0; i < count; i++) {
            list.add(mockCommit(commitMessages[i], commitSha[i]))
        }
        list
    }

    List<GHPullRequest> generateMockPR(int count) {
        def list = []
        for (int i = 0; i < count; i++) {
            def user = Mock(GHUser)
            user.getLogin() >> "TestUser"

            def pr = Mock(GHPullRequest)
            pr.getNumber() >> i
            pr.getTitle() >> pullRequestTitles[i]
            pr.getHtmlUrl() >> new URL(repoBaseUrl + "issue/" + i)
            pr.getUser() >> user
            list.add(pr)
        }
        list
    }

    def "renders"() {
        given: "a renderer"
        renderer = new DefaultChangeRenderer<BaseChangeSet>()

        when:
        def result = renderer.render(testChangeset)

        then:
        noExceptionThrown()
        commitMessages.every { result.contains(it.readLines().first()) }
        pullRequestTitles.every { result.contains(it) }
    }

    def "renders release date in form YYYY-MM-dd"() {
        given: "a renderer"
        renderer = new DefaultChangeRenderer<BaseChangeSet>()

        and: "a name and date for the changeset"
        testChangeset.name = releaseName
        testChangeset.date = releaseDate

        when:
        def result = renderer.render(testChangeset)

        then:
        noExceptionThrown()
        result.contains("# ${releaseName} - ${releaseDateString}")

        where:
        releaseName = "TestRelease"
        releaseDate = new Date(0)
        releaseDateString = new SimpleDateFormat("YYYY-MM-dd").format(releaseDate)
    }

    @Unroll
    def "renders #linkTo links #type style"() {
        given: "a renderer"
        renderer = new DefaultChangeRenderer<BaseChangeSet>()

        and: "a link setting"
        renderer.generateInlineLinks = (type == "inline")

        when:
        def result = renderer.render(testChangeset)

        then:
        noExceptionThrown()

        and: "commit links are rendered in correct style"
        if (linkTo == "commit") {
            if (renderer.generateInlineLinks) {
                commitSha.every {
                    def shaShort = it.substring(0, 8)
                    def link = "[[${shaShort}]]"
                    def reference = "[${shaShort}]: https://github.com/test/commit/${it}"
                    result.contains(link) && result.contains(reference)
                }
            } else {
                commitSha.every {
                    def shaShort = it.substring(0, 8)
                    def link = "[[${shaShort}](https://github.com/test/commit/${it})]"
                    result.contains(link)
                }
            }
        }

        if (linkTo == "pull request") {
            and: "pull request links are rendered in correct style"
            if (renderer.generateInlineLinks) {
                pullRequestNumbers.every {
                    def link = "[[#${it}]]"
                    def reference = "[#${it}]: https://github.com/test/issue/${it}"
                    result.contains(link) && result.contains(reference)
                }
            } else {
                pullRequestNumbers.every {
                    def link = "[[#${it}](https://github.com/test/issue/${it})]"
                    result.contains(link)
                }
            }
        }
        where:
        type        | linkTo
        "reference" | "commit"
        "inline"    | "commit"
        "reference" | "pull request"
        "inline"    | "pull request"
    }

    @Unroll(":getUserLink generates #message")
    def ":getUserLink generates author links for commits"() {
        given: "a commit object"
        def commit = mockCommit("sample change", "0123456789ABCDEF0123456789ABCDEF01234567", isGithubAuthor)

        when:
        def link = DefaultChangeRenderer.getUserLink(commit)

        then:
        noExceptionThrown()
        link != null

        where:
        isGithubAuthor | expectedLink
        true           | new Link("@TestUser", "https://github.com/TestUser")
        false          | new Link("GitTestUser", "mailto://test@user.com")
        message = (isGithubAuthor) ? "github author links when author is github user" : "commit author mailto links when author is not a github user"
    }

    @Unroll
    def "renders headlines in #headlineStyle"() {
        given: "a renderer"
        renderer = new DefaultChangeRenderer<BaseChangeSet>()

        and: "headline setting"
        renderer.setHeadlineType(headlineStyle)

        when:
        def result = renderer.render(testChangeset)

        then:
        noExceptionThrown()

        expectedRenderedHeadlines.every { result.contains(it) }

        where:
        headlineStyle       | _
        HeadlineType.atx    | _
        HeadlineType.setext | _

        expectedHeadlines = ["Pull Requests", "Commits"]
        expectedRenderedHeadlines = expectedHeadlines.collect { headline ->
            if (headlineStyle == HeadlineType.atx) {
                "## ${headline}"
            } else {
                headline + "\n" + "".padLeft(headline.length(), "-")
            }
        }
    }
}
