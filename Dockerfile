FROM eclipse-temurin:17-jre

WORKDIR /opt/magnolia

COPY apache-tomcat ./apache-tomcat
COPY light-modules ./light-modules

RUN chmod +x ./apache-tomcat/bin/*.sh \
    && rm -rf ./apache-tomcat/webapps/magnoliaPublic \
    && cp -a ./apache-tomcat/webapps/magnoliaAuthor ./apache-tomcat/webapps/magnoliaPublic \
    && mkdir -p \
      ./apache-tomcat/logs \
      ./apache-tomcat/temp \
      ./apache-tomcat/work \
      ./apache-tomcat/webapps/magnoliaAuthor/WEB-INF/config/activation-key \
      ./apache-tomcat/webapps/magnoliaAuthor/cache \
      ./apache-tomcat/webapps/magnoliaAuthor/history \
      ./apache-tomcat/webapps/magnoliaAuthor/logs \
      ./apache-tomcat/webapps/magnoliaAuthor/tmp \
      ./apache-tomcat/webapps/magnoliaPublic/WEB-INF/config/magnoliaPublic \
      ./apache-tomcat/webapps/magnoliaPublic/cache \
      ./apache-tomcat/webapps/magnoliaPublic/history \
      ./apache-tomcat/webapps/magnoliaPublic/logs \
      ./apache-tomcat/webapps/magnoliaPublic/tmp

COPY docker/magnolia-public.properties ./apache-tomcat/webapps/magnoliaPublic/WEB-INF/config/magnoliaPublic/magnolia.properties
COPY docker/entrypoint.sh ./docker/entrypoint.sh

RUN chmod +x ./docker/entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["./docker/entrypoint.sh"]
CMD ["./apache-tomcat/bin/catalina.sh", "run"]
