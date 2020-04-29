# force execution of default interpreter found in PATH
eval 'exec perl -S $0 $*' if 0;

#
# configure_s5p_proseo.pl
# -----------------------
#
# Configure prosEO for the Sentinel-5 Precursor (S5P) mission
#
use 5.016;
use strict;

use Getopt::Long qw(:config pass_through);
use File::Glob qw(:bsd_glob);
use FileHandle;
# use XML::Twig;

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
	task_number_of_cpus => 16,
	task_breakpoint_file_names => [],
	configuration_product_quality => 'TEST',
	product_class_visibility => 'PUBLIC',
	selection_rule_mode => 'OPER'
};
my $ADMIN_USER = 'sysadm';
my $ADMIN_PWD = 'sysadm';
my $USERMGR_USER = 'usermgr';
my $USERMGR_PWD = 'usermgr';
my $USERMGR_AUTHORITIES = 'ROLE_USERMGR';
my $USER_USER = 'proseo';
my $USER_PWD = 'proseo';
my $USER_GROUP = 'oper';
my $USER_AUTHORITIES = 'ROLE_USER';
my $CLI_SCRIPT_NAME = 'cli_script.txt';

#
# -- Main script
#

#
# -- Basic structures
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
            '${(new java.text.DecimalFormat(\\"00000\\")).format(null == orbit.orbitNumber ? 0 : orbit.orbitNumber)}_' .
            '${parameters.get(\\"copernicusCollection\\").getParameterValue()}_' .
            '${configuredProcessor.processor.processorVersion.replaceAll(\\"\\\\\\\\.\\", \\"\\")}_' .
            '${T(java.time.format.DateTimeFormatter).ofPattern(\\"uuuuMMdd\'T\'HHmmss\\").withZone(T(java.time.ZoneId).of(\\"UTC\\")).format(generationTime)}.nc'

};
my $spacecraft = { code => 'PTS', name => 'prosEO Test Satellite' };
my @orbits = (
    { orbitNumber => 3000, startTime => '2019-11-04T09:00:00.200000', stopTime => '2019-11-04T10:41:10.300000' },
    { orbitNumber => 3001, startTime => '2019-11-04T10:41:10.300000', stopTime => '2019-11-04T12:22:20.400000' },
    { orbitNumber => 3002, startTime => '2019-11-04T12:22:20.400000', stopTime => '2019-11-04T14:03:30.500000' },
    { orbitNumber => 3003, startTime => '2019-11-04T14:03:30.500000', stopTime => '2019-11-04T15:44:40.600000' },
    { orbitNumber => 3004, startTime => '2019-11-04T15:44:40.600000', stopTime => '2019-11-04T17:25:50.700000' },
    { orbitNumber => 3005, startTime => '2019-11-04T17:25:50.700000', stopTime => '2019-11-04T19:07:00.800000' }
);

