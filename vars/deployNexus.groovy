def call(String registryUrl) {
  // Strip http:// or https:// from the beginning of the URL
  String cleanRegistry = registryUrl.replaceFirst("^https?://", "")

  sh './mvnw package -DskipTests'
  sh "docker build -t ${cleanRegistry}/petclinic:latest ."

  withCredentials([usernamePassword(
    credentialsId: 'nexus-credentials',
    usernameVariable: 'USER',
    passwordVariable: 'PASS'
  )]) {
    sh "echo \$PASS | docker login ${cleanRegistry} -u \$USER --password-stdin"
    sh "docker push ${cleanRegistry}/petclinic:latest"
  }
}
