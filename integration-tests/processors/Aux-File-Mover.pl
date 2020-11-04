# force execution of default interpreter found in PATH
eval 'exec perl -S $0 $*' if 0;

#
# Aux-File-Mover.pl
# -----------------
#
# Moves AUX files from subdirectories in the given pickup points
#
use 5.016;
use strict;

use Getopt::Long qw(:config pass_through);
use Cwd;
use File::Glob qw(:bsd_glob);
use File::Copy;
use File::Find;
use File::Path qw(remove_tree);
use Log::Log4perl;

use utils;

my $logger = Log::Log4perl->get_logger();

#
# -- helper functions
#

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

$logger->error_die("paramerter mismatch in (@ARGV)\n")
  unless ( $PP_LIST );

#
# -- find files in all subdirectories of all given pickup points and move them to the pickup point
#

foreach my $PP ( split( /,/, $PP_LIST ) ) {
	$logger->info( "Processing pickup point $PP" );
	
	my @subdirs = <$PP/*>;
	
	foreach my $subdir ( @subdirs ) {
		if ( -d $subdir ) {
			$logger->info( "Processing subdirectory $subdir" );
			find( sub { move( $File::Find::name, $PP ) }, $subdir );
			remove_tree( $subdir );
		}
	}
}

exit 0;
