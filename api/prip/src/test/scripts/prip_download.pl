# force execution of default interpreter found in PATH
eval 'exec perl -S $0 $*' if 0;

#
# prip_download.pl
# ----------------
#
# Download product files from PRIP API and record download results and performance.
#
use 5.016;
use strict;

use English;
use FileHandle;
use JSON;
use Log::Log4perl;
use Time::Piece;
use Time::Seconds;

# Setup logging
Log::Log4perl->init(
        \qq{
      log4perl.rootLogger=ALL, SCREEN    
      log4perl.appender.SCREEN=Log::Log4perl::Appender::ScreenColoredLevels
      log4perl.appender.SCREEN.layout=PatternLayout
      log4perl.appender.SCREEN.layout.ConversionPattern=%d [%p] %m%n
   }
);
my $logger = Log::Log4perl->get_logger();

# Calculating MD5 checksums is somewhat system specific; we want BSD style output
my $MD5_COMMAND = ( $OSNAME eq 'linux' ? 'md5sum --tag' : 'md5' );
# Basic CURL command
my $CURL_COMMAND = 'curl -k --location-trusted'; # since RPRO PRIP ICD 1.0draft5, --location-trusted should be obsolete, but not tested yet

# PRIP API URL --> adapt to actual PRIP host/port as required
my $PRIP_URL = 'https://<PRIP host:port>/proseo/prip/odata/v1';

# Configuration constants --> adapt to your needs
my $mission = 'S5P';
my $user = 'prip-user';
my $password = 'mypassword';
my $filename_start = 'S5P_TEST_L2__CO____';

# Setup output file
my $output_file_name = 'prip_download.out';
my $output_file = FileHandle->new();
$logger->error_die( "Cannot open output file $output_file_name" ) unless $output_file->open( "> $output_file_name" );

$logger->info( "Starting download fo files starting with '$filename_start' for mission '$mission'" );
    
# -- Get product list from PRIP API --
my $curl_result = `$CURL_COMMAND --user \'$mission\\$user:$password\' \"$PRIP_URL/Products?\\\$filter=startswith(Name,\'$filename_start\')\"`;
$logger->debug( "Received product list:\n" . substr( $curl_result, 0, 80 ) . "..." );
my $product_list = from_json( $curl_result );
    
# -- Loop over all products --
foreach my $product ( @{ $product_list->{'value'} } ) {
    # Get product info
    my $product_uuid = $product->{'Id'};
    my $product_file = $product->{'Name'};
    my $product_checksum = $product->{'Checksums'}[0]->{'Value'};
    my $product_size = $product->{'ContentLength'};
    my $product_date = $product->{'PublicationDate'};
    
    $logger->info( "Downloading product $product_file" );

    # Save start time
    my $download_start = localtime;
        
    # Download product
    my $rc = system( "$CURL_COMMAND --user \'$mission\\$user:$password\' \'$PRIP_URL/Products($product_uuid)/\$value\' > $product_file" );

    # Save stop time
    my $download_end = localtime;
        
    # Check correct download
    my $checksum = 'FAILED';
    if ( 0 == $rc ) {
        $checksum = `$MD5_COMMAND $product_file | cut -d '=' -f 2 | cut -d ' ' -f 2`;
        chomp $checksum;
        $logger->warn( "Checksum $checksum of downloaded product does not match expected checksum $product_checksum" )
            unless $checksum =~ /$product_checksum/i;
    }
    else {
        $logger->error( "Download of product $product_file failed with return code " . ( $rc << 8 ) );
    }
        
    # Calculate throughput in Mbps (where 8 Mbps = 1 MB/s; see Wikipedia: https://en.wikipedia.org/wiki/Data-rate_units#Megabit_per_second)
    my $throughput = 'N/A';
    if ( $download_end > $download_start) {
        $throughput = ( $product_size * 8 / 1000000) / ( $download_end - $download_start )->seconds();
    }
        
    # Report download result
    print $output_file "$product_file|$product_size|$product_date|" . $download_start->datetime . "|" 
          . $download_end->datetime . "|" . sprintf( "%.2f", $throughput ) . "|$checksum\n";
}

$output_file->close();