# force execution of default interpreter found in PATH
eval 'exec perl -S $0 $*' if 0;

#
# O3TCL-Preparator.pl
# -------------------
#
# Prepare processing of O3TCL products
#
use 5.016;
use strict;

use Getopt::Long qw(:config pass_through);
use File::Glob qw(:bsd_glob);
use YAML::Tiny;
use Log::Log4perl;
use Time::Piece;

use utils;
use template_utils;

my $logger = Log::Log4perl->get_logger();
my $TIME_FORMAT = '%Y%m%dT%H%M%S';

#
# - main script
#

# -- parse options
my $dummy;
GetOptions(
	'-out0=s'             => \my $WORKING,
	'-out1=s'             => \my $OUTPUT,
	'-template=s'         => \my $TEMPLATE,
	'-pp_o3=s'            => \my $pp,
	'-pdrId=s'            => \my $pdrId,
	'-startTime=s'        => \my $startTime,
	'-stopTime=s'         => \my $stopTime,
	'-logLevel=s'         => \my $logLevel,
	'-errLevel=s'         => \my $errLevel,
	'-threads=s'          => \my $threads,
	'-loggingRoot=s'      => \my $loggingRoot,
	'-loggingDumplog=s'   => \my $loggingDumplog,
	'-collectionNumber=s' => \my $collectionNumber,
	'-processingMode=s'   => \my $processingMode,
	'-rtype=s'            => \$dummy,
	'-debug=s'            => \my $DEBUG
);

#
# -- check options
#

$logger->error_die("paramerter mismatch in (@ARGV)\n") unless ( $WORKING && $OUTPUT );

#
# -  define yaml JOF representation filename
#

$logger->info( "Preparing creation of new YAML file " . $WORKING . "/JobOrder" . $pdrId . ".yml" );
my $yamlJOFile = $WORKING . "/JobOrder." . $pdrId . ".yml";

#
# -- create JobOrderFile for L1b processor
#

#
# set correct templates and paths
#

$logger->info("Parsing YAML-Template from \'$TEMPLATE\'\n");
my $template = YAML::Tiny->read($TEMPLATE);

# timestamp
my $timestamp = `date +%Y%m%dT%H%M%S`;
chop $timestamp;

# processor version
my $templateProcessorVersion = get_parameter( $template, "Version", 0, ('Ipf_Conf') );
my @templateProcessorVersionParts = ( $templateProcessorVersion =~ /(\d)\.(\d)\.(\d)/ );
my $versionString =
    sprintf( '%02d', $templateProcessorVersionParts[0] )
  . sprintf( '%02d', $templateProcessorVersionParts[1] )
  . sprintf( '%02d', $templateProcessorVersionParts[2] );

#
# -- put specific parameters into yaml file
#

#identify required L0 data
my $startTimeDate = Time::Piece->strptime( $startTime, $TIME_FORMAT );
# $startTimeDate = $startTimeDate - 7*24*60*60;
my $stopTimeDate = Time::Piece->strptime( $stopTime, $TIME_FORMAT );
# $stopTimeDate = $stopTimeDate + 7*24*60*60;

my $test           = 0;
my @fileCandidates = <$pp/*/S5P*.nc>;
my @files;
foreach my $file (@fileCandidates) {

	# example filename S5P_TEST_SO2____20150819T224613_20150819T225113_00043__000000_20160531T131758.nc
	my ( $coverageStart, $coverageStop ) = ( $file =~ /S5P_...._....*_(........T......)_(........T......).*/ );
	$coverageStart = Time::Piece->strptime( $coverageStart, $TIME_FORMAT );
	$coverageStop  = Time::Piece->strptime( $coverageStop,  $TIME_FORMAT );
	if (
			   ( $coverageStop >= ($startTimeDate - 2*24*60*60))
			&& ( $coverageStart <= ($stopTimeDate + 2*24*60*60))
	  )
	{
		$logger->info("Adding file $file to input product list");
		push( @files, $file );
	}
}

if ( $#files < 0 ) {
	$logger->error_die("No matching input data available! Exiting.");
}

# add parameters to jofa
my $start     = $startTimeDate->strftime($TIME_FORMAT);
$startTime = $startTimeDate->strftime('%Y%m%d_%H%M%S');
$startTime = $startTime . "000000";
my $stop     = $stopTimeDate->strftime($TIME_FORMAT);
$stopTime = $stopTimeDate->strftime('%Y%m%d_%H%M%S');
$stopTime = $stopTime . "000000";

# set standard parameters
set_parameter( $template, 'Stdout_Log_Level', $logLevel,  ('Ipf_Conf') );
set_parameter( $template, 'Stderr_Log_Level', $errLevel,  ('Ipf_Conf') );
set_parameter( $template, 'Start',            $startTime, ( 'Ipf_Conf', 'Sensing_Time' ) );
set_parameter( $template, 'Stop',             $stopTime,  ( 'Ipf_Conf', 'Sensing_Time' ) );
add_dynamic_parameter( $template, 'logging.root',    $loggingRoot );
add_dynamic_parameter( $template, 'logging.dumplog', $loggingDumplog );
add_dynamic_parameter( $template, 'Threads',         $threads );
add_dynamic_parameter( $template, 'Processing_Mode', $processingMode );

# add input products
add_input( $template, 'L2__O3____', 'Physical', @files );

# add output products
my $signatur = $start . "_" . $stop . "_00000_" . $collectionNumber . "_" . $versionString . "_" . $timestamp;
add_output( $template, "L2__O3_TCL", "Physical", $OUTPUT . "/S5P_NRTI_L2__O3_TCL_" . $signatur . ".nc" );

#
# -- write JOF output
#

$logger->info( "Writing YAML-JOF " . $yamlJOFile );

$template->write($yamlJOFile);

exit 0;
