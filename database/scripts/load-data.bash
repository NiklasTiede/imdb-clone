
#rm "${PWD}"/processed_imdb_movies.csv
#cp "${PWD}"/processed_imdb_movies.csv /tmp/
#
#awk -v l=1000000 '(NR==1){header=$0;next}
#                (NR%l==2) {
#                   close(file);
#                   file=sprintf("%s.%0.5d.csv",FILENAME,++c)
#                   sub(/csv[.]/,"",file)
#                   print header > file
#                }
#                {print > file}' processed_imdb_movies.csv
#
#mkdir /tmp/load_movies
#mv "${PWD}"/processed_imdb_movies.*.csv /tmp/load_movies/
#rm /tmp/processed_imdb_movies.csv




for filename in /tmp/load_movies/processed_imdb_movies.00001.csv; do
    echo "$filename";
    sed -e 's/%FILENAME%/'$filename'/g' load-data.sql | mysql IMDBCLONE --defaults-extra-file=~/.my.cnf;  # --defaults-extra-file=~/.my.cnf
done

#for filename in /tmp/load_movies/*.csv; do
##    echo "$filename";
#    sed -e 's/%FILENAME%/'$filename'/g' load-data.sql | mysql IMDBCLONE;  # --defaults-extra-file=~/.my.cnf
#done

#rm /tmp/load_movies/processed_imdb_movies.*.csv












#mysql --login-path=local  -e "statement"
#
#mysql_config_editor set --login-path=local --host=localhost --user=root --password


#echo "[client]                                                           ─╯
#user=root
#password=secret" >> ~/.my.cnf

#mysql --defaults-extra-file=~/.my.cnf [all my other options]





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


###########################################
idee: --secure-file-priv=/tmp/

