// vars/scanSonar.groovy
def call() {
    container('maven') {
        stage('SonarQube Scan') {
            withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                sh """
                    mvn sonar:sonar \
                        -Dsonar.projectKey=${env.serviceName} \
                        -Dsonar.java.binaries=. \
                        -Dsonar.token=\$SONAR_TOKEN \
                        -Dmaven.repo.local=/root/.m2/repository
                """
            }
        }
    }
}
