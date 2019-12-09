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
use XML::Twig;

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
	selection_rule_mode => 'OPER'
};
my $curl_string = 'curl --insecure --data @- --header "Content-Type: application/json" --user s5p-proseo:sieb37.Schlaefer http://localhost:';
my $EOF = "EOF";
my $mission_mgr_curl = $curl_string . '8082/proseo/order-mgr/v0.1/missions' . " <<". $EOF;
my $orbit_mgr_curl = $curl_string . '8082/proseo/order-mgr/v0.1/orbits' . " <<". $EOF;
my $procclass_mgr_curl = $curl_string . '8083/proseo/processor-mgr/v0.1/processorclasses' . " <<". $EOF;
my $processor_mgr_curl = $curl_string . '8083/proseo/processor-mgr/v0.1/processors' . " <<". $EOF;
my $configuration_mgr_curl = $curl_string . '8083/proseo/processor-mgr/v0.1/configurations' . " <<". $EOF;
my $configproc_mgr_curl = $curl_string . '8083/proseo/processor-mgr/v0.1/configuredprocessors' . " <<". $EOF;
my $prodclass_mgr_curl = $curl_string . '8084/proseo/productclass-mgr/v0.1/productclasses';
my $selrule_mgr_curl = $curl_string . '8084/proseo/productclass-mgr/v0.1/productclasses/$PCID/selectionrules' . " <<". $EOF;

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
            'PTM_\\${fileClass}_\\${productClass.missionType}_' .
            '\\${T(java.time.format.DateTimeFormatter).ofPattern(\\"uuuuMMdd\'T\'HHmmss\\").withZone(T(java.time.ZoneId).of(\\"UTC\\")).format(sensingStartTime)}_' .
            '\\${T(java.time.format.DateTimeFormatter).ofPattern(\\"uuuuMMdd\'T\'HHmmss\\").withZone(T(java.time.ZoneId).of(\\"UTC\\")).format(sensingStopTime)}_' .
            '\\${(new java.text.DecimalFormat(\\"00000\\")).format(orbit.orbitNumber)}_' .
            '\\${parameters.get(\\"copernicusCollection\\").getParameterValue()}_' .
            '\\${configuredProcessor.processor.processorVersion.replaceAll(\\"\\\\\\\\.\\", \\"\\")}_' .
            '\\${T(java.time.format.DateTimeFormatter).ofPattern(\\"uuuuMMdd\'T\'HHmmss\\").withZone(T(java.time.ZoneId).of(\\"UTC\\")).format(generationTime)}.nc'

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
    	processorVersion => '0.0.1',
    	configuredProcessors => [ 'PTML1B 0.0.1 OPER 2019-11-04' ],
    	tasks => [ 
    	   { taskName => 'ptm_l01b', taskVersion => '0.0.1' }
    	],
    	dockerImage => 'localhost:5000/proseo-sample-wrapper:0.0.1-SNAPSHOT'
    },
    {
        processorName => 'PTML2', 
        processorVersion => '0.0.1',
        configuredProcessors => [ 'PTML2 0.0.1 OPER 2019-11-04' ],
        tasks => [ 
           { taskName => 'ptm_l2', taskVersion => '0.0.1' }
        ],
        dockerImage => 'localhost:5000/proseo-sample-wrapper:0.0.1-SNAPSHOT'
    },
    {
        processorName => 'PTML3', 
        processorVersion => '0.0.1',
        configuredProcessors => [ 'PTML3 0.0.1 OPER 2019-11-04' ],
        tasks => [ 
           { taskName => 'ptm_l3', taskVersion => '0.0.1' }
        ],
        dockerImage => 'localhost:5000/proseo-sample-wrapper:0.0.1-SNAPSHOT'
    }
);
my @configurations = (
    { 
    	processorName => 'PTML1B', 
    	configurationVersion => 'OPER 2019-11-04', 
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
    	configuredProcessors => [ 'PTML1B 0.0.1 OPER 2019-11-04' ]
    },
    { 
    	processorName => 'PTML2', 
    	configurationVersion => 'OPER 2019-11-04', 
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
        configuredProcessors => [ 'PTML2 0.0.1 OPER 2019-11-04' ]
    },
    { 
    	processorName => 'PTML3', 
    	configurationVersion => 'OPER 2019-11-04', 
        dynProcParameters => [ 
           { key => 'logging.root', parameterType => 'STRING', parameterValue => 'notice' },
           { key => 'logging.dumplog', parameterType => 'STRING', parameterValue => 'null' },
           { key => 'Threads', parameterType => 'INTEGER', parameterValue => 16 },
           { key => 'Processing_Mode', parameterType => 'STRING', parameterValue => 'NRTI' }
        ],
        configurationFiles => [],
        staticInputFiles => [],
        configuredProcessors => [ 'PTML3 0.0.1 OPER 2019-11-04' ]
    }
);
my @configured_processors = (
    {
    	identifier => 'PTML1B 0.0.1 OPER 2019-11-04',
    	processorName => 'PTML1B',
    	processorVersion => '0.0.1',
    	configurationVersion => 'OPER 2019-11-04' 
    },
    {
        identifier => 'PTML2 0.0.1 OPER 2019-11-04',
        processorName => 'PTML2',
        processorVersion => '0.0.1',
        configurationVersion => 'OPER 2019-11-04' 
    },
    {
        identifier => 'PTML3 0.0.1 OPER 2019-11-04',
        processorName => 'PTML3',
        processorVersion => '0.0.1',
        configurationVersion => 'OPER 2019-11-04' 
    }
);

