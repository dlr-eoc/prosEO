# force execution of default interpreter found in PATH
eval 'exec perl -S $0 $*' if 0;

#
# NRT-L1b-Preparator.pl
# ---------------------
#
# Prepare processing of NRT L1b products
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

#
# -- helper functions
#
sub add_L1b_output ($$$) {
	my ( $yaml, $signatur, $directory ) = @_;
	add_output( $yaml, 'directory.out', 'Directory', $directory );
	add_output( $yaml, 'l1b_ra_bd1',    'Physical',  $directory . "/S5P_NRTI_L1B_RA_BD1_" . $signatur . ".nc" );
	add_output( $yaml, 'l1b_ra_bd2',    'Physical',  $directory . "/S5P_NRTI_L1B_RA_BD2_" . $signatur . ".nc" );
	add_output( $yaml, 'l1b_ra_bd3',    'Physical',  $directory . "/S5P_NRTI_L1B_RA_BD3_" . $signatur . ".nc" );
	add_output( $yaml, 'l1b_ra_bd4',    'Physical',  $directory . "/S5P_NRTI_L1B_RA_BD4_" . $signatur . ".nc" );
	add_output( $yaml, 'l1b_ra_bd5',    'Physical',  $directory . "/S5P_NRTI_L1B_RA_BD5_" . $signatur . ".nc" );
	add_output( $yaml, 'l1b_ra_bd6',    'Physical',  $directory . "/S5P_NRTI_L1B_RA_BD6_" . $signatur . ".nc" );
	add_output( $yaml, 'l1b_ra_bd7',    'Physical',  $directory . "/S5P_NRTI_L1B_RA_BD7_" . $signatur . ".nc" );
	add_output( $yaml, 'l1b_ra_bd8',    'Physical',  $directory . "/S5P_NRTI_L1B_RA_BD8_" . $signatur . ".nc" );
	add_output( $yaml, 'l1b_ir_uvn',    'Physical',  $directory . "/S5P_NRTI_L1B_IR_UVN_" . $signatur . ".nc" );
	add_output( $yaml, 'l1b_ir_sir',    'Physical',  $directory . "/S5P_NRTI_L1B_IR_SIR_" . $signatur . ".nc" );
	add_output( $yaml, 'l1b_ca_uvn',    'Physical',  $directory . "/S5P_NRTI_L1B_CA_UVN_" . $signatur . ".nc" );
	add_output( $yaml, 'l1b_ca_sir',    'Physical',  $directory . "/S5P_NRTI_L1B_CA_SIR_" . $signatur . ".nc" );
	add_output( $yaml, 'l1b_eng_db',    'Physical',  $directory . "/S5P_NRTI_L1B_ENG_DB_" . $signatur . ".nc" );
}

