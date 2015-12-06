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

Build jar
./gradlew jar

Debug
java -Xdebug -Xrunjdwp:transport=dt_socket,address=5005,server=y -cp build/libs/UvisService-1.0.jar dk.uvis.Application
Then connect eclipse debugger as a remote application



## Service
The application is configured to run on port 8088

When running, the oData service root is available at 
`http://localhost:8081/UvisService/DemoService.svc/`

The service supports queries like
`People?$select=Name&$expand=Activities($expand=Project($select=Name))`
