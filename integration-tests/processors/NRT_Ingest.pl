#!/usr/bin/perl -w
#
# NOOP processor for use of generic component in NRT check
# $Id: NRT_Ingest.pl
#
use 5.016;
use strict;

use Getopt::Long qw(:config pass_through);

print "NOOP: no transfer required";
exit 0;  
