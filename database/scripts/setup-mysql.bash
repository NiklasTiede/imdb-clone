

sudo apt install mysql-common mysql-server



# idee: --secure-file-priv=/tmp/

#
mysql_secure_installation <<EOF
y
secret
secret
y
y
y
y
EOF


# restart
sudo service mysql restart






