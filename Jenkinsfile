pipeline {
    agent {
        kubernetes {
            yaml '''
apiVersion: v1
kind: Pod
spec:
  containers:
    - name: jnlp
      image: alskung/biddinggo-jenkins-agent:latest
      imagePullPolicy: Always
      resources:
        requests:
          cpu: "500m"
          memory: "1Gi"
        limits:
          cpu: "2"
          memory: "2Gi"
      volumeMounts:
        - name: docker-sock
          mountPath: /var/run/docker.sock
  volumes:
    - name: docker-sock
      hostPath:
        path: /var/run/docker.sock
        type: Socket
'''
        }
    }

    parameters {
        string(name: 'DOCKER_IMAGE_VERSION', defaultValue: '', description: 'Docker image tag. Empty value uses the current Git short SHA.')
        booleanParam(name: 'PUSH_IMAGE', defaultValue: true, description: 'Push the built image to Docker Hub.')
        booleanParam(name: 'UPDATE_MANIFEST', defaultValue: true, description: 'Update k8s/dockerhub/kustomization.yaml with the pushed image tag.')
    }

    environment {
        DOCKER_IMAGE = 'alskung/biddinggo-backend'
        DOCKERHUB_CREDENTIALS_ID = 'dockerhub-access'
        GITHUB_CREDENTIALS_ID = 'github-token-biddinggo'
        KUSTOMIZATION_FILE = 'k8s/dockerhub/kustomization.yaml'
        GIT_REPO_URL = 'github.com/alskung1101/be25-2nd-biddingmate-biddinggo.git'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Check Skip CI') {
            steps {
                script {
                    env.SKIP_CI = sh(
                        script: "git log -1 --pretty=%B | grep -qi '\\[skip ci\\]' && echo true || echo false",
                        returnStdout: true
                    ).trim()

                    if (env.SKIP_CI == 'true') {
                        currentBuild.displayName = "#${env.BUILD_NUMBER} skipped"
                        echo "Skip CI commit detected. Build stages will be skipped."
                    }
                }
            }
        }

        stage('Resolve Version') {
            when {
                expression { return env.SKIP_CI != 'true' }
            }
            steps {
                script {
                    def shortSha = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.IMAGE_TAG = params.DOCKER_IMAGE_VERSION?.trim() ? params.DOCKER_IMAGE_VERSION.trim() : shortSha
                    currentBuild.displayName = "#${env.BUILD_NUMBER} ${env.IMAGE_TAG}"
                    echo "Image tag resolved: ${env.IMAGE_TAG}"
                }
            }
        }

        stage('Build App') {
            when {
                expression { return env.SKIP_CI != 'true' }
            }
            steps {
                sh 'chmod +x ./gradlew'
                sh './gradlew clean bootJar -x test'
            }
        }

        stage('Build Docker Image') {
            when {
                expression { return env.SKIP_CI != 'true' }
            }
            steps {
                sh '''
                    docker version
                    docker build \
                      -t ${DOCKER_IMAGE}:${IMAGE_TAG} \
                      -t ${DOCKER_IMAGE}:latest \
                      .
                '''
            }
        }

        stage('Push Docker Image') {
            when {
                expression { return params.PUSH_IMAGE && env.SKIP_CI != 'true' }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: env.DOCKERHUB_CREDENTIALS_ID, usernameVariable: 'DOCKERHUB_USERNAME', passwordVariable: 'DOCKERHUB_PASSWORD')]) {
                    sh '''
                        echo "${DOCKERHUB_PASSWORD}" | docker login -u "${DOCKERHUB_USERNAME}" --password-stdin
                        docker push ${DOCKER_IMAGE}:${IMAGE_TAG}
                        docker push ${DOCKER_IMAGE}:latest
                        docker logout
                    '''
                }
            }
        }

        stage('Update K8s Manifest') {
            when {
                expression { return params.UPDATE_MANIFEST && env.SKIP_CI != 'true' }
            }
            steps {
                script {
                    def manifest = readFile(env.KUSTOMIZATION_FILE)
                    manifest = manifest.replaceAll(/newTag: .*/, "newTag: ${env.IMAGE_TAG}")
                    writeFile(file: env.KUSTOMIZATION_FILE, text: manifest)
                }

                sh 'git diff -- k8s/dockerhub/kustomization.yaml'

                withCredentials([usernamePassword(credentialsId: env.GITHUB_CREDENTIALS_ID, usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD')]) {
                    sh '''
                        if git diff --quiet -- k8s/dockerhub/kustomization.yaml; then
                          echo "No manifest changes to commit."
                          exit 0
                        fi

                        git config user.name "jenkins"
                        git config user.email "jenkins@local"
                        git add k8s/dockerhub/kustomization.yaml
                        git commit -m "배포 이미지 태그 갱신: ${IMAGE_TAG} [skip ci]"

                        BRANCH_NAME="${BRANCH_NAME:-feature/deploy-setup}"
                        git push "https://${GIT_USERNAME}:${GIT_PASSWORD}@${GIT_REPO_URL}" HEAD:${BRANCH_NAME}
                    '''
                }
            }
        }
    }

    post {
        success {
            echo "CI pipeline completed. Image: ${env.DOCKER_IMAGE}:${env.IMAGE_TAG}"
            script {
                if (env.SKIP_CI == 'true') {
                    echo "Discord notification skipped for [skip ci] commit."
                } else {
                    withCredentials([string(credentialsId: 'discord-webhook-url', variable: 'DISCORD_WEBHOOK_URL')]) {
                        sh '''
                            curl -H "Content-Type: application/json" \
                              -d "{\\"content\\":\\"[Jenkins] CI 성공 - Job: ${JOB_NAME} #${BUILD_NUMBER} - Image: ${DOCKER_IMAGE}:${IMAGE_TAG:-unknown} - Branch: ${BRANCH_NAME:-feature/deploy-setup}\\"}" \
                              "${DISCORD_WEBHOOK_URL}"
                        '''
                    }
                }
            }
        }
        failure {
            echo "CI pipeline failed. Check the stage logs above."
            script {
                if (env.SKIP_CI == 'true') {
                    echo "Discord failure notification skipped for [skip ci] commit."
                } else {
                    withCredentials([string(credentialsId: 'discord-webhook-url', variable: 'DISCORD_WEBHOOK_URL')]) {
                        sh '''
                            curl -H "Content-Type: application/json" \
                              -d "{\\"content\\":\\"[Jenkins] CI 실패 - Job: ${JOB_NAME} #${BUILD_NUMBER} - Image: ${DOCKER_IMAGE}:${IMAGE_TAG:-unknown} - Branch: ${BRANCH_NAME:-feature/deploy-setup}\\"}" \
                              "${DISCORD_WEBHOOK_URL}"
                        '''
                    }
                }
            }
        }
    }
}
