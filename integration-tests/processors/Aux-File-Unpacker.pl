# force execution of default interpreter found in PATH
eval 'exec perl -S $0 $*' if 0;

#
# Aux-File-Unpacker.pl
# --------------------
#
# Unpack AUX files within subdirectories of the given pickup points
#
use 5.016;
use strict;

use Getopt::Long qw(:config pass_through);
use Cwd;
use File::Copy;
use File::Glob qw(:bsd_glob);
use File::Find;
use Log::Log4perl;

use utils;

my $logger = Log::Log4perl->get_logger();
my $WAIT_INTERVAL = 30; # test every 30 s whether extraction by other process is done
my $WAIT_TIMEOUT = 600; # stop waiting for extraction by other process after 10 min

#
# -- helper functions
#

#
# -- Extract and delete the archive currently named by $File::Find::name
#
sub unpack() {
	my $TEMP_DIR = '.tmp';
	if ( -d $TEMP_DIR ) {
		# Extraction currently in progress, just wait!
		my $wait_time = 0;
		while ( -d $TEMP_DIR ) {
			$logger->info( "Waiting for unpacking of $File::Find::name to complete ..." );
			$wait_time += $WAIT_INTERVAL;
			$logger->error_die( "Wait for unpacking of $File::Find::name timed out after $WAIT_TIMEOUT s" ) unless $wait_time < $WAIT_TIMEOUT;
			sleep $WAIT_INTERVAL;
		}
	} elsif ( /\.tar$/ ) {
		mkdir( $TEMP_DIR );
		chdir( $TEMP_DIR );
		$logger->info( "Unpacking $File::Find::name ..." );
		( `tar xf ../$_` == 0 ) or $logger->error_die( "Failure during extraction of $File::Find::name" );
		foreach my $file ( <*.nc> ) {
			move( $file, '..' );
		}
		chdir( '..' );
		rmdir( $TEMP_DIR );
		unlink( $File::Find::name ); 
	}
}

#
# - main script
#

# -- parse options
GetOptions(
	'-pp_list=s'	=> \my $PP_LIST,
	'-debug=s'		=> \my $DEBUG
);

#
# -- check options
#

$logger->error_die("parameter mismatch in (@ARGV)\n")
  unless ( $PP_LIST );

#
# -- find tar archives in all subdirectories of all given pickup points and extract and delete them
#

foreach my $PP ( split( /,/, $PP_LIST ) ) {
	$logger->info( "Processing pickup point $PP" );
	
	my @subdirs = <$PP/*>;
	
	foreach my $subdir ( @subdirs ) {
		if ( -d $subdir ) {
			$logger->info( "Processing subdirectory $subdir" );
			find( { wanted => \&unpack, follow => 1 }, $subdir );
		}
	}
}

exit 0;
