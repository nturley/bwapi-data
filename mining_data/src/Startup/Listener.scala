package Startup

import bwapi.{BWEventListener, Color, Player, TechType, UnitType, UpgradeType}

import scala.collection.mutable
import scala.collection.JavaConverters._
import java.io._


object Listener extends BWEventListener{
  val mirror:bwapi.Mirror = new bwapi.Mirror()

  var agent : bwapi.Unit = _
  var game : bwapi.Game = _
  var lastOrder : bwapi.Order = _
  var startReturnFrame : Int = _
  var target : bwapi.Unit = _
  var startedMiningTarget : Int = 0

  var pos2roundTrip : mutable.HashMap[bwapi.Position, Int] = _

  def initialize(): Unit = {
    println("initialize...")
    mirror.getModule.setEventListener(this)
    mirror.startGame()
  }

  private def safeCall(a:() => Unit): Unit = {
    try {
      a.apply()
    } catch { case e:Exception =>
      val except = e
      val stacktrace = e.getStackTrace
      println(e.toString)
    }
  }

  override def onStart(): Unit = {
    game = mirror.getGame
    game.enableFlag(1)
    game.setLocalSpeed(0)
    pos2roundTrip = mutable.HashMap.empty[bwapi.Position, Int]
    println(game.self().getRace.toString)
    println(game.mapFileName())
    println(game.self().getStartLocation)

  }


  override def onEnd(b: Boolean): Unit = {
    val pw = new PrintWriter(new File(game.mapFileName()+"_"+game.self().getRace.toString+game.self.getStartLocation+".json"))
    val startLoc = game.self().getStartLocation.toPosition
    val data = pos2roundTrip.map(posTrip => {
      val relativePos = new bwapi.Position(startLoc.getX - posTrip._1.getX, startLoc.getY - posTrip._1.getY)
      "    { mineral: " + relativePos + ", tripTime: " + posTrip._2.toString+" }"
    }).mkString(",\n")
    pw.write("{\n  race: \"" + game.self().getRace.toString+"\",\n")
    pw.write("  map: \"" + game.mapFileName() +"\",\n")
    pw.write("  location: " + game.self.getStartLocation.toPosition.toString + ",\n")
    pw.write("  tripTimes:\n  [\n" + data + "\n  ]\n}")
    pw.close
  }

  def getNewTarget(pos:bwapi.Position): bwapi.Unit = {
    val valids = game.getNeutralUnits.asScala
      .filter(_.getType.isMineralField)
      .filter(u => !pos2roundTrip.contains(u.getPosition))
    if (valids.isEmpty) {
      game.leaveGame()
      return null
    }
    valids.minBy(_.getPosition.getApproxDistance(pos))
  }

  val roundTripOrders = List(bwapi.Order.ReturnMinerals, bwapi.Order.MoveToMinerals)

  override def onFrame(): Unit = {
    safeCall(() => {
      if (agent != null) {
        game.drawTextMap(agent.getPosition, agent.getOrder.toString)
        if (agent.getOrder != lastOrder) {
          if (agent.isIdle || (target != null && pos2roundTrip.contains(target.getPosition))) {
            target = getNewTarget(agent.getPosition)
            if (target != null) agent.gather(target)
            startedMiningTarget = 0
          }
          if (lastOrder != null) {
            if (agent.getOrder == bwapi.Order.MiningMinerals && target != null && agent.getOrderTarget != null && agent.getOrderTarget.getID == target.getID) {
              startedMiningTarget += 1
            }
            if (startedMiningTarget == 2 && agent.getOrder == bwapi.Order.ReturnMinerals) {
              startReturnFrame = game.getFrameCount
            }
            if (startedMiningTarget == 2 && lastOrder == bwapi.Order.MoveToMinerals) {
              pos2roundTrip(target.getPosition) = game.getFrameCount - startReturnFrame
              println(pos2roundTrip)
            }
          }
          lastOrder = agent.getOrder
        }
      }
    })
  }

  override def onSendText(s: String):                     Unit = {  }
  override def onReceiveText(player: Player, s: String):  Unit = {  }
  override def onPlayerLeft(player: Player):              Unit = {  }
  override def onPlayerDropped(player: Player):           Unit = {  }
  override def onNukeDetect(position: bwapi.Position):    Unit = {  }
  override def onUnitComplete(unit: bwapi.Unit):          Unit = {
    safeCall(() => {
      if (unit.getPlayer.getID == game.self().getID && unit.getType.isWorker) {
        agent = unit
      }
    })
  }
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
