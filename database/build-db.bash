
# create table


# split datafile


# load multiple files into db
for n in `ls MyFile_*.csv`; do echo $n; sed -e 's/%FILENAME%/'$n'/g' load_data.sql | mysql TargetDB; done


# delete csv files


docker-entrypoint-initdb.d/


docker run --name imdb-db-test -p 3306:3306 -e MYSQL_ROOT_PASSWORD=secret -e MYSQL_DATABASE=IMDBCLONE -d mysql:latest --secure-file-priv=docker-entrypoint-initdb.d/

docker exec -it imdb-db-test /bin/bash



###########################################
idee: --secure-file-priv=/tmp/


! dont run database in docker container! run stateless apps in docker, but stateful apps on hardware!

how to install db:
sudo apt install mysql-common mysql-server

sudo mysql -u root

sudo mysql

ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password by 'secret';

sudo mysql_secure_installation

cd /etc/mysql/mysql.conf.d
add to file:
[mysqld]
secure-file-priv = ""

sudo service mysql restart











