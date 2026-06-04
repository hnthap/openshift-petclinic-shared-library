// vars/packageApp.groovy
def call() {
    container('maven') {
        stage('Package Application') {
            sh '''
                set -eu

                mvn package \
                    -DskipTests \
                    -Dmaven.repo.local=/root/.m2/repository
            '''
        }
    }
}
