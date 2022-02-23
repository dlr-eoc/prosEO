prosEO Storage Manager
======================

The Storage Manager is a component of the prosEO architecture, which runs within a Processing Facility, next to
the prosEO Processing Engine (e.g. Kubernetes).

It is an abstraction around the actual storage system available at a Processing Facility. It is responsible for
efficiently loading new product data into the storage system, both in single-product
and in bulk-import mode. It can also remove products from the storage system.

It also covers the "File System Cache" component, which is an intermediate storage for product files for access by the
data processors, accessible as a POSIX file system.

## Coarse Requirements
- Offer Endpoints for uploading/downloading input products and generated products to/from the Processing Facility
  (by other prosEO components or by external users/systems)
- Offer "file-store" endpoints for ingesting processed products
- Offer "file-retrieve" endpoints for processors/containers
- Offer monitoring endpoints showing the available storage units, their capacity and their usage percentage, and the number of products stored
- Offer an abstraction of all used storage systems - aka virtual FS
- Handle data stored on POSIX file systems or on object storage conforming to the AWS S3 protocol
- Provide maximum performance for processors accessing the stored data (e. g. by using a fast cache, and by ensuring that the
  actual product data is not transferred unnecessarily over wide-area network connections)

## Possible Implementation Solutions
Three design approaches for the implementation of the above requirements have been investigated:
- Use Alluxio (https://docs.alluxio.io) only
- Fallback approach: Manage file systems (POSIX, S3) directly

### Alluxio (Overview)
- Creates a virtual distributed FS across processing-engine's worker nodes (alluxio://virtual/path/to/myfile) (https://docs.alluxio.io/os/user/stable/en/advanced/Namespace-Management.html)
- Auto-adjusts when doing cluster-scale-outs/scale-ins
- Offers tiered storage (memory -> SSD -> hard disk (online) -> object storage (possibly offline))
- Is transparent to the underlying storage systems (e.g. S3); persistence is always in the underlying storage (e.g. S3);
  common and legacy FS's (e.g. NFS) are usable as so called under-stores
- Runs within the Processing Engine, side by side to prosEO processor containers
- File I/O is done via one or more of REST, JAVA, python APIs
- Can act as the prosEO File-System-Cache, by providing often used datasets (e.g. ref-data)
  or intermediate-products needed for further processing in fast accessible in-memory/SSD/hard disk cache

## Use of Alluxio Only (Implications for prosEO)
- File-I/O inside the processing engine (=inside the alluxio cluster) needs to be done via Java-API at the level of the processor-wrapper. (https://docs.alluxio.io/os/user/stable/en/api/FS-API.html)
- File-I/O to and from the processing engine (=from outside the alluxio cluster) needs to be done via the Alluxio Proxy REST-API. (https://docs.alluxio.io/os/restdoc/stable/proxy/index.html)
- The Alluxio FS could be mounted via FUSE to allow POSIX-like file ops. --> shall be avoided due bad performance
- Alluxio worker & proxy processes shall run on every cluster-node.
- Alluxio worker process (=one per cluster-node) shall use mainly SSD-based block storage (e.g. 250GB per node); Usage of RAMDISKS (=alluxio default tier) shall be reduced to e.g. 4GB per worker-node. --> fine-grained placement of files is possible
- In order to achieve high I/O when down/uploading data from outside of the processing engine (via REST), a load balancer in front of the alluxio-proxy endpoints shall be used.
- Cluster-node VMs shall be templated/defined by us, not as a managed service by the cloud provider
- All files within a prosEO Processing Facility are accessible via the alluxioFS (alluxio://path/to/xyz), nevertheless the files are cached or only stored in the "underFS" --> unique file URI

## Fallback Approach
- Do not create a distributed cache-FS
- Rely on
  * S3 only, 
  * S3 and a POSIX file system (NFS, GPFS) in combination, 
  * A POSIX file system (NFS, GPFS) only.

## Fallback Approach (Implications for prosEO)
- Self-development of APIs for file I/O from and to outside of the processing engine
- S3 can deliver all required interfaces; myriad of client implementations
- When using self-managed NFS, scaling of cluster-nodes is cumbersome
- Usage of managed NFS solutions from cloud-vendors (OTC, AWS) could be an option
- Creation of an virtual/abstract FS above e.g. S3, or NFS (like Alluxio does) is challenging

## Discussion
- Alluxio:
  * Introduces additional layer of complexity (deployment/config)
  * Offers solid APIs; all requirements fulfilled
  * Offers a way for maintaining a fast worker-cache
  * Unique file URIs
  * Drawback: Other prosEO components depend on an API defined and managed by a third party
- Fallback:
  * Easy to maintain & well established solutions (S3, NFS)
  * Creation of purpose-tailored custom API
  * When backing mainly on S3, all requirements are fulfilled

## Conclusion
The initial implementation will manage the file systems directly ("fallback approach") to avoid the additional overhead of
working with the Alluxio library.
A future implementation could create a shallow wrapper around Alluxio, thereby removing the dependency on a third-party-managed API, and allowing
for the creation of custom API entry points.

## References and Additional Reading
- <https://docs.alluxio.io>
- <https://software.intel.com/en-us/articles/speed-big-data-analytics-on-the-cloud-with-an-in-memory-data-accelerator>
