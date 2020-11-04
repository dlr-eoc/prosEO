#!/usr/bin/perl
#
# This processor identifies the start time to be processed next
# $Id: NRT_StartTime_Selector.pl
#
use 5.016;
use strict;

use Getopt::Long qw(:config pass_through);
use File::Basename;
use Log::Log4perl;
use Time::Piece;

use utils;

my $logger = Log::Log4perl->get_logger();

sub extractOrbitData ($$) {
	my ( $oefDir, $startTime ) = @_;
	my @oef_files = <$oefDir/S5P_????_MPL__SPF___????????T??????_????????T??????_????.TGZ>;
	$logger->error_die("can't access $oefDir\n") if $? != 0;

	my $oef = '';

	# Temporal Coverage Required for 3 consecutive orbits starting with startTime - 1.5 orbit length
	my $startDate = Time::Piece->strptime( $startTime, '%Y%m%dT%H%M%S' ) - 101 * 90;
	# my $stopDate = $startDate + 60 * 420; # This amounts to 3 x 140 min, but probably correct would be 3 x 1:41:30 h! Variable not used anyway
    my $stopDate = $startDate + 3 * 101 * 60; # Three orbit lengths (more or less)

	# Variables defining 3 orbits
	my $orbitNumber0;
	my $orbitStart0;
	my $orbitNumber1;
	my $orbitStart1;
	my $orbitNumber2;
	my $orbitStart2;

	# Identify best oef file (newest file covering the whole orbit [roughly lasting 100 minutes])
	# TODO Actually the orbit does not start at startTime - the slice does, the orbit itself might have started up to 100 min earlier
	#      The OEF file therefore must at least cover the time from startTime - 1 orbit length to startTime + 1 orbit length
	#      This would explain, why we select three orbits for the NRTControlRule to choose from
	#      The current code fails, if the start time is earlier than the first orbit in the OEF, 
	#      but not before the validity start time of the OEF. This does not happen in systematic processing, because the OEF's
	#      are never that young (they are at least one day old - on 2020-09-14 the file with the latest validity was for
	#      2020-09-13 to 2020-10-11), but it happens in reprocessing (which was never envisioned for NRTI in the first place ...)
	foreach my $oef_file (@oef_files) {

		# chop $oef_file;
		my ( $fileStartTime, $fileStopTime ) = ( $oef_file =~ /.*S5P_.{4}_MPL__SPF___(.{15})_(.{15})_.{4}.*/ );

		if ( $fileStartTime eq "" || $fileStopTime eq "" ) {
			$logger->error_die("could not extract start and stop time from file $oef_file. Exiting.");
		}

		$logger->info("Checking File $oef_file");

		my $fileStartTimeDate = Time::Piece->strptime( $fileStartTime, '%Y%m%dT%H%M%S' );
		my $fileStopTimeDate  = Time::Piece->strptime( $fileStopTime,  '%Y%m%dT%H%M%S' );
		$logger->info("Extracted validity start time $fileStartTime and validity stop time $fileStopTime");

		if (   ( $startDate >= $fileStartTimeDate )
            && ( $stopDate <= $fileStopTimeDate ) )
            #&& ( $startDate + 60 * 100 <= $fileStopTimeDate ) ) # Should be 3 orbits, i. e. 3 x 1:40 h! (see above)
		{
			$oef = $oef_file;
		}
	}

	if ( $oef eq "" ) {
		$logger->info("Lookup:No matching OEF available. Exiting");
		exit 0;
	}
	else {
		$logger->info("Lookup: Selected file $oef for analysis of next orbit to be processed");
	}

	# Extract orbit information

	my @smx_events = `tar -xf $oef -O | grep -B2 'Spacecraft Midnight Crossing' | grep EVRQ_Time`;

	my $lastOrbitStart;
	my $lastOrbitNumber;

	foreach my $smx_event (@smx_events) {
		my $smx_event = ( $smx_event =~ /<EVRQ_Time>UTC=(.*)<\/EVRQ_Time>/ )[0];
		my ( $smxDate, $smxDateMilliSeconds ) = ( $smx_event =~ /(.{4}-.{2}-.{2}T.{2}:.{2}:.{2})\.(.{3})/ );
		$smxDate = Time::Piece->strptime( $smxDate, '%Y-%m-%dT%H:%M:%S' );
		if ( ( $startDate < $smxDate ) ) {

			if ( $orbitStart0 eq "" ) {
				$orbitStart0  = $lastOrbitStart;
				$orbitNumber0 = $lastOrbitNumber;
			}
			elsif ( $orbitStart1 eq "" ) {
				$orbitStart1  = $lastOrbitStart;
				$orbitNumber1 = $lastOrbitNumber;
			}
			elsif ( $orbitStart2 eq "" ) {
				$orbitStart2  = $lastOrbitStart;
				$orbitNumber2 = $lastOrbitNumber;
			}
			else {
				last;
			}
		}
		$lastOrbitStart  = $smxDate->strftime('%Y%m%dT%H%M%S');
		$lastOrbitNumber = `tar -xf $oef -O | grep -A6 '$smx_event' | grep EV_Absolute_orbit`;
		$lastOrbitNumber = ( $lastOrbitNumber =~ /<EV_Absolute_orbit>(.*)<\/EV_Absolute_orbit>/ )[0];

	}
	print "\nPDR_ADD_PARAM|orbitStart0|" . $orbitStart0 . "\n";
	print "\nPDR_ADD_PARAM|orbitNumber0|" . $orbitNumber0 . "\n";
	print "\nPDR_ADD_PARAM|orbitStart1|" . $orbitStart1 . "\n";
	print "\nPDR_ADD_PARAM|orbitNumber1|" . $orbitNumber1 . "\n";
	print "\nPDR_ADD_PARAM|orbitStart2|" . $orbitStart2 . "\n";
	print "\nPDR_ADD_PARAM|orbitNumber2|" . $orbitNumber2 . "\n";

}

