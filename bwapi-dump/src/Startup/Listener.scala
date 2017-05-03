package Startup

import bwapi.{BWEventListener, Player, TechType, UnitType, UpgradeType}

import scala.reflect.runtime.universe._
import java.io._

import scala.collection.JavaConverters._

object Listener extends BWEventListener{
  val mirror:bwapi.Mirror = new bwapi.Mirror()

  def initialize(): Unit = {
    println("initialize...")
    mirror.getModule.setEventListener(this)
    mirror.startGame()
  }

  def toJsonField(name:String, data: String, quotes: Boolean = false): String = {
    val quote = if (quotes) "\"" else ""
    "      \"" + name + "\" : " + quote + data + quote
  }

  override def onStart(): Unit = {
    val game = mirror.getGame
    try {
      val t = typeOf[bwapi.UnitType]
      val pw = new PrintWriter(new File("bwapi-data.txt"))
      val wtt = weakTypeTag[bwapi.UnitType]
      val clazz = wtt.mirror.runtimeClass(t)

      val unitStrs = clazz.getFields.map(f => {
        val uType = f.get(wtt).asInstanceOf[bwapi.UnitType]
        val uAttrs = uType.getClass.getDeclaredMethods
          .filter(m => m.getParameterCount == 0 && m.getName != "size")
          .map(m => {
            val retType = m.getReturnType
            val attrStr = if (retType == classOf[Boolean] ||
                              retType == classOf[Int] ||
                              retType == classOf[bwapi.TilePosition] ||
                              retType == classOf[Double]) {
              toJsonField(m.getName, m.invoke(uType).toString)
            } else if (m.getName.startsWith("upgrades")) {
              val upgradeList = m.invoke(uType).asInstanceOf[java.util.List[UpgradeType]].asScala.map("\"" + _.toString + "\"")
              toJsonField(m.getName, "[" + upgradeList.mkString(", ") + "]")
            } else if (retType == classOf[java.util.List[TechType]]) {
              val techList = m.invoke(uType).asInstanceOf[java.util.List[TechType]].asScala.map("\"" + _.toString + "\"")
              toJsonField(m.getName, "[" + techList.mkString(", ") + "]")
            } else if (m.getName == "requiredUnits") {
              val unitList = m.invoke(uType).asInstanceOf[java.util.Map[bwapi.UnitType, Int]].asScala.map("\"" + _._1.toString + "\"")
              toJsonField(m.getName, "[" + unitList.mkString(", ") + "]")
            } else if (m.getName == "whatBuilds") {
              toJsonField(m.getName, m.invoke(uType).asInstanceOf[bwapi.Pair[UnitType, Int]].first.toString, quotes = true)
            } else {
              toJsonField(m.getName, m.invoke(uType).toString, quotes = true)
            }
            attrStr
          })
        "    {\n" + uAttrs.mkString(",\n")+"\n    }"
      })
      pw.write("{\n  \"unitTypes\" :\n")
      pw.write("  [\n" + unitStrs.mkString(",\n") + "\n  ]\n}")
      pw.close()
    } catch {
      case e : Exception => {
        println(e)
        println(e.getStackTrace)
      }
    }
  }


  override def onEnd(b: Boolean): Unit = {
  }

  override def onFrame(): Unit = {

  }
  override def onSendText(s: String):                     Unit = {  }
  override def onReceiveText(player: Player, s: String):  Unit = {  }
  override def onPlayerLeft(player: Player):              Unit = {  }
  override def onPlayerDropped(player: Player):           Unit = {  }
  override def onNukeDetect(position: bwapi.Position):    Unit = {  }
  override def onUnitComplete(unit: bwapi.Unit):          Unit = {  }
  override def onUnitCreate(unit: bwapi.Unit):            Unit = {  }
  override def onUnitDestroy(unit: bwapi.Unit):           Unit = {  }
  override def onUnitDiscover(unit: bwapi.Unit):          Unit = {  }
  override def onUnitEvade(unit: bwapi.Unit):             Unit = {  }
  override def onUnitHide(unit: bwapi.Unit):              Unit = {  }
  override def onUnitMorph(unit: bwapi.Unit):             Unit = {  }
  override def onUnitRenegade(unit: bwapi.Unit):          Unit = {  }
  override def onUnitShow(unit: bwapi.Unit):              Unit = {  }
  override def onSaveGame(s: String):                     Unit = {  }
}
