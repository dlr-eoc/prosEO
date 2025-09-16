# force execution of default interpreter found in PATH
eval 'exec perl -S $0 $*' if 0;

#
# configure_proseo_test_mission.pl
# --------------------------------
#
# Configure prosEO for the prosEO Test Mission (PTM)
#
use 5.016;
use strict;

use Getopt::Long qw(:config pass_through);
use File::Glob qw(:bsd_glob);
use File::Path qw(make_path);
use FileHandle;

#
# -- Default values (same for all objects)
#
my $defaults = {
	processor_is_test => 0,
	processor_min_disk_space => 1024,
	processor_max_time => 0,
	processor_sensing_time_flag => 1,
	task_is_critical => 1,
	task_criticality_level => 10,
	task_number_of_cpus => 3,
	task_breakpoint_file_names => [],
	configuration_product_quality => 'TEST',
	product_class_visibility => 'PUBLIC',
	selection_rule_mode => 'OPER'
};
my $CLI_SCRIPT_NAME = 'cli_script.txt';
my $OUT_FILE_DIR = 'testfiles';

#
# -- Main script
#

#
# -- User management
#
my $ADMIN_USER = 'sysadm';
# The sysadm password must be provided in the working directory in a credentials file

my $USERMGR_USER = 'usermgr';
my $USERMGR_PWD = 'userMgr.456';

my $CONFIG_USER = 'proseo';
my $CONFIG_PWD = 'prosEO.789';

my @groups = (
    {
        name => 'operator',
        authorities => [ 'MISSION_READER', 'PRODUCTCLASS_READER', 'PRODUCT_READER_ALL', 'PROCESSOR_READER',
           'ORDER_READER', 'ORDER_MGR', 'ORDER_PLANNER', 'ORDER_MONITOR', 'FACILITY_READER', 'FACILITY_MONITOR' ]
    },
    {
        name => 'archivist',
        authorities => [ 'MISSION_READER', 'PRODUCTCLASS_READER', 'PRODUCT_READER_ALL', 'PRODUCT_INGESTOR', 'PRODUCT_MGR',
           'FACILITY_READER', 'FACILITY_MONITOR' ]
    },
    {
        name => 'engineer',
        authorities => [ 'MISSION_READER', 'MISSION_MGR', 'PRODUCTCLASS_READER', 'PRODUCTCLASS_MGR', 'PRODUCT_READER_ALL',
           'PROCESSOR_READER', 'PROCESSORCLASS_MGR', 'CONFIGURATION_MGR', 'WORKFLOW_MGR',
           'FACILITY_READER', 'FACILITY_MGR', 'FACILITY_MONITOR', 'ORDER_READER', 'ORDER_MONITOR', 'ARCHIVE_READER', 
           'ARCHIVE_MGR' ]
    },
    {
        name => 'approver',
        authorities => [ 'ORDER_READER', 'ORDER_APPROVER' ]
    },
    {
        name => 'prippublic',
        authorities => [ 'PRIP_USER', 'PRODUCT_READER' ]
    },
    {
        name => 'externalprocessor',
        authorities => [ 'PRIP_USER', 'PRODUCT_READER_RESTRICTED', 'PRODUCT_INGESTOR' ]
    },
    {
        name => 'internalprocessor',
        authorities => [ 'PRODUCT_GENERATOR', 'PRODUCT_INGESTOR', 'JOBSTEP_PROCESSOR']
    }
);
my @users = (
    {
        name => $USERMGR_USER,
        pwd => $USERMGR_PWD,
        authorities => [ 'MISSION_READER', 'USERMGR', 'CLI_USER', 'GUI_USER' ],
        groups => [ ]
    },
    {
        name => $CONFIG_USER,
        pwd => $CONFIG_PWD,
        authorities => [ 'CLI_USER', 'GUI_USER', 'PRIP_USER' ],
        groups => [ 'operator', 'archivist', 'engineer', 'approver' ]
    },
    {
        name => 'sciuserprip',
        pwd => 'sciUserPrip.012',
        authorities => [],
        groups => [ 'prippublic' ]
    },
    {
        name => 'cfidevprip',
        pwd => 'cfiDevPrip.678',
        authorities => [],
        groups => [ 'externalprocessor' ]
    },
    {
        name => 'wrapper',
        pwd => 'ingest&Plan',
        authorities => [],
        groups => [ 'internalprocessor' ]
    }
);

