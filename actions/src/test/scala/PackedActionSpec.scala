package salvo.actions.test

import java.net.{ InetSocketAddress, InetAddress }
import org.specs2.mutable._
import salvo.util._
import salvo.tree._
import salvo.tree.test._
import salvo.dist._
import salvo.actions._
import org.apache.commons.lang3.RandomStringUtils.random

class PackedActionsSpec extends Specification with TestUtils with Logging {
  "Actions" should {
    "distribute packed version" in new UsingTempDir {
      val version = Version.now()

      object first extends Tree(tempDir / "first")
      first.init()
      val dist = new Dist(first)
      dist.init(ignoreExisting = true)

      object second extends Tree(tempDir / "second")
      second.init()
      new Dist(second).init(ignoreExisting = true)

      first.validate()
      second.validate()

      first.incoming.create(version, state = Dir.Incomplete, repr = Unpacked)

      val digestBefore =
        for (path <- first.incoming(version).map(first.incoming / (_, Unpacked) / "garbage")) yield {
          writeTo(random(1024 * 1024 * 10), path)
          digest(path)
        }
      digestBefore must beSome[String]

      first.incoming.transition(version, state = Dir.Ready)
      first.append(version)

      val serverListen = socketAddress("0.0.0.0:44663")
      val server = new dist.Server(serverListen)
      server.start()

      val seed = new SeedAction(() => new dist.PrimarySeed(version, duration = 15))()
      val leech = new LeechAction(second, version, serverListen, duration = 15)()

      seed.start()
      leech.start()
      seed.await()
      leech.await()
      seed.stop()
      leech.stop(transition = true)
      server.stop()

      second.history(version).map(second.history / (_, Unpacked) / "garbage") must eventually(retries = 10, sleep = 3.seconds)(
        beSome[Path].which(after => digestBefore must beSome[String].which(before => before must_== digest(after))))
    }
    "distribute latest packed version" in new UsingTempDir {
      val version = Version.now()

      object first extends Tree(tempDir / "first")
      first.init()
      val dist = new Dist(first)
      dist.init(ignoreExisting = true)

      object second extends Tree(tempDir / "second")
      second.init()
      new Dist(second).init(ignoreExisting = true)

      first.validate()
      second.validate()

      first.incoming.create(version, state = Dir.Incomplete, repr = Unpacked)

      val digestBefore =
        for (path <- first.incoming(version).map(first.incoming / (_, Unpacked) / "garbage")) yield {
          writeTo(random(1024 * 1024 * 10), path)
          digest(path)
        }
      digestBefore must beSome[String]

      first.incoming.transition(version, state = Dir.Ready)
      first.append(version)

      val serverListen = socketAddress("0.0.0.0:44664")
      val server = new dist.Server(serverListen)
      server.start()

      val seed = new SeedAction(() => new dist.PrimarySeed(version, duration = 15))()
      seed.start()

      val leech =
        new LeechAction(
          tree = second,
          version = new Dist(second).remote(serverListen).latest_!,
          server = serverListen,
          duration = 15)()
      leech.start()

      seed.await()
      leech.await()

      seed.stop()
      leech.stop(transition = true)

      server.stop()

      second.history(version).map(second.history / (_, Unpacked) / "garbage") must eventually(retries = 10, sleep = 3.seconds)(
        beSome[Path].which(after => digestBefore must beSome[String].which(before => before must_== digest(after))))
    }
  }
}