#
# -- Selection rules (OFFL mode only!)
#
my %product_types;
my %mission_types;
my %enclosing_product_types;
my %product_processor_class;
my %selection_rules;
my %applicable_processors;

# Product types without processor
$product_types{'L0'} = 'L0';
$mission_types{'L0'} = 'L0________';
    
$product_types{'AUX_IERS_B'} = 'AUX_IERS_B';
$mission_types{'AUX_IERS_B'} = 'AUX_IERS_B';
    
# Selection rule for PTM L1B
# Expected time coverage of the L1B products is on orbit
$product_types{'L1B'} = 'L1B';
$product_types{'L1B_PART1'} = 'L1B_PART1';
$product_types{'L1B_PART2'} = 'L1B_PART2';
$mission_types{'L1B'}        = 'L1B_______';
$mission_types{'L1B_PART1'} = 'L1B_PART1';
$mission_types{'L1B_PART2'} = 'L1B_PART2';
$enclosing_product_types{'L1B_PART1'} = 'L1B';
$enclosing_product_types{'L1B_PART2'} = 'L1B';
$product_processor_class{'L1B'} = 'PTML1B';
# Output L1B
$selection_rules{'L1B'} = '
    FOR L0 SELECT ValIntersect(0, 0);
    FOR AUX_IERS_B SELECT LatestValIntersect(60 D, 60 D)';
$applicable_processors{'L1B'} = [ 'PTML1B 0.0.1 OPER 2019-11-04' ];

# Selection rules for PTM L2
# Expected time coverage of the L2 products is on orbit (same as for the L1B product)

# Output PTM_L2A
$product_types{'PTM_L2A'} = 'PTM_L2A';
$mission_types{'PTM_L2A'} = 'PTM_L2A';
$product_processor_class{'PTM_L2A'} = 'PTML2';
$selection_rules{'PTM_L2A'} = '
    FOR L1B SELECT LatestValCover(0, 0)';
$applicable_processors{'PTM_L2A'} = [ 'PTML2 0.0.1 OPER 2019-11-04' ];
# Output PTM_L2B
$product_types{'PTM_L2B'} = 'PTM_L2B';
$mission_types{'PTM_L2B'} = 'PTM_L2B';
$product_processor_class{'PTM_L2B'} = 'PTML2';
$selection_rules{'PTM_L2B'} = '
    FOR L1B_PART1 SELECT LatestValCover(0, 0)';
$applicable_processors{'PTM_L2B'} = [ 'PTML2 0.0.1 OPER 2019-11-04' ];

# Selection rules for PTM L3
# Expected time coverage of the L3 products is 4 hours
# Output PTM_L3
$product_types{'PTM_L3'} = 'PTM_L3';
$mission_types{'PTM_L3'} = 'PTM_L3';
$product_processor_class{'PTM_L3'} = 'PTML3';
$selection_rules{'PTM_L3'} = '
    FOR PTM_L2A SELECT ValIntersect(0, 0) MINCOVER(90);
    FOR PTM_L2B SELECT ValIntersect(0, 0) MINCOVER(90)';
$applicable_processors{'PTM_L3'} = [ 'PTML3 0.0.1 OPER 2019-11-04' ];

# --- Output creation script ---