#
# -- Mission basics
#
my $mission = { 
	code => 'PTM', 
	name => 'prosEO Test Mission',
	fileClasses => [ 'TEST', 'OPER' ],
	processingModes => [ 'OPER' ],
	productFileTemplate =>
            'PTM_${fileClass}_${productClass.productType}_' .
            '${T(java.time.format.DateTimeFormatter).ofPattern(\\"uuuuMMdd\'T\'HHmmss\\").withZone(T(java.time.ZoneId).of(\\"UTC\\")).format(sensingStartTime)}_' .
            '${T(java.time.format.DateTimeFormatter).ofPattern(\\"uuuuMMdd\'T\'HHmmss\\").withZone(T(java.time.ZoneId).of(\\"UTC\\")).format(sensingStopTime)}_' .
            '${(new java.text.DecimalFormat(\\"00000\\")).format(null == orbit ? 0 : orbit.orbitNumber)}_' .
            '${parameters.get(\\"revision\\").getParameterValue()}_' .
            '${configuredProcessor.processor.processorVersion.replaceAll(\\"\\\\\\\\.\\", \\"\\")}_' .
            '${T(java.time.format.DateTimeFormatter).ofPattern(\\"uuuuMMdd\'T\'HHmmss\\").withZone(T(java.time.ZoneId).of(\\"UTC\\")).format(generationTime)}.nc',
    processingCentre => 'PTM-PDGS',
    productRetentionPeriod => 30 * 24 * 3600
};
my $spacecraft = { code => 'PTS', name => 'prosEO Test Satellite', payloadName => 'PTS-SENSOR', payloadDescription => 'Super sensor for PTM' };
my @orbits = (
    { orbitNumber => 3000, startTime => '2019-11-04T09:00:00.200000', stopTime => '2019-11-04T10:41:10.300000' },
    { orbitNumber => 3001, startTime => '2019-11-04T10:41:10.300000', stopTime => '2019-11-04T12:22:20.400000' },
    { orbitNumber => 3002, startTime => '2019-11-04T12:22:20.400000', stopTime => '2019-11-04T14:03:30.500000' },
    { orbitNumber => 3003, startTime => '2019-11-04T14:03:30.500000', stopTime => '2019-11-04T15:44:40.600000' },
    { orbitNumber => 3004, startTime => '2019-11-04T15:44:40.600000', stopTime => '2019-11-04T17:25:50.700000' },
    { orbitNumber => 3005, startTime => '2019-11-04T17:25:50.700000', stopTime => '2019-11-04T19:07:00.800000' }
);

