import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {

  val hmrc = "uk.gov.hmrc"
  val hmrcMongo = s"$hmrc.mongo"

  val mongoVersion = "0.73.0"
  val bootstrapVersion = "7.15.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    hmrc                %% "bootstrap-backend-play-28"  % bootstrapVersion,
    hmrc                %% "domain"                     % "8.3.0-play-28",
    hmrc                %% "play-hal"                   % "3.4.0-play-28",
    hmrc                %% "json-encryption"            % "5.1.0-play-28",
    "com.typesafe.play" %% "play-json-joda"             % "2.9.2",
    hmrcMongo           %% "hmrc-mongo-play-28"         % mongoVersion
  )

  def test(scope: String = "test, it"): Seq[ModuleID] = Seq(
    hmrc                     %% "bootstrap-test-play-28"             % bootstrapVersion    % scope,
    "org.scalatestplus"      %% "mockito-3-4"              % "3.2.1.0"           % scope,
    "org.scalatestplus"      %% "scalacheck-1-15"          % "3.2.10.0"          % scope,
    "com.vladsch.flexmark"   % "flexmark-all"              % "0.64.6"           % scope,
    "org.scalaj"             %% "scalaj-http"              % "2.4.2"             % scope,
    hmrcMongo                %% "hmrc-mongo-test-play-28"  % mongoVersion        % scope,
  )
}
