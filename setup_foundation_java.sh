#!/bin/bash

PROJECT_DIR=${PWD}
JAVA_FOUNDATION_DIR=${PROJECT_DIR}/cpswt-core
source $HOME/.bashrc


echo "Maven install and deploy projects from ${JAVA_FOUNDATION_DIR}"
cd ${JAVA_FOUNDATION_DIR}
mvn clean install deploy -U -B


echo "=================================================================================="
echo "Completed the compilation, installation, deployment of the C2W foundation packages"
echo "=================================================================================="
