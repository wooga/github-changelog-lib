#!groovy
@Library('github.com/wooga/atlas-jenkins-pipeline@1.x') _

withCredentials([usernamePassword(credentialsId: 'github_integration', passwordVariable: 'githubPassword', usernameVariable: 'githubUser'),
                 string(credentialsId: 'github_changelog_lib_coveralls_token', variable: 'coveralls_token')]) {

    def testEnvironment = [
                               "ATLAS_GITHUB_INTEGRATION_USER=${githubUser}",
                               "ATLAS_GITHUB_INTEGRATION_PASSWORD=${githubPassword}"
                          ]

    buildJavaLibrary plaforms: ['linux'], coverallsToken: coveralls_token, testEnvironment: testEnvironment
}