#
# -- Processor classes, versions, configurations and workflows
#
my @processor_classes = ( 'PTML1B', 'PTML2', 'PTML3' );
my @processors = (
    {
    	processorName => 'PTML1B', 
    	processorVersion => '1.2.0',
    	configuredProcessors => [ 'PTML1B_1.2.0_OPER_2020-03-25' ],
    	tasks => [ 
    	   { taskName => 'ptm_l01b', taskVersion => '1.2.0' }
    	],
    	dockerImage => 'localhost:5000/proseo-sample-wrapper:1.2.0'
    },
    {
        processorName => 'PTML2', 
        processorVersion => '1.2.0',
        configuredProcessors => [ 'PTML2_1.2.0_OPER_2020-03-25' ],
        tasks => [ 
           { taskName => 'ptm_l2', taskVersion => '1.2.0' }
        ],
        dockerImage => 'localhost:5000/proseo-sample-wrapper:1.2.0'
    },
    {
        processorName => 'PTML3', 
        processorVersion => '1.2.0',
        configuredProcessors => [ 'PTML3_1.2.0_OPER_2020-03-25' ],
        tasks => [ 
           { taskName => 'ptm_l3', taskVersion => '1.2.0' }
        ],
        dockerImage => 'localhost:5000/proseo-sample-wrapper:1.2.0'
    }
);
my @configurations = (
    { 
    	processorName => 'PTML1B', 
    	configurationVersion => 'OPER_2020-03-25', 
    	mode => 'OPER',
    	dynProcParameters => [ 
    	   { key => 'logging.root', parameterType => 'STRING', parameterValue => 'notice' },
           { key => 'logging.dumplog', parameterType => 'STRING', parameterValue => 'null' },
           { key => 'Threads', parameterType => 'INTEGER', parameterValue => 16 },
           { key => 'Processing_Mode', parameterType => 'STRING', parameterValue => 'OPER' }
    	],
    	configurationFiles => [],
    	staticInputFiles => [
    	   { fileType => 'processing_configuration', fileNameType => 'Physical', fileNames => [ '/usr/share/sample-processor/conf/ptm_l1b_config.xml' ] },
    	],
    	configuredProcessors => [ 'PTML1B_1.2.0_OPER_2020-03-25' ]
    },
    { 
    	processorName => 'PTML2', 
    	configurationVersion => 'OPER_2020-03-25', 
        mode => 'OPER',
        dynProcParameters => [ 
           { key => 'logging.root', parameterType => 'STRING', parameterValue => 'notice' },
           { key => 'logging.dumplog', parameterType => 'STRING', parameterValue => 'null' },
           { key => 'Threads', parameterType => 'INTEGER', parameterValue => 10 },
           { key => 'Processing_Mode', parameterType => 'STRING', parameterValue => 'OPER' }
        ],
        configurationFiles => [
            { fileVersion => '1.0', fileName => '/usr/share/sample-processor/conf/ptm_l2_config.xml' }
        ],
        staticInputFiles => [],
        configuredProcessors => [ 'PTML2_1.2.0_OPER_2020-03-25' ]
    },
    { 
    	processorName => 'PTML3', 
    	configurationVersion => 'OPER_2020-03-25', 
        mode => 'OPER',
        dynProcParameters => [ 
           { key => 'logging.root', parameterType => 'STRING', parameterValue => 'notice' },
           { key => 'logging.dumplog', parameterType => 'STRING', parameterValue => 'null' },
           { key => 'Threads', parameterType => 'INTEGER', parameterValue => 16 },
           { key => 'Processing_Mode', parameterType => 'STRING', parameterValue => 'OPER' }
        ],
        configurationFiles => [],
        staticInputFiles => [],
        configuredProcessors => [ 'PTML3_1.2.0_OPER_2020-03-25' ]
    }
);
my @configured_processors = (
    {
    	identifier => 'PTML1B_1.2.0_OPER_2020-03-25',
    	processorName => 'PTML1B',
    	processorVersion => '1.2.0',
    	configurationVersion => 'OPER_2020-03-25' 
    },
    {
        identifier => 'PTML2_1.2.0_OPER_2020-03-25',
        processorName => 'PTML2',
        processorVersion => '1.2.0',
        configurationVersion => 'OPER_2020-03-25' 
    },
    {
        identifier => 'PTML3_1.2.0_OPER_2020-03-25',
        processorName => 'PTML3',
        processorVersion => '1.2.0',
        configurationVersion => 'OPER_2020-03-25' 
    }
);
my @workflows = (
    {
    	name => 'PTML2-to-L3',
    	uuid => 'd4466bcb-3256-416a-9d9c-dc86592e43bb',
    	description => 'Create level 3 products from level 2',
    	workflowVersion => '1.0',
    	enabled => 'true',
    	inputProductClass => 'PTM_L2_A',
    	outputProductClass => 'PTM_L3',
    	configuredProcessor => 'PTML3_1.2.0_OPER_2020-03-25',
    	outputFileClass => 'TEST',
    	processingMode => 'OPER',
    	slicingType => 'NONE',
    	workflowOptions => [
    	   {
    	       name => 'Threads',
    	       optionType => 'number',
    	       defaultValue => 16,
    	       valueRange => []
    	   }
    	],
    	outputParameters => [
	        {
	            key => 'revision',
	            parameterType => 'INTEGER',
	            parameterValue => 99
	        }
    	]
    }
);

#
# -- Selection rules (OFFL mode only!)
#
my @product_types;
my %visibilities;
my %enclosing_product_types;
my %processing_levels;
my %product_processor_class;
my %slicing_types;
my %slice_durations;
my %selection_rules;
my %applicable_processors;

# Product types without processor
push @product_types, 'PTM_L0';
$visibilities{'PTM_L0'} = 'RESTRICTED';
push @product_types, 'AUX_IERS_B';
$visibilities{'AUX_IERS_B'} = 'INTERNAL';
    
# Selection rule for PTM L1B
# Expected time coverage of the L1B products is on orbit
push @product_types, 'PTM_L1B';
push @product_types, 'PTM_L1B_P1';
push @product_types, 'PTM_L1B_P2';
$enclosing_product_types{'PTM_L1B_P1'} = 'PTM_L1B';
$enclosing_product_types{'PTM_L1B_P2'} = 'PTM_L1B';
$processing_levels{'PTM_L1B'} = 'L1B';
$processing_levels{'PTM_L1B_P1'} = 'L1B';
$processing_levels{'PTM_L1B_P2'} = 'L1B';
$slicing_types{'PTM_L1B'} = 'ORBIT';
$product_processor_class{'PTM_L1B'} = 'PTML1B';
# Output L1B
$selection_rules{'PTM_L1B'} = '
    FOR PTM_L0 SELECT ValIntersect(0, 0);
    FOR AUX_IERS_B SELECT LatestValIntersect(60 D, 60 D)';
