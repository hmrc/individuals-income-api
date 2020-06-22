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
    hmrc                %% "bootstrap-play-26" % "1.8.0",
    hmrc                %% "domain"            % "5.9.0-play-26",
    hmrc                %% "auth-client"       % "3.0.0-play-26",
    hmrc                %% "play-hal"          % "1.9.0-play-26",
    hmrc                %% "play-hmrc-api"     % "4.1.0-play-26",
    hmrc                %% "mongo-caching"     % "6.13.0-play-26",
    hmrc                %% "json-encryption"   % "4.8.0-play-26",
    "com.typesafe.play" %% "play-json-joda"    % "2.6.14"
  )

  def test(scope: String = "test,it") = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play"       % "3.1.3"             % scope,
    "org.scalatest"          %% "scalatest"                % "3.0.8"             % scope,
    "org.scalaj"             %% "scalaj-http"              % "2.4.2"             % scope,
    "org.mockito"            % "mockito-core"              % "3.2.4"             % scope,
    "org.pegdown"            % "pegdown"                   % "1.6.0"             % scope,
    "com.typesafe.play"      %% "play-test"                % PlayVersion.current % scope,
    "com.github.tomakehurst" % "wiremock-jre8"             % "2.26.0"            % scope,
    hmrc                     %% "reactivemongo-test"       % "4.16.0-play-26"    % scope,
    hmrc                     %% "service-integration-test" % "0.9.0-play-26"     % scope
  )



}
