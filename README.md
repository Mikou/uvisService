# Uvis Service

This application provides an oData service exposing 3 entities.

The 3 entities are

* Person
* Project
* Activity

## Build

To build the application
`./gradlew build`

Build the application for eclipse
`./gradlew eclipse`

To debug the application
`./gradlew serve -Dorg.gradle.debug=true`
Then connect eclipse debugger as a remote application



## Service
The application is configured to run on port 8088

When running, the oData service root is available at 
`http://localhost:8081/UvisService/DemoService.svc/`

The service supports queries like
`People?$select=Name&$expand=Activities($expand=Project($select=Name))`
