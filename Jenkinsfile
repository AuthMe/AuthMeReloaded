pipeline {
    tools {
        maven 'Maven 3'
        jdk 'OracleJDK 8'
    }

    agent any

    options {
        timestamps()
        timeout(time: 5, unit: 'MINUTES')
    }

    environment {
        COVERALLS_TOKEN = credentials('coveralls-token')
        DISCORD_WEBHOOK_URL = credentials('discord-webhook-url')
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
        stage ('compile') {
            steps {
                sh 'mvn compile'
            }
        }
        stage ('test') {
            steps {
                sh 'mvn test coveralls:report -DrepoToken=$COVERALLS_TOKEN -Dmaven.test.failure.ignore=true'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    jacoco(execPattern: '**/*.exec')
                }
            }
        }
        stage ('sources') {
            when {
                branch "master"
            }
            steps {
                sh 'mvn source:jar'
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
                sh 'mvn javadoc:javadoc javadoc:jar'
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
            discordSend webhookURL: '$DISCORD_WEBHOOK_URL'
        }
        success {
            githubNotify description: 'The jenkins build was successful',  status: 'SUCCESS'
        }
        failure {
            githubNotify description: 'The jenkins build failed',  status: 'FAILURE'
        }
    }
}
