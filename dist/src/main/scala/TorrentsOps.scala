package salvo.dist

import salvo.util._
import salvo.tree._

import org.eclipse.jetty.server.{ Handler, Server }
import org.eclipse.jetty.server.handler.{ DefaultHandler, HandlerList, ResourceHandler }

trait TorrentsOps {
  dist: Dist =>
  val Port = 44663 // XXX: ??? unhardcode me
  def server() = {
    val server = new Server(Port)
    val resource_handler = new ResourceHandler()
    resource_handler.setDirectoriesListed(true)
    resource_handler.setResourceBase(dir.toAbsolutePath().toString)
    val handlers = new HandlerList()
    handlers.setHandlers(Array(resource_handler /*, new DefaultHandler()*/ ))
    server.setHandler(handlers)
    server
  }
}
