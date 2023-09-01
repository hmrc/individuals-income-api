import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  val hmrc = "uk.gov.hmrc"
  val hmrcMongo = "uk.gov.hmrc.mongo"

  val mongoVersion = "0.73.0"

  val compile = Seq(
    ws,
    hmrc                %% "bootstrap-backend-play-28"  % "7.8.0",
    hmrc                %% "domain"                     % "8.3.0-play-28",
    hmrc                %% "play-hal"                   % "3.4.0-play-28",
    hmrc                %% "play-hmrc-api"              % "7.1.0-play-28",
    hmrc                %% "json-encryption"            % "5.1.0-play-28",
    "com.typesafe.play" %% "play-json-joda"             % "2.9.2",
    hmrcMongo           %% "hmrc-mongo-play-28"         % mongoVersion
  )

  def test(scope: String = "test,it") = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play"       % "5.1.0"             % scope,
    "org.scalatestplus"      %% "mockito-3-4"              % "3.2.1.0"           % scope,
    "org.scalatestplus"      %% "scalacheck-1-15"          % "3.2.10.0"          % scope,
    "com.vladsch.flexmark"   % "flexmark-all"              % "0.35.10"           % scope,
    "org.scalaj"             %% "scalaj-http"              % "2.4.2"             % scope,
    "org.pegdown"            % "pegdown"                   % "1.6.0"             % scope,
    "com.github.tomakehurst" % "wiremock-jre8"             % "2.27.2"            % scope,
    hmrcMongo                %% "hmrc-mongo-test-play-28"  % mongoVersion        % scope,
    hmrc                     %% "service-integration-test" % "1.4.0-play-28"     % scope,
  )
}
