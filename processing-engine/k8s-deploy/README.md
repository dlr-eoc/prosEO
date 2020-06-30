Initial Setup
=============

There are two ways you can set up for your k8s deployment: directly on a
UNIX-like operating system, or in a Docker container.

The design rationale either way is to have the most up-to-date tooling
available, without keeping it in the repository. At the same time,
configuration data must be kept in the repository.

The exception to this up-to-date tooling is `kubectl` - that we want in the
version we're deploying kubernetes in.

UNIX-like OS
------------

1. Install a current version of [Python](https://www.python.org/).
1. Install `pipenv`: `$ python3 -m pip install --user -U pipenv`
1. Install Python-based tools via `$ pipenv install`
1. Install a current version of [Go](https://golang.org/)
1. Enter the pip environment. Due to it's support for the `.env` file, you
   automatically enter an environment in which go packages can be installed.
   `$ pipenv shell`
1. Install Go-based tools via `$ go get $(cat gopackages.txt)`

After this is done, in order to have your tools available, all you need
to do is run `$ pipenv shell` again.

### Upgrading

1. Inside or outside of the pip environment, run `$ pipenv update`
1. Inside the pip environment, run `$ go get -u $(cat gopackages.txt)`

Docker
------

1. Run `$ docker build` - the script will reproduce all the above steps in
   a docker image.

After this is done, in order to have your tools available, all you need
to do is run `$ docker shell`. It starts the docker container and enters a
shell within it.
