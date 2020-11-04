Initial Setup
=============

These instructions are to be run in the `k8s-deploy` directory, where
`Pipfile`, `Dockerfile`, etc. are found.

UNIX-like OS
------------

1. Initialize git submodules:
   ```bash
   $ git pull && git submodule update --init
   ```
1. Install a current version of [Python](https://www.python.org/).
1. Install `pipenv`:
   ```bash
   $ python3 -m pip install --user -U pipenv
   ```
1. Install Python-based tools:
   ```bash
   $ pipenv install
   ```
1. Install a current version of [Go](https://golang.org/)
1. Install kubespray dependencies:
   ```bash
   $ pipenv install -r kubespray/requirements.txt
   ```
1. Enter the pip environment. Due to it's support for the `.env` file, you
   automatically enter an environment in which go packages can be installed.
   ```bash
   $ pipenv shell
   ```
1. Install Go-based tools:
   ```bash
   $ go get $(cat tools/gopackages.txt)
   ```
1. Install kubectl:
  ```bash
  $ mkdir bin
  $ export KUBE_VERSION=$(grep '^kube_version:' kubespray/roles/download/defaults/main.yml | cut -d' ' -f2)
  $ cd bin
  $ curl -LO https://storage.googleapis.com/kubernetes-release/release/${KUBE_VERSION}/bin/linux/amd64/kubectl
  $ chmod +x kubectl
  ```

After this is done, in order to have your tools available, all you need
to do is run `$ pipenv shell` again.

### Prerequisites

- unrestricted internet access

### Upgrading

1. Inside or outside of the pip environment, run `$ pipenv update`
1. If necessary, re-run `$ pip install -r kubespray/requirements.txt`
  inside the pip environment.
1. Inside the pip environment, run `$ go get -u $(cat tools/gopackages.txt)`
1. Re-run the setup steps for installing `kubectl`.

If you want to use a newer version of kubespray:

```bash
$ cd kubespray
$ git checkout <version-tag>
$ cd ..
$ git add kubespray
$ git commit -m "Bumped kubespray to <version-tag>"
```

Docker
------

1. Run `$ docker/build.sh` - the script will reproduce all the above steps in
   a docker image.

After this is done, in order to have your tools available, all you need
to do is run `$ docker/run.sh`. It starts the docker container and enters a
shell within it.

### Prerequisites

- Docker engine running at your control host
- unrestricted internet access

### Upgrading

All configuration and `kubespray` is mapped into the container at run-time,
so if you update either, there is no need to re-build the image. However,
any dependencies installed from `Pipfile`, `gopackages.txt` or
`kubespray/requirements.txt` are installed at build-time. If those change,
it is best to re-build the image.

Verify Environment
------------------

After entering either environment, at least the following tools must be
available:

```bash
$ terraform --version
$ ansible --version
$ openstack --version
$ kubectl
```