# Output mission
say '# prosEO Test Mission and spacecraft:';
say $mission_mgr_curl;
say '{';
say '    "code": "' . $mission->{code} . '",';
say '    "name": "' . $mission->{name} . '",';
print '    "fileClasses": [ ';
my $first = 1;
foreach my $file_class ( @{ $mission->{fileClasses} } ) {
    if ( $first ) {
        $first = 0;
    }
    else {
        print ', ';
    }
    print '"' . $file_class . '"';
}
say ' ],';
print '    "processingModes": [ ';
$first = 1;
foreach my $processing_mode ( @{ $mission->{processingModes} } ) {
    if ( $first ) {
        $first = 0;
    }
    else {
        print ', ';
    }
    print '"' . $processing_mode . '"';
}
say ' ],';
say '    "productFileTemplate": "' . $mission->{productFileTemplate} . '",';
say '    "spacecrafts": [ {';
say '        "code": "' . $spacecraft->{code} . '",';
say '        "name": "' . $spacecraft->{name} . '"';
say '    } ]';
say '}';
say $EOF;

say '';
say '# Spacecraft orbits:';
say $orbit_mgr_curl;
say '[';
$first = 1;
foreach my $orbit ( @orbits ) {
	if ( $first ) {
		$first = 0;
	}
	else {
		print ",\n";
	}
	print '    { "spacecraftCode": "' . $spacecraft->{code} . '", "orbitNumber": ' , $orbit->{orbitNumber} . ', "startTime": "' . $orbit->{startTime} . '", "stopTime": "' . $orbit->{stopTime} . '" }';
}
say "\n]";
say $EOF;

# Output processors, versions and configurations
say '';
say '# prosEO Test Mission processor classes:';
#say '[';
#$first = 1;
foreach my $processor_class ( @processor_classes ) {
	#if ( $first ) {
	#	$first = 0;
	#}
	#else {
	#	print ',\n';
	#}
	say $procclass_mgr_curl;
	print '  { "missionCode": "' . $mission->{code} . '", "processorName": "' . $processor_class . '", "productClasses": [] }';
	say "\n" . $EOF;
}
#say "\n]";
say '';

say '# prosEO Test Mission processor versions:';
#say '[';
#$first = 1;
foreach my $processor ( @processors ) {
	#if ( $first ) {
	#	$first = 0;
	#}
	#else {
	#	print ',\n';
	#}
    say $processor_mgr_curl;
	print '    { "missionCode": "' . $mission->{code} . '", "processorName": "' . $processor->{processorName} . '", "processorVersion": "' . $processor->{processorVersion} . '", ';
	print '"isTest": ' . ( $defaults->{processor_is_test} == 0 ? 'false' : 'true' ) . ', "minDiskSpace": ' . $defaults->{processor_min_disk_space} . ', ';
	print '"maxTime": ' . $defaults->{processor_max_time} . ', "sensingTimeFlag": ' . ( $defaults->{processor_sensing_time_flag} ? 'true' : 'false' ) . ', ';
	print '"configuredProcessors": [ ';
#	my $innerfirst = 1;
#	my $processor_configured_processors = $processor->{configuredProcessors};
#	foreach my $configured_processor ( @{ $processor_configured_processors } ) {
#		if ( $innerfirst ) {
#			$innerfirst = 0;
#		}
#		else {
#			print ', ';
#		}
#		print '"$configured_processor"';
#	}
	print ' ], "tasks": [ ';
	my $innerfirst = 1;
	foreach my $task ( @{ $processor->{tasks} }) {
        if ( $innerfirst ) {
            $innerfirst = 0;
        }
        else {
            print ', ';
        }
		print '{ "taskName": "' . $task->{taskName} . '", "taskVersion": "' . $task->{taskVersion} . '", ';
		print '"isCritical": ' . ( $defaults->{task_is_critical} ? 'true' : 'false' ) . ', ';
		print '"criticalityLevel": ' . $defaults->{task_criticality_level} . ', "numberOfCpus": ' . $defaults->{task_number_of_cpus} . ', ';
		print '"breakpointFileNames": [ ';
        my $innerinnerfirst = 1;
        foreach my $breakpointFileName ( @{ $defaults->{task_breakpoint_file_names} } )	{
        	if ( $innerinnerfirst ) {
        		$innerinnerfirst = 0;
        	}
        	else {
        		print ', ';
        	}
        	print '"' . $breakpointFileName . '"';
        }
        print ' ] }';
	}
	print ' ], "dockerImage": "' . $processor->{dockerImage} . '" }';
    say "\n" . $EOF;
}
#say ' ]';
say '';

