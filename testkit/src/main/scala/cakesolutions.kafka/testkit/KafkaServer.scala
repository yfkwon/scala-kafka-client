package cakesolutions.kafka.testkit

import java.io.File
import java.net.ServerSocket
import java.util.Properties

import kafka.server.{KafkaConfig, KafkaServerStartable}
import org.apache.curator.test.TestingServer
import org.slf4j.LoggerFactory

import scala.util.Try

object KafkaServer {

  /**
    * Choose an available port
    */
  protected def choosePort(): Int = choosePorts(1).head

  /**
    * Choose a number of random available ports
    */
  protected def choosePorts(count: Int): List[Int] = {
    val sockets =
      for (i <- 0 until count)
        yield new ServerSocket(0)
    val socketList = sockets.toList
    val ports = socketList.map(_.getLocalPort)
    socketList.foreach(_.close())
    ports
  }

  /**
    * Create a test config for the given node id
    */
  private def createBrokerConfig(
                                  nodeId: Int,
                                  port: Int = choosePort(),
                                  zookeeperConnect: String,
                                  enableControlledShutdown: Boolean = true,
                                  logDir: File): KafkaConfig = {
    val props = new Properties
    props.put("broker.id", nodeId.toString)
    props.put("host.name", "localhost")
    props.put("port", port.toString)
    props.put("log.dir", logDir.getAbsolutePath)
    props.put("zookeeper.connect", zookeeperConnect)
    props.put("replica.socket.timeout.ms", "1500")
    props.put("controlled.shutdown.enable", enableControlledShutdown.toString)
    new KafkaConfig(props)
  }
}

/**
  * A startable kafka server.  Kafka and ZookeeperPort is generated by default.
  *
  * @param kafkaPort
  * @param zookeeperPort
  */
class KafkaServer(val kafkaPort: Int = KafkaServer.choosePort(),
                  val zookeeperPort: Int = KafkaServer.choosePort()) {

  import KafkaServer._

  val logDir = TestUtils.constructTempDir("kafka-local")

  val log = LoggerFactory.getLogger(getClass)
  val zookeeperConnect = "127.0.0.1:" + zookeeperPort

  //Start a zookeeper server
  val zkServer = new TestingServer(zookeeperPort)

  //Build Kafka config with zookeeper connection
  val config = createBrokerConfig(1, kafkaPort, zookeeperConnect, logDir = logDir)
  log.info("ZK Connect String: {}", zkServer.getConnectString)

  // Kafka Test Server
  val kafkaServer = new KafkaServerStartable(config)

  def startup() = {
    kafkaServer.startup()
    log.info(s"Started kafka on port [${kafkaPort}]")
  }

  def close() = {
    log.info(s"Stopping kafka on port [${kafkaPort}")
    kafkaServer.shutdown()
    zkServer.stop()
    Try(TestUtils.deleteFile(logDir)).failed.foreach(_.printStackTrace)
  }
}
