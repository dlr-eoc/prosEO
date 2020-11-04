#
# sftp download client
#
# example:
# ./sftp-download-clt.pl -ldir=/home/reissig/tmp/dest -rdir=tmp/src -rfp=TX- -host=localhost -user=reissig -pass=***** -lfdb=/home/reissig/tmp/fdb.txt -d
#
# $Author: tdm $
# $Id: ftpfunc.pm,v 1.1 2013/12/03 07:25:43 tdm Exp $
#
# (c)DLR 2003 
#
#

sub callback {
    my($ftp, $data, $offset, $size) = @_;
    print "Wrote $offset / $size bytes\n" if $DEBUG > 1;
}

# init secure channel
sub init_connection {
    my($HOST, $USER, $PASS, $DEBUG, $TIMEOUT, $PASSIVE_MODE) = @_;
    print "connecting ftp://$HOST/\n";
    
    $TIMEOUT = 240 unless $TIMEOUT;
    print "connection timeout is $TIMEOUT sec\n" if $DEBUG > 1;
    print "connection mode is " . (($PASSIVE_MODE)? "passive":"active") ."\n";

    my $ftp = Net::FTP->new($HOST, 
                            Timeout => $TIMEOUT, 
                            Passive => $PASSIVE_MODE,
                            Debug   => $SYSDEBUG);
    die "[error] can't connect $HOST" unless $ftp;
    
    print "login as $USER\n";
    $ftp->login($USER, $PASS) || die "[error] Cannot login, ",  $ftp->message;

    if ($ftp) {
        print "[connected]\n";  
    } else {
        print "[connection failed], $ftp->message\n";
        exit -1;
    }
    $ftp->binary;
    $ftp;
}

sub stat {
    my($ftp, $rpath)=@_;

    my($lspath, $dir) = ($rpath =~ /(.*)\/(.*)/);
    my @list = $ftp->dir($lspath);
    foreach(@list) {
        /$dir$/ && return 1;
    }    
    0;
}


sub size {
    my($ftp, $rfile)=@_;    
    $ftp->size($rfile);
}

sub mkdir {
    my($ftp, $rpath, $silent) = @_;
    print "mkdir $rpath ... " if $DEBUG;
    if(&stat($ftp, $rpath)) {
	print "[exists]\n" if $DEBUG; 
	return;
    }        
    unless ($ftp->mkdir($rpath))  {
        print "[failed]\n" if $DEBUG;
    } else {
        print "[ok]\n" if $DEBUG;
    }
}

sub put {
    my($ftp, $src, $dest) = @_;
    die "can not upload $src to $dest\n" unless 
        $ftp->put($src, $dest);
}

sub rename {
    my($ftp, $src, $dest) = @_;
    print "rename $src to $dest ... \n" if $DEBUG;
    unless ($ftp->rename($src, $dest)) {
        print "[failed] mv $src $dest\n";
        return -1;
    }        
    0;
}

sub rmdir {    
    my($ftp, $dir) = @_;
    print "rmdir $dir\n" if $DEBUG;
    ($ftp->rmdir($dir))?0:1;
}

sub remove {    
    my($ftp, $dir) = @_;
    print "delete $dir ... \n" if $DEBUG;
    ($ftp->delete($dir))?0:1;
}

# -- list remote directory
sub ls {
    my($ftp, $dirname)=@_;
    print "ls $dirname ... \n" if $DEBUG;
    $ftp->dir("$dirname");
}

sub quit {
    my($ftp)=@_;
    $ftp->quit;
}

1;
