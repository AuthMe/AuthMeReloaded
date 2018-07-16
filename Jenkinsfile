pipeline {
    agent {
        docker {
            image 'sgdc3/maven-argon2:3.5.4-jdk-10'
            args '-v $HOME/.m2:/root/.m2'
        }
    }

    options {
        timeout(time: 5, unit: 'MINUTES')
        timestamps()
    }

    stages {
        stage ('Checkout') {
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

        stage ('Clean') {
            steps {
                sh 'mvn -B clean'
            }
        }

        stage ('Build') {
            when {
                not {
                    branch "master"
                }
            }
            steps {
                sh 'mvn -B clean package'
            }
            post {
                success {
                    archiveArtifacts artifacts: '**/target/*.jar', excludes: '**/target/*-noshade.jar', fingerprint: true
                }
            }
        }

        stage ('Build & Deploy') {
            when {
                branch "master"
            }
            steps {
                sh 'mvn -B clean package javadoc:aggregate-jar source:jar deploy'
            }
            post {
                success {
                    archiveArtifacts artifacts: '**/target/*.jar', excludes: '**/target/*-noshade.jar', fingerprint: true
                    step([
                        $class: 'JavadocArchiver',
                        javadocDir: 'target/apidocs',
                        keepAll: true
                    ])
                }
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
        success {
            githubNotify description: 'The jenkins build was successful',  status: 'SUCCESS'
        }
        failure {
            githubNotify description: 'The jenkins build failed',  status: 'FAILURE'
        }
    }
}
