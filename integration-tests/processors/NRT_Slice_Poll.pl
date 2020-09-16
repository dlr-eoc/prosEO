#!/usr/bin/perl
#
# This processor polls for new NRT slices during reception of a orbit
# $Id: NRT_Slice_Poll.pl
#
use 5.016;
use strict;

use Time::Piece;
use Getopt::Long qw(:config pass_through);
use File::Glob qw(:bsd_glob);
use File::Basename;
use File::stat;
use Log::Log4perl;

use utils;

my $logger = Log::Log4perl->get_logger();

$logger->info( "Lookup: " . '$Id: NRT_Slice_Poll.pl' );

# get cmd args
GetOptions(
	'-pickup-point=s'     => \my $PP,
	'-startTime=s'        => \my $STARTTIME,
	'-stopTime=s'         => \my $STOPTIME,
	'-processed-until=s'  => \my $PROCESSED,
	'-timeout=s'          => \my $timeout,
	'-acquisitionOrbit=s' => \my $acquisitionOrbit,
	'-orbit=s'	          => \my $orbit,
    '-slice=s'            => \my $slice,
	'-sliceOverlap=s'     => \my $OVERLAP,
	'-rtype=s'            => \my $TYPE,
	'-sa-debug=s'         => \my $DEBUG
);

unless ($PP) {
	$logger->error_die("parameter mismatch - aborted.");
}

#
# check until when already processed by lookup processor

#
# -- extract start and stop time
#
my $startTime = Time::Piece->strptime( $STARTTIME, '%Y%m%dT%H%M%S' );
my $stopTime  = Time::Piece->strptime( $STOPTIME,  '%Y%m%dT%H%M%S' );

$stopTime = $stopTime + 60 * $OVERLAP;

# determine number of 30 second iterations for checking of new slices
for ( my $iterations = $timeout / 0.5 ; $iterations > 0 ; $iterations-- ) {

	#
	# check pp for files with the following pattern, which have not been handled, yet
	# Example S5P_OPER_L0__ENG_A__20190904T002048_20190904T004047_09799_04.RAW
	# (only .RAW files make sense, and it is sufficient to check for the ENG_A_ file type to reduce the number of files to be checked)
	#

	$logger->info("Checking pp $PP");
	my @l0_products =
	  `find $PP/S5P_????_L0__?????_??/ -maxdepth 1 -name S5P_????_L0__ENG_A__???????????????_???????????????_?????_*.RAW |sort`;
#	!!TODO: does not work without sorting!
#	my @l0_products = <$PP/S5P_????_L0__?????_??/S5P_????_L0__ENG_A__???????????????_???????????????_?????_*.RAW>;
	$logger->error_die("can't access $PP\n") if $? != 0;

	#
	# select all products, covering partially required timespan
	#

	my $earliestStart = '';
	my $latestStop    = '';

	foreach my $l0_product (@l0_products) {
		#chop $l0_product;
		my ( $candidateStartTime, $candidateStopTime, $candidateOrbitNumber ) =
		  ( $l0_product =~ /.*S5P_.{4}_L0__ENG_A__(.{15})_(.{15})_(.{5}).*/ );
		unless ( $candidateStartTime && $candidateStopTime ) {
			$logger->error_die("cannot parse start and stop time from $l0_product");
		}
		$candidateStartTime = Time::Piece->strptime( $candidateStartTime, '%Y%m%dT%H%M%S' );
		$candidateStopTime  = Time::Piece->strptime( $candidateStopTime,  '%Y%m%dT%H%M%S' );
		
		# Analyze all products intersecting with the intended slice time range (including overlap)
		# If there is not enough data for the intended slice, stop creating slices and wait for the next acquisition orbit
		if ( $acquisitionOrbit >= $candidateOrbitNumber                              # picking up left-over data from the previous orbit
            && $candidateStopTime >= $startTime && $candidateStartTime < $stopTime ) # interval closed on left side, open on right side, otherwise we may lose up to a second of data
		{
			$logger->info('... checking product ' . $l0_product . ' with candStart = ' . $candidateStartTime . ', candStop = ', $candidateStopTime);
			$logger->info('... comparing to earliestStart = ' . $earliestStart . ', latestStop = ' . $latestStop);
			if (   ( $earliestStart && ( $candidateStartTime <= $earliestStart ) )
				|| (!$earliestStart && ( $candidateStartTime <= $startTime ) ) )
			{
				$earliestStart = $candidateStartTime;
			}
			if (   ( $latestStop && ( $candidateStopTime >= $latestStop ) )
				|| (!$latestStop && ( $candidateStopTime >= $startTime ) ) )
			{
				$latestStop = $candidateStopTime;
			}

			$logger->info('... found earliestStart = ' . $earliestStart . ', latestStop = ' . $latestStop);
		}
		
		# Success if input data is found covering the whole slice including overlap
		if ( $earliestStart <= $startTime && $latestStop >= $stopTime ) {
			# Write end of slice without overlap to file for later retrieval by NRT_StartTime_Selector.pl
			my $cmd = "echo " . $STOPTIME . "," . $orbit . "," . $slice . " > $PROCESSED";
			$logger->error_die("cannot log stop time $STOPTIME with command $cmd") unless `$cmd` >= 0;
			
			# Exit from loop with success
			print "\nPDR_ADD_PARAM|newSliceFound|true\n";
			exit 5;
		}
	}
	sleep 30;
}
print "\nPDR_ADD_PARAM|newSliceFound|false\n";

# - tell shell adapter to evaluate log for parameters
exit 5;
