#!/usr/bin/perl
#
# The program checks if new data are available from the pickup point.
# It generates one NRTControl PdR per acquisition orbit.
#
# Author: Ralf Reissig (c) DLR 2004
#
# $Id: NRT-Lookup.pl,v 1.3 2013/12/03 07:32:23 tdm Exp $
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

$logger->info("NRT-Lookup: Starting ");

# get cmd args
my $dummy;
GetOptions(
	'-pickup-point=s'       => \my $PP,
	'-file-mover=s'         => \$dummy,
	'-ingest-history=s'     => \my $HISTORY,
	'-processing-timeout=s' => \my $RELATIVE_TIMEOUT,
	'-ingestion-schema=s'   => \$dummy,
	'-rtype=s'              => \my $TYPE,
	'-sa-debug=s'           => \my $DEBUG
);

unless ($PP) {
	$logger->error_die("parameter mismatch - aborted.");
}

my $datestring = "2015-01-01T00:00:00";
if ( -e $HISTORY ) {
	open FILE, $HISTORY or die "Couldn't open file: $!";
	binmode FILE;
	$datestring = <FILE>;
	chop $datestring;
	$datestring = ( $datestring =~ /.*,(.*)/ )[0];
	close FILE;
}

# check pp for files with the following pattern, which have not be handled, yed
# Example S5P_GSOV_L0__SAT_A__20140830T035905_20140830T040405_12345_07XML

$logger->info("Lookup: checking pp ...");
my @l0_products = <$PP/S5P_????_L0__?????_??/S5P_????_L0_???????_???????????????_???????????????_?????_??.???>;
$logger->error_die("can't access $PP\n") unless scalar(@l0_products);

my $lastOrbit       = 0;
my $now             = Time::Piece->new;
my $absoluteTimeout = $now - $RELATIVE_TIMEOUT;

$logger->info("Checking for data newer then $absoluteTimeout");

foreach my $l0_product (@l0_products) {
	next unless ( gmtime( stat($l0_product)->mtime )->datetime ge $datestring );
	my ( $startTime, $acquisitionOrbit ) = ( $l0_product =~ /.*S5P_.{4}_L0_.{7}_(.{15})_.{15}_(.{5}).*/ );
	$logger->error_die("Could not extract acquisition orbit number from $l0_product\n") if ( $startTime eq "" ) || ( $acquisitionOrbit eq "" );
	my $absoluteTimeout = Time::Piece->strptime( $startTime, '%Y%m%dT%H%M%S' ) + $RELATIVE_TIMEOUT;
	if ( $acquisitionOrbit > $lastOrbit && $absoluteTimeout > $now ) {
		$lastOrbit = $acquisitionOrbit;

		# check if orbit was already processed
		if ( -e $HISTORY ) {
			# TODO grep ersetzen
			my $historyCheck = `grep $acquisitionOrbit $HISTORY`;
			chop $historyCheck;
			if ( $historyCheck eq "" ) {
				my $timestamp = Time::Piece->strptime( ( stat($l0_product) )[0][9], '%s' );
				$timestamp = $timestamp->strftime('%Y-%m-%dT%H:%M:%S');

				# TODO echo ersetzen
				`echo "$acquisitionOrbit,$timestamp" >> $HISTORY`;
				print "PDR_NEW_PARAM|acquisitionOrbit|$acquisitionOrbit\n";
				print "PDR_NEW_PRODUCT|S5P.TROPOMI.L0|$l0_product\n";
				print "PDR_NEW_PARAM|startTime|$startTime\n";
				exit 5;
			}
		}
		else {
			my $timestamp = Time::Piece->strptime( ( stat($l0_product) )[0][9], '%s' );
			$timestamp = $timestamp->strftime('%Y-%m-%dT%H:%M:%S');

			# TODO echo ersetzen
			`echo "$acquisitionOrbit,$timestamp" >> $HISTORY`;
			print "PDR_NEW_PARAM|acquisitionOrbit|$acquisitionOrbit\n";
			print "PDR_NEW_PRODUCT|S5P.TROPOMI.L0|$l0_product\n";
			print "PDR_NEW_PARAM|startTime|$startTime\n";
			exit 5;
		}
	}
}

exit 1;
