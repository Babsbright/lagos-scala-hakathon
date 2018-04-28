lazy val akkaHttpVersion = "10.1.1"
lazy val akkaVersion    = "2.5.12"

//lazy val africasTalking = RootProject(uri("git://github.com/osleonard/africastalking-scala/#master"))
lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.lagosscala.hakathon",
      scalaVersion    := "2.12.5"
    )),
    name := "lagos-scala-hakathon",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
      "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"            % "3.0.1"         % Test
    )
  )
  //.dependsOn(africasTalking)
