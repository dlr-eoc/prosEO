# force execution of default interpreter found in PATH
eval 'exec perl -S $0 $*' if 0;

#
# NRT-L2-Preparator.pl
# --------------------
#
# Prepare processing of NRT L2 products
#
use 5.016;
use strict;

use Getopt::Long qw(:config pass_through);
use Cwd;
use File::Basename;
use File::Glob qw(:bsd_glob);
use File::Find;
use Archive::Tar;
use IO::Uncompress::Gunzip qw(gunzip $GunzipError);
use YAML::Tiny;
use Log::Log4perl;
use Time::Piece;

use utils;
use template_utils;

my $logger = Log::Log4perl->get_logger();

my $TIME_FORMAT = '%Y%m%dT%H%M%S';

#
# -- helper functions
#
sub add_input_product ($$$$) {
	my ( $template, $productKey, $productArray, $code ) = @_;
	
	if ( $productKey =~ /AUX_.*/ ) { 
		# Selection of aux products has been done by NRTAuxSelector
		for my $productDir (@$productArray) {
			my @filenames = <$productDir/*>;
			my %auxProducts;
			for my $filename (@filenames) {
				my $auxProductType = ( $filename =~ /.*\/S5P_...._(..........).*/ )[0];
				if ( $auxProductType eq "" ) {
					if ( $filename =~ /.*\/NISE.*/ ) {
						# NISE has different file name pattern!
						$auxProductType = "AUX_NISE__";
					} else {
						$auxProductType = "UNDEFINED_";
						$logger->warn( 'Unable to extract aux product type for input product ' . $productKey . ' from file name ' . $filename );
					}
					push ( @{ $auxProducts{ $auxProductType } }, $filename );
				}
				elsif ( "AUX_CTMFCT" eq $auxProductType ) {
					# Find real directory behind aux file name (probably a symbolic link)
					my $link = readlink( $filename );
					my $realdir = dirname( $link ? $link : $filename );
					# Include all files of this granule in JOF
					my @fctfiles = <$realdir/*.nc>;
					foreach my $fctfile ( @fctfiles ) {
						push ( @{ $auxProducts{ $auxProductType } }, $fctfile );
					}
				}
				else {
					push ( @{ $auxProducts{ $auxProductType } }, $filename );
				}
			}
			foreach my $auxProductType ( keys %auxProducts ) {
				add_input( $template, $auxProductType, 'Physical', @{ $auxProducts{ $auxProductType } } );
			}
		}
	}
    elsif ( $productKey eq "L1B-IRRADIANCE" ) {
        # L1B-IRRADIANCE in case this preparator is used in OFFL context (DDS!!)
        for my $productDir (@$productArray) {
            my $first    = 1;
            my $filename = '';
            find( sub { /^.*L1B_IR.*\.nc\z/s && $first && ( $filename = $File::Find::name ) && $first--; }, $productDir );
            my $auxProductType = ( $filename =~ /.*\/S5P_...._(..........).*/ )[0];
            add_input( $template, $auxProductType, 'Physical', ($filename) );
        }
    }
	elsif ( $productKey eq "FRESCO" ) {
		for my $productDir (@$productArray) {
			my $first    = 1;
			my $filename = '';
			find( sub { /^.*L2__FRESCO.*\.nc\z/s && $first && ( $filename = $File::Find::name ) && $first--; }, $productDir );
			add_input( $template, 'L2__FRESCO', 'Physical', ($filename) );
		}
	}
	
	# ----- [ Code comparison NRTI/OFFL ]
	elsif ( $productKey eq "L1B" ) {
		for my $productDir (@$productArray) {
			my @filenames = ();
			find( sub { /^.*_L1B_.*\.nc\z/s && push( @filenames, $File::Find::name ); }, $productDir );
			for my $filename (@filenames) {
				my $filetype = ( $filename =~ /.*\/S5P_...._(L1B_.{6})_.*nc/ )[0];
				if ( !( $filetype =~ /.*L1B_IR_.*/ || $filetype =~ /.*L1B_ENG.*/ ||  $filename =~ /.*L1BNRT.*/) ) {
					add_input( $template, $filetype, 'Physical', ($filename) );
				}
			}
		}
	}
	# -----

	elsif ( $productKey eq "AAI" ) {
		for my $productDir (@$productArray) {
			my @filenames = ();
			find( sub { /^.*L2__AER_AI.*\.nc\z/s && push( @filenames, $File::Find::name ); }, $productDir );
			add_input( $template, "L2__AER_AI", 'Physical', $filenames[0] );
		}
	}
	elsif ( $productKey eq "O3" ) {
		for my $productDir (@$productArray) {
			my @filenames = ();
			find( sub { /^.*L2__O3.*\.nc\z/s && push( @filenames, $File::Find::name ); }, $productDir );
			add_input( $template, "L2__O3____", 'Physical', $filenames[0] );
		}
	}
	elsif ( $productKey eq "CLOUD" ) {
		for my $productDir (@$productArray) {
			my @filenames = ();
			find( sub { /^.*L2__CLOUD.*\.nc\z/s && push( @filenames, $File::Find::name ); }, $productDir );
			add_input( $template, "L2__CLOUD_", 'Physical', $filenames[0] );
		}
	}
	elsif ( $productKey eq "O3PR" ) {
		for my $productDir (@$productArray) {
			my @filenames = ();
			find( sub { /^.*L2__O3__PR.*\.nc\z/s && push( @filenames, $File::Find::name ); }, $productDir );
			add_input( $template, "L2__O3__PR", 'Physical', $filenames[0] );
		}
	}
	elsif ( $productKey eq "NPP" ) {
		for my $productDir (@$productArray) {
			if ( !( -e "$productDir/.done" ) ) {

				my $curdir = cwd();
				chdir( $productDir );
				find( 
					sub { 
						/^.*\.tar\z/s &&
						( Archive::Tar->extract_archive( $File::Find::name ) or $logger->error_die( "Failure during extraction of NPP product" ) ); 
					}, 
					$productDir 
				);
				find( 
					sub { 
						/^.*\.tar\z/s &&
						( gunzip( $File::Find::name, $File::Find::name =~ s/\.gz//r ) or $logger->error_die( "Failure during extraction of NPP product" ) ); 
					}, 
					$productDir 
				);
				open my $done, '>' . $productDir . '/.done' or $logger->error_die("Failure during extraction of NPP product");
				close $done;
				chdir( $curdir );
			}

			my @filenames = ();
			find( sub { /^SVM.*\.h5\z/s && push( @filenames, $File::Find::name ); }, $productDir );
			add_input( $template, "VIIRS_L1B_RR", 'Physical', @filenames );

			@filenames = ();
			find( sub { /^GMODO.*\.h5\z/s && push( @filenames, $File::Find::name ); }, $productDir );
			add_input( $template, "VIIRS_L1B_GEO", 'Physical', @filenames );

			@filenames = ();
			find( sub { /^IICMO.*\.h5\z/s && push( @filenames, $File::Find::name ); }, $productDir );
			add_input( $template, "VIIRS_CM", 'Physical', @filenames );
		}
	}
	else {
		$logger->error_die("Configuration for input of type $productKey missing\n");
	}

}

sub add_L2_output ($$$$) {
	my ( $yaml, $code, $signatur, $directory ) = @_;

	my $fileType = '';

	if ( $code eq "CO" ) {
		$fileType = "L2__CO____";
	}
	elsif ( $code eq "AAI" ) {
		$fileType = "L2__AER_AI";
	}
	elsif ( $code eq "FRESCO" ) {
		$fileType = "L2__FRESCO";
	}
	elsif ( $code eq "NO2" ) {
		$fileType = "L2__NO2___";
	}
	elsif ( $code eq "ALH" ) {
		$fileType = "L2__AER_LH";
	}
	elsif ( $code eq "O3PR" ) {
		$fileType = "L2__O3__PR";
	}
	elsif ( $code eq "O3TPR" ) {
		$fileType = "L2__O3_TPR";
	}
	elsif ( $code eq "SO2" ) {
		$fileType = "L2__SO2___";
	}
	elsif ( $code eq "O3" ) {
		$fileType = "L2__O3____";
	}
	elsif ( $code eq "CLOUD" ) {
		$fileType = "L2__CLOUD_";
	}
	elsif ( $code eq "HCHO" ) {
		$fileType = "L2__HCHO__";
	}
	else {
		$logger->error_die("Configuration for output of type $code missing\n");
	}
	add_output( $yaml, $fileType, 'Physical', $directory . "/S5P_NRTI_" . $fileType . "_" . $signatur . ".nc" );
}

#
# - main script
#

# -- parse options
GetOptions(
	'-out0=s'              => \my $WORKING,
	'-out1=s'              => \my $OUTPUT_L2,
    '-pp_bgo3=s'           => \my $PP_BGO3,
    '-pp_bgcld=s'          => \my $PP_BGCLD,
	'-templateDir=s'       => \my $TEMPLATEDIR,
	'-collectionNumber=s'  => \my $collectionNumber,
	'-assocName=s'         => \my $assocName,
	'-orbit=s'             => \my $orbitNumber,
	'-pdrId=s'             => \my $pdrId,
	'-processingMode=s'    => \my $processingMode,
	'-revisionL2=s'        => \my $revision,
	'-startTime=s'         => \my $startTime,
	'-stopTime=s'          => \my $stopTime,
	'-logLevel=s'          => \my $logLevel,
	'-errLevel=s'          => \my $errLevel,
	'-processingStation=s' => \my $processingStation,
	'-threads=s'           => \my $threads,
	'-loggingRoot=s'       => \my $loggingRoot,
	'-loggingDumplog=s'    => \my $loggingDumplog,
	'-absoluteTimeout=s'   => \my $absoluteTimeout,
	'-rtype=s'             => \my $TYPE,
	'-debug=s'             => \my $DEBUG
);

#
# -- check options
#

$logger->error_die("paramerter mismatch in (@ARGV)\n")
  unless ( $WORKING && $OUTPUT_L2 && $TEMPLATEDIR );

#
# -- check product availability
#

my $code = uc( ( $TYPE =~ /nrt-(.*)-preparation/ )[0] );
my $TEMPLATE = $TEMPLATEDIR . "/" . $code . "_TEMPLATE.yml";

$logger->error_die("Input product not available!")
  unless ( ( -e $WORKING ) && ( -e $OUTPUT_L2 ) && ( -e $TEMPLATE ) );

#
# -- in case of NO2, store the revision for reference by the NO2 transfer/reception in s5p-psm-idaf_transfer
#
if ( $code eq 'NO2' ) {
	$logger->error_die( 'Cannot create revision file!' ) unless open( my $revisionFile, '>', $OUTPUT_L2 . '/revision.txt' );
	print $revisionFile $revision . "\n";
	close $revisionFile;
}

#
# -- sort aux input products
#

my %inputProducts;
foreach my $inputProduct (@ARGV) {
	$inputProduct = ( $inputProduct =~ /-in.*=(.*)/ )[0];
	if ( !( $inputProduct eq "" ) ) {
		my $productType = ( $inputProduct =~ /.*\/(.*)-\d*/ )[0];
		push( @{ $inputProducts{$productType} }, $inputProduct );
	}
}

#
# -  define yaml JOF representation filename
#

$logger->info( "Preparing creation of new YAML file " . $WORKING . "/JobOrder" . $pdrId . ".yml" );
my $yamlJOFile = $WORKING . "/JobOrder." . $pdrId . ".yml";

#
# -- create JobOrderFile for L2 processor
#

$logger->info("Parsing YAML-Template from \'$TEMPLATE\'\n");
my $template = YAML::Tiny->read($TEMPLATE);

# timestamp
my $timestamp = `date +%Y%m%dT%H%M%S`;
chop $timestamp;

# processor version
my $templateProcessorVersion = get_parameter( $template, "Version", 0, ('Ipf_Conf') );
my @templateProcessorVersionParts = ( $templateProcessorVersion =~ /(\d\d)\.(\d\d)\.(\d\d)/ );
my $versionString =
    sprintf( '%02d', $templateProcessorVersionParts[0] )
  . sprintf( '%02d', $templateProcessorVersionParts[1] )
  . sprintf( '%02d', $templateProcessorVersionParts[2] );

#
# -- put specific parameters into yaml file
#

my $start = $startTime;
$startTime =~ tr/T/_/;
$startTime = $startTime . "000000";

my $stop = $stopTime;
$stopTime =~ tr/T/_/;
$stopTime = $stopTime . "000000";

#
# -- put general Processor Configuration to JOF
#

set_parameter( $template, 'Stdout_Log_Level', $logLevel, ('Ipf_Conf') );
set_parameter( $template, 'Stderr_Log_Level', $errLevel, ('Ipf_Conf') );

if (  $code eq 'HCHO' || $code eq 'CLOUD' || $code eq 'SO2' || $code eq 'O3' ) {
	set_parameter( $template, 'Start', $startTime, ( 'Ipf_Conf', 'Sensing_Time' ) );
	set_parameter( $template, 'Stop',  $stopTime,  ( 'Ipf_Conf', 'Sensing_Time' ) );
}
else {
	set_parameter( $template, 'Start', '00000000_000000000000', ( 'Ipf_Conf', 'Sensing_Time' ) );
	set_parameter( $template, 'Stop',  '99999999_999999999999', ( 'Ipf_Conf', 'Sensing_Time' ) );
}

add_dynamic_parameter( $template, 'logging.root',    $loggingRoot );
add_dynamic_parameter( $template, 'logging.dumplog', $loggingDumplog );
add_dynamic_parameter( $template, 'Threads',         $threads );
add_dynamic_parameter( $template, 'Processing_Mode', $processingMode );

#
# -- add input products
#

for my $productKey ( keys %inputProducts ) {
	my $productArray = $inputProducts{$productKey};
	add_input_product( $template, $productKey, $productArray, $code );
}


#
# -- add local auxiliary input
# *** TODO Code must be removed as soon as BGO3 and BGCLD are available in the PL ***
my $sensingStart = Time::Piece->strptime( $start, $TIME_FORMAT );
my $sensingStop = Time::Piece->strptime( $stop, $TIME_FORMAT );

if ( $code eq "O3" ) {
    my @auxFileCandidates = `ls $PP_BGO3/*.nc|sort -r`;
    my $auxFile; 
    foreach my $auxFileCandidate (@auxFileCandidates) {
        chop $auxFileCandidate;
        my ( $candidateStart, $candidateStop ) = ( $auxFileCandidate =~ /S5P_...._AUX_BGO3___(........T......)_(........T......).*/ );
        $candidateStart = Time::Piece->strptime( $candidateStart, $TIME_FORMAT );
        $candidateStop  = Time::Piece->strptime( $candidateStop,  $TIME_FORMAT );
        if ( $candidateStart < $sensingStop && $candidateStop > $sensingStart - 28 * 24 * 60 * 60 ) {
            $auxFile = $auxFileCandidate;
            last;
        }
    }
    if ( -e $auxFile ) {
        $logger->info("Adding BGO3 Product $auxFile.");
        add_input( $template, "AUX_BGO3__", 'Physical', ($auxFile) );
    }
    else {
        $logger->warn("No BGO3-Product found! Continuing without background correction.");
    }
}

if ( $code eq "CLOUD" ) {
    my @auxFileCandidates = `ls $PP_BGCLD/*.nc|sort -r`;
    my $auxFile; 
    foreach my $auxFileCandidate (@auxFileCandidates) {
        chop $auxFileCandidate;
        my ( $candidateStart, $candidateStop ) = ( $auxFileCandidate =~ /S5P_...._AUX_BGCLD__(........T......)_(........T......).*/ );
        $candidateStart = Time::Piece->strptime( $candidateStart, $TIME_FORMAT );
        $candidateStop  = Time::Piece->strptime( $candidateStop,  $TIME_FORMAT );
        if ( $candidateStart < $sensingStop && $candidateStop > $sensingStart - 28 * 24 * 60 * 60 ) {
            $auxFile = $auxFileCandidate;
            last;
        }
    }
    if ( -e $auxFile ) {
        $logger->info("Adding BGCLD Product $auxFile.");
        add_input( $template, "AUX_BGCLD_", 'Physical', ($auxFile) );
    }
    else {
        $logger->warn("No BGCLD-Product found! Continuing without background correction.");
    }
}
# *** END Code must be removed ***

# add output products
$orbitNumber = sprintf( "%05d", $orbitNumber );
my $signatur = $start . "_" . $stop . "_" . $orbitNumber . "_" . $collectionNumber . "_" . $versionString . "_" . $timestamp;

add_L2_output( $template, $code, $signatur, $OUTPUT_L2 );

#
# -- write JOF output
#

$logger->info( "Writing YAML-JOF " . $yamlJOFile );

$template->write($yamlJOFile);

exit 0;
