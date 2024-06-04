import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {

  val hmrc = "uk.gov.hmrc"
  val hmrcMongo = s"$hmrc.mongo"

  val playVersion = "play-30"
  val mongoVersion = "1.7.0"
  val bootstrapVersion = "8.6.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    hmrc      %% s"bootstrap-backend-$playVersion" % bootstrapVersion,
    hmrc      %% s"domain-$playVersion"            % "9.0.0",
    hmrc      %% s"play-hal-$playVersion"          % "4.0.0",
    hmrc      %% s"crypto-json-$playVersion"       % "7.6.0",
    hmrcMongo %% s"hmrc-mongo-$playVersion"        % mongoVersion
  )

  def test(scope: String = "test, it"): Seq[ModuleID] = Seq(
    hmrc                   %% s"bootstrap-test-$playVersion"  % bootstrapVersion % scope,
    "org.scalatestplus"    %% "mockito-3-4"                   % "3.2.10.0"       % scope,
    "org.scalatestplus"    %% "scalacheck-1-17"               % "3.2.17.0"       % scope,
    "org.scalaj"           %% "scalaj-http"                   % "2.4.2"          % scope,
    hmrcMongo              %% s"hmrc-mongo-test-$playVersion" % mongoVersion     % scope,
  )
}
