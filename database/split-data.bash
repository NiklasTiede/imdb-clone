awk -v l=1000000 '(NR==1){header=$0;next}
                (NR%l==2) {
                   close(file);
                   file=sprintf("%s.%0.5d.csv",FILENAME,++c)
                   sub(/csv[.]/,"",file)
                   print header > file
                }
                {print > file}' proc_imdb_movies.csv