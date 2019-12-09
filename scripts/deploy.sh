sudo ./gradlew shadowJar
sudo gcloud compute scp build/libs/* instance-1:/home/bsscco/error-report-v2