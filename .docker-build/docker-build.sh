#!/bin/bash
#!/bin/bash
set -e                                                                                                    # Exit the script as soon as any statement returns a non-zero exit code.

SCRIPT=$(readlink -f $0)
SCRIPT_PATH=$(dirname "$SCRIPT")

# Move to script dir
cd "${SCRIPT_PATH}"

# Read env file
if [ -f .env ]
then
  export $(cat .env | sed 's/#.*//g' | xargs)
fi

## Variables definition
# APP_REGISTRY_IMAGE = harbor.solumesl.com/solum/aims-client:1.2.3.4-DEV-common
# APP_REGISTRY = harbor.solumesl.com
# APP_NAME = aims-client
# TAG_VERSION = 1.2.3.4
# TAG_TIER = DEV
# TAG_CUSTOMER = common

#if [ -z "${CI_REGISTRY}" ]; then { echo >&2 "Please provide \$CI_REGISTRY 'registry.example.com' variable before running this script."; exit 1;} fi
#if [ -z "${CI_REGISTRY_IMAGE}" ]; then { echo >&2 "Please provide \$CI_REGISTRY_IMAGE 'registry.example.com/gitlab-org/gitlab-foss' variable before running this script."; exit 1;} fi
if [ -z "${CI_REGISTRY_USER}" ]; then { echo >&2 "Please provide \$CI_REGISTRY_USER 'registry-ci-usern' variable before running this script."; exit 1;} fi
if [ -z "${CI_REGISTRY_PASSWORD}" ]; then { echo >&2 "Please provide \$CI_REGISTRY_PASSWORD 'registry-ci-password' variable before running this script."; exit 1;} fi

if [ -z "${APP_NAME}" ]; then { echo >&2 "Please provide \$APP_NAME variable before running this script."; exit 1;} fi
if [ -z "${APP_REGISTRY_IMAGE}" ]; then { echo >&2 "Please provide \$APP_REGISTRY_IMAGE variable before running this script."; exit 1;} fi
if [ -z "${APP_MAINTAINER}" ]; then { echo >&2 "Please provide \$APP_MAINTAINER variable before running this script."; exit 1;} fi
if [ -z "${APP_COMPONENT}" ]; then { echo >&2 "Please provide \$APP_COMPONENT variable before running this script."; exit 1;} fi
if [ -z "${APP_PART_OF}" ]; then { echo >&2 "Please provide \$APP_PART_OF variable before running this script."; exit 1;} fi


# Get jar artifact
JAR_FILE_PATH=$(find "${SCRIPT_PATH}/../target/" -name *.jar | head -1)

# Get app registry harbor.solumesl.com from harbor.solumesl.com/project/image
APP_REGISTRY="$( cut -d '/' -f 1 <<< "$APP_REGISTRY_IMAGE" )";

# read_pom_xml return variables
APP_ARTIFACT=""
APP_VERSION=""
APP_PATCH_VERSION=""
APP_MINOR_VERSION=""
APP_MAYOR_VERSION=""

print_function_header (){
  FUNCTION_NAME=$1
  FUNCTION_DESCRIPTION=$2

  echo " "
  echo "----------------------------------------------------------"
  echo "${FUNCTION_NAME}: ${FUNCTION_DESCRIPTION}"
  echo "----------------------------------------------------------"
  echo " "

}

print_function_footer (){

  echo " "
  echo "----------------------------------------------------------"
  echo " "

}

export_variables () {

  print_function_header "export_variables" "Exporting all variables"

  echo "$(export)"

  print_function_footer
}

read_pom_xml () {
  print_function_header "read_pom_xml" "Reading pom.xml"

  POM_XML_FILE_PATH=$(realpath "${SCRIPT_PATH}/../pom.xml")
  # POM_XML_FILE_PATH=$(realpath "pom.xml")
  [ -f "${POM_XML_FILE_PATH}" ] || { echo "pom.xml file not found '${POM_XML_FILE_PATH}'!"; return 0;}

  APP_ARTIFACT="$(cat "${POM_XML_FILE_PATH}" | xq -r '.project.artifactId')"
  APP_VERSION="$(cat "${POM_XML_FILE_PATH}" | xq -r '.project.version')"
  APP_PATCH_VERSION="$(echo "${APP_VERSION}" | grep -oEm 1 '\w+\.\w+\.\w+' | head -1)"
  APP_MINOR_VERSION="$(echo "${APP_VERSION}" | grep -oEm 1 '\w+\.\w+' | head -1)"
  APP_MAYOR_VERSION="$(echo "${APP_VERSION}" | grep -oEm 1 '\w+' | head -1)"

  echo ""
  echo "Application Name: ${APP_ARTIFACT}"
  echo "Application Version: ${APP_VERSION}"
  echo "Application Patch Version: ${APP_PATCH_VERSION}"
  echo "Application Minor Version: ${APP_MINOR_VERSION}"
  echo "Application Mayor Version: ${APP_MAYOR_VERSION}"
  echo ""

}

