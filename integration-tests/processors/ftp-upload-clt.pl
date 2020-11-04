# force execution of default interpreter found in PATH
eval 'exec perl -S $0 $*' if 0;
#
# ftp upload client
#
# example:
# ftp-upload-clt.pl -ldir=/home/reissig/tmp/upl -rdir=tmp/dest/ -host=localhost -user=xxxx -pass=***** -debug=0
# 
# $Author: tdm $
# $Id: ftp-upload-clt.pl,v 1.1 2013/12/03 07:24:41 tdm Exp $
#
# (c) DLR 2003-2005
#
BEGIN { push @INC, substr($0, 0, rindex($0, "\/"));
        push @INC, substr($0, 0, rindex($0, "\/")) . "/modules";
        push @INC, substr($0, 0, rindex($0, "\/")) . "/modules/lib";
    }


use Net::FTP;
use Getopt::Long;
use Cwd;
use MIME::Base64;
use ftpfunc;
use ftputil;

($VERSION) = '$Revision: 1.1 $' =~ /(\d+\.\d+)/;

# -- functions
sub usage {
    print "USAGE:\n$0 -ldir=local_dir -rdir=remote_dir -host=ftp_host -user=user -[enc]pass=password [-fp=file_pattern] [-lfdb=local_transfer_db] [-debug=0|1|2] \n\t-rec ... recursive processing of input\n\t[-timeout=num ... connection timeout in sec ]\n\t[-passive ... passive mode(pasv) ]\n\t[-encrypt-password=<string> ... base64 encryption for passwords ]\n";
}

&upload;
