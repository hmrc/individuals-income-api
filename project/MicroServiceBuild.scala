import play.core.PlayVersion
import play.sbt.PlayImport._
import play.sbt.routes.RoutesKeys.routesImport
import sbt._

object MicroServiceBuild extends Build with MicroService {

  val appName = "individuals-income-api"

  override lazy val appDependencies: Seq[ModuleID] = compile ++ test()
  override lazy val playSettings : Seq[Setting[_]] = Seq(routesImport ++= Seq("uk.gov.hmrc.domain._", "uk.gov.hmrc.individualsincomeapi.domain._", "uk.gov.hmrc.individualsincomeapi.Binders._"))

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "microservice-bootstrap" % "6.9.0",
    "uk.gov.hmrc" %% "domain" % "4.1.0",
    "uk.gov.hmrc" %% "auth-client" % "2.2.0",
    "uk.gov.hmrc" %% "play-hal" % "1.2.0",
    "uk.gov.hmrc" %% "play-hmrc-api" % "2.0.0",
    "uk.gov.hmrc" %% "http-caching-client" % "7.1.0"
  )

  def test(scope: String = "test,it") = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "2.3.0" % scope,
    "org.scalatest" %% "scalatest" % "2.2.6" % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" %  scope,
    "org.scalaj" %% "scalaj-http" % "1.1.6" % scope,
    "org.mockito" % "mockito-core" % "2.11.0" % scope,
    "org.pegdown" % "pegdown" % "1.6.0" % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "com.github.tomakehurst" % "wiremock" % "2.6.0" % scope,
    "uk.gov.hmrc" %% "reactivemongo-test" % "2.0.0" % scope
  )

}
