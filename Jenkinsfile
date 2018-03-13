def silentsh(cmd) {
    sh('#!/bin/sh -e\n' + cmd)
}

pipeline {
    agent any
    tools {
        maven 'Maven 3'
        jdk 'OracleJDK 8'
    }
    stages {
        stage ('prepare') {
            steps {
                script {
                    env.CI_SKIP = "false"
                    result = silentsh(script: "git log -1 | grep '(?s).[CI[-\\s]SKIP].*'", returnStatus: true)
                    if (result == 0) {
                        env.CI_SKIP = "true"
                        error "'[CI-SKIP]' found in git commit message. Aborting."
                    }
                }
            }
        }
        stage ('clean') {
            steps {
                echo 'Cleaning the maven workspace...'
                silentsh 'mvn clean'
            }
        }
        stage ('dependencies') {
            steps {
                echo 'Downloading dependencies...'
                silentsh 'mvn dependency:go-offline'
            }
        }
        stage ('validate') {
            steps {
                echo 'Validating the maven project...'
                silentsh 'mvn -o validate'
            }
        }
        stage ('compile') {
            steps {
                echo 'Compiling source classes...'
                silentsh 'mvn -o compile'
            }
        }
        stage ('compile-test') {
            steps {
                echo 'Compiling test classes...'
                silentsh 'mvn -o test-compile'
            }
        }
        stage ('test') {
            steps {
                echo 'Performing unit testing...'
                silentsh 'mvn -o test'
            }
            post {
                success {
                    echo 'Archiving test results...'
                    junit 'target/surefire-reports/**/*.xml'
                }
            }
        }
        stage ('package') {
            steps {
                echo 'Preparing the final package...'
                silentsh 'mvn -o package'
            }
            post {
                success {
                    echo 'Archiving the final package...'
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }
        stage ('sources') {
            when {
                branch "master"
            }
            steps {
                echo 'Generating sources...'
                silentsh 'mvn -o source:jar'
            }
            post {
                success {
                    echo 'Archiving sources...'
                    archiveArtifacts artifacts: 'target/*-souces.jar', fingerprint: true
                }
            }
        }
        stage ('javadoc') {
            when {
                branch "master"
            }
            steps {
                echo 'Generaing javadocs...'
                silentsh 'mvn -o javadoc:javadoc javadoc:jar'
            }
            post {
                success {
                    echo 'Archiving javadocs...'
                    step([
                        $class: 'JavadocArchiver',
                        javadocDir: 'target/site/apidocs',
                        keepAll: true
                    ])
                    archiveArtifacts artifacts: 'target/*-javadoc.jar', fingerprint: true
                }
            }
        }
        stage ('verify') {
            steps {
                echo 'Performing integration testing...'
                silentsh 'mvn -o verify'
            }
        }
        stage ('install') {
            steps {
                echo 'Installing artifacts to the local repository...'
                silentsh 'mvn -o install'
            }
        }
        stage ('deploy') {
            when {
                branch "master"
            }
            steps {
                echo 'Deploying to repository...'
                silentsh 'mvn -o deploy'
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
