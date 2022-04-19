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

package com.wooga.github.changelog.render

import com.wooga.github.changelog.changeSet.ChangeSet
import com.wooga.github.changelog.render.markdown.Headline
import com.wooga.github.changelog.render.markdown.HeadlineType
import com.wooga.github.changelog.render.markdown.Link
import org.kohsuke.github.GHCommit
import org.kohsuke.github.GHPullRequest

import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter

class DefaultChangeRenderer<E extends ChangeSet<GHCommit, GHPullRequest>> implements ChangeRenderer<E>, MarkdownRenderer {

    boolean generateInlineLinks = true
    HeadlineType headlineType = HeadlineType.atx

    static Link getUserLink(GHPullRequest pr) {
        new Link("@" + pr.user.login, "https://github.com/" + pr.user.login)
    }

    static Link getUserLink(GHCommit commit) {
        if (commit.author) {
            new Link("@" + commit.author.login, "https://github.com/" + commit.author.login)
        } else {
            new Link(commit.commitShortInfo.author.name, "mailto://" + commit.commitShortInfo.author.email)
        }
    }

    @Override
    String render(E changes) {
        Set<Link> links = new HashSet<Link>()

        new StringBuilder().with {
            SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd")
            append(new Headline("${changes.name} - ${sdf.format(changes.date)}", 1, headlineType))
            if (!changes.pullRequests.empty) {
                append(new Headline("Pull Requests", 2, headlineType))
                changes.pullRequests.each { pr ->
                    def prLink = new Link("#" + pr.number, pr.getHtmlUrl())
                    def userLink = getUserLink(pr)

                    if (!generateInlineLinks) {
                        links.add(prLink)
                        links.add(userLink)
                    }

                    append("* [${prLink.link(generateInlineLinks)}] ${pr.title} ${userLink.link(generateInlineLinks)}\n")


                }
                append("\n")
            }

            if (!changes.logs.empty) {
                append(new Headline("Commits", 2, headlineType))
                changes.logs.each { commit ->
                    def shaShort = commit.getSHA1().substring(0, 8)
                    def commitLink = new Link(shaShort, commit.htmlUrl)
                    def authorLink = getUserLink(commit)

                    if (!generateInlineLinks) {
                        links.add(commitLink)
                        links.add(authorLink)
                    }

                    append("* [${commitLink.link(generateInlineLinks)}] ${commit.commitShortInfo.message.readLines().first()} ${authorLink.link(generateInlineLinks)}\n")
                }
                append("\n")
            }

            append(links.sort({ a, b -> a.text <=> b.text }).collect({ it.reference() }).join("\n"))
            it
        }.toString()
    }

}
