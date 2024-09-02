# income-tax-session-data

This is the repository for the Income Tax Session Data service.
To check the status of the pipeline for this service. Visit the [Pipeline](https://build.tax.service.gov.uk/job/ITSA_SVC/job/View%20and%20Change/job/income-tax-session-data-pipeline/)

# Uses

With this service, teams can safely store customer data outside their service.
The unique identifier fields: sessionId and internalId are the compound index for the database.
These can be fetched from auth service.
There are also an index on the lastUpdated field which is used for TTL.

To get data from the service, the get route must be called. The call must be made in the same user session in which the data was posted.
The fields: utr, nino, mtditid and sessionId will be returned from the get call.

When posting data, the fields: utr, nino and mtditid must be supplied. SessionId and InternalId come from auth.
If the SessionId and InternalId from the incoming request are unique, and do not match any in the database, a 200 response will be returned.
If they are not unique, then a 409 response will be returned, but the record will be added to the database with the updated TTL.


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