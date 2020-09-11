import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  val hmrc = "uk.gov.hmrc"
  val akka = "com.typesafe.akka"

  val akkaVersion = "2.5.23"
  val akkaHttpVersion = "10.0.15"

  val overrides = Seq(
    akka %% "akka-stream" % akkaVersion,
    akka %% "akka-protobuf" % akkaVersion,
    akka %% "akka-slf4j" % akkaVersion,
    akka %% "akka-actor" % akkaVersion,
    akka %% "akka-http-core" % akkaHttpVersion
  )

  val compile = Seq(
    ws,
    hmrc                %% "bootstrap-backend-play-26"  % "2.24.0",
    hmrc                %% "domain"                     % "5.9.0-play-26",
    hmrc                %% "auth-client"                % "3.0.0-play-26",
    hmrc                %% "play-hal"                   % "2.1.0-play-26",
    hmrc                %% "play-hmrc-api"              % "4.1.0-play-26",
    hmrc                %% "mongo-caching"              % "6.15.0-play-26",
    hmrc                %% "json-encryption"            % "4.8.0-play-26",
    "com.typesafe.play" %% "play-json-joda"             % "2.9.1"
  )

  def test(scope: String = "test,it") = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play"       % "3.1.3"             % scope,
    "org.scalatest"          %% "scalatest"                % "3.0.8"             % scope,
    "org.scalaj"             %% "scalaj-http"              % "2.4.2"             % scope,
    "org.mockito"            % "mockito-core"              % "3.5.10"             % scope,
    "org.pegdown"            % "pegdown"                   % "1.6.0"             % scope,
    "com.typesafe.play"      %% "play-test"                % PlayVersion.current % scope,
    "com.github.tomakehurst" % "wiremock-jre8"             % "2.27.2"            % scope,
    hmrc                     %% "reactivemongo-test"       % "4.21.0-play-26"    % scope,
    hmrc                     %% "service-integration-test" % "0.12.0-play-26"     % scope
  )



}