sub add_L0_input ($$@) {
	my ( $yaml, $L0_DIR, @L0_files ) = @_;
	my $element = $yaml->[0];
	add_input( $yaml, 'directory.inp', 'Directory', $L0_DIR );

	#find files to to be added to JOF-yaml representation
	# (already present in parameter @L0_files)

	#sort files by type
	my @l0anc   = ();
	my @l0eng   = ();
	my @l0band1 = ();
	my @l0band2 = ();
	my @l0band3 = ();
	my @l0band4 = ();
	my @l0band5 = ();
	my @l0band6 = ();
	my @l0band7 = ();
	my @l0band8 = ();

	foreach my $L0_file (@L0_files) {
		if ( $L0_file =~ /.*(L0__SAT_A_).*/ ) {
			push( @l0anc, $L0_file );
		}
		if ( $L0_file =~ /.*(L0__ENG_A_).*/ ) {
			push( @l0eng, $L0_file );
		}
		if ( $L0_file =~ /.*(L0__ODB_1_).*/ ) {
			push( @l0band1, $L0_file );
		}
		if ( $L0_file =~ /.*(L0__ODB_2_).*/ ) {
			push( @l0band2, $L0_file );
		}
		if ( $L0_file =~ /.*(L0__ODB_3_).*/ ) {
			push( @l0band3, $L0_file );
		}
		if ( $L0_file =~ /.*(L0__ODB_4_).*/ ) {
			push( @l0band4, $L0_file );
		}
		if ( $L0_file =~ /.*(L0__ODB_5_).*/ ) {
			push( @l0band5, $L0_file );
		}
		if ( $L0_file =~ /.*(L0__ODB_6_).*/ ) {
			push( @l0band6, $L0_file );
		}
		if ( $L0_file =~ /.*(L0__ODB_7_).*/ ) {
			push( @l0band7, $L0_file );
		}
		if ( $L0_file =~ /.*(L0__ODB_8_).*/ ) {
			push( @l0band8, $L0_file );
		}
	}

	add_input( $yaml, 'l0anc',   'Physical', @l0anc );
	add_input( $yaml, 'l0eng',   'Physical', @l0eng );
	add_input( $yaml, 'l0band1', 'Physical', @l0band1 );
	add_input( $yaml, 'l0band2', 'Physical', @l0band2 );
	add_input( $yaml, 'l0band3', 'Physical', @l0band3 );
	add_input( $yaml, 'l0band4', 'Physical', @l0band4 );
	add_input( $yaml, 'l0band5', 'Physical', @l0band5 );
	add_input( $yaml, 'l0band6', 'Physical', @l0band6 );
	add_input( $yaml, 'l0band7', 'Physical', @l0band7 );
	add_input( $yaml, 'l0band8', 'Physical', @l0band8 );

}

#
# - main script
#

# -- parse options
my $dummy;
GetOptions(
	'-pickup-point=s'      => \my $INPUT_L0,
	'-pp_CKD_STAT=s'       => \my $CKD_STAT_PP,
	'-pp_CKD_DYN=s'        => \my $CKD_DYN_PP,
	'-pp_IERSB=s'          => \my $INPUT_IERSB,
	'-pp_IERSC=s'          => \my $INPUT_IERSC,
	'-out0=s'              => \my $WORKING,
	'-out1=s'              => \my $OUTPUT_L1B,
	'-template=s'          => \my $TEMPLATE,
	'-assocName=s'         => \my $assocName,
	'-pdrId=s'             => \my $pdrId,
	'-sliceOverlap=s'      => \my $OVERLAP,
	'-startTime=s'         => \my $startTime,
	'-stopTime=s'          => \my $stopTime,
	'-logLevel=s'          => \my $logLevel,
	'-errLevel=s'          => \my $errLevel,
	'-processingStation=s' => \my $processingStation,
	'-orbit=s'             => \my $orbitNumber,
	'-threads=s'           => \my $threads,
	'-loggingRoot=s'       => \my $loggingRoot,
	'-loggingDumplog=s'    => \my $loggingDumplog,
	'-collectionNumber=s'  => \my $collectionNumber,
	'-processingMode=s'    => \my $processingMode,
	'-rtype=s'             => \$dummy,
	'-debug=s'             => \my $DEBUG
);

#
# -- check options
#

$logger->error_die("paramerter mismatch in (@ARGV)\n")
	unless ( $WORKING && $OUTPUT_L1B && $INPUT_L0 );

#
# -  define yaml JOF representation filename
#

$logger->info( "Preparing creation of new YAML file " . $WORKING . "/JobOrder" . $pdrId . ".yml" );
my $yamlJOFile = $WORKING . "/JobOrder." . $pdrId . ".yml";

#
# -- create JobOrderFile for L1b processor
#

$logger->info("Parsing YAML-Template from \'$TEMPLATE\'\n");
my $template = YAML::Tiny->read($TEMPLATE);

# timestamp
my $timestamp = `date +%Y%m%dT%H%M%S`;
chop $timestamp;

