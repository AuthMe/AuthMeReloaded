pipeline {
    agent any
    tools {
        maven 'Maven 3'
        jdk 'OracleJDK 8'
    }
    stages {
        stage ('check-commit') {
            steps {
                script {
                    env.CI_SKIP = "false"
                    result = sh (script: "git log -1 | grep '(?s).[CI[-\\s]SKIP].*'", returnStatus: true)
                    if (result == 0) {
                        env.CI_SKIP = "true"
                        error "'[CI-SKIP]' found in git commit message. Aborting."
                    }
                }
            }
        }
        stage ('clean') {
            steps {
                sh 'mvn clean'
            }
        }
        stage ('dependencies') {
            steps {
                sh 'mvn dependency:resolve-plugins dependency:go-offline'
            }
        }
        stage ('compile') {
            steps {
                sh 'mvn -o -DskipTests install'
            }
        }
        stage ('test') {
            steps {
                sh 'mvn -o surefire:test'
            }
            post {
                success {
                    junit 'target/surefire-reports/**/*.xml'
                }
            }
        }
        stage ('sources') {
            when {
                branch "master"
            }
            steps {
                sh 'mvn -o source:jar'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*-souces.jar', fingerprint: true
                }
            }
        }
        stage ('javadoc') {
            when {
                branch "master"
            }
            steps {
                sh 'mvn -o javadoc:javadoc javadoc:jar'
            }
            post {
                success {
                    step([
                        $class: 'JavadocArchiver',
                        javadocDir: 'target/site/apidocs',
                        keepAll: true
                    ])
                    archiveArtifacts artifacts: 'target/*-javadoc.jar', fingerprint: true
                }
            }
        }
        stage ('deploy') {
            when {
                branch "master"
            }
            steps {
                sh 'mvn -DskipTests deploy'
            }
        }
    }    
    post {
        always {
            script {
                if (env.CI_SKIP == "true") {
                    currentBuild.result = 'NOT_BUILT'
                }
            }
        }
    }
}
