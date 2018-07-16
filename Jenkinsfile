pipeline {
    agent {
        docker {
            image 'maven:3.5.4-jdk-10'
            args '-v $HOME/.m2:/root/.m2'
        }
    }

    options {
        timeout(time: 8, unit: 'MINUTES')
        timestamps()
    }

    environment {
        COMMIT = sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%h'").trim()
        VERSION = readMavenPom().getVersion()
    }

    triggers {
        githubPush()
    }

    stages {
        stage ('Check commit') {
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

        stage ('Install system dependencies') {
            steps {
                sh 'apt install libargon2-0-dev && ln -s /usr/lib/x86_64-linux-gnu/libargon2.so /usr/lib/libargon2.so'
            }
        }

        stage ('Build') {
            steps {
                script {
                    currentBuild.displayName = "${VERSION} #${BUILD_NUMBER}".replace("-SNAPSHOT", "")
                    currentBuild.description = "git-AuthMeReloaded-${COMMIT}"
                }
                sh 'mvn -B clean package'
            }
            post {
                success {
                    archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
                    step([
                        $class: 'JavadocArchiver',
                        javadocDir: 'target/site/apidocs',
                        keepAll: true
                    ])
                }
            }
        }

        stage ('Deploy') {
            when {
                branch "master"
            }
            steps {
                sh 'mvn -B deploy -DskipTests'
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
