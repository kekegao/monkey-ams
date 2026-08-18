pipeline {
    agent any

    stages {

        stage('Checkout') {
            steps {
                echo '========== Checkout monkey-ams =========='

                git(
                    branch: 'main',
                    url: 'https://github.com/kekegao/monkey-ams.git'
                )
            }
        }

        stage('Check Workspace') {
            steps {
                echo '========== 检查 Workspace =========='

                sh '''
                    echo "========== Jenkins Workspace =========="

                    pwd

                    echo "========== POM FILES =========="

                    find . -name "pom.xml" -type f
                '''
            }
        }

        stage('Maven构建') {
            steps {
                echo '========== 构建 monkey-ams =========='

                sh 'chmod +x mvnw'

                sh '''
                    ./mvnw clean package -DskipTests
                '''
            }
        }


        /* stage('Build Parent POM') {
            steps {
                echo '========== 构建 monkey-ams 父 POM =========='

                sh '''
                    docker run --rm \
                        -v jenkins_jenkins_home:/var/jenkins_home \
                        -v jenkins-maven-repo:/root/.m2 \
                        -w /var/jenkins_home/workspace/monkey-ams \
                        maven:3.9.11-eclipse-temurin-17 \
                        mvn clean install -N -DskipTests
                '''
            }
        } */

        stage('Verify Parent POM') {
            steps {
                echo '========== 验证父 POM =========='

                sh '''
                    docker run --rm \
                        -v jenkins-maven-repo:/root/.m2 \
                        maven:3.9.11-eclipse-temurin-17 \
                        sh -c "find /root/.m2/repository -type f | head -50"
                '''
            }
        }
    }

    post {
        success {
            echo '========== 父 POM 构建成功 =========='
        }

        failure {
            echo '========== 父 POM 构建失败 =========='
        }
    }
}