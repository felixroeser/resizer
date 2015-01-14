package com.octojon

import com.typesafe.config._

object Global {
  lazy val config = loadConfig

  private def loadConfig = {
    try {
      val configFileName = System.getenv("CONFIG")
      ConfigFactory.parseFile(new java.io.File(configFileName))
    } catch {
      case npe: java.lang.NullPointerException => {
        ConfigFactory.load()
      }
    }
  }
}

object RealmMapper {
  lazy val map = Global.config.getConfig("realmMapper")
  def get(r: String): String = { map.getString(r) }
}

object CacheDir {
  lazy val cachePath = Global.config.getString("cachePath")
  def path: String = { cachePath }
}

case class PurgeRequest(realm: String, url: String) {
  val realmTemplate = RealmMapper.get(realm)
  val baseImageName = java.security.MessageDigest.getInstance("SHA-1").digest( url.getBytes("UTF-8") ).map("%02x".format(_)).mkString
  val dirName = s"${CacheDir.path}/$realm/$baseImageName"
  lazy val dir = new java.io.File(dirName)
}
