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
                echo 'Cleaning the maven workspace...'
                sh 'mvn clean'
            }
        }
        stage ('dependencies') {
            steps {
                echo 'Downloading dependencies...'
                sh 'mvn dependency:go-offline'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/nukkit-*-SNAPSHOT.jar', fingerprint: true
                }
            }
        }
        stage ('validate') {
            steps {
                echo 'Validating the maven project...'
                sh 'mvn -o validate'
            }
        }
        stage ('compile') {
            steps {
                echo 'Compiling source classes...'
                sh 'mvn -o compile'
            }
        }
        stage ('compile-test') {
            steps {
                echo 'Compiling test classes...'
                sh 'mvn -o test-compile'
            }
        }
        stage ('test') {
            steps {
                echo 'Performing unit testing...'
                sh 'mvn -o test'
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
                sh 'mvn -o package'
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
                sh 'mvn -o source:jar'
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
                sh 'mvn -o javadoc:javadoc javadoc:jar'
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
                sh 'mvn -o verify'
            }
        }
        stage ('install') {
            steps {
                echo 'Installing artifacts to the local repository...'
                sh 'mvn -o install'
            }
        }
        stage ('deploy') {
            when {
                branch "master"
            }
            steps {
                echo 'Deploying to repository...'
                sh 'mvn -o deploy'
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
}