$applicable_processors{'PTM_L1B'} = [ 'PTML1B_1.2.0_OPER_2020-03-25' ];

# Selection rules for PTM L2
# Expected time coverage of the L2 products is on orbit (same as for the L1B product)

# Output PTM_L2_A
push @product_types, 'PTM_L2_A';
$processing_levels{'PTM_L2_A'} = 'L2A';
$slicing_types{'PTM_L2_A'} = 'ORBIT';
$product_processor_class{'PTM_L2_A'} = 'PTML2';
$selection_rules{'PTM_L2_A'} = '
    FOR PTM_L1B SELECT LatestValCover(0, 0)';
$applicable_processors{'PTM_L2_A'} = [ 'PTML2_1.2.0_OPER_2020-03-25' ];
# Output PTM_L2_B
push @product_types, 'PTM_L2_B';
$processing_levels{'PTM_L2_B'} = 'L2B';
$slicing_types{'PTM_L2_B'} = 'ORBIT';
$product_processor_class{'PTM_L2_B'} = 'PTML2';
$selection_rules{'PTM_L2_B'} = '
    FOR PTM_L1B_P1 SELECT LatestValCover(0, 0)';
$applicable_processors{'PTM_L2_B'} = [ 'PTML2_1.2.0_OPER_2020-03-25' ];

# Selection rules for PTM L3
# Expected time coverage of the L3 products is 4 hours
# Output PTM_L3
push @product_types, 'PTM_L3';
$processing_levels{'PTM_L3'} = 'L3';
$slicing_types{'PTM_L3'} = 'TIME_SLICE';
$slice_durations{'PTM_L3'} = 14400;
$product_processor_class{'PTM_L3'} = 'PTML3';
$selection_rules{'PTM_L3'} = '
    FOR PTM_L2_A SELECT ValIntersect(0, 0) MINCOVER(90);
    FOR PTM_L2_B SELECT ValIntersect(0, 0) MINCOVER(90)';
$applicable_processors{'PTM_L3'} = [ 'PTML3_1.2.0_OPER_2020-03-25' ];


# --- Output creation script ---

# Ensure output file directory exists
if ( ! -e $OUT_FILE_DIR ) {
	make_path( $OUT_FILE_DIR );
}

# Create output file for CLI script
say '... starting CLI script';
my $cli_script = FileHandle->new( $CLI_SCRIPT_NAME, 'w' ) or error_die( 'Cannot open script file ' . $CLI_SCRIPT_NAME );

# Login as admin user
print $cli_script "login -i$ADMIN_USER.cred\n";

# Output mission
say '... creating mission';
my $filename = $OUT_FILE_DIR . "/" . $mission->{code} . '.json';
my $fh = FileHandle->new( $filename, 'w' ) or error_die( 'Cannot open output file ' . $filename );
    
print $fh "{\n";
print $fh '    "code": "' . $mission->{code} . '",' . "\n";
print $fh '    "name": "' . $mission->{name} . '",' . "\n";
print $fh '    "fileClasses": [ ';
my $first = 1;
foreach my $file_class ( @{ $mission->{fileClasses} } ) {
    if ( $first ) {
        $first = 0;
    }
    else {
        print $fh ', ';
    }
    print $fh '"' . $file_class . '"';
}
print $fh ' ],' . "\n";
print $fh '    "processingModes": [ ';
$first = 1;
foreach my $processing_mode ( @{ $mission->{processingModes} } ) {
    if ( $first ) {
        $first = 0;
    }
    else {
        print $fh ', ';
    }
    print $fh '"' . $processing_mode . '"';
}
print $fh ' ],' . "\n";
print $fh '    "productFileTemplate": "' . $mission->{productFileTemplate} . '",' . "\n";
print $fh '    "processingCentre": "' . $mission->{processingCentre} . '",' . "\n";
print $fh '    "productRetentionPeriod": ' . $mission->{productRetentionPeriod} . ',' . "\n";
print $fh '    "spacecrafts": [ {' . "\n";
print $fh '        "code": "' . $spacecraft->{code} . '",' . "\n";
print $fh '        "name": "' . $spacecraft->{name} . '",' . "\n";
print $fh '        "payloads": [ {' . "\n";
print $fh '            "name": "' . $spacecraft->{payloadName} . '",' . "\n";
print $fh '            "description": "' . $spacecraft->{payloadDescription} . '"' . "\n";
print $fh '        } ]' . "\n";
print $fh '    } ]' . "\n";
print $fh '}' . "\n";
$fh->close();
print $cli_script 'mission create --file=' . $filename . "\n";

