#!/bin/bash

#####################################################
# Wazuh OVA IP Migration + Full Certificate Regeneration
# Author: ChatGPT (2025)
# System: Single-node OVA Ubuntu 22 LTS
#####################################################

OLD_IP="127.0.0.1"
NEW_IP="10.7.2.200"
DATE=$(date +%Y%m%d-%H%M%S)
CERT_TOOL_DIR="/var/ossec/tools/ssl_certs"
CERT_DEST="/etc/filebeat/certs"
WAZUH_CERT_DEST="/var/ossec/etc"
DASH_CERT_DEST="/etc/wazuh-dashboard/certs"
INDEXER_CERT_DEST="/etc/wazuh-indexer/certs"

# Log function
log() { echo -e "\e[32m[+] $1\e[0m"; }
warn() { echo -e "\e[33m[!] $1\e[0m"; }
fail() { echo -e "\e[31m[-] $1\e[0m"; exit 1; }

# Check root
if [[ "$EUID" -ne 0 ]]; then
  fail "Please run as root."
fi

log "Starting Wazuh IP Migration..."

# Backup function
backup_file() {
    FILE=$1
    if [ -f "$FILE" ]; then
        cp "$FILE" "$FILE.bak.$DATE"
        log "Backup created for $FILE"
    else
        warn "$FILE not found."
    fi
}

# Step 1: Backup and Replace IPs in configs

log "Replacing IP in Wazuh Manager configs..."
backup_file /var/ossec/etc/cluster.json
backup_file /var/ossec/etc/ossec.conf
sed -i "s/$OLD_IP/$NEW_IP/g" /var/ossec/etc/cluster.json
sed -i "s/$OLD_IP/$NEW_IP/g" /var/ossec/etc/ossec.conf

log "Replacing IP in Filebeat configs..."
backup_file /etc/filebeat/filebeat.yml
sed -i "s/$OLD_IP/$NEW_IP/g" /etc/filebeat/filebeat.yml

log "Replacing IP in Dashboard configs..."
backup_file /etc/wazuh-dashboard/opensearch_dashboards.yml
sed -i "s/$OLD_IP/$NEW_IP/g" /etc/wazuh-dashboard/opensearch_dashboards.yml

log "Replacing IP in Indexer configs..."
if [ -f /etc/wazuh-indexer/opensearch.yml ]; then
  backup_file /etc/wazuh-indexer/opensearch.yml
  sed -i "s/$OLD_IP/$NEW_IP/g" /etc/wazuh-indexer/opensearch.yml
fi

# Step 2: Certificate regeneration

if [ ! -d "$CERT_TOOL_DIR" ]; then
  fail "Wazuh certificate tool not found at $CERT_TOOL_DIR"
fi

log "Generating new certificates for IP: $NEW_IP"

cd "$CERT_TOOL_DIR" || fail "Failed to enter $CERT_TOOL_DIR"

# Regenerate certificates non-interactively
cat > ./certs.yml <<EOF
certs:
  - name: node01
    ip: $NEW_IP
    role: master
EOF

./generate_certs.sh > /tmp/cert-gen.log 2>&1

if [ $? -ne 0 ]; then
  fail "Certificate generation failed. Check /tmp/cert-gen.log"
fi

log "Certificates successfully generated."

# Step 3: Distribute certificates

log "Distributing certificates to components..."

# Wazuh Manager
cp -f ./root-ca.pem $WAZUH_CERT_DEST/
cp -f ./node01.pem $WAZUH_CERT_DEST/
cp -f ./node01-key.pem $WAZUH_CERT_DEST/
log "Wazuh Manager certs updated."

# Filebeat
mkdir -p $CERT_DEST
cp -f ./root-ca.pem $CERT_DEST/
cp -f ./node01.pem $CERT_DEST/filebeat.pem
cp -f ./node01-key.pem $CERT_DEST/filebeat-key.pem
log "Filebeat certs updated."

# Dashboard
mkdir -p $DASH_CERT_DEST
cp -f ./root-ca.pem $DASH_CERT_DEST/
cp -f ./node01.pem $DASH_CERT_DEST/dashboard.pem
cp -f ./node01-key.pem $DASH_CERT_DEST/dashboard-key.pem
log "Dashboard certs updated."

# Indexer
if [ -d "$INDEXER_CERT_DEST" ]; then
  cp -f ./root-ca.pem $INDEXER_CERT_DEST/
  cp -f ./node01.pem $INDEXER_CERT_DEST/indexer.pem
  cp -f ./node01-key.pem $INDEXER_CERT_DEST/indexer-key.pem
  log "Indexer certs updated."
fi

# Step 4: Restart all services

log "Restarting Wazuh stack..."
systemctl restart wazuh-manager
systemctl restart filebeat
systemctl restart wazuh-dashboard
systemctl restart wazuh-indexer 2>/dev/null || warn "Indexer service not found (probably standalone mode)."

log "Migration completed successfully!"
log "✅ Access your dashboard at: https://$NEW_IP:5601"
log "✅ Verify logs if any issues appear: /var/ossec/logs/ossec.log"
