#!/usr/bin/env bash

cd "$(dirname "$0")" || exit 1

# read_pom_xml return variables
export APP_VERSION=""
export APP_REGISTRY="harbor.solumesl.com"
export KUBECONFIG="${PWD}/.kube/${CI_COMMIT_BRANCH}"

read_pom_xml() {
    POM_XML_FILE_PATH="../pom.xml"
    [ -f "${POM_XML_FILE_PATH}" ] || {
        echo "pom.xml file not found '${POM_XML_FILE_PATH}'!"
        return 0
    }

    APP_VERSION="$(xq -r '.project.version' <"${POM_XML_FILE_PATH}")"

    echo ""
    echo "Application Version: ${APP_VERSION}"
    echo ""
}

deploy_kustomization() {
    kustomization_path=$1

    date_iso_8601=$(date -Iseconds)
    date_iso=$(cut -d '+' -f 1 <<<"$date_iso_8601")
    date_iso="${date_iso//:/_}"

    echo "Replacing gitlab-date in ${kustomization_path}/deployment.yaml with ${date_iso}"
    sed -i -e "s|gitlab-date: .*|gitlab-date: ${date_iso}|" "${kustomization_path}/deployment.yaml"

    echo "Replacing newTag in ${kustomization_path}/kustomization.yaml with ${APP_VERSION}"
    sed -i -e "s|newTag: .*|newTag: \"${APP_VERSION}\"|" "${kustomization_path}/kustomization.yaml"
}

apply() {
    read_pom_xml
    deploy_kustomization aims-saas-adapter-mdm
    kustomize build --enable-helm | kubectl apply -f -
}

diff() {
    read_pom_xml
    deploy_kustomization aims-saas-adapter-mdm
    kustomize build --enable-helm | kubectl diff -f -
}

[ "$1" == "apply" ] && apply
[ "$1" == "diff" ] && diff

exit 0
