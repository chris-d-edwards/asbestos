The Asbestos project - extending XDS Toolkit to support the FHIR standard.

Asbestos defines a micro-service environment to support the combined testing of the IHE XDS collection of 
profiles as well as the FHIR-based profiles. When complete, all the necessary components will be pulled into a Docker
environment to create a consolidated runtime. For now this is a work-in-progress.

For now all the components are part of the repository except for HAPI FHIR which is brought in
as a git submodule.

# To clone from github

    git clone https://github.com/usnistgov/asbestos.git 
    
to pull shell of project. This will create directory asbestos

    cd asbestos
    
next you will need the Javascript utilities if they are not already present (this shows what I did on Ubuntu - YMMV)

Install Yarn
    
    curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | sudo apt-key add -
    echo "deb https://dl.yarnpkg.com/debian/ stable main" | sudo tee /etc/apt/sources.list.d/yarn.list
    sudo apt-get update && sudo apt-get install yarn
    sudo apt-get update && sudo apt-get install --no-install-recommends yarn
    
verify 
 
     yarn --version
     
Add Vue utilities

    sudo yarn global add @vue/cli
    
to update the Javascript dependencies. These can be run at any time to refresh the Javascript libraries. Then run:

    cd asbestos-view
    npm init
    npm install
    
in a terminal to start UI in development mode. Do this from the view directory.

    cd asbestos-view
    npm run serve
    
In IntelliJ, choose ECMAScript 6.