$logger->info( "NRT_StartTime_Selector " . '$Id: NRT_StartTime_Selector.pl' );

# get cmd args
GetOptions(
	'-pp-oef=s'       => \my $oefDir,
	'-lastStop=s'     => \my $lastStop,
	'-startTime=s'    => \my $startTime,
	'-sliceLength=s'  => \my $sliceLength,
	'-sliceOverlap=s' => \my $sliceOverlap,
	'-rtype=s'        => \my $TYPE
);

unless ( !( $oefDir eq "" ) && !( $lastStop eq "" ) && !( $sliceLength eq "" ) && !( $sliceOverlap eq "" ) ) {
	$logger->error_die( "parameter mismatch lastStop or pp-oef or sliceLength not defined - aborted." );
}

# If no slice number can be determined, the default will be to start counting again
my $orbit = 0;
my $slice = 0;

$logger->info( "Lookup: checking for last request end..." );

if ( -e $lastStop ) {
	open FILE, $lastStop or die "Couldn't open file: $!";
	my $lastStopRecord = <FILE>;
	close FILE;
	chomp $lastStopRecord;
	$logger->info( "Last stop record: " . $lastStopRecord );

	my ( $newStartTime, $newOrbit, $newSlice ) = ( $lastStopRecord =~ /(\S*),(\S*),(\S*).*/ );
	my $newStartTimeDateTime = Time::Piece->strptime( $newStartTime, '%Y%m%dT%H%M%S' );
	my $startTimeDateTime    = Time::Piece->strptime( $startTime,    '%Y%m%dT%H%M%S' );
	# If the start time given is at the expected next start time or thereafter, but not by more than a slice length incl. overlap,
	# use the expected start time and slice number. In no case may the start time be before the expected next start time, because
	# that would mean that a range of data is processed twice.
	# Example: startTime 2019-09-04T11:13:13, sliceLength 5, sliceOverlap 1
	# --> newStartTime from file after 2019-09-04T11:06:13 up to and including 2019-09-04T11:13:13
	#     (the ideal value being 2019-09-04T11:13:13, i. e. exactly at the requested start time)
	
	if (   ( $newStartTimeDateTime + $sliceLength * 60 + $sliceOverlap * 2 * 60 > $startTimeDateTime )
		&& ( $newStartTimeDateTime <= $startTimeDateTime ) )
	{
		$startTime = $newStartTime;
		$orbit     = $newOrbit;
		$slice     = $newSlice;
	} else {
		$logger->info( "Requested start time outside of expected time interval" );
	}
} else {
	$logger->info( "Timestamp file $lastStop not found!" );
}

# Extract matching orbit number
extractOrbitData( $oefDir, $startTime );

print "\nPDR_ADD_PARAM|startTime|" . $startTime . "\n";
print "\nPDR_ADD_PARAM|orbit|" . $orbit . "\n";
print "\nPDR_ADD_PARAM|slice|" . $slice . "\n";

# - tell shell adapter to evaluate log for parameters
exit 5;
