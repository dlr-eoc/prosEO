storage manager
===============

Component of prosEO architecture which runs within a processing facility, next to
the processing engine (e.g. k8s).

This component is an abstraction around the
actual storage system available at a Processing Facility. It is responsible for
efficiently loading new product data into the storage system, both in single-product
and in bulk-import mode. It can also remove products from the storage system.

## coarse requirements
- offer Endpoints for downloading/uploading produced/input products
- offer "file-store" endpoints for ingesting processed products
- offer "file-retrieve" endpoints for processors/containers
- offer monitoring endpoints
- offer an abstraction of all used storage systems - aka virtual FS

## implementation solutions
- Alluxio (https://docs.alluxio.io)
- Fallback-Approach

## Alluxio (overview)
- creates a virtual distributed FS across processing-engine's worker nodes (alluxio://virtual/path/to/myfile) (https://docs.alluxio.io/os/user/stable/en/advanced/Namespace-Management.html)
- auto-adjusts when doing cluster-scale-outs/scale-ins
- offers tiered storage (mem->ssd->hdd)
- is transparent to the underlying storage systems (e.g. S3); persistence is always in the underlying storage (e.g. S3); common and legacy FS's (e.g. NFS) are usable as so called under-stores
- runs within processing engine, side by side to prosEO processor-containers
- File I/O is done via one or more of REST, JAVA, python APIs
- can act as the prosEO File-System-Cache, by providing often used datasets (e.g. ref-data)
or intermediate-products needed for further processing in fast accessible in-memory|ssd|hdd cache

## Alluxio (implications for prosEO)
- File-I/O inside the processing engine (=inside the alluxio cluster) shall be done via Java-API at the level of the processor-wrapper. (https://docs.alluxio.io/os/user/stable/en/api/FS-API.html)
- File-I/O to and from the processing engine (=from outside the alluxio cluster) shall be done via the Alluxio Proxy REST-API. (https://docs.alluxio.io/os/restdoc/stable/proxy/index.html)
- the Alluxio FS could be mounted via FUSE to allow POSIX-like file ops. --> shall be avoided due bad performance
- alluxio worker & proxy processes shall run on every cluster-node.
- alluxio worker process (=one per cluster-node) shall use mainly SSD-based block storage (e.g. 250GB per node); Usage of RAMDISKS (=alluxio default tier) shall be reduced to e.g. 4GB per worker-node. --> fine-grained placement of files is possible
- In order to achieve high I/O when down/uploading data from outside of the processing engine (via REST), a load balancer in front of the alluxio-proxy endpoints shall be used.
- cluster-node VM's shall be templated/defined by us, not by managed
- all files within a prosEO processing-facility are accessible via the alluxioFS (alluxio://path/to/xyz) nevertheless the files are cached or only stored in the "underFS" --> unique file URI

## Fallback-Approach
- do not create a distributed cache-FS
- rely on S3
- or rely on S3 & NFS, or NFS only

## Fallback-Approach (implications for prosEO)
- self-development of APIs for file I/O from and to outside of the processing engine
- S3 can deliver all required interfaces; myriad of client implementations
- when using self-managed NFS, scaling of cluster-nodes is cumbersome
- usage of managed NFS-solutions from cloud-vendors (OTC, AWS) could be an option
- creation of an virtual/abstract FS above e.g. S3, or NFS (like alluxio does) is challenging

## Discussion
- Alluxio:
  * introduces additional layer of complexity (deployment/config)
  * offers solid APIs; all requirements fulfilled
  * offers a way for maintaining a fast worker-cache
  * unique file URIs
- Fallback:
  * easy to maintain & well established solutions (S3, NFS)
  * creation of single-purpose API's
  * no abstracted view of Storage possible
  * when backing mainly on S3, all requirements are fulfilled

## some refs
- https://software.intel.com/en-us/articles/speed-big-data-analytics-on-the-cloud-with-an-in-memory-data-accelerator
