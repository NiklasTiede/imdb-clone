This folder tracks the Kubernetes migration for the home-server deployment.

Target first milestone:

- Ansible bootstraps Ubuntu 24 LTS on `robotnik@um560`.
- k3s runs as a single-node Kubernetes cluster.
- k3s bundled Traefik and local-path storage stay enabled initially.
- Argo CD is installed but not exposed publicly.
- Argo CD watches `infrastructure/clusters/home/apps`.

Bootstrap from the repository root:

```bash
cd infrastructure/ansible
ansible-playbook site.yml
```

Verify the cluster over SSH:

```bash
ssh robotnik@um560
sudo kubectl get nodes
sudo kubectl get pods -A
sudo kubectl get applications -n argocd
```

Open Argo CD privately through SSH port-forwarding:

```bash
ssh -L 8080:localhost:8080 robotnik@um560 \
  "sudo kubectl -n argocd port-forward svc/argocd-server 8080:443"
```

Then open `https://localhost:8080` locally. The initial admin password can be read on the server:

```bash
sudo kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath='{.data.password}' | base64 -d
```
