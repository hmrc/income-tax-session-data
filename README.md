# income-tax-session-data

This is the repository for the Income Tax Session Data service.
To check the status of the pipeline for this service. Visit the [Pipeline](https://build.tax.service.gov.uk/job/ITSA_SVC/job/View%20and%20Change/job/income-tax-session-data-pipeline/)

# Uses

With this service, teams can safely store customer data outside their service. The unique identifier field sessionID is the primary key for the database.
This can be fetched from the sessionId in HeaderCarrier or can be customised by consumers of this service.

Requirements
------------

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least a [JRE] to run.

## Run the application in service manager

```
sm2 --start INCOME_TAX_SESSION_DATA
```

## Run the application locally

```
sbt 'run 30027'
```

## Test the application

To test the application execute:

```
sbt test it/test
```

## Scalafmt
Check all project files are formatted as expected as follows:

```
sbt scalafmtCheckAll scalafmtCheck
```

Format *.sbt and project/*.scala files as follows:

```
sbt scalafmtSbt
```

Format all project files as follows:

```
sbt scalafmtAll
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").