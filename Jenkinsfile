pipeline {
  agent none
  environment {
    SERVICE_NAME_1='eureka1'
    SERVICE_NAME_2='eureka2'
    SERVICE_NAME_3='eureka3'
    DOCKER_IMAGE='bremersee/eureka'
    DEV_TAG='snapshot'
    PROD_TAG='latest'
  }
  stages {
    stage('Test') {
      agent {
        label 'maven'
      }
      tools {
        jdk 'jdk11'
        maven 'm3'
      }
      when {
        not {
          branch 'feature/*'
        }
      }
      steps {
        sh 'java -version'
        sh 'mvn -B --version'
        sh 'mvn -B clean test'
      }
      post {
        always {
          junit '**/surefire-reports/*.xml'
          jacoco(
              execPattern: '**/coverage-reports/*.exec'
          )
        }
      }
    }
    stage('Push snapshot') {
      agent {
        label 'maven'
      }
      when {
        branch 'develop'
      }
      tools {
        jdk 'jdk11'
        maven 'm3'
      }
      steps {
        sh '''
          mvn -B -DskipTests -Ddockerfile.skip=false clean package dockerfile:push
          mvn -B -DskipTests -Ddockerfile.skip=false -Ddockerfile.tag=snapshot clean package dockerfile:push
          docker system prune -a -f
        '''
      }
    }
    stage('Push release') {
      agent {
        label 'maven'
      }
      when {
        branch 'master'
      }
      tools {
        jdk 'jdk11'
        maven 'm3'
      }
      steps {
        sh '''
          mvn -B -DskipTests -Ddockerfile.skip=false clean package dockerfile:push
          mvn -B -DskipTests -Ddockerfile.skip=false -Ddockerfile.tag=latest clean package dockerfile:push
          docker system prune -a -f
        '''
      }
    }
    stage('Deploy on dev-swarm') {
      agent {
        label 'dev-swarm'
      }
      when {
        branch 'develop'
      }
      steps {
        sh '''
          if docker service ls | grep -q ${SERVICE_NAME_1}; then
            echo "Updating service ${SERVICE_NAME_1} with docker image ${DOCKER_IMAGE}:${DEV_TAG}."
            docker service update --image ${DOCKER_IMAGE}:${DEV_TAG} ${SERVICE_NAME_1}
          else
            echo "Creating service ${SERVICE_NAME_1} with docker image ${DOCKER_IMAGE}:${DEV_TAG}."
            docker node update --label-add eureka=1 $(docker node ls -q -f name=dev-manager-1)
            chmod 755 docker-swarm/service1.sh
            docker-swarm/service1.sh "${DOCKER_IMAGE}:${DEV_TAG}"
          fi
          if docker service ls | grep -q ${SERVICE_NAME_2}; then
            echo "Updating service ${SERVICE_NAME_2} with docker image ${DOCKER_IMAGE}:${DEV_TAG}."
            docker service update --image ${DOCKER_IMAGE}:${DEV_TAG} ${SERVICE_NAME_2}
          else
            echo "Creating service ${SERVICE_NAME_2} with docker image ${DOCKER_IMAGE}:${DEV_TAG}."
            docker node update --label-add eureka=2 $(docker node ls -q -f name=dev-manager-2)
            chmod 755 docker-swarm/service2.sh
            docker-swarm/service2.sh "${DOCKER_IMAGE}:${DEV_TAG}"
          fi
          if docker service ls | grep -q ${SERVICE_NAME_3}; then
            echo "Updating service ${SERVICE_NAME_3} with docker image ${DOCKER_IMAGE}:${DEV_TAG}."
            docker service update --image ${DOCKER_IMAGE}:${DEV_TAG} ${SERVICE_NAME_3}
          else
            echo "Creating service ${SERVICE_NAME_3} with docker image ${DOCKER_IMAGE}:${DEV_TAG}."
            docker node update --label-add eureka=3 $(docker node ls -q -f name=dev-manager-3)
            chmod 755 docker-swarm/service3.sh
            docker-swarm/service3.sh "${DOCKER_IMAGE}:${DEV_TAG}"
          fi
        '''
      }
    }
    stage('Deploy on prod-swarm') {
      agent {
        label 'prod-swarm'
      }
      when {
        branch 'master'
      }
      steps {
        sh '''
          if docker service ls | grep -q ${SERVICE_NAME_1}; then
            echo "Updating service ${SERVICE_NAME_1} with docker image ${DOCKER_IMAGE}:${PROD_TAG}."
            docker service update --image ${DOCKER_IMAGE}:${PROD_TAG} ${SERVICE_NAME_1}
          else
            echo "Creating service ${SERVICE_NAME_1} with docker image ${DOCKER_IMAGE}:${PROD_TAG}."
            docker node update --label-add eureka=1 $(docker node ls -q -f name=prod-manager-1)
            chmod 755 docker-swarm/service1.sh
            docker-swarm/service1.sh "${DOCKER_IMAGE}:${PROD_TAG}"
          fi
          if docker service ls | grep -q ${SERVICE_NAME_2}; then
            echo "Updating service ${SERVICE_NAME_2} with docker image ${DOCKER_IMAGE}:${PROD_TAG}."
            docker service update --image ${DOCKER_IMAGE}:${PROD_TAG} ${SERVICE_NAME_2}
          else
            echo "Creating service ${SERVICE_NAME_2} with docker image ${DOCKER_IMAGE}:${PROD_TAG}."
            docker node update --label-add eureka=2 $(docker node ls -q -f name=prod-manager-2)
            chmod 755 docker-swarm/service2.sh
            docker-swarm/service2.sh "${DOCKER_IMAGE}:${PROD_TAG}"
          fi
          if docker service ls | grep -q ${SERVICE_NAME_3}; then
            echo "Updating service ${SERVICE_NAME_3} with docker image ${DOCKER_IMAGE}:${PROD_TAG}."
            docker service update --image ${DOCKER_IMAGE}:${PROD_TAG} ${SERVICE_NAME_3}
          else
            echo "Creating service ${SERVICE_NAME_3} with docker image ${DOCKER_IMAGE}:${PROD_TAG}."
            docker node update --label-add eureka=3 $(docker node ls -q -f name=prod-manager-3)
            chmod 755 docker-swarm/service3.sh
            docker-swarm/service3.sh "${DOCKER_IMAGE}:${PROD_TAG}"
          fi
        '''
      }
    }
    stage('Deploy snapshot site') {
      agent {
        label 'maven'
      }
      environment {
        CODECOV_TOKEN = credentials('eureka-codecov-token')
      }
      when {
        branch 'develop'
      }
      tools {
        jdk 'jdk11'
        maven 'm3'
      }
      steps {
        sh 'mvn -B clean site-deploy'
      }
      post {
        always {
          sh 'curl -s https://codecov.io/bash | bash -s - -t ${CODECOV_TOKEN}'
        }
      }
    }
    stage('Deploy release site') {
      agent {
        label 'maven'
      }
      environment {
        CODECOV_TOKEN = credentials('eureka-codecov-token')
      }
      when {
        branch 'master'
      }
      tools {
        jdk 'jdk11'
        maven 'm3'
      }
      steps {
        sh 'mvn -B -P gh-pages-site clean site site:stage scm-publish:publish-scm'
      }
      post {
        always {
          sh 'curl -s https://codecov.io/bash | bash -s - -t ${CODECOV_TOKEN}'
        }
      }
    }
    stage('Test feature') {
      agent {
        label 'maven'
      }
      when {
        branch 'feature/*'
      }
      tools {
        jdk 'jdk11'
        maven 'm3'
      }
      steps {
        sh 'java -version'
        sh 'mvn -B --version'
        sh 'mvn -B -P feature,allow-features clean test'
      }
      post {
        always {
          junit '**/surefire-reports/*.xml'
          jacoco(
              execPattern: '**/coverage-reports/*.exec'
          )
        }
      }
    }
  }
}