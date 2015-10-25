# Uvis Service

This application provides an oData service exposing 3 entities.

The 3 entities are

* Person
* Project
* Activity

The application is configured to run on port 8088

When running, the oData service root is available at 
`http://localhost:8081/UvisService/DemoService.svc/`

The service supports queries like
`People?$select=Name&$expand=Activities($expand=Project($select=Name))`
