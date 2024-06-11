### Docker Build instructions

1. Compile with Gradle
2. `cd standalone/loader/build/libs`
3. `cp AquaPerms-*.jar aquaperms-standalone.jar`
4. `docker build . -t aquaperms -f ../../../docker/Dockerfile`
