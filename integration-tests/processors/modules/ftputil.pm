#
# sftp download client
#
# example:
# ./sftp-download-clt.pl -ldir=/home/reissig/tmp/dest -rdir=tmp/src -rfp=TX- -host=localhost -user=reissig -pass=***** -lfdb=/home/reissig/tmp/fdb.txt -d
#
# $Author: reissig $
# $Id: ftputil.pm,v 1.32 2010/09/14 14:30:02 reissig Exp $
#
# (c)DLR 2003 
#

#

use MIME::Base64;
use File::Basename;

# -- global stuff
$RFP='.*';

# -- functions
sub usage {
    print "USAGE:\n$0 -ldir=local_dir -rdir=remote_dir -rfp=rmote_file_pattern -host=ftp_host -user=user -[enc]pass=password -lfdb=local_transfer_db \n\t[-drf ... delete file from  server ] \n\t[-d ... debug on ]\n\t[-encrypt-password=<string> ... base64 encryption for passwords ]\n";
}

# -- append entry in local db
sub mark_transferred {
    my($name, $size, $time) = @_;
    open (DB, ">>$LFDB") || die "couldn't open $LFDB";
    print DB "$name\t$size\t$time\n";
    close(DB);
}

# reads db and returns everything as hash
sub load_db {
    my %tfiles;
    return \%tfiles unless -e $LFDB;

    open(DB, $LFDB) || die "can not open database file $dbname\n";
    m/(\S+)\s+(\S+)\s+(\S+)/ && ($tfiles{"$1$2$3"} = $_) 
        while <DB>;
    close (DB);
    
    \%tfiles;
}


