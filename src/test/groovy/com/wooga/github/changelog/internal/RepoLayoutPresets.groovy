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

import com.wooga.spock.extensions.github.Repository
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Credentials
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Person
import org.ajoberstar.grgit.operation.MergeOp

class RepoLayoutPresets {


    static void parallelDevelopmentOnTwoBranchesWithPullRequests(Repository repo) {
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

        File localPath = File.createTempDir(repo.ownerName, repo.name)
        localPath.deleteOnExit()

        Credentials credentials = new Credentials(repo.userName, repo.token)
        Grgit git = Grgit.clone(dir: localPath.path, uri: repo.httpTransportUrl, credentials: credentials) as Grgit



        git.checkout(branch: "master")
        List<Commit> log = git.log(includes: ['HEAD'], maxCommits: 1)
        Commit initialCommit = log.first()
        def author = initialCommit.author

        def config = git.getRepository().jgit.repository.config
        config.load()
        config.setString("user", null, "name", author.name)
        config.setString("user", null, "email",author.email)
        config.setBoolean("user", null, "useConfigOnly", true)
        config.save()

        git.commit(message: "commit 2")
        git.commit(message: "commit 3", author: new Person("externalAuthor","external@company.com"))
        git.commit(message: "commit 4")
        git.commit(message: "commit 5")
        git.push(remote: "origin", all: true)
        repo.createRelease("0.1.0", "v0.1.0")

        git.branch.add(name: 'fix/one', startPoint: 'master')
        git.checkout(branch: "fix/one")

        git.commit(message: "commit 1 on fix/one")
        git.commit(message: "commit 2 on fix/one")
        git.commit(message: "commit 3 on fix/one")
        git.commit(message: "commit 4 on fix/one")
        git.commit(message: "commit 5 on fix/one")
        git.push(remote: "origin", all: true)

        def fixOnePr = repo.createPullRequest("A Fix PR 1", "fix/one", "master","")
        fixOnePr.setLabels("fix","one")
        repo.mergePullRequest(fixOnePr)

        git.checkout(branch: "master")
        git.pull(rebase:true, branch: "master")

        git.branch.add(name: 'develop', startPoint: 'master')
        git.branch.add(name: 'feature/one', startPoint: 'develop')

        git.checkout(branch: "feature/one")
        git.commit(message: "commit 1 on feature/one")
        git.checkout(branch: "master")
        git.commit(message: "commit 6")
        git.checkout(branch: "feature/one")
        git.commit(message: "commit 2 on feature/one")
        git.checkout(branch: "master")
        git.commit(message: "commit 7")
        git.checkout(branch: "feature/one")
        git.commit(message: "commit 3 on feature/one")
        git.checkout(branch: "master")
        git.commit(message: "commit 8")
        git.checkout(branch: "feature/one")
        git.commit(message: "commit 4 on feature/one")
        git.checkout(branch: "master")
        git.commit(message: "commit 9")
        git.checkout(branch: "feature/one")
        git.commit(message: "commit 5 on feature/one")
        git.push(remote: "origin", all: true)

        def featureOnePr = repo.createPullRequest("A Test Change PR 1", "feature/one", "develop", "")
        featureOnePr.setLabels("feature","one")
        repo.mergePullRequest(featureOnePr)

        git.checkout(branch: "develop")
        git.pull(rebase:true, branch: "develop")
        repo.createRelease("0.2.0-rc.1", "v0.2.0-rc.1", "develop")

        git.checkout(branch: "master")
        git.pull(rebase:true, branch: "master")

        git.branch.add(name: 'fix/two', startPoint: 'master')
        git.checkout(branch: "fix/two")

        git.commit(message: "commit 1 on fix/two")
        git.commit(message: "commit 2 on fix/two")
        git.commit(message: "commit 3 on fix/two")
        git.push(remote: "origin", all: true)

        def fixTwoPr = repo.createPullRequest("A Fix PR 2", "fix/two", "master","")
        fixTwoPr.setLabels("fix","two")
        repo.mergePullRequest(fixTwoPr)

        git.checkout(branch: "master")
        git.pull(rebase:true, branch: "master")

        repo.createRelease("0.1.1", "v0.1.1")

        git.checkout(branch: "develop")
        git.pull(rebase:true, branch: "develop")
        git.merge(head: "master", mode: MergeOp.Mode.CREATE_COMMIT)

        git.commit(message: "commit 1 on develop")
        git.commit(message: "commit 2 on develop")
        git.commit(message: "commit 3 on develop")

        git.push(remote: "origin", all: true)
        repo.createRelease("0.2.0-rc.2", "v0.2.0-rc.2", "develop")

        git.checkout(branch: "master")
        git.pull(rebase:true, branch: "master")
        git.merge(head: "develop", mode: MergeOp.Mode.CREATE_COMMIT)
        git.push(remote: "origin", all: true)

        repo.createRelease("0.2.0", "v0.2.0")
        git.pull(rebase:true, branch: "master")
        println("")
    }
}
