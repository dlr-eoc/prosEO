#!/bin/bash

set -e

SSH_KEY_PATH="${PWD}/ssh-keys"
SSH_KEY_FILE="${SSH_KEY_PATH}/cluster-key"
SSH_KEY_TYPE="ed25519"
SSH_CONFIG="${SSH_KEY_PATH}/ssh_config"

if [ ! -z "$1" ] ; then
  SSH_KEY_TYPE="$1"
fi

if [ ! -d "${SSH_KEY_PATH}" ] ; then
  echo "Creating SSH key directory..."
  mkdir -p "${SSH_KEY_PATH}"
fi

if [ ! -f "${SSH_KEY_FILE}" ] ; then
  echo "Generating SSH key..."
  ssh-keygen -t "${SSH_KEY_TYPE}" -f "${SSH_KEY_NAME}"
fi

if [ ! -f "${SSH_CONFIG}" ] ; then
  echo "Trying to genreate SSH config..."

  echo >"${SSH_CONFIG}.tmp"
  while IFS= read -r HOSTLINE ; do
    BASTION_IP=$(echo ${HOSTLINE} | cut -d' ' -f1)
    BASTION_HOST=$(echo ${HOSTLINE} | cut -d' ' -f2)

    cat >>"${SSH_CONFIG}.tmp" <<EOF
Host ${BASTION_HOST}
  HostName ${BASTION_IP}
  User linux
  IdentityFile ${SSH_KEY_FILE}
  TCPKeepAlive yes
  IdentitiesOnly yes
EOF

  done < <(./hosts --hostfile | grep bastion-)

  if [ "$(wc -l "${SSH_CONFIG}.tmp" | cut -d' ' -f1)" -gt 1 ] ; then
    mv "${SSH_CONFIG}.tmp" "${SSH_CONFIG}"
    echo "done: ${SSH_CONFIG}"
  else
    rm "${SSH_CONFIG}.tmp"
    echo "No hosts found, maybe you need to generate an inventory first?"
  fi
fi
