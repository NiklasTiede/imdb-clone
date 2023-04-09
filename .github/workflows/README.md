
# Current Simple Git Workflow

For the beginning I will just work directly on the
master-branch. CI will be triggered with every
push. And for publishing the docker image I
have to trigger this manually (workflow_dispatch).

There's only one kubernetes 'production' namespace 
on Kubernetes.

# Planned Feature-Branch Git Workflow

In the future, trunk-based development should be introduced.
Feature branches which are connected to issues will be created 
and merged with pull requests after a review process. These 
short-lived branches will enable merging small changes
continuously and keeping master branch as source of truth.

Furthermore, two Kubernetes namespaces (production and 
integration) will be created with a master- and a develop-branch 
so that changes can be thoroughly tested on integration 
environment before it can be released on production.
