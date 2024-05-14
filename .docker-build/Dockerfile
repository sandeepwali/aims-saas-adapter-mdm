ARG BUILD_REGISTRY_IMAGE
ARG SOURCE_REGISTRY_IMAGE

FROM $BUILD_REGISTRY_IMAGE AS build
COPY / /home/app

RUN echo "Setting variables" && \
        APP_SRC_DIR='/home/app' && \
        APP_BUILD_DIR='/home/app/build' && \
        APP_ARTIFACT=$(cat ${APP_SRC_DIR}/pom.xml | xq -r '.project.artifactId') && \
        APP_VERSION=$(cat ${APP_SRC_DIR}/pom.xml | xq -r '.project.version') && \
        JAR_FILE_PATH=$(find "${APP_SRC_DIR}/target/" -name *.jar | head -1) && \
        JAR_FILE_NAME=$(basename -- ${JAR_FILE_PATH}) &&\
    echo "Copy application ${JAR_FILE_NAME}" && \
        mkdir -p "${APP_BUILD_DIR}" && \
        cp "${JAR_FILE_PATH}" "${APP_BUILD_DIR}/aims-saas-adapter-mdm.jar" && \
        cp -r "${APP_SRC_DIR}/src/main/resources/"* "${APP_BUILD_DIR}" && \
        sed -i -e "s|^solum.saas.server.username=.*|solum.saas.server.username=|" "${APP_BUILD_DIR}/application.properties" && \
        sed -i -e "s|^solum.saas.server.password=.*|solum.saas.server.password=|" "${APP_BUILD_DIR}/application.properties" && \
        sed -i -e "s|^spring.datasource.username=.*|spring.datasource.username=|" "${APP_BUILD_DIR}/application.properties" && \
        sed -i -e "s|^spring.datasource.password=.*|spring.datasource.password=|" "${APP_BUILD_DIR}/application.properties" && \
        sed -i -e "s|^spring.rabbitmq.host=.*|spring.rabbitmq.host=|" "${APP_BUILD_DIR}/application.properties" && \
        sed -i -e "s|^spring.rabbitmq.port=.*|spring.rabbitmq.port=|" "${APP_BUILD_DIR}/application.properties" && \
        sed -i -e "s|^spring.rabbitmq.username=.*|spring.rabbitmq.username=|" "${APP_BUILD_DIR}/application.properties" && \
        sed -i -e "s|^spring.rabbitmq.password=.*|spring.rabbitmq.password=|" "${APP_BUILD_DIR}/application.properties" && \
    echo "Finished!"

FROM $SOURCE_REGISTRY_IMAGE

ARG MAINTAINER
ARG NAME
ARG COMPONENT
ARG PART_OF
ARG VERSION
ARG TIER
ARG CUSTOMER
ARG CI_COMMIT_AUTHOR
ARG CI_COMMIT_SHORT_SHA

LABEL maintainer="${MAINTAINER}" \
    name="${NAME}" \
    component="${COMPONENT}" \
    part-of="${PART_OF}" \
    version="${VERSION}" \
    tier="${TIER}" \
    customer="${CUSTOMER}" \
    ci-commit-author="${CI_COMMIT_AUTHOR}" \
    ci-commit-short-sha="${CI_COMMIT_SHORT_SHA}"

#COPY --from=build --chown=1010 /app /app
COPY --from=build --chown=1010 /home/app/build /app
COPY --from=build --chown=1010 /home/app/.docker-build/root /

# Set default user to aims and group to 0
# -> Set default user to aims 1010
# -> Set the user group to 0 for remapping user when the docker container starts
# -> https://docs.docker.com/engine/security/userns-remap/#disable-namespace-remapping-for-a-container

RUN groupadd --gid 1010 aims &&\
    useradd -s /bin/sh --uid 1010 --gid 1010 aims &&\
    chown -R 1010:0 /app &&\
    chmod -R g=u /app &&\
    chmod 775 /docker-entrypoint.sh

WORKDIR /app
USER 1010

ENTRYPOINT [ "/docker-entrypoint.sh" ]
CMD [ "" ]


