// vars/compileApp.groovy
def call() {
    container('maven') {
        stage('Compile') {
            sh 'mvn clean compile -Dmaven.repo.local=/root/.m2/repository'
        }
    }
}
