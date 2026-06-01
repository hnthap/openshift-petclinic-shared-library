def call(String branchName, String registryUrl) {
  def port = branchName == 'main' ? '8083' : '8084'
  def envName = branchName == 'main' ? 'prod' : 'uat'
  String cleanRegistry = registryUrl.replaceFirst("^https?://", "")
  
  withCredentials([
    usernamePassword(
      credentialsId: 'nexus-credentials',
      usernameVariable: 'USER',
      passwordVariable: 'PASS'
    ),
    sshUserPrivateKey(
      credentialsId: 'azure-ssh-key',
      keyFileVariable: 'SSH_KEY',
      usernameVariable: 'SSH_USER',
    )
  ]) {
    sh """
    ssh -i \$SSH_KEY -o StrictHostKeyChecking=no \$SSH_USER@${env.APP_TARGET_IP} << EOF
      echo \$PASS | docker login ${cleanRegistry} -u \$USER --password-stdin
      docker rm -f petclinic-${envName} || true
      docker pull ${cleanRegistry}/petclinic:latest
      docker run -d -p ${port}:8080 --name petclinic-${envName} --restart unless-stopped ${cleanRegistry}/petclinic:latest
EOF
    """
  }
}
