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

  def jsonAttributify(m:java.lang.reflect.Method, attrVal: AnyRef): String = {
    val retType = m.getReturnType
    val attrStr = if (retType == classOf[Boolean] ||
                      retType == classOf[Int] ||
                      retType == classOf[bwapi.TilePosition] ||
                      retType == classOf[Double]) {
      toJsonField(m.getName, attrVal.toString)
    } else if (m.getName.startsWith("upgrades")) {
      val upgradeList = attrVal.asInstanceOf[java.util.List[UpgradeType]].asScala.map("\"" + _.toString + "\"")
      toJsonField(m.getName, "[" + upgradeList.mkString(", ") + "]")
    } else if (retType == classOf[java.util.List[TechType]]) {
      val techList = attrVal.asInstanceOf[java.util.List[TechType]].asScala.map("\"" + _.toString + "\"")
      toJsonField(m.getName, "[" + techList.mkString(", ") + "]")
    } else if (m.getName == "requiredUnits") {
      val unitList = attrVal.asInstanceOf[java.util.Map[bwapi.UnitType, Int]].asScala.map("\"" + _._1.toString + "\"")
      toJsonField(m.getName, "[" + unitList.mkString(", ") + "]")
    } else if (m.getName == "whatBuilds") {
      toJsonField(m.getName, attrVal.asInstanceOf[bwapi.Pair[UnitType, Int]].first.toString, quotes = true)
    } else {
      toJsonField(m.getName, attrVal.toString, quotes = true)
    }
    attrStr
  }

  override def onStart(): Unit = {
    try {
      val t = typeOf[bwapi.UnitType]
      val pw = new PrintWriter(new File("../types.json"))
      val wtt = weakTypeTag[bwapi.UnitType]
      val clazz = wtt.mirror.runtimeClass(t)

      val unitStrs = clazz.getFields.map(f => {
        val unitTypeInstance = f.get(wtt).asInstanceOf[bwapi.UnitType]
        val uAttrs = unitTypeInstance.getClass.getDeclaredMethods
          .filter(m => m.getParameterCount == 0 && m.getName != "size")
          .map(m => jsonAttributify(m, m.invoke(unitTypeInstance)))
        "    {\n" + uAttrs.mkString(",\n")+"\n    }"
      })


      val techtype = typeOf[bwapi.TechType]
      val wTechType = weakTypeTag[bwapi.TechType]
      val techClass = wTechType.mirror.runtimeClass(techtype)

      val techStrs = techClass.getFields.map(f => {
        val techInstance = f.get(wTechType).asInstanceOf[bwapi.TechType]
        val tAttrs = techInstance.getClass.getDeclaredMethods
          .filter(m => m.getParameterCount == 0 && m.getName != "size")
          .map(m => jsonAttributify(m, m.invoke(techInstance)))
        "    {\n" + tAttrs.mkString(",\n")+"\n    }"
      })

      val upgradeType = typeOf[bwapi.UpgradeType]
      val wUpgradeType = weakTypeTag[bwapi.UpgradeType]
      val upgradeClass = wUpgradeType.mirror.runtimeClass(upgradeType)

      val upgradeStrs = upgradeClass.getFields.map(f => {
        val upgradeInstance = f.get(wUpgradeType).asInstanceOf[bwapi.UpgradeType]
        if (upgradeInstance.maxRepeats() > 1) {
          val multiNames = upgradeInstance.getClass.getDeclaredMethods
            .filter(m => !m.getName.endsWith("_native") && m.getParameterCount == 1 && m.getName != "get")
            .map(_.getName)
          val singleAttrs = upgradeInstance.getClass.getDeclaredMethods
            .filter(m => !multiNames.contains(m.getName) && m.getParameterCount == 0 && m.getName != "get")
            .map(m => jsonAttributify(m, m.invoke(upgradeInstance)))
          val multiAttrs = (1 to upgradeInstance.maxRepeats()).map(level => {
            val levelAttrs = upgradeInstance.getClass.getDeclaredMethods
              .filter(m => !m.getName.endsWith("_native") && m.getParameterCount == 1 && m.getName != "get")
              .map(m => jsonAttributify(m, m.invoke(upgradeInstance, level : java.lang.Integer)))
            levelAttrs.mkString(",\n") +
              ",\n      \"level\": " + level + ",\n" +
              singleAttrs.mkString(",\n")
          })
          "    {\n" + multiAttrs.mkString("\n    },\n    {\n") + "\n    }"
        } else {
          val uAttrs = upgradeInstance.getClass.getDeclaredMethods
            .filter(m => m.getParameterCount == 0 && m.getName != "size")
            .map(m => jsonAttributify(m, m.invoke(upgradeInstance)))
          "    {\n" + uAttrs.mkString(",\n") + "\n    }"
        }
      })

      pw.write("{\n  \"unitTypes\" :\n")
      pw.write("  [\n" + unitStrs.mkString(",\n") + "\n  ],\n")
      pw.write("  \"techTypes\" :\n")
      pw.write("  [\n" + techStrs.mkString(",\n") + "\n  ],\n")

      pw.write("  \"upgradeTypes\" :\n")
      pw.write("  [\n" + upgradeStrs.mkString(",\n") + "\n  ]\n}")
      pw.close()

    } catch {
      case e : Exception => {
        println(e)
        println(e.getStackTrace)
        println("uh oh")
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