docker_login () {

  print_function_header "docker_login" "APP_REGISTRY - ${APP_REGISTRY}"

  echo "login ${APP_REGISTRY}"
  echo "${CI_REGISTRY_PASSWORD}" | docker login "${APP_REGISTRY}" --username "${CI_REGISTRY_USER}" --password-stdin

  print_function_footer
}

docker_build () {
    #
    # Description:
    #   Replace old variable value with new empty variable value
    #
    # Usage:
    #   replace_with_old_variable_value old_variable new_variable
    #
    # Example:
    #   DATABASE_URL="jdbc:postgresql://127.0.0.1:6010/AIMS_PORTAL_DB"
    #   AIMS_CLIENT_DATABASE_URL=
    #   replace_with_old_variable_value DATABASE_URL AIMS_CLIENT_DATABASE_URL
    #

  print_function_header "docker_build" "APP_REGISTRY_IMAGE - ${APP_REGISTRY_IMAGE}"

  docker build --network host \
      --no-cache \
      --pull \
      --build-arg "SOURCE_REGISTRY_IMAGE=${SOURCE_REGISTRY_IMAGE}" \
      --build-arg "BUILD_REGISTRY_IMAGE=${BUILD_REGISTRY_IMAGE}" \
      --build-arg "APP_REGISTRY_IMAGE=${APP_REGISTRY_IMAGE}" \
      --build-arg "MAINTAINER=${APP_MAINTAINER}" \
      --build-arg "NAME=${APP_NAME}" \
      --build-arg "COMPONENT=${APP_COMPONENT}" \
      --build-arg "PART_OF=${APP_PART_OF}" \
      --build-arg "VERSION=${APP_VERSION}" \
      --build-arg "CI_COMMIT_AUTHOR=${CI_COMMIT_AUTHOR}" \
      --build-arg "CI_COMMIT_SHORT_SHA=${CI_COMMIT_SHORT_SHA}" \
      -t "${APP_REGISTRY_IMAGE}:${APP_VERSION}" \
      -t "${APP_REGISTRY_IMAGE}:${APP_PATCH_VERSION}" \
      -t "${APP_REGISTRY_IMAGE}:${APP_MINOR_VERSION}" \
      -t "${APP_REGISTRY_IMAGE}:${APP_MAYOR_VERSION}" \
      -f "${SCRIPT_PATH}/Dockerfile" ..

  print_function_footer
}

docker_push () {

  print_function_header "docker_push" "APP_REGISTRY_IMAGE - ${APP_REGISTRY_IMAGE}"

  # Check if tag exists then push.
  [ ! -z "${APP_VERSION}" ] && docker image push "${APP_REGISTRY_IMAGE}:${APP_VERSION}"
  [ ! -z "${APP_PATCH_VERSION}" ] && docker image push "${APP_REGISTRY_IMAGE}:${APP_PATCH_VERSION}"
  [ ! -z "${APP_MINOR_VERSION}" ] && docker image push "${APP_REGISTRY_IMAGE}:${APP_MINOR_VERSION}"
  [ ! -z "${APP_MAYOR_VERSION}" ] && docker image push "${APP_REGISTRY_IMAGE}:${APP_MAYOR_VERSION}"

  print_function_footer
}

main() {

  echo " "
  echo "----------------------------------------------------------"
  echo "Starting build.sh script"
  echo "----------------------------------------------------------"
  echo " "

  read_pom_xml

  export_variables

  docker_login
  docker_build
  docker_push

  echo " "
  echo "----------------------------------------------------------"
  echo "Finished."

}

main
