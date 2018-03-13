pipeline {
    agent any
    tools {
        maven 'Maven 3'
        jdk 'OracleJDK 8'
    }
    stages {
        stage ('Clean') {
            steps {
                echo 'Cleaning the maven workspace...'
                sh 'mvn clean'
            }
        }
        stage ('Dependencies') {
            steps {
                echo 'Downloading dependencies...'
                sh 'mvn dependency:go-offline'
            }
            post {
                success {
                    junit 'target/surefire-reports/**/*.xml'
                    archiveArtifacts artifacts: 'target/nukkit-*-SNAPSHOT.jar', fingerprint: true
                }
            }
        }
        stage ('Validate') {
            steps {
                echo 'Validating the maven project...'
                sh 'mvn -o validate'
            }
        }
        stage ('Compile') {
            steps {
                echo 'Compiling source classes...'
                sh 'mvn -o compile'
            }
        }
        stage ('Compile-Test') {
            steps {
                echo 'Compiling test classes...'
                sh 'mvn -o test-compile'
            }
        }
        stage ('Test') {
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
        stage ('Package') {
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
        stage ('Sources') {
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
        stage ('Javadoc') {
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
        stage ('Verify') {
            steps {
                echo 'Performing integration testing...'
                sh 'mvn -o verify'
            }
        }
        stage ('Install') {
            steps {
                echo 'Installing artifacts to the local repository...'
                sh 'mvn -o install'
            }
        }
        stage ('Deploy') {
            when {
                branch "master"
            }
            steps {
                echo 'Deploying to repository...'
                sh 'mvn -o deploy'
            }
        }
    }
}