# processor version
my $templateProcessorVersion = get_parameter( $template, "Version", 0, ('Ipf_Conf') );
my @templateProcessorVersionParts = ( $templateProcessorVersion =~ /(\d+)\.(\d+)\.(\d+)/ );
my $versionString =
    sprintf( '%02d', $templateProcessorVersionParts[0] )
  . sprintf( '%02d', $templateProcessorVersionParts[1] )
  . sprintf( '%02d', $templateProcessorVersionParts[2] );

#
# -- put specific parameters into yaml file
#

#identify required L0 data

my $startTimeDate = Time::Piece->strptime( $startTime, '%Y%m%dT%H%M%S' );
$startTimeDate = $startTimeDate - $OVERLAP * 60;

my $stopTimeDate = Time::Piece->strptime( $stopTime, '%Y%m%dT%H%M%S' );
$stopTimeDate = $stopTimeDate + $OVERLAP * 60;

my $test             = 0;
#my @l0FileCandidates = `ls $INPUT_L0/S5P_????_L0__?????_??/S5P*.RAW`;
my @l0FileCandidates = <$INPUT_L0/S5P_????_L0__?????_??/S5P*.RAW>;
my @l0Files;
foreach my $l0File (@l0FileCandidates) {
	my ( $coverageStart, $coverageStop ) = ( $l0File =~ /S5P_...._..__.....__(...............)_(...............).*/ );
	$coverageStart = Time::Piece->strptime( $coverageStart, '%Y%m%dT%H%M%S' );
	$coverageStop  = Time::Piece->strptime( $coverageStop,  '%Y%m%dT%H%M%S' );
	if (
		!(
			(
				   ( $coverageStart < $startTimeDate && $coverageStop < $startTimeDate )
				|| ( $coverageStart > $stopTimeDate && $coverageStop > $stopTimeDate )
			)
		)
	  )
	{
		push( @l0Files, $l0File );
	}
}

# add parameters to jofa
my $start = $startTimeDate->strftime('%Y%m%dT%H%M%S');
$startTime = $startTimeDate->strftime('%Y%m%d_%H%M%S');
$startTime = $startTime . "000000";
my $stop = $stopTimeDate->strftime('%Y%m%dT%H%M%S');
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
add_dynamic_parameter( $template, 'orbit number',    $orbitNumber );
add_dynamic_parameter( $template, 'Processing_Mode', $processingMode );

# add input products
add_input( $template, 'iersbulletinb', 'Directory', ($INPUT_IERSB) );
add_input( $template, 'iersbulletinc', 'Directory', ($INPUT_IERSC) );

my $INPUT_CKD = (sort { -M $a <=> -M $b } <$CKD_STAT_PP/*.?5>)[0];
#? <-- muss drinbleiben, sonst spinnt die Syntaxanalyse im Eclipse
add_input( $template, 'ckd_static', 'Physical', ($INPUT_CKD) );

my $INPUT_ICM_UVN = (sort { -M $a <=> -M $b } <$CKD_DYN_PP/*CKDUVN*.?5>)[0];
add_input( $template, 'ckd_dyn_uvn', 'Physical', ($INPUT_ICM_UVN) );

my $INPUT_ICM_SIR = (sort { -M $a <=> -M $b } <$CKD_DYN_PP/*CKDSIR*.?5>)[0];
add_input( $template, 'ckd_dyn_sir', 'Physical', ($INPUT_ICM_SIR) );

# Add L0 product as last input file
add_L0_input( $template, $INPUT_L0, @l0Files );

# add output products
$orbitNumber = sprintf( "%05d", $orbitNumber );
my $signatur = $start . "_" . $stop . "_" . $orbitNumber . "_" . $collectionNumber . "_" . $versionString . "_" . $timestamp;
add_L1b_output( $template, $signatur, $OUTPUT_L1B );

#
# -- write JOF output
#

$logger->info( "Writing YAML-JOF " . $yamlJOFile );

$template->write($yamlJOFile);

exit 0;
