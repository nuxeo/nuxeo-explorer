# Explorer Helm Template

---

## About

The intent of these Helm templates is to deploy preview environments on PRs or reference branches.

<table>
    <tr>
        <td align="center">:exclamation:</td>
        <td align="center">These templates have not been tested for production.</td>
    </tr>
</table>

You can find here the [preview](./preview) Helm template allowing to deploy a Nuxeo with Explorer features.

The Nuxeo Helm template can be found [there](https://github.com/nuxeo/nuxeo-helm-chart/tree/0.x).

The template uses [exposecontroller](https://github.com/jenkins-x/exposecontroller) to generate Ingress objects.

## Deploy the Stack Locally

The [start_locally.sh](./start_locally.sh) script allows to deploy the preview chart on a local Kubernetes instance.

:warning: This script has been tested on Docker For Mac. It should work on any Kubernetes implementation.

For Linux, see the [MicroK8s Experiment](#MicroK8s-Experiment) section.

### Requirements

Software requirements:

- GNU sed
- envsubst
- docker
- kubectl
- helm 2
- kubernetes cluster
- ingress

Maven + Java 8 are also needed in order to build the Docker images to deploy.

You will also need your username/password to access packages.nuxeo.com and an instance-clid.

### Installation

Once you have a working Kubernetes cluster, you need to install ingress, see [Ingress doc](https://github.com/kubernetes/ingress-nginx/blob/master/docs/deploy/index.md).

#### MacOSX

To install GNU sed:

```bash
brew install gnu-sed
```

To install envsubst:

```bash
brew install gettext
brew link --force gettext
```

To install helm 2:

```bash
brew install helm@2
```

#### Linux

To install envsubst:

```bash
apt-get install gettext-base
```

To install helm 2:

```bash
curl -L https://git.io/get_helm.sh | bash
```

### Usage

> :warning: The build produces a lot of Docker image letfovers.
> Run occasionally:
> ```bash
> docker image prune
> ```

First, you need to compile everything, go to the `nuxeo-explorer` directory and then:

```bash
mvn clean install
```

This will produce the regular Maven artifacts (jars, marketplace package) along with the Docker images below:

- `nuxeo/nuxeo-explorer:${project.version}`: Nuxeo with `plaform-explorer` marketplace package (installed during image boot)

Second, start deployment with Helm by typing:

```bash
helm/start_locally.sh
```

This will deploy the whole stack into the `nuxeo-explorer` namespace.

Finally, check the deployment is ok:

```bash
kubectl -n nuxeo-explorer get pod
```

You should get all pods in the namespace, check their status before using the stack:

```bash
NAME                                 READY   STATUS    RESTARTS   AGE
preview-56df7b5794-26txn             1/1     Running   0          6m
```

Once is ready, Nuxeo is available at: <https://preview-nuxeo-explorer.docker.localhost/nuxeo>

You can compile and deploy everything by running Maven and the script again.

### Advanced Usage

We recommend usage of [k9s](https://github.com/derailed/k9s).

#### Re-deploy Nuxeo

You want to re-deploy Nuxeo pod because you made changes on some of the explorer package module.

You need to compile the sources and package the Docker image:

```bash
mvn install -f modules/pom.xml && mvn install -f package/pom.xml
```

Then restart Nuxeo:

```bash
kubectl -n nuxeo-explorer scale deployment preview --replicas=0
kubectl -n nuxeo-explorer scale deployment preview --replicas=1
```

### Note

Your packages.nuxeo.com login and instance-clid are saved in the `default` namespace to avoid asking you each time.

### MicroK8s Experiment

#### MicroK8s

Install [MicroK8s](https://microk8s.io/#get-started).

If you have an existing Kubernetes configuration, make sure to update the context so that the `kubectl` commands points
to the MicroK8s cluster instead of any other one you could be using.

This can be done by replacing the content of `~/.kube/config` by the output of `microk8s config`.

Enable the required add-ons:

```bash
microk8s enable dns
microk8s enable helm
microk8s enable ingress
```

#### Helm

Make sure the latest version of Helm 2 is installed.

To make sure Tiller is initialized in the K8s cluster, run:

```bash
microk8s helm init
helm repo update
```

Then, check that Helm is properly installed:

```bash
helm version
```

#### Docker Images

You need to make the Docker images locally built available in MicroK8s, following this [section](https://microk8s.io/docs/registry-images#working-with-locally-built-images-without-a-registry)
of the MicroK8s documentation.

List the images built locally:

```bash
docker images
REPOSITORY                                         TAG                 IMAGE ID            CREATED             SIZE
nuxeo/nuxeo-explorer                               20.0.0-SNAPSHOT     3b4f5c228251        6 hours ago         1.85GB
```

Save it and inject it into the MicroK8s image cache:

```bash
docker save 3b4f5c228251 > 3b4f5c228251.tar

microk8s ctr image import 3b4f5c228251.tar
```

We've noticed that the images don't seem to be listed when running:

```bash
microk8s ctr images list
```

Though, they seem to have been properly injected.

#### Script

Fix up the script:

- Replace `helm init --wait >&5` with `helm init >&5`.
