// vars/checkHealth.groovy
def call() {
    stage('Health Check') {
        def isMain      = env.BRANCH_NAME == 'main'
        def envName     = isMain ? 'prod' : 'uat'
        def appResource = "${env.APP_NAME}-${envName}"

        withEnv([
            "NAMESPACE=${env.OCP_NAMESPACE}",
            "APP_RESOURCE=${appResource}"
        ]) {
            sh '''
                set -eu

                echo "Checking route for application: $APP_RESOURCE"

                ROUTE_HOST="$(oc -n "$NAMESPACE" get route "$APP_RESOURCE" -o jsonpath='{.spec.host}')"

                if [ -z "$ROUTE_HOST" ]; then
                    echo "Route host is empty for $APP_RESOURCE"
                    exit 1
                fi
                
                HEALTH_URL="http://${ROUTE_HOST}/actuator/health"

                echo "Waiting before health check..."
                sleep 15

                echo "Checking health endpoint: $HEALTH_URL"
                curl -f "$HEALTH_URL" || {
                    echo "Health check failed: $HEALTH_URL"
                    exit 1
                }
            '''
        }
    }
}
