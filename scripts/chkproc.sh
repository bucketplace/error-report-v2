daemon=`netstat -tlnp | grep :::19000 | wc -l`
if [ "$daemon" -eq "0" ] ; then
        nohup java -jar /home/bsscco/error-report-v2/error-report-v2-*.jar &
fi