# Argo CD Helm Install

Argo CD is installed with the official Helm chart.

```powershell
.\infra\helm\argocd\install.ps1
```

Local access:

```text
http://localhost:30090
https://localhost:30490
```

Applications are applied separately from:

```text
argocd/biddinggo-application.yaml
../be25-3rd-biddingmate-biddinggo/argocd/biddinggo-frontend-application.yaml
```
