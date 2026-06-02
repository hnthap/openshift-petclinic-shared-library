// vars/runUnitTests.groovy
def call() {
    container('maven') {
        stage('Unit Test') {
            sh 'mvn test -Dmaven.repo.local=/root/.m2/repository'
        }
    }
}