# Create first mission users
say '... creating mission users';

foreach my $user ( @users ) {
    my $credential_file_name = $OUT_FILE_DIR . '/' . $user->{name} . '.cred';
    my $credential_file = FileHandle->new( $credential_file_name, 'w' ) or error_die( 'Cannot create credentials file for ' . $user->{name} );
    print $credential_file $user->{name} . "\n";
    print $credential_file $user->{pwd} . "\n";
    close $credential_file;
    chmod 0600, $credential_file_name;
    
    if ( $USERMGR_USER eq $user->{name} ) {
       print $cli_script 'user create  --mission=' . $mission->{code} . ' --identFile=' . $credential_file_name . ' ' . $user->{name};
    }
    else {
        print $cli_script 'user create --identFile=' . $credential_file_name . ' ' . $user->{name};
    }
    $first = 1;
    foreach my $authority ( @{ $user->{authorities} } ) {
        if ( $first ) {
            $first = 0;
            print $cli_script ' authorities=';
        }
        else {
            print $cli_script ',';
        }
        print $cli_script 'ROLE_' . $authority;
    }
    print $cli_script "\n";
    
    if ( $USERMGR_USER eq $user->{name} ) {
       print $cli_script "login --identFile=$OUT_FILE_DIR/$USERMGR_USER.cred $mission->{code}\n";
    }
}
foreach my $group ( @groups ) {
    print $cli_script 'group create ' . $group->{name} . "\n";
    foreach my $authority ( @{ $group->{authorities} } ) {
        print $cli_script 'group grant ' . $group->{name} . ' ROLE_' . $authority . "\n";
    }
}
foreach my $user ( @users ) {
    foreach my $group ( @{ $user->{groups} } ) {
        print $cli_script 'group add ' . $group . ' ' . $user->{name} . "\n";
    }
}

# Perform the remaining configuration steps as regular user
my $credential_file = FileHandle->new( $OUT_FILE_DIR . '/' . $CONFIG_USER . '.cred', 'w' ) or error_die( 'Cannot create credentials file for ' . $CONFIG_USER );
print $credential_file $CONFIG_USER . "\n";
print $credential_file $CONFIG_PWD . "\n";
close $credential_file;
chmod 0600, $OUT_FILE_DIR . '/' . $CONFIG_USER . '.cred';

print $cli_script 'login -i' . $OUT_FILE_DIR . '/' . $CONFIG_USER . '.cred '. $mission->{code} . "\n";

# Create spacecraft orbits
say '... creating spacecraft orbits';
$filename = $OUT_FILE_DIR . "/" . $mission->{code} . '_orbits.json';
$fh = FileHandle->new( $filename, 'w' ) or error_die( 'Cannot open output file ' . $filename );

print $fh '[' . "\n";
$first = 1;
foreach my $orbit ( @orbits ) {
	if ( $first ) {
		$first = 0;
	}
	else {
		print $fh ",\n";
	}
	print $fh '    { "missionCode": "' . $mission->{code} . '", "spacecraftCode": "' . $spacecraft->{code} . '", "orbitNumber": ' , $orbit->{orbitNumber} . ', "startTime": "' . $orbit->{startTime} . '", "stopTime": "' . $orbit->{stopTime} . '" }';
}
print $fh "\n]\n";
$fh->close();
print $cli_script 'orbit create --file=' . $filename . "\n";

# Create processor classes
say '... creating processor classes';
foreach my $processor_class ( @processor_classes ) {
	$filename = $OUT_FILE_DIR . "/" . $mission->{code} . '_' . $processor_class . '.json';
	$fh = FileHandle->new( $filename, 'w' ) or error_die( 'Cannot open output file ' . $filename );

	print $fh '{ "missionCode": "' . $mission->{code} . '", "processorName": "' . $processor_class . '", "productClasses": [] }' . "\n";

	$fh->close();
    print $cli_script 'processor class create --file=' . $filename . "\n";
}

