// vars/scanSonar.groovy
def call() {
    container('maven') {
        stage('SonarQube Scan') {
            withCredentials([string(
                credentialsId: env.SONARQUBE_TOKEN_ID,
                variable: 'SONAR_TOKEN'
            )])
            {
                sh """
                    mvn sonar:sonar \
                        -Dsonar.host.url="${env.SONARQUBE_URL}" \
                        -Dsonar.projectKey="${env.APP_NAME}" \
                        -Dsonar.java.binaries=target/classes \
                        -Dsonar.token="\$SONAR_TOKEN" \
                        -Dmaven.repo.local=/root/.m2/repository
                """
            }
        }
    }
}
