# individuals-income-api

[ ![Download](https://api.bintray.com/packages/hmrc/releases/individuals-income-api/images/download.svg) ](https://bintray.com/hmrc/releases/individuals-income-api/_latestVersion)

This API provides individual's income information (PAYE only) from HM Revenue and Customs (HMRC). Income data is only available for tax years commencing 2013-2014.

### Documentation
The documentation on [confluence](https://confluence.tools.tax.service.gov.uk/display/MDS/Development+space) includes:
- Configuration driven management of data and scopes
- Scope driven query strings for Integration Framework (IF)
- Caching strategy to alleviate load on backend systems
 
Please ensure you reference the OGD Data Item matrix to ensure the right data items are mapped and keep this document up to date if further data items are added.
### Running tests

Unit, integration and component tests can be run with the following:

    sbt test it:test component:test

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