# Create processor versions
say '... creating processor versions';
foreach my $processor ( @processors ) {
    $filename = $OUT_FILE_DIR . "/" . $mission->{code} . '_' . $processor->{processorName} . '_' . $processor->{processorVersion} . '.json';
    $fh = FileHandle->new( $filename, 'w' ) or error_die( 'Cannot open output file ' . $filename );

	print $fh '{ "missionCode": "' . $mission->{code} . '", "processorName": "' . $processor->{processorName} . '", "processorVersion": "' . $processor->{processorVersion} . '", ';
	print $fh '"isTest": ' . ( $defaults->{processor_is_test} == 0 ? 'false' : 'true' ) . ', "minDiskSpace": ' . $defaults->{processor_min_disk_space} . ', ';
	print $fh '"maxTime": ' . $defaults->{processor_max_time} . ', "sensingTimeFlag": ' . ( $defaults->{processor_sensing_time_flag} ? 'true' : 'false' ) . ', ';
	print $fh '"configuredProcessors": [ ';
	print $fh ' ], "tasks": [ ';
	my $innerfirst = 1;
	foreach my $task ( @{ $processor->{tasks} }) {
        if ( $innerfirst ) {
            $innerfirst = 0;
        }
        else {
            print $fh ', ';
        }
		print $fh '{ "taskName": "' . $task->{taskName} . '", "taskVersion": "' . $task->{taskVersion} . '", ';
		print $fh '"isCritical": ' . ( $defaults->{task_is_critical} ? 'true' : 'false' ) . ', ';
		print $fh '"criticalityLevel": ' . $defaults->{task_criticality_level} . ', "numberOfCpus": ' . $defaults->{task_number_of_cpus} . ', ';
		print $fh '"breakpointFileNames": [ ';
        my $innerinnerfirst = 1;
        foreach my $breakpointFileName ( @{ $defaults->{task_breakpoint_file_names} } )	{
        	if ( $innerinnerfirst ) {
        		$innerinnerfirst = 0;
        	}
        	else {
        		print $fh ', ';
        	}
        	print $fh '"' . $breakpointFileName . '"';
        }
        print $fh ' ] }';
	}
	print $fh ' ], "dockerImage": "' . $processor->{dockerImage} . '" }';

    $fh->close();
    print $cli_script 'processor create --file=' . $filename . "\n";
}

# Create configurations
say '... creating configurations';
foreach my $configuration ( @configurations ) {
    $filename = $OUT_FILE_DIR . "/" . $mission->{code} . '_' . $configuration->{processorName} . '_conf_' . $configuration->{configurationVersion} . '.json';
    $fh = FileHandle->new( $filename, 'w' ) or error_die( 'Cannot open output file ' . $filename );

    print $fh '{ "missionCode": "' . $mission->{code} 
        . '", "processorName": "' . $configuration->{processorName} 
        . '", "configurationVersion": "' . $configuration->{configurationVersion} 
        . ( $configuration->{mode} ? '", "mode": "' .$configuration->{mode} : '' )
        . '", "productQuality": "' . $defaults->{configuration_product_quality} 
        . '", "dynProcParameters": [ ';
    my $innerfirst = 1;
    foreach my $dynProcParam ( @{ $configuration->{dynProcParameters} } ) {
        if ( $innerfirst ) {
            $innerfirst = 0;
        }
        else {
            print $fh ', ';
        }
        print $fh '{ "key": "' . $dynProcParam->{key} . '", "parameterType": "' , $dynProcParam->{parameterType} . '", "parameterValue": "' . $dynProcParam->{parameterValue} . '" }';
    }
    print $fh ' ], "configurationFiles": [ ';
    $innerfirst = 1;
    foreach my $configurationFile ( @{ $configuration->{configurationFiles} } ) {
        if ( $innerfirst ) {
            $innerfirst = 0;
        }
        else {
            print $fh ', ';
        }
        print $fh ' { "fileVersion": "'. $configurationFile->{fileVersion} . '", "fileName": "' . $configurationFile->{fileName} . '" }';
    }
    print $fh ' ], "staticInputFiles": [ ';
    $innerfirst = 1;
    foreach my $staticInputFile ( @{ $configuration->{staticInputFiles} } ) {
        if ( $innerfirst ) {
            $innerfirst = 0;
        }
        else {
            print $fh ', ';
        }
        print $fh ' { "fileType": "'. $staticInputFile->{fileType} . '", "fileNameType": "' . $staticInputFile->{fileNameType} . '", "fileNames": [ ';
        my $innerinnerfirst = 1;
        foreach my $staticInputFileName ( @{ $staticInputFile->{fileNames} } ) {
            if ( $innerinnerfirst ) {
                $innerinnerfirst = 0;
            }
            else {
                print $fh ', ';
            }
            print $fh '"' . $staticInputFileName . '"';
        }
        print $fh ' ] }';
    }
    print $fh ' ], "configuredProcessors": [ ] }' . "\n";
    
    $fh->close();
    print $cli_script 'configuration create --file=' . $filename . "\n";
}

