This directory must be populated with a configuration file for kubectl named `kubectl.conf`.

The required file can be copied from the Kubernetes master node after deployment of the Kubernetes cluster. The source
path is `~root/.kube/config` (assuming that Kubernetes was installed under the root user). Note that the IP address of
the server must be set to the actual IP address instead of 127.0.0.1.