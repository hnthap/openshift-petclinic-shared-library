// vars/checkHealth.groovy
def call() {
    stage('Health Check') {
        def isMain      = env.BRANCH_NAME == 'main'
        def healthPort  = isMain ? '8183' : '8184'

        sh """
            sleep 15
            curl -f http://${env.APP_TARGET_IP}:${healthPort}/actuator/health || exit 1
        """
    }
}
