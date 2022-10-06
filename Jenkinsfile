#!groovy
@Library('github.com/wooga/atlas-jenkins-pipeline@1.x') _

withCredentials([usernamePassword(credentialsId: 'github_integration', passwordVariable: 'githubPassword', usernameVariable: 'githubUser'),
                 string(credentialsId: 'snyk-wooga-frontend-integration-token', variable: 'SNYK_TOKEN')]) {

    def testEnvironment = [
                               "ATLAS_GITHUB_INTEGRATION_USER=${githubUser}",
                               "ATLAS_GITHUB_INTEGRATION_PASSWORD=${githubPassword}"
                          ]

    buildJavaLibraryOSSRH testEnvironment: testEnvironment
}
