package com.octojon

import java.io._

case class ResizeRequest(realm: String, rawOptions: String, url: String) {
  val realmTemplate = RealmMapper.get(realm)

  val digester = java.security.MessageDigest.getInstance("SHA-1")
  val baseImageName = digester.digest( url.getBytes("UTF-8") ).map("%02x".format(_)).mkString
  val dirName = s"${CacheDir.path}/$realm/$baseImageName"
  lazy val dir = new File(dirName)
  lazy val originalFile = new File(dirName + "/original")

  val sizedImageName = digester.digest( rawOptions.getBytes("UTF-8") ).map("%02x".format(_)).mkString
  lazy val sizedImageFile = new File(dirName + "/" + sizedImageName)

  lazy val expandedUrl = realmTemplate.replace(" ", "").replace("{{url}}", url)
  lazy val options = ParsedOptions(rawOptions)
}

object Regexs {
  val wah  = """(\d+)x(\d+)""".r
  val max  = """(\d+)""".r
  val qual = """q(\d{1,2})""".r
  val fill = """c(\d{1,3})-(\d{1,3})-(\d{1,3})""".r
}

case class ParsedOptions(rawOptions: String) {
  val map = rawOptions.split(",").foldLeft(Map.empty[String, List[Int]]) { (map, s) =>
    s match {
      case Regexs.wah(w, h)   => map + ("fit" -> List(w.toInt, h.toInt) )
      case Regexs.max(d)      => map + ("resize" -> List(d.toInt) )
      case Regexs.qual(q)     => map + ("quality" -> List(q.toInt) )
      case Regexs.fill(r,g,b) => map + ("fill" -> List(r.toInt, g.toInt, b.toInt)  )
      case _ => println(s"no match for $s") ; map
    }
  }

  val fillValues = map.getOrElse("fill", List(255, 255, 255))
  val fillColor = new com.sksamuel.scrimage.RGBColor(fillValues.head, fillValues.apply(1), fillValues.apply(2))

  def contains(key: String) = map.contains(key)
  def get(key: String) = map.get(key)
}
