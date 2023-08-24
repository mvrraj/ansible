pipeline {
    agent any

    stages {
        stage('Fetch Latest GitHub Tag') {
            steps {
                script {
                    def githubApiUrl = 'https://api.github.com/repos/your-org/your-repo/releases/latest'
                    def githubCredentials = credentials('your-github-credentials-id') // Replace with your GitHub credentials ID
                    def response = sh(script: "curl -sSL -u ${githubCredentials} ${githubApiUrl}", returnStatus: true, returnStdout: true)

                    if (response == 0) {
                        def versionJson = sh(script: "curl -sSL -u ${githubCredentials} ${githubApiUrl}", returnStdout: true)
                        def version = readJSON text: versionJson
                        def latestTag = version.tag_name

                        // Extract the numeric part of the tag and increment it
                        def versionParts = latestTag.tokenize('.')
                        def major = versionParts[0] as Integer
                        def minor = versionParts[1] as Integer
                        def patch = versionParts[2] as Integer
                        patch++
                        def newTag = "${major}.${minor}.${patch}"

                        // Set the new tag as an environment variable for later use
                        env.NEW_GIT_TAG = newTag

                        echo "Fetched latest tag ${latestTag} from GitHub"
                        echo "Incremented tag to ${newTag}"
                    } else {
                        error "Failed to fetch latest tag from GitHub"
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                // Checkout your repository from Git
                // Replace 'your-repo-url' and 'your-credentials-id' with appropriate values
                checkout([$class: 'GitSCM',
                    branches: [[name: '*/master']],
                    userRemoteConfigs: [[url: 'your-repo-url']]])
            }
        }

        stage('Create and Push New Tag') {
            steps {
                script {
                    def newTag = env.NEW_GIT_TAG
                    sh "git tag ${newTag}"
                    sh "git push origin ${newTag}"
                }
            }
        }

        stage('Zip Repository') {
            steps {
                sh "zip -r my-repo.zip ."
            }
        }

        stage('Push to Nexus') {
            steps {
                script {
                    def nexusUrl = "https://your-nexus-repo-url"
                    def repositoryId = "your-repo-id"
                    def nexusCredentials = 'your-nexus-credentials-id'

                    withCredentials([string(credentialsId: nexusCredentials, variable: 'NEXUS_CREDENTIALS')]) {
                        sh "curl -u ${NEXUS_CREDENTIALS} --upload-file my-repo.zip \"${nexusUrl}/repository/${repositoryId}/my-repo.zip\""
                    }
                }
            }
        }
    }
}
