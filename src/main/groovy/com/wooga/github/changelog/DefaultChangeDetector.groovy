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
import com.wooga.github.changelog.internal.ChangeDetectorException
import org.kohsuke.github.*

import java.text.SimpleDateFormat

class DefaultChangeDetector implements ChangeDetector<BaseChangeSet<GHCommit, GHPullRequest>> {

    final GHRepository hub
    final GitHub client

    DefaultChangeDetector(GitHub client, String repository) {
        this(client, client.getRepository(repository))
    }

    DefaultChangeDetector(GitHub client, GHRepository hub) {
        this.client = client
        this.hub = hub
    }

    @Override
    BaseChangeSet detectChangesFromShaToHead(String from, String branchName = hub.defaultBranch) throws ChangeDetectorException {
        detectChangesFromSha(from, null, branchName)
    }

    @Override
    BaseChangeSet detectChangesFromSha(String from, String to, String branchName = hub.defaultBranch) throws ChangeDetectorException {
        GHCommit fromCommit = null
        GHCommit toCommit

        if (from) {
            fromCommit = commitFromSha(from)
        }

        if (to) {
            toCommit = commitFromSha(to)
        } else {
            toCommit = hub.getCommit(hub.getBranch(branchName).SHA1)
        }

        detectChanges(fromCommit, toCommit, branchName)
    }

    @Override
    BaseChangeSet detectChangesFromTagToHead(String from, String branchName = hub.defaultBranch) throws ChangeDetectorException {
        detectChangesFromTag(from, null, branchName)
    }

    @Override
    BaseChangeSet detectChangesFromTag(String from, String to, String branchName = hub.defaultBranch) throws ChangeDetectorException {
        GHCommit fromCommit = null
        GHCommit toCommit

        if (from) {
            fromCommit = commitFromTag(from)
        }

        if (to) {
            toCommit = commitFromTag(to)
        } else {
            toCommit = hub.getCommit(hub.getBranch(branchName).SHA1)
        }

        detectChanges(fromCommit, toCommit, branchName)
    }

    BaseChangeSet detectChangesToHead(GHCommit from, String branchName = hub.defaultBranch) throws ChangeDetectorException {
        detectChanges(from,null,branchName)
    }

    BaseChangeSet detectChanges(GHCommit from, GHCommit to, String branchName = hub.defaultBranch) throws ChangeDetectorException {
        Date fromDate = (from) ? from.commitDate : null
        Date toDate = to.commitDate
        if (fromDate && toDate && (fromDate > toDate)) {
            throw new ChangeDetectorException("Commit ${from.SHA1} is newer than ${to.SHA1}")
        }

        List<GHCommit> commits = fetchCommits(hub, fromDate, toDate, branchName)
        def startIndex = commits.findIndexOf { it.getSHA1() == to.getSHA1() }
        def endIndex = (from) ? commits.findIndexOf { it.getSHA1() == from.getSHA1() } : commits.size()

        Map<String, GHCommit> commitMap = commits.inject([:]) { Map<String, GHCommit> meta, GHCommit commit ->
            meta[commit.SHA1] = commit
            meta
        }

        if (!isConnectedTo(commits.last(), commits.first(), commitMap)) {
            throw new ChangeDetectorException("Commit ${commits.first().SHA1} and ${commits.last().SHA1} are not connected")
        }

        commits = commits.subList(startIndex, endIndex)
        List<GHPullRequest> pullRequests = []

        if(!commits.empty) {
            assert commits.first().getSHA1() == to.getSHA1()
            commits = filterUnrelatedCommits(commits)
            pullRequests = fetchPullRequestsFromCommits(client, hub, commits, branchName)
        }

        new BaseChangeSet(hub.fullName, from.toString(), to.toString(), commits, pullRequests)
    }

    protected GHCommit commitFromTag(String tagName) {
        try {
            GHRef fromRef = hub.getRef("tags/${tagName}")
            if (fromRef.object.type == "commit") {
                return hub.getCommit(fromRef.object.sha)
            } else {
                GHTagObject fromTag = hub.getTagObject(fromRef.object.sha)
                return hub.getCommit(fromTag.object.sha)
            }

        } catch (GHFileNotFoundException e) {
            throw new ChangeDetectorException("Tag ${tagName} could no be found", e)
        }
    }

    protected GHCommit commitFromSha(String sha) {
        try {
            return hub.getCommit(sha)
        } catch (GHFileNotFoundException e) {
            throw new ChangeDetectorException("Commit with sha ${sha} could no be found", e)
        }
    }

    private static boolean isConnectedTo(GHCommit base, GHCommit head, Map<String, GHCommit> log) {
        if (base.SHA1 == head.SHA1) {
            return true
        }

        List<GHCommit> parentCommits = head.parentSHA1s.inject(new ArrayList<GHCommit>()) { List<GHCommit> memo, String sha ->
            if (log.containsKey(sha)) {
                memo << log.get(sha)
            }
            memo
        }

        if (parentCommits.size() == 0) {
            return false
        }

        return parentCommits.any { isConnectedTo(base, it, log) }
    }

    protected static List<GHCommit> filterUnrelatedCommits(List<GHCommit> commits) {
        GHCommit head = commits.first()
        Map<String, GHCommit> commitMap = commits.inject([:]) { Map<String, GHCommit> meta, GHCommit commit ->
            meta[commit.SHA1] = commit
            meta
        }

        commits = commits.reverse()
        commits = commits.findAll { isConnectedTo(it, head, commitMap) }

        commits.reverse()
    }

    protected static List<GHCommit> fetchCommits(GHRepository hub, Date from, Date to, branchName = hub.defaultBranch) {
        def builder = hub.queryCommits()
        builder.from(branchName)

        if (from) {
            builder.since(githubDate(from))
        }
        if (to) {
            builder.until(githubDate(to))
        }

        builder.list().collect()
    }

    protected static Date githubDate(Date apiDate, long offset = 0) {
        new Date(apiDate.time + offset + apiDate.timezoneOffset * 60 * 1000)
    }

    protected static List<GHPullRequest> fetchPullRequestsFromCommits(GitHub root, GHRepository hub, List<GHCommit> log, String branchName = hub.defaultBranch) {
        if (log.empty) {
            return new ArrayList<GHPullRequest>()
        }

        def searchBuilder = root.searchIssues()
        def formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

        //adjust from and to dates by a few seconds
        def fromData = formatter.format(githubDate(log.last().commitDate, -2000))
        def toDate = formatter.format(githubDate(log.first().commitDate, 2000))

        def logShaList = log.collect { it.getSHA1() }

        searchBuilder.q("repo:${hub.fullName}")
        searchBuilder.q("merged:${fromData}..${toDate}")
        searchBuilder.q("type:pr")

        def mergedPrList = searchBuilder.list()
        mergedPrList = mergedPrList.collect { hub.getPullRequest(it.number) }
        mergedPrList.removeIf({ !logShaList.contains(it.mergeCommitSha) })
        mergedPrList
    }
}


