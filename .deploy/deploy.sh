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

# read_pom_xml return variables
export APP_ARTIFACT=""
export APP_VERSION=""
export APP_PATCH_VERSION=""
export APP_MINOR_VERSION=""
export APP_MAYOR_VERSION=""
export APP_REGISTRY="harbor.solumesl.com"
export KUBECONFIG="${PWD}/.kube/config"

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

read_pom_xml () {
  print_function_header "read_pom_xml" "Reading pom.xml"

  POM_XML_FILE_PATH=$(realpath "${SCRIPT_PATH}/../pom.xml")
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
export_variables () {

  print_function_header "export_variables" "Exporting all variables"

  echo "$(export)"

  print_function_footer
}

replace_yaml () {

    yaml_file_path=$1
    app_image=$2

    date_iso_8601=$(date -Iseconds)
    date_iso=$(cut -d '+' -f 1 <<< "$date_iso_8601")
    date_iso="${date_iso//:/_}"

    print_function_header "replace_yaml" "Replacing ${yaml_file_path} variables"

    echo "Replacing date with ${date_iso}"
    sed -i -e "s|date: .*|date: ${date_iso}|" "${yaml_file_path}"

    echo "Replacing image with ${app_image}"
    sed -i -e "s|image: .*|image: ${app_image}|" "${yaml_file_path}"

    print_function_footer
}

replace_kube_config () {
    kube_config_file_path=$1
    token=$2

    print_function_header "replace_kube_config" "Replacing ${kube_config_file_path} variables"

    echo "Replacing token with $(echo "${token}" | sha512sum) (SHA512)"
    sed -i -e "s|token: .*|token: \"${token}\"|" "${kube_config_file_path}"

    print_function_footer
}

deploy_kustomization () {
  kustomization_path=$1

  print_function_header "deploy_kustomization" "Deploying kustomization"

  date_iso_8601=$(date -Iseconds)
  date_iso=$(cut -d '+' -f 1 <<< "$date_iso_8601")
  date_iso="${date_iso//:/_}"

  echo "Replacing gitlab-date in ${kustomization_path}/deployment.yaml with ${date_iso}"
  sed -i -e "s|gitlab-date: .*|gitlab-date: ${date_iso}|" "${kustomization_path}/deployment.yaml"

  echo "Replacing newTag in ${kustomization_path}/kustomization.yaml with ${APP_VERSION}"
  sed -i -e "s|newTag: .*|newTag: \"${APP_VERSION}\"|" "${kustomization_path}/kustomization.yaml"

  kubectl apply -k "${kustomization_path}"

  sleep 1

  kubectl rollout status --namespace aims-saas-adapter-mdm-dev -w deployment/aims-saas-adapter-mdm-dev

  print_function_footer
}

install_kubectl () {
  print_function_header "install_kubectl" "Installing kubectl"

  curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
  chmod +x kubectl
  mv kubectl /usr/bin/

  print_function_footer
}

install_kustomize () {
  print_function_header "install_kustomize" "Installing kustomize"

  curl -s "https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh"  | bash
  chmod +x kustomize 
  mv kustomize /usr/bin/

  print_function_footer
}

install_helm () {
  print_function_header "install_helm" "Installing helm"

  curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

  print_function_footer
}

install_openssl () {

  apk add openssl

}

notify_slack () {

  app=$1
  version=$2
  link=$3

  username=$4
  channel=$5
  url=$6

  icon_emoji=":ghost:"
  icon_url="https://about.gitlab.com/images/press/logo/png/gitlab-icon-rgb.png"
  text="Deployment of ${app} ${version} is completed on aims-mgb-test.k8s.de.solumesl.com! <${link}|Click here> to get more information."

  curl -X POST --data-urlencode "payload={\"channel\": \"${channel}\", \"username\": \"${username}\", \"text\": \"${text}\", \"icon_url\": \"${icon_url}\"}" ${url}
}

main () {
  echo " "
  echo "----------------------------------------------------------"
  echo "Starting deploy.sh script"
  echo "----------------------------------------------------------"
  echo " "

  read_pom_xml
  export_variables

  #replace_yaml "${SCRIPT_PATH}/aims-mgb-test-aims-client.yaml" "harbor.solumesl.com/mgb/aims-client:${APP_VERSION}"
  #replace_kube_config "${SCRIPT_PATH}/.kube/config" "${KUBECTL_K8S_DE_MGB_TOKEN}"

  install_openssl
  install_kubectl
  install_kustomize
  install_helm
  #deploy_yaml "${SCRIPT_PATH}/aims-mgb-test-aims-client.yaml"

  kustomize build "${SCRIPT_PATH}/aims-rabbitmq" --enable-helm | kubectl apply -f -
  sleep 1
  kubectl rollout status --namespace aims-saas-adapter-mdm-dev -w statefulset/aims-rabbitmq

  #kustomize build "${SCRIPT_PATH}/aims-db-postgresql" --enable-helm | kubectl apply -f -
  kubectl apply -k "${SCRIPT_PATH}/aims-db-postgresql"
  sleep 1
  kubectl rollout status --namespace aims-saas-adapter-mdm-dev -w statefulset/aims-db-postgresql
  
  deploy_kustomization "${SCRIPT_PATH}/aims-saas-adapter-mdm-dev"

  echo " "
  echo "----------------------------------------------------------"
  echo "Finished."
}

main

exit