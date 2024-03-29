# Logging

### Adding services

##### Adding a message class
A new message class should follow the naming scheme: NewServiceMessage.java.

The implementation should be done similarly to one of the existing message classes. Used and available ranges for the message codes are given below. The build process will fail should duplicates occur. You can refer to src/site/logging.html to get a convenient overview of all messages and find the duplicate easily.

Note that an alphabetic order of the enum constants is not necessary. Rather, new constants should be added at the end to ensure a continuous numeration.

##### Adding a test class
Test classes can be implemented similarly to one of the existing test classes.


##### Updating the documentation class
The class logging/documentation/LoggingDocumentation.java needs to be updated to correctly produce the final documentation and find duplicate codes.

Add the new service to the methods addTableOfContents() and addMessages().

#### Updating HttpPrefix
Where necessary, add a new enum constant in logging/http/HttpPrefix.java, to be used in warning headers.

### Ranges
- 1000-1199	FacilityMgr
- 1200-1499
- 1500-1699	Geotools
- 1700-1999	Notification
- 2000-2299	Ingestor
- 2300-2499
- 2500-2699	Model
- 2700-2999	
- 3000-3199	Monitor
- 3200-3499
- 3500-3799	OrderMgr
- 3800-3999	OAuth 
- 4000-4499	Planner
- 4500-4799	ProcessorMgr
- 4800-4999
- 5000-5299	ProductClass Mgr
- 5300-5499	 
- 5500-5799	Storage Mgr
- 5800-5999	
- 6000-6499	UI
- 6500-6799	UserMgr
- 6800-6999	AIP-Client	
- 7000-7199	ESA API Monitor
- 7200-7499	ODIP
- 7500-7699	Product Archive Manager	
- 7700-7999	PRIP
- 8000-8199	
- 8200-8499	
- 8500-8999	
- 9000-9999 General