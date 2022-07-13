

# copy file into folder
cp ${PWD}/proc_imdb_movies.csv /tmp/

# split file
awk -v l=1000000 '(NR==1){header=$0;next}
                (NR%l==2) {
                   close(file);
                   file=sprintf("%s.%0.2d.csv",FILENAME,++c)
                   sub(/csv[.]/,"",file)
                   print header > file
                }
                {print > file}' proc_imdb_movies.csv

# load multiple files into db
for n in `ls MyFile_*.csv`; do echo $n; sed -e 's/%FILENAME%/'$n'/g' load_data.sql | mysql TargetDB; done


# make query to confirm number of entries


# delete datafiles
rm

