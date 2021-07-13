package de.dlr.proseo.storagemgr.rest;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class InfoControllerImplTest {
	
	@Autowired 
	InfoControllerImpl infoController; 
	

	@Test
	public void testGetRestInfo() {
	//	assertNotNull(infoController);
	}

}
