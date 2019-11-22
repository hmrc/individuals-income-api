import play.core.PlayVersion
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.ExternalService
import uk.gov.hmrc.ServiceManagerPlugin.Keys.itDependenciesList
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "individuals-income-api"
val hmrc = "uk.gov.hmrc"

lazy val appDependencies: Seq[ModuleID] = compile ++ test()
lazy val playSettings : Seq[Setting[_]] = Seq(routesImport ++= Seq("uk.gov.hmrc.domain._", "uk.gov.hmrc.individualsincomeapi.domain._", "uk.gov.hmrc.individualsincomeapi.Binders._"))
lazy val plugins : Seq[Plugins] = Seq.empty
lazy val externalServices = List(ExternalService("AUTH"), ExternalService("INDIVIDUALS_MATCHING_API"), ExternalService("DES"))

val compile = Seq(
  ws,
  hmrc %% "bootstrap-play-26" % "1.3.0",
  hmrc %% "domain"            % "5.6.0-play-26",
  hmrc %% "auth-client"       % "2.31.0-play-26",
  hmrc %% "play-hal"          % "1.9.0-play-26",
  hmrc %% "play-hmrc-api"     % "3.6.0-play-26",
  hmrc %% "mongo-caching"     % "6.6.0-play-26",
  hmrc %% "json-encryption"   % "4.4.0-play-26",
  "com.typesafe.play" %% "play-json-joda" % "2.6.10"
)

def test(scope: String = "test,it") = Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.0" % scope,
  "org.scalatest" %% "scalatest" % "3.0.0" % scope,
  "org.scalaj" %% "scalaj-http" % "1.1.6" % scope,
  "org.mockito" % "mockito-core" % "2.11.0" % scope,
  "org.pegdown" % "pegdown" % "1.6.0" % scope,
  "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
  "com.github.tomakehurst" % "wiremock-jre8" % "2.21.0" % scope,
  hmrc %% "reactivemongo-test" % "4.15.0-play-26" % scope,
  hmrc %% "service-integration-test" % "0.9.0-play-26" % scope
)

def intTestFilter(name: String): Boolean = name startsWith "it"
def unitFilter(name: String): Boolean = name startsWith "unit"
def componentFilter(name: String): Boolean = name startsWith "component"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory) ++ plugins: _*)
  .settings(playSettings : _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    libraryDependencies ++= appDependencies,
    testOptions in Test := Seq(Tests.Filter(unitFilter)),
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
  )
  .settings(unmanagedResourceDirectories in Compile += baseDirectory.value / "resources")
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(itDependenciesList := externalServices)
  .settings(
    Keys.fork in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest <<= (baseDirectory in IntegrationTest)(base => Seq(base / "test")),
    testOptions in IntegrationTest := Seq(Tests.Filter(intTestFilter)),
    addTestReportOption(IntegrationTest, "int-test-reports"),
    testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    parallelExecution in IntegrationTest := false)
  .configs(ComponentTest)
  .settings(inConfig(ComponentTest)(Defaults.testSettings): _*)
  .settings(
    testOptions in ComponentTest := Seq(Tests.Filter(componentFilter)),
    unmanagedSourceDirectories   in ComponentTest <<= (baseDirectory in ComponentTest)(base => Seq(base / "test")),
    testGrouping in ComponentTest := oneForkedJvmPerTest((definedTests in ComponentTest).value),
    parallelExecution in ComponentTest := false
  )
  .settings(resolvers ++= Seq(
    Resolver.bintrayRepo("hmrc", "releases"),
    Resolver.jcenterRepo
  ))
  .settings(PlayKeys.playDefaultPort := 9652)
  .settings(majorVersion := 0)

lazy val ComponentTest = config("component") extend Test

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
  tests map {
    test => Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
  }