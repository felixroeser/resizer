package com.octojon

import com.typesafe.config._

object RealmMapper {
  lazy val map = ConfigFactory.load().getConfig("realmMapper")
  def get(r: String): String = { map.getString(r) }
}

object CacheDir {
  lazy val cachePath = ConfigFactory.load().getString("cachePath")
  def path: String = { cachePath }
}

case class PurgeRequest(realm: String, url: String) {
  val realmTemplate = RealmMapper.get(realm)
  val baseImageName = java.security.MessageDigest.getInstance("SHA-1").digest( url.getBytes("UTF-8") ).map("%02x".format(_)).mkString
  val dirName = s"${CacheDir.path}/$realm/$baseImageName"
  lazy val dir = new java.io.File(dirName)
}
