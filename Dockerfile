ARG APP_INSIGHTS_AGENT_VERSION=2.5.1-BETA
FROM hmctspublic.azurecr.io/base/java:openjdk-11-distroless-1.4

COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/rd-user-profile-api.jar /opt/app/

EXPOSE 8091

CMD [ "rd-user-profile-api.jar" ]