#!/bin/bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
SOURCE_DIR="${SCRIPT_DIR}/../"

SSH_KEY_PATH="${PWD}/ssh-keys"
SSH_KEY_FILE="${SSH_KEY_PATH}/cluster-key"
SSH_KEY_TYPE="ed25519"
SSH_CONFIG="${SSH_KEY_PATH}/ssh_config"
SSH_ENVFILE="${PWD}/config/ssh.sh"
SSH_AGENT_ENVFILE="${PWD}/config/ssh_agent.sh"
SSH_ANSIBLE_ENVFILE="${PWD}/config/ansible.sh"

if [ ! -z "$1" ] ; then
  SSH_KEY_TYPE="$1"
fi

if [ ! -d "${SSH_KEY_PATH}" ] ; then
  echo "Creating SSH key directory..."
  mkdir -p "${SSH_KEY_PATH}"
  echo "done."
fi

if [ ! -f "${SSH_KEY_FILE}" ] ; then
  echo "Generating SSH key..."
  ssh-keygen -t "${SSH_KEY_TYPE}" -f "${SSH_KEY_NAME}"
  echo "done."
fi

if [ ! -f "${SSH_CONFIG}" ] ; then
  echo "Trying to genreate SSH config..."

  # Include user config
  if [ -f ~/.ssh/config ] ; then
    echo "Including user config."
    cat >"${SSH_CONFIG}.tmp" <<EOF
Include ~/.ssh/config

EOF
  else
    echo >"${SSH_CONFIG}.tmp"
  fi

  # Bastion hosts
  while IFS= read -r HOSTLINE ; do
    BASTION_IP=$(echo ${HOSTLINE} | cut -d' ' -f1)
    BASTION_HOST=$(echo ${HOSTLINE} | cut -d' ' -f2)

    cat >>"${SSH_CONFIG}.tmp" <<EOF
Host ${BASTION_HOST} ${BASTION_IP}
  HostName ${BASTION_IP}
  IdentitiesOnly yes
  IdentityFile ${SSH_KEY_FILE}
  User linux
  StrictHostKeyChecking no
  TCPKeepAlive yes

EOF
  done < <(./hosts --hostfile | grep bastion-)

  # Other hosts - if there is a bastion host
  if [ ! -z "${BASTION_HOST}" ] ; then
    while IFS= read -r HOSTLINE ; do
      IP=$(echo ${HOSTLINE} | cut -d' ' -f1)
      HOST=$(echo ${HOSTLINE} | cut -d' ' -f2)

      echo $HOST - $IP
      cat >>"${SSH_CONFIG}.tmp" <<EOF
Host ${HOST} ${IP}
  HostName ${IP}
  IdentitiesOnly yes
  IdentityFile ${SSH_KEY_FILE}
  User linux
  ProxyJump ${BASTION_HOST}

EOF
    done < <(./hosts --hostfile | grep -v bastion- | grep -v '^#')
  fi

  # Move into place
  if grep -q Host "${SSH_CONFIG}.tmp" ; then
    mv "${SSH_CONFIG}.tmp" "${SSH_CONFIG}"
    echo "done: ${SSH_CONFIG}"
  else
    rm "${SSH_CONFIG}.tmp"
    echo "No hosts found, maybe you need to generate an inventory first?"
  fi
fi

if [ ! -f "${SSH_ENVFILE}" ] ; then
  echo "Creating SSH alias..."
  echo "alias ssh='ssh -F \"${SSH_CONFIG}\"'" >"${SSH_ENVFILE}"
  echo "done."
fi

if [ ! -f "${SSH_AGENT_ENVFILE}" ] ; then
  echo "Creating SSH agent environment..."
  cp "${SOURCE_DIR}/templates/ssh_agent.sh" "${SSH_AGENT_ENVFILE}"
  echo "done."
fi

if [ ! -f "${SSH_ANSIBLE_ENVFILE}" ] ; then
  echo "Creating SSH settings for ansible..."
  cat >"${SSH_ANSIBLE_ENVFILE}" <<EOF
export ANSIBLE_HOST_KEY_CHECKING=False
EOF
  echo "done."
fi

