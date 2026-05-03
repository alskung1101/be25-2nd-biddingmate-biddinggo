# Biddinggo Local Kubernetes

This directory contains non-secret Kubernetes manifests for the local Biddinggo stack.

Secrets are intentionally not committed. Create them from local values before applying manifests.

## Apply Order

1. Create the namespace.
2. Create Kubernetes Secrets locally.
3. Apply the manifests with Kustomize.
4. Check rollout and health.

```powershell
.\scripts\create-k8s-secrets.ps1
kubectl apply -k k8s
kubectl rollout status deployment/biddinggo-backend -n biddinggo
curl.exe -i http://localhost:30080/actuator/health
```

## Docker Hub / Argo CD

Use the local manifests while testing with Docker Desktop's local image store:

```powershell
docker build -t biddinggo-backend:local .
kubectl apply -k k8s
```

For Docker Hub and Argo CD, use `k8s/dockerhub`.

```powershell
docker build -t alskung/biddinggo-backend:latest .
docker push alskung/biddinggo-backend:latest
kubectl apply -k k8s/dockerhub
```

## Notes

- Backend starts with `replicas: 1` because `AuctionScheduler` currently runs every 5 seconds inside the app. Increase backend replicas after adding a distributed scheduler lock or moving the scheduler to a dedicated worker.
- MariaDB and Redis use StatefulSets with PVCs so data survives Pod recreation in the local cluster.
- The default backend image uses `imagePullPolicy: Never` for local Docker Desktop Kubernetes.
- The `dockerhub` overlay changes the backend image to Docker Hub and sets `imagePullPolicy: IfNotPresent`.
- Backend traffic is routed through the local Nginx Ingress Controller on `http://localhost:30080`.

## Jenkins Credentials

The root `Jenkinsfile` expects these Jenkins credentials:

- `dockerhub-access`: Docker Hub username/password or access token for pushing `alskung/biddinggo-backend`.
- `github-token-biddinggo`: GitHub username/token credential for committing the updated image tag back to this repository.

The Jenkins pipeline uses `alskung/biddinggo-jenkins-agent:latest` as its Kubernetes agent image.
Build and push it before running the pipeline:

```powershell
docker build -t alskung/biddinggo-jenkins-agent:latest -f jenkins-agent/Dockerfile jenkins-agent
docker push alskung/biddinggo-jenkins-agent:latest
```
