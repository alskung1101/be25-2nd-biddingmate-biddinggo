$ErrorActionPreference = "Stop"

helm repo add argo https://argoproj.github.io/argo-helm
helm repo update argo

helm upgrade --install argocd argo/argo-cd `
  --namespace argocd `
  --create-namespace `
  --values infra/helm/argocd/values.yaml