# -- download files from remote server
sub download {
    # -- start main
    print "download-clt v $VERSION started\n";  
    # -- parse cmd line
    # Process options.
    if ( @ARGV > 0 ) {
        GetOptions(
		   'd'=> \$DEBUG,
                   'd=s' => \$DEBUG,
		   'debug=s'=> \$DEBUG,
                   'drf'=> \$DRF,    # delete remote file after transfer
                   'dlc'=> \$DLC,    # disable local db check
                   'fsc'=> \$FSC,    # file size check, undoc. feature :)
                   'rfs=s' => \$RFS, # required file size
                   'ldir=s' => \$LDIR,
                   'rdir=s' => \$RDIR,
                   'rfp=s'  => \$RFP,
                   'host=s' => \$HOST,
                   'user=s' => \$USER,
                   'pass=s' => \$PASS,
                   'key=s' =>  \$KEY,
                   'encpass=s' => \$EPASS,
                   'lfdb=s' => \$LFDB,
                   'rtype=s' => \$dummy,
                   'help' => \$help,
                   'h' => \$help,
                   'r' => \$rec,
                   'passive' => \$PASSIVE_MODE,
                   'followlinks' => \$FOLLOW_LINKS,
                   'manual' => \$man,
                   'timeout=s' => \$TIMEOUT,
                   'init_con_timeout=s' => \$ITIMEOUT,
                   'encrypt-password=s' => \$plainPassword);
    } else {
        usage;
        exit -1;
    }

    if ( $man or $help ) {
        usage;
        exit 0;
    }

    if ( $plainPassword ) {
        print "encoded password for \'$plainPassword\' is " .
            encode_base64($plainPassword);
        exit 0;
    }

    die '[error] missing parameter' 
        unless ($TDIR && $LDIR && $RDIR && $HOST && $USER && $LFDB);

    # -- encoded passwords?
    $PASS = decode_base64($EPASS) if $EPASS;

    # init connection is defined in main routine
    $SYSDEBUG = 0;
    $SYSDEBUG = $DEBUG if $DEBUG > 2;
    my $ftp = &init_connection($HOST, $USER, $PASS, 
                               $SYSDEBUG, $TIMEOUT, $PASSIVE_MODE, 
                               $KEY, $ITIMEOUT);

    if ($ftp) {
        print "connected!\n";  
    } else {
        die '[error] connection failed!';
    }

    # -- create tmp dir where we are going to transfer files
    $RDIR="$RDIR";
    $LDIRTMP = "$LDIR/.tmp_" . time() ."/";
    system "mkdir $LDIRTMP" unless -e $LDIRTMP;
    die ' Cannot create $LDIRTMP\n' if $? != 0;

    # load local db
    $transdb = &load_db;

    # -- dir listing
    
    @remote = &get_remote_files($ftp, $RDIR, $FOLLOW_LINKS);
    if ($DEBUG > 1) {
        print "REMOTE: $_\n" foreach(@remote); 
    }

    print "seeing # " . ($#remote + 1) . " files in $RDIR\n" if $DEBUG > 0;
    $ctf=0;
    foreach (@remote) {
        print "process: $_ ...\n" if $DEBUG > 2;
        next if /^d/ || /<DIR>/ || /^total/; # no dirs
        next unless /$RFP/;                  # only matching lines,
        unless (/(\S+)\s+(\w{3}\s+\d{1,2}\s+\S+)\s+(\S+)$/) { # that can be parsed    
            print "[warning] can not parse: $_\n"; 
            next;
        } 

        ($fsize, $ftime, $fname)=($1,$2,$3);
        $fname =~ s;\s;\\ ;g;
        $ftime =~ s/\s+/-/g;
        #die "can't parse: $_\n" unless (defined $fname && defined $fsize);
        print "got: name[$fname] size($fsize) time($ftime)\n" if $DEBUG;

        if(!$DLC && (exists $transdb->{"$fname$fsize$ftime"})) {
            print "[$fname] already transferred!\n" if $DEBUG;
            &remove($ftp,"$fname") if $DRF;
            next;
        }

        if($FSC && $fsize != $RFS) {
            print "[error] (wrong file size detected, aborting)\n" ;
            next;
        }

        # -- transfer new  file
        #my($dir, $base) = ($fname =~ /(.*)\/(.*)/);        
        print "[$fname] transferring ($fsize)\t... ";          
        $base = basename($fname);
        $dir  = dirname($fname);

        $ftp->get($fname, $LDIRTMP . $base);

        $lsize = -s $LDIRTMP . $base;
        die "[error] ($fname: wrong byte number received. local($lsize) remote($fsize))" 
            if($fsize != $lsize);

        # -- move file up 
        $mv = system "mv $LDIRTMP"."$base $LDIR/$base";
        die "[error] Cannot move $LDIRTMP"."$base to $LDIR/$base\n" if $mv;

        print "[ok]\n";
        &remove($ftp, "$fname") if $DRF;
        &mark_transferred($fname, $fsize, $ftime);
        $ctf++;
    }

    &quit($ftp);
    `rmdir $LDIRTMP` if -e $LDIRTMP;
    print "ftp-clt finished, $ctf file(s) transferred.\n";
}

# -- upload dir structure onto remote server
sub upload {
    $TMPDIR = "tmp_" . time();
    $cexit = 0;

    # -- start main
    print "upload-clt v $VERSION started\n";  
    # -- parse cmd line
    # Process options.
    if ( @ARGV > 0 ) {
        GetOptions(
		   'd'=> \$DEBUG,
                   'd=s' => \$DEBUG,
		   'debug=s'=> \$DEBUG,
                   'ldir=s' => \$LDIR,
                   'lfdb=s' => \$LFDB,
                   'in0=s'  => \$LDIR, # undoc. feature requested by Karl-Heinz
                   'fp=s'   => \$FP,
                   'rdir=s' => \$RDIR,
		   'tdir=s' => \$TDIR,
                   'host=s' => \$HOST,
                   'user=s' => \$USER,
                   'pass=s' => \$PASS,
                   'key=s' =>  \$KEY,
                   'encpass=s' => \$EPASS,
                   'dat'    => \$DELETE_AFTER_TRANSFER,
                   'overwrite'=> \$OVERWRITE_DEST,
                   'r'      => \$RECURSIVE,
                   'rtype=s'=> \$dummy,
                   'help'   => \$help,
                   'h'      => \$help,
                   'manual' => \$man,
                   'passive' => \$PASSIVE_MODE,
                   'timeout=s' => \$TIMEOUT,
                   'init_con_timeout=s' => \$ITIMEOUT,
                   'encrypt-password=s' => \$plainPassword);
    } else {
        usage;
        exit -1;
    }

    if ( $man || $help ) {
        usage();
        exit 0;
    }

    if ( $plainPassword ) {
        print "encoded password for \'$plainPassword\' is " .
            encode_base64($plainPassword);
        exit 0;
    }

    die "[error] missing parameter.\n" 
        unless $TDIR && $LDIR && $RDIR && $HOST && $USER;

    die "[error] can't access local dir $LDIR"
        unless -e $LDIR;

    # set default filter 
    $FP=".*" unless $FP;
    %tfiles; 
    @tdirs;

    # -- encoded passwords?
    $PASS = decode_base64($EPASS) if $EPASS;
    $SYSDEBUG = 0;
    $SYSDEBUG = 1 if $DEBUG > 2;

    local @tfiles; # files to be transferred
    local %tdirs; # dirs to be transferred

    # -- make connection
    $ftp = &init_connection($HOST, $USER, $PASS, $SYSDEBUG, $TIMEOUT, $PASSIVE_MODE, $KEY, $ITIMEOUT);

    # load local db
    $transdb = &load_db if defined $LFDB;

    $cmd = "find $LDIR -type f";
    print "exec: $cmd\n" if $DEBUG;
    open (FIND, "$cmd |") || die "couldn't access local files in $LDIR\n!";
    
    while ($file  = <FIND> ) { # get all local files 
        chop $file;

        unless($RECURSIVE) { # skip sub dirs
            $_=$file;
            s/($LDIR)\///;            
            if(/\//) { # ommit file in subdir 
                print "skipping $file in non recursive mode\n" if $DEBUG;
                next;
            }
        }

        unless($file =~ /$FP/) { # match pattern
            print "skipping $file\n" if $DEBUG;
            next;
        }

        print "processing $file\n" if $DEBUG;
        local($path, $basename) = $file =~ m/$LDIR\/{0,1}(.*)\/(.*)$/;    

        # -- get directory structure
        if($path) { # file lives in subdirectory 
	    local $rpath = $TDIR;
            #local $rpath = "$RDIR/$TMPDIR";
            local $lpath;
            foreach (split('/', $path)) { # get dir structure
                $rpath .="/$_"; 
                $lpath .="/$_"; 
                $tdirs{"$rpath"} = $lpath;
            }
        }

	# -- check if file has already been uploaded
        if(defined $LFDB) {
	    local($dev, $ino, $mode, $nlink, $uid, 
		  $gid, $rdev, $size, $atime, $mtime, 
		  $ctime, $blksize, $blocks) = stat($file);
	    ($fsize, $ftime, $fname)=($size,$mtime,$file);
	    print "DB lookup: $fname$fsize$ftime\n" if $DEBUG;
	    if(exists $transdb->{"$fname$fsize$ftime"}) {
		print "[$fname] already transferred!\n" if $DEBUG;
		next;
	    }
	}

        # -- get file
        push @tfiles, "$path/$basename";
    }

    # -- create directories
    # create top level directory if not there
    &mkdir($ftp, "$RDIR") unless &stat($ftp, "$RDIR");
    &mkdir($ftp, $_) foreach(sort keys %tdirs);

    # put files into temp dir

    foreach(@tfiles) {
	local($src, $dest) = ("$LDIR/$_","$TDIR/$_");
        #local($src, $dest) = ("$LDIR/$_","$RDIR/$TMPDIR/$_");
        local $fsize = -s $src;
        &put($ftp, $src, $dest);
        if( &size($ftp, $dest) != $fsize ) {
            print "[failed]\n";
            $ftp->quit;
            exit -1;
        }
    }

    #rename src dir to final dir. Not really a loop - ends after first execution.
 
    foreach(sort values %tdirs) {
        local($src, $dest) = ("$TDIR/$_", "$RDIR/$_");        

        if (&rename($ftp, $src, $dest)){
            die "can not move $src to $dest\n";
	    }
	    last;
    }
	# -- log transfer if requested
	if(defined $LFDB) {
	    # transfer ok, log the filename
	    local $file = "$LDIR/$_"; $file =~ s://:/:g;
	    local($dev, $ino, $mode, $nlink, $uid, 
		  $gid, $rdev, $size, $atime, $mtime, 
		  $ctime, $blksize, $blocks) = stat($file);
	    ($fsize, $ftime, $fname)=($size,$mtime,$file);
	    &mark_transferred($fname, $fsize, $ftime);
	    print "DB save: $fname$fsize$ftime\n" if $DEBUG;
	}


    
    &quit($ftp);
    print "done, " . ($#tfiles + 1)  . " file(s) transferred.";
    print " ... but some errors occured during data transfer!\n" 
        if $cexit != 0;

    print "\n$HOST ... [disconnected].\n";
    exit $cexit if $cexit != 0;
}

sub get_remote_files {
    my($ftp, $dir, $links)=@_;
    my @files=();
    my @ls=();

    #print "DIR  ($rec): $dir\n";
    foreach(&ls($ftp, $dir)) {

        print "ls:$_\n" if $DEBUG > 4;

        # symbolic link
        if(/^l/) {
            if ($links && /(\S+)\s{0,}$/) {                            
                local $newDir = $1;
                print "resolving link: $newDir\n" if $DEBUG > 1;
                local @dirs = &ls($ftp, $newDir);
                $_ = $dirs[0];
                s/(\S+)$/$newDir/;
                push @ls, $_; 
            }
        } else {
            # dir or file 
            if (/(.*)\s(.*)$/) {            
                push @ls, $1." ".$dir."/".$2; # store fully fledged file info
            }
        }
    }

    if($rec) { # recurse dir structure on next level
        my $rfiles=();
        /^d.*\s(.*)$/ && push @ls, &get_remote_files($ftp, "$1", $links) 
            foreach(@ls);
    }

    # insert path info
    /^[-,l].*\s(.*)$/ && push @files, $_ foreach(@ls);

    @files;
}

1;
