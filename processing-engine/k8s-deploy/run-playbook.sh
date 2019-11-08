#!/bin/bash
set -e
ansible-playbook -i ansible-conf/hosts $@
