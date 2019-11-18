name := "mediamur"
version := "0.1"
scalaVersion := "2.12.7"
enablePlugins(JavaAppPackaging)
maintainer := "matthieu.totet@gmail.com"
val akkaHttpVersion = "10.1.10"
val akkaVersion = "2.6.0"
val twitter4JVersion = "4.0.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion
)

libraryDependencies ++= Seq(
  "org.twitter4j" % "twitter4j-core" % twitter4JVersion,
  "org.twitter4j" % "twitter4j-stream" % twitter4JVersion,
  "org.twitter4j" % "twitter4j-async" % twitter4JVersion
)

libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.7"

mainClass in assembly := Some("fr.totetmatt.mediamur.Mediamur")
mainClass in Compile := Some("fr.totetmatt.mediamur.Mediamur")

name in Universal := name.value

// removes all jar mappings in universal and appends the fat jar
mappings in Universal := {
  val universalMappings = (mappings in Universal).value
  val fatJar = (assembly in Compile).value
  val filtered = universalMappings filter {
    case (file, name) =>  ! name.endsWith(".jar")
  }

  filtered :+ (fatJar -> ("lib/" + fatJar.getName))


}
scriptClasspath := Seq( (assemblyJarName in assembly).value )
// the bash scripts classpath only needs the fat jar

mappings in Universal += file("./conf/application.conf.default") -> "application.conf"
mappings in Universal += file("README.md") -> "README.md"
javaOptions in Universal ++= Seq(
  "-Dconfig.file=application.conf"
)