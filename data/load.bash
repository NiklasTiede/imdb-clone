

for n in `ls MyFile_*.csv`;
  do echo $n; sed -e 's/%FILENAME%/'$n'/g' load-data.sql | mysql TargetDB;
done

# chmod of file 777
echo '\n[mysqld]\nsecure_file_priv = ""' >> my.cnf
# back to 644 again
