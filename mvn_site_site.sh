#!/bin/bash
#
# mvn_site_site.sh
# ----------------
#
# Run mvn site:site individually for all modules and submodules

for dir in api/prip api base-wrapper common facility-mgr ingestor interfaces model order-mgr planner processor-mgr productclass-mgr \
           samples/sample-processor samples/sample-wrapper samples storage-mgr ui/backend ui/cli ui/gui ui user-mgr . ; do
    echo
    echo '********************************'
    echo Executing mvn site:site for $dir
    echo '********************************'
    cd $dir
    mvn site:site
    cd -
done

echo
echo '********************************'
echo Executing mvn site:stage
echo '********************************'
mvn site:stage

