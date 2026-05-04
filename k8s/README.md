# Biddinggo 로컬 Kubernetes 배포

이 디렉터리는 Biddinggo 로컬 Kubernetes 배포에 필요한 비밀값이 없는 manifest를 관리합니다.

DB 비밀번호, Redis 비밀번호, OAuth 키, JWT secret 같은 값은 Git에 올리지 않습니다. 배포 전에 로컬 `.env` 값을 기준으로 Kubernetes Secret을 먼저 생성해야 합니다.

## 적용 순서

1. `biddinggo` namespace를 생성합니다.
2. 로컬 환경값으로 Kubernetes Secret을 생성합니다.
3. Kustomize manifest를 적용합니다.
4. rollout과 health 상태를 확인합니다.

```powershell
.\scripts\create-k8s-secrets.ps1
kubectl apply -k k8s
kubectl rollout status deployment/biddinggo-backend -n biddinggo
curl.exe -i http://biddinggo.local:30080/actuator/health
```

## Docker Hub / Argo CD

Docker Desktop의 로컬 이미지 저장소로만 테스트할 때는 기본 manifest를 사용합니다.

```powershell
docker build -t biddinggo-backend:local .
kubectl apply -k k8s
```

Jenkins, Docker Hub, Argo CD를 통한 배포에서는 `k8s/dockerhub` overlay를 사용합니다.

```powershell
docker build -t alskung/biddinggo-backend:latest .
docker push alskung/biddinggo-backend:latest
kubectl apply -k k8s/dockerhub
```

## 현재 배포 구조

- 백엔드는 `replicas: 2`로 실행합니다.
- 경매 스케줄러는 Redis 분산락을 사용해서 replica가 2개 이상이어도 중복 실행을 막습니다.
- MariaDB와 Redis는 StatefulSet과 PVC를 사용해서 파드가 재생성되어도 데이터가 유지됩니다.
- 기본 manifest는 Docker Desktop 로컬 이미지용으로 `imagePullPolicy: Never`를 사용합니다.
- `dockerhub` overlay는 Docker Hub 이미지를 사용하고 `imagePullPolicy: IfNotPresent`로 변경합니다.
- 프론트엔드와 백엔드는 로컬 Nginx Ingress Controller를 통해 `http://biddinggo.local:30080`로 접근합니다.

## Jenkins Credentials

루트 `Jenkinsfile`은 Jenkins에 아래 Credentials가 등록되어 있어야 동작합니다.

- `dockerhub-access`: `alskung/biddinggo-backend` 이미지를 push하기 위한 Docker Hub 계정 또는 access token
- `github-token-biddinggo`: Jenkins가 이미지 태그가 바뀐 manifest를 다시 GitHub에 commit/push하기 위한 GitHub 계정/token
- `discord-webhook-url`: Jenkins 빌드 결과를 Discord로 보내기 위한 webhook URL

## Argo CD GitHub 접근

Argo CD는 GitHub Deploy Key 방식으로 이 저장소를 읽습니다.

- GitHub repository Deploy Key에는 public key만 등록합니다.
- private key는 GitHub에 등록하지 않습니다.
- private key는 Argo CD repository Secret에만 저장합니다.

## Jenkins Agent 이미지

Jenkins pipeline은 Kubernetes agent 이미지로 `alskung/biddinggo-jenkins-agent:latest`를 사용합니다.

agent 이미지를 처음 만들거나 수정한 경우에는 아래 명령으로 Docker Hub에 push합니다.

```powershell
docker build -t alskung/biddinggo-jenkins-agent:latest -f jenkins-agent/Dockerfile jenkins-agent
docker push alskung/biddinggo-jenkins-agent:latest
```