say '# prosEO Test Mission processor configurations:';
#say '[';
#$first = 1;
foreach my $configuration ( @configurations ) {
    #if ( $first ) {
    #    $first = 0;
    #}
    #else {
    #    print ',\n';
    #}
    say $configuration_mgr_curl;
    print '    { "missionCode": "' . $mission->{code} . '", "processorName": "' . $configuration->{processorName} . '", "configurationVersion": "' . $configuration->{configurationVersion} . '", "dynProcParameters": [ ';
    my $innerfirst = 1;
    foreach my $dynProcParam ( @{ $configuration->{dynProcParameters} } ) {
        if ( $innerfirst ) {
            $innerfirst = 0;
        }
        else {
            print ', ';
        }
        print '{ "key": "' . $dynProcParam->{key} . '", "parameterType": "' , $dynProcParam->{parameterType} . '", "parameterValue": "' . $dynProcParam->{parameterValue} . '" }';
    }
    print ' ], "configurationFiles": [ ';
    $innerfirst = 1;
    foreach my $configurationFile ( @{ $configuration->{configurationFiles} } ) {
        if ( $innerfirst ) {
            $innerfirst = 0;
        }
        else {
            print ', ';
        }
        print ' { "fileVersion": "'. $configurationFile->{fileVersion} . '", "fileName": "' . $configurationFile->{fileName} . '" }';
    }
    print ' ], "staticInputFiles": [ ';
    $innerfirst = 1;
    foreach my $staticInputFile ( @{ $configuration->{staticInputFiles} } ) {
        if ( $innerfirst ) {
            $innerfirst = 0;
        }
        else {
            print ', ';
        }
        print ' { "fileType": "'. $staticInputFile->{fileType} . '", "fileNameType": "' . $staticInputFile->{fileNameType} . '", "fileNames": [ ';
        my $innerinnerfirst = 1;
        foreach my $staticInputFileName ( @{ $staticInputFile->{fileNames} } ) {
            if ( $innerinnerfirst ) {
                $innerinnerfirst = 0;
            }
            else {
                print ', ';
            }
            print '"' . $staticInputFileName . '"';
        }
        print ' ] }';
    }
    print ' ], "configuredProcessors": [ ';
#    $innerfirst = 1;
#    foreach my $configuredProcessor ( @{ $configuration->{configuredProcessors} } ) {
#        if ( $innerfirst ) {
#            $innerfirst = 0;
#        }
#        else {
#            print ', ';
#        }
#        print '"$configuredProcessor"';
#    }
    print ' ] }';
    say "\n" . $EOF;
}
#say ' ]';
say '';

say '# prosEO Test Mission configured processors:';
#say '[';
#$first = 1;
foreach my $configured_processor ( @configured_processors ) {
    #if ( $first ) {
    #    $first = 0;
    #}
    #else {
    #    print ",\n";
    #}
    say $configproc_mgr_curl;
    print '    { "identifier": "' . $configured_processor->{identifier} . '", "missionCode": "' . $mission->{code} . '", "processorName": "' . $configured_processor->{processorName} . '", ';
    print '"processorVersion": "' . $configured_processor->{processorVersion} . '", "configurationVersion": "' . $configured_processor->{configurationVersion} . '" } ';
    say "\n" . $EOF;
}
#say ' ]';

# Output product classes
my @class_key_sequence = ( 'L0', 'AUX_IERS_B', 'L1B', 'L1B_PART1', 'L1B_PART2', 'PTM_L2A', 'PTM_L2B', 'PTM_L3' );
say '';
say "# prosEO Test Mission product classes:";
foreach my $product_class ( @class_key_sequence ) {
	print 'PCID=`echo \'';
	print '{ ';
	print '"missionCode": "' . $mission->{code} . '"';
	print ', "productType": "' . $product_types{$product_class} . '"';
	print ', "missionType": "' . $mission_types{$product_class} . '"';
	if ( $product_processor_class{$product_class} ) {
        print ', "processorClass": "' . $product_processor_class{$product_class} . '"';
	}
	if ($enclosing_product_types{$product_class}) {
        print ', "enclosingClass": "' . $enclosing_product_types{$product_class} . '"';
	}
    say ' }\' | ' . $prodclass_mgr_curl . ' | cut -d \',\' -f 1 | cut -d \':\' -f 2`';
	if ($selection_rules{$product_class}) {
#	   say ',';
#	   print '  "selectionRuleString" : "' . $selection_rules{$product_class} . '"';
        say $selrule_mgr_curl;
        print '[ { "selectionRule": "' . ( $selection_rules{$product_class} =~ s/\R//gr ) . '", "mode": "' . $defaults->{selection_rule_mode} . '", "configuredProcessors": [ ';
        my $innerfirst = 1;
        foreach my $selrule_processor ( @{ $applicable_processors{$product_class} } ) {
		    if ( $innerfirst ) {
		        $innerfirst = 0;
		    }
		    else {
		        print ', ';
		    }
            print '"' . $selrule_processor . '"';
        }
        say ' ] } ]';
        say $EOF;
	}
}
say 'echo ""; echo "--- prosEO Test Mission setup complete ---"';
