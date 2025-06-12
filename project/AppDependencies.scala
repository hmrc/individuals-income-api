import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {

  val hmrc = "uk.gov.hmrc"
  val hmrcMongo = s"$hmrc.mongo"

  val playVersion = "play-30"
  val mongoVersion = "2.6.0"
  val bootstrapVersion = "9.13.0"
  val mockitoScalaVersion = "3.2.18.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    hmrc      %% s"bootstrap-backend-$playVersion" % bootstrapVersion,
    hmrc      %% s"domain-$playVersion"            % "11.0.0",
    hmrc      %% s"play-hal-$playVersion"          % "4.1.0",
    hmrc      %% s"crypto-json-$playVersion"       % "8.2.0",
    hmrcMongo %% s"hmrc-mongo-$playVersion"        % mongoVersion
  )

  def test(scope: String = "test, it"): Seq[ModuleID] = Seq(
    hmrc                %% s"bootstrap-test-$playVersion"  % bootstrapVersion    % scope,
    "org.scalatestplus" %% "mockito-4-11"                  % mockitoScalaVersion % scope,
    hmrc                %% "stub-data-generator"          % "1.5.0"                 % scope,
    "com.codacy"        %% "scalaj-http"                  % "2.5.0"          % scope,
    hmrcMongo           %% s"hmrc-mongo-test-$playVersion" % mongoVersion        % scope,
    "org.scalatestplus" %% "scalacheck-1-17"               % "3.2.18.0"          % scope
  )
}