#
# -- Processor classes, versions and configurations
#
my @processor_classes = ( 'PTML1B', 'PTML2', 'PTML3' );
my @processors = (
    {
    	processorName => 'PTML1B', 
    	processorVersion => '0.1.0',
    	configuredProcessors => [ 'PTML1B_0.1.0_OPER_2020-03-25' ],
    	tasks => [ 
    	   { taskName => 'ptm_l01b', taskVersion => '0.1.0' }
    	],
    	dockerImage => 'localhost:5000/proseo-sample-wrapper:0.1.0-SNAPSHOT'
    },
    {
        processorName => 'PTML2', 
        processorVersion => '0.1.0',
        configuredProcessors => [ 'PTML2_0.1.0_OPER_2020-03-25' ],
        tasks => [ 
           { taskName => 'ptm_l2', taskVersion => '0.1.0' }
        ],
        dockerImage => 'localhost:5000/proseo-sample-wrapper:0.1.0-SNAPSHOT'
    },
    {
        processorName => 'PTML3', 
        processorVersion => '0.1.0',
        configuredProcessors => [ 'PTML3_0.1.0_OPER_2020-03-25' ],
        tasks => [ 
           { taskName => 'ptm_l3', taskVersion => '0.1.0' }
        ],
        dockerImage => 'localhost:5000/proseo-sample-wrapper:0.1.0-SNAPSHOT'
    }
);
my @configurations = (
    { 
    	processorName => 'PTML1B', 
    	configurationVersion => 'OPER_2020-03-25', 
    	dynProcParameters => [ 
    	   { key => 'logging.root', parameterType => 'STRING', parameterValue => 'notice' },
           { key => 'logging.dumplog', parameterType => 'STRING', parameterValue => 'null' },
           { key => 'Threads', parameterType => 'INTEGER', parameterValue => 16 },
           { key => 'Processing_Mode', parameterType => 'STRING', parameterValue => 'NRTI' }
    	],
    	configurationFiles => [],
    	staticInputFiles => [
    	   { fileType => 'processing_configuration', fileNameType => 'Physical', fileNames => [ '/usr/share/sample-processor/conf/ptm_l1b_config.xml' ] },
    	],
    	configuredProcessors => [ 'PTML1B_0.1.0_OPER_2020-03-25' ]
    },
    { 
    	processorName => 'PTML2', 
    	configurationVersion => 'OPER_2020-03-25', 
        dynProcParameters => [ 
           { key => 'logging.root', parameterType => 'STRING', parameterValue => 'notice' },
           { key => 'logging.dumplog', parameterType => 'STRING', parameterValue => 'null' },
           { key => 'Threads', parameterType => 'INTEGER', parameterValue => 10 },
           { key => 'Processing_Mode', parameterType => 'STRING', parameterValue => 'OFFL' }
        ],
        configurationFiles => [
            { fileVersion => '1.0', fileName => '/usr/share/sample-processor/conf/ptm_l2_config.xml' }
        ],
        staticInputFiles => [],
        configuredProcessors => [ 'PTML2_0.1.0_OPER_2020-03-25' ]
    },
    { 
    	processorName => 'PTML3', 
    	configurationVersion => 'OPER_2020-03-25', 
        dynProcParameters => [ 
           { key => 'logging.root', parameterType => 'STRING', parameterValue => 'notice' },
           { key => 'logging.dumplog', parameterType => 'STRING', parameterValue => 'null' },
           { key => 'Threads', parameterType => 'INTEGER', parameterValue => 16 },
           { key => 'Processing_Mode', parameterType => 'STRING', parameterValue => 'NRTI' }
        ],
        configurationFiles => [],
        staticInputFiles => [],
        configuredProcessors => [ 'PTML3_0.1.0_OPER_2020-03-25' ]
    }
);
my @configured_processors = (
    {
    	identifier => 'PTML1B_0.1.0_OPER_2020-03-25',
    	processorName => 'PTML1B',
    	processorVersion => '0.1.0',
    	configurationVersion => 'OPER_2020-03-25' 
    },
    {
        identifier => 'PTML2_0.1.0_OPER_2020-03-25',
        processorName => 'PTML2',
        processorVersion => '0.1.0',
        configurationVersion => 'OPER_2020-03-25' 
    },
    {
        identifier => 'PTML3_0.1.0_OPER_2020-03-25',
        processorName => 'PTML3',
        processorVersion => '0.1.0',
        configurationVersion => 'OPER_2020-03-25' 
    }
);

#
# -- Selection rules (OFFL mode only!)
#
my %product_types;
my %enclosing_product_types;
my %product_processor_class;
my %slicing_types;
my %slice_durations;
my %selection_rules;
my %applicable_processors;

# Product types without processor
$product_types{'L0________'} = 'L0________';
    
$product_types{'AUX_IERS_B'} = 'AUX_IERS_B';
    
# Selection rule for PTM L1B
# Expected time coverage of the L1B products is on orbit
$product_types{'L1B_______'} = 'L1B_______';
$product_types{'L1B_PART1'} = 'L1B_PART1';
$product_types{'L1B_PART2'} = 'L1B_PART2';
$enclosing_product_types{'L1B_PART1'} = 'L1B_______';
$enclosing_product_types{'L1B_PART2'} = 'L1B_______';
$slicing_types{'L1B_______'} = 'ORBIT';
$product_processor_class{'L1B_______'} = 'PTML1B';
# Output L1B
$selection_rules{'L1B_______'} = '
    FOR L0________ SELECT ValIntersect(0, 0);
    FOR AUX_IERS_B SELECT LatestValIntersect(60 D, 60 D)';
$applicable_processors{'L1B_______'} = [ 'PTML1B_0.1.0_OPER_2020-03-25' ];

# Selection rules for PTM L2
# Expected time coverage of the L2 products is on orbit (same as for the L1B product)

# Output PTM_L2A
$product_types{'PTM_L2A'} = 'PTM_L2A';
$slicing_types{'PTM_L2A'} = 'ORBIT';
$product_processor_class{'PTM_L2A'} = 'PTML2';
$selection_rules{'PTM_L2A'} = '
    FOR L1B_______ SELECT LatestValCover(0, 0)';
$applicable_processors{'PTM_L2A'} = [ 'PTML2_0.1.0_OPER_2020-03-25' ];
# Output PTM_L2B
$product_types{'PTM_L2B'} = 'PTM_L2B';
$slicing_types{'PTM_L2B'} = 'ORBIT';
$product_processor_class{'PTM_L2B'} = 'PTML2';
$selection_rules{'PTM_L2B'} = '
    FOR L1B_PART1 SELECT LatestValCover(0, 0)';
$applicable_processors{'PTM_L2B'} = [ 'PTML2_0.1.0_OPER_2020-03-25' ];

# Selection rules for PTM L3
# Expected time coverage of the L3 products is 4 hours
# Output PTM_L3
$product_types{'PTM_L3'} = 'PTM_L3';
$slicing_types{'PTM_L3'} = 'TIME_SLICE';
$slice_durations{'PTM_L3'} = 14400;
$product_processor_class{'PTM_L3'} = 'PTML3';
$selection_rules{'PTM_L3'} = '
    FOR PTM_L2A SELECT ValIntersect(0, 0) MINCOVER(90);
    FOR PTM_L2B SELECT ValIntersect(0, 0) MINCOVER(90)';
$applicable_processors{'PTM_L3'} = [ 'PTML3_0.1.0_OPER_2020-03-25' ];

# --- Output creation script ---
# Create output file for CLI script
say '... starting CLI script';
my $cli_script = FileHandle->new( $CLI_SCRIPT_NAME, 'w' ) or error_die( 'Cannot open script file ' . $CLI_SCRIPT_NAME );

# Login as admin user
print $cli_script "login -usysadm -psysadm\n";

# Output mission
say '... creating mission';
my $filename = $mission->{code} . '.json';
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
print $fh '    "spacecrafts": [ {' . "\n";
print $fh '        "code": "' . $spacecraft->{code} . '",' . "\n";
print $fh '        "name": "' . $spacecraft->{name} . '"' . "\n";
print $fh '    } ]' . "\n";
print $fh '}' . "\n";
$fh->close();
print $cli_script 'mission create --file=' . $filename . "\n";

# Create first mission users
say '... creating mission users';
print $cli_script 'user create --mission=' . $mission->{code} . ' ' . $USERMGR_USER . ' password=' . $USERMGR_PWD . ' authorities=' . $USERMGR_AUTHORITIES . "\n";
print $cli_script 'login --user=' . $USERMGR_USER . ' --password=' . $USERMGR_PWD . ' PTM' . "\n";
print $cli_script 'user create ' . $USER_USER . ' password=' . $USER_PWD . "\n";
print $cli_script 'group create ' . $USER_GROUP . ' authorities=' . $USER_AUTHORITIES . "\n";
print $cli_script 'group add ' . $USER_GROUP . ' ' . $USER_USER . "\n";

# Perform the remaining configuration steps as regular user
print $cli_script 'login --user=' . $USER_USER . ' --password=' . $USER_PWD . ' PTM' . "\n";

# Create spacecraft orbits
say '... creating spacecraft orbits';
$filename = $mission->{code} . '_orbits.json';
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
	print $fh '    { "spacecraftCode": "' . $spacecraft->{code} . '", "orbitNumber": ' , $orbit->{orbitNumber} . ', "startTime": "' . $orbit->{startTime} . '", "stopTime": "' . $orbit->{stopTime} . '" }';
}
print $fh "\n]\n";
$fh->close();
print $cli_script 'orbit create --file=' . $filename . "\n";

# Create processors classes
say '... creating processor classes';
foreach my $processor_class ( @processor_classes ) {
	$filename = $mission->{code} . '_' . $processor_class . '.json';
	$fh = FileHandle->new( $filename, 'w' ) or error_die( 'Cannot open output file ' . $filename );

	print $fh '{ "missionCode": "' . $mission->{code} . '", "processorName": "' . $processor_class . '", "productClasses": [] }' . "\n";

	$fh->close();
    print $cli_script 'processor class create --file=' . $filename . "\n";
}

# Create processor versions
say '... creating processor versions';
foreach my $processor ( @processors ) {
    $filename = $mission->{code} . '_' . $processor->{processorName} . '_' . $processor->{processorVersion} . '.json';
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
    $filename = $mission->{code} . '_' . $configuration->{processorName} . '_conf_' . $configuration->{configurationVersion} . '.json';
    $fh = FileHandle->new( $filename, 'w' ) or error_die( 'Cannot open output file ' . $filename );

    print $fh '{ "missionCode": "' . $mission->{code} 
        . '", "processorName": "' . $configuration->{processorName} 
        . '", "configurationVersion": "' . $configuration->{configurationVersion} 
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

# Create product classes (sequence is important due to selection rule dependencies)
say '... creating product classes';
my @class_key_sequence = ( 'L0________', 'AUX_IERS_B', 'L1B_______', 'L1B_PART1', 'L1B_PART2', 'PTM_L2A', 'PTM_L2B', 'PTM_L3' );

foreach my $product_class ( @class_key_sequence ) {
    $filename = $mission->{code} . '_' . $product_types{$product_class} . '.json';
    $fh = FileHandle->new( $filename, 'w' ) or error_die( 'Cannot open output file ' . $filename );

	print $fh '{ ';
	print $fh '"missionCode": "' . $mission->{code} . '"';
	print $fh ', "productType": "' . $product_types{$product_class} . '"';
    print $fh ', "visibility": "' . $defaults->{product_class_visibility} . '"';
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

	if ($selection_rules{$product_class}) {
	    $filename = $mission->{code} . '_' . $product_types{$product_class} . '_rule.txt';
	    $fh = FileHandle->new( $filename, 'w' ) or error_die( 'Cannot open output file ' . $filename );

        print $fh $selection_rules{$product_class} . "\n";
        $fh->close();

		print $cli_script 'productclass rule create --file=' . $filename . ' --format=PLAIN '
		    . $product_types{$product_class}
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
print $cli_script "exit\n";
$cli_script->close();

say '--- prosEO Test Mission setup complete ---';
