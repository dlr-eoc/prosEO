package de.dlr.proseo.storagemgr.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.rest.model.General;
import de.dlr.proseo.storagemgr.rest.model.Info;
import de.dlr.proseo.storagemgr.rest.model.K8sNodeMountPoints;

@Component
public class InfoControllerImpl implements InfoController{

	@Autowired
	StorageManagerConfiguration cfg = new StorageManagerConfiguration();
	
	@Override
	public ResponseEntity<Info> getInfo() {
		
		Info response = new Info();
		
		General gen = new General();
		gen.setAlluxioUnderFsMaxPrefixes(Long.valueOf(cfg.getAlluxioUnderFsMaxPrefixes()));
		gen.setAlluxioUnderFsS3Bucket(cfg.getAlluxioUnderFsS3Bucket());
		gen.setAlluxioUnderFsS3BucketEndPoint(cfg.getAlluxioUnderFsS3BucketEndPoint());
		gen.setAlluxioUnderFsS3BucketPrefix(cfg.getAlluxioUnderFsS3BucketPrefix());
		gen.setJoborderBucket(cfg.getJoborderBucket());
		gen.setJoborderBucketPrefix(cfg.getJoborderPrefix());
		gen.setProcFacility(cfg.getProcFacilityName());
		gen.setS3EndPoint(cfg.getS3EndPoint());
		gen.setS3MaxNumberOfBuckets(Long.valueOf(cfg.getS3MaxNumberOfBuckets()));
		response.setGeneral(gen);
		
		K8sNodeMountPoints k8s = new K8sNodeMountPoints();
		k8s.setAlluxioCacheMount(cfg.getAlluxioK8sMountPointCache());
		k8s.setAlluxioFuseMount(cfg.getAlluxioK8sMountPointFuse());
		k8s.setUnregisteredProductsMount(cfg.getUnregisteredProductsK8sMountPoint());
		response.setK8sNodeMountPoints(k8s);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

}
