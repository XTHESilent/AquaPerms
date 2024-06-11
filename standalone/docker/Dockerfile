FROM eclipse-temurin:21-alpine
RUN apk add --no-cache netcat-openbsd

# create a simple 'send' command that will allow users
# to run, for example: docker exec <container> send lp info
RUN printf '#!/bin/sh\n\
echo "$@" | nc -NU /opt/aquaperms/aquaperms.sock\n' >> /usr/bin/send && chmod 777 /usr/bin/send

# setup user
RUN addgroup -S app && adduser -S -G app app
USER app

# copy jar file into image
WORKDIR /opt/aquaperms
COPY aquaperms-standalone.jar .

# create volume for data directory
RUN mkdir data
VOLUME ["/opt/aquaperms/data"]

# preload and relocate dependency jars
RUN java -jar aquaperms-standalone.jar preloadDependencies

CMD ["java", "-jar", "aquaperms-standalone.jar", "--docker"]

HEALTHCHECK --interval=30s --timeout=15s --start-period=20s \
    CMD wget http://localhost:3001/health -q -O - | grep -c '"up":true' || exit 1