# Create configured processors
say '... creating configured processors';
foreach my $configured_processor ( @configured_processors ) {
	print $cli_script 'processor configuration create ' 
	    . $configured_processor->{identifier} . ' ' 
	    . $configured_processor->{processorName} . ' '
        . $configured_processor->{processorVersion} . ' ' 
        . $configured_processor->{configurationVersion} . "\n";
}

# Create product classes
say '... creating product classes';

foreach my $product_class ( @product_types ) {
    $filename = $OUT_FILE_DIR . "/" . $mission->{code} . '_' . $product_class . '.json';
    $fh = FileHandle->new( $filename, 'w' ) or error_die( 'Cannot open output file ' . $filename );

	print $fh '{ ';
	print $fh '"missionCode": "' . $mission->{code} . '"';
	print $fh ', "productType": "' . $product_class . '"';
    if ( $processing_levels{$product_class} ) {
        print $fh ', "processingLevel": "' . $processing_levels{$product_class} . '"';
    }
    if ( $visibilities{$product_class} ) {
        print $fh ', "visibility": "' . $visibilities{$product_class} . '"';
    } else {
        print $fh ', "visibility": "' . $defaults->{product_class_visibility} . '"';
    }
	if ( $product_processor_class{$product_class} ) {
        print $fh ', "processorClass": "' . $product_processor_class{$product_class} . '"';
	}
    if ( $slicing_types{$product_class} ) {
        print $fh ', "defaultSlicingType": "' . $slicing_types{$product_class} . '"';
    }
    if ( $slice_durations{$product_class} ) {
        print $fh ', "defaultSliceDuration": ' . $slice_durations{$product_class};
    }
	if ($enclosing_product_types{$product_class}) {
        print $fh ', "enclosingClass": "' . $enclosing_product_types{$product_class} . '"';
	}
    print $fh " }\n";
    $fh->close();
    print $cli_script 'productclass create --file=' . $filename . "\n";
}

foreach my $product_class ( @product_types ) {
	if ($selection_rules{$product_class}) {
	    $filename = $OUT_FILE_DIR . "/" . $mission->{code} . '_' . $product_class . '_rule.txt';
	    $fh = FileHandle->new( $filename, 'w' ) or error_die( 'Cannot open output file ' . $filename );

        print $fh $selection_rules{$product_class} . "\n";
        $fh->close();

		print $cli_script 'productclass rule create --file=' . $filename . ' --format=PLAIN '
		    . $product_class
		    . ' mode=' . $defaults->{selection_rule_mode} 
            . ' configuredProcessors=';
        my $innerfirst = 1;
        foreach my $selrule_processor ( @{ $applicable_processors{$product_class} } ) {
		    if ( $innerfirst ) {
		        $innerfirst = 0;
		    }
		    else {
		        print $cli_script ',';
		    }
            print $cli_script $selrule_processor;
        }
        print $cli_script "\n";
	}
}

say '... creating workflows';

foreach my $workflow ( @workflows ) {
    $filename = $OUT_FILE_DIR . "/" . $mission->{code} . '_' . $workflow->{name} . '.json';
    $fh = FileHandle->new( $filename, 'w' ) or error_die( 'Cannot open output file ' . $filename );

    print $fh '{ ';
    print $fh '"missionCode": "' . $mission->{code} . '"';
    print $fh ', "name": "' . $workflow->{name} . '"';
    
    if ( $workflow->{uuid} ) {
    	print $fh ', "uuid": "' . $workflow->{uuid} . '"';
    }
    if ( $workflow->{description} ) {
    	print $fh ', "description": "' . $workflow->{description} . '"';
    }
    print $fh ', "workflowVersion": "' . $workflow->{workflowVersion} . '"';
    print $fh ', "enabled": ' . $workflow->{enabled};
    print $fh ', "inputProductClass": "' . $workflow->{inputProductClass} . '"';
    print $fh ', "outputProductClass": "' . $workflow->{outputProductClass} . '"';
    print $fh ', "configuredProcessor": "' . $workflow->{configuredProcessor} . '"';
    print $fh ', "outputFileClass": "' . $workflow->{outputFileClass} . '"';
    print $fh ', "processingMode": "' . $workflow->{processingMode} . '"';
    print $fh ', "slicingType": "' . $workflow->{slicingType} . '"';
    if ( $workflow->{sliceDuration} ) {
        print $fh ', "sliceDuration": ' . $workflow->{sliceDuration};
    }
    if ( $workflow->{sliceOverlap} ) {
        print $fh ', "sliceOverlap": ' . $workflow->{sliceOverlap};
    }
    
    if ( $workflow->{inputFilters} ) {
    	print $fh ', "inputFilters": [ ';
    	my $innerfirst = 1;
        foreach my $inputFilter ( $workflow->{inputFilters} ) {
	        if ( $innerfirst ) {
	            $innerfirst = 0;
	        }
	        else {
	            print $fh ', ';
	        }
            print $fh '{ "productClass": "' . $inputFilter->{productClass} . ', "filterConditions": [ ';
            my $innerinnerfirst = 1;
            foreach my $filterCondition ( $inputFilter->{filterConditions} ) {
	            if ( $innerinnerfirst ) {
	                $innerinnerfirst = 0;
	            }
	            else {
	                print $fh ', ';
	            }
                print $fh '{ "key": "' . $filterCondition->{key} . ', "parameterType": ' . $filterCondition->{parameterType}
                    . '", "parameterValue": "' . $filterCondition->{parameterValue} . '" }';
            }
            print $fh ' ] }';
        }
        print $fh ' ]';
    }
    
    print $fh ', "workflowOptions": [ ';
    my $first = 1;
    foreach my $option ( @{ $workflow->{workflowOptions} } ) {
        if ( $first ) {
            $first = 0;
        }
        else {
            print $fh ', ';
        }
    	print $fh '{ "missionCode": "' . $mission->{code} . '", "workflowName": "' . $workflow->{name} . '"';
        print $fh ', "name": "' . $option->{name} . '", "optionType": "' . $option->{optionType} . '"';
    	if ( $option->{defaultValue} ) {
    		print $fh ', "defaultValue": "' . $option->{defaultValue} . '"';
    	}
    	print $fh ', "valueRange": [ ';
        my $innerfirst = 1;
        foreach my $range ( @{ $option->{valueRange} } ) {
            if ( $innerfirst ) {
                $innerfirst = 0;
            }
            else {
                print $fh ', ';
            }
            print $fh '"' . $range . '"';
        }
        print $fh ' ] }';
    }
    print $fh ' ]';
    
    if ( $workflow->{classOutputParameters} ) {
    	print $fh ', "classOutputParameters": [ ';
        my $innerfirst = 1;
        foreach my $classOutputParameter ( @{ $workflow->{classOutputParameters} } ) {
            if ( $innerfirst ) {
                $innerfirst = 0;
            }
            else {
                print $fh ', ';
            }
            print $fh '{ "productClass": "' . $classOutputParameter->{productClass} . '", "outputParameters": [ ';
            my $innerinnerfirst = 1;
            foreach my $parameter ( @{ $classOutputParameter->{outputParameters} } ) {
                if ( $innerinnerfirst ) {
                    $innerinnerfirst = 0;
                }
                else {
                    print $fh ', ';
                }
                print $fh '{ "key": "' . $parameter->{key} . '", "parameterType": "' . $parameter->{parameterType}
                    . '", "parameterValue": "' . $parameter->{parameterValue} . '" }';
            }
            print $fh ' ] }';
        }
        print $fh ' ] }';
    }
    
    print $fh ', "outputParameters": [ ';
    $first = 1;
    foreach my $parameter ( @{ $workflow->{outputParameters} } ) {
        if ( $first ) {
            $first = 0;
        }
        else {
            print $fh ', ';
        }
        print $fh '{ "key": "' . $parameter->{key} . '", "parameterType": "' . $parameter->{parameterType}
            . '", "parameterValue": "' . $parameter->{parameterValue} . '" }';
    }
    print $fh " ] }\n";
    
    $fh->close();
    print $cli_script 'workflow create --file=' . $filename . "\n";
}

print $cli_script "exit\n";
$cli_script->close();

say 'IMPORTANT:';
say '(1) Please change user passwords after completing script-based configuration!';
say '(2) Make sure the sysadm credentials are present in sysadm.cred!';

say '--- prosEO Test Mission setup complete ---';
