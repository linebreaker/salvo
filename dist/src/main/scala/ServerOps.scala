package salvo.dist

import salvo.util._
import salvo.tree._

import org.eclipse.jetty.server.{ Handler, Server => JettyServer }
import org.eclipse.jetty.server.handler.{ DefaultHandler, HandlerList, ResourceHandler }
import org.eclipse.jetty.servlet.{ ServletHandler, ServletHolder }
import javax.servlet.http.{ HttpServlet, HttpServletRequest, HttpServletResponse }
import java.net.InetSocketAddress

trait ServerOps {
  dist: Dist =>

  val DefaultPort = 44663

  class Server(listen: InetSocketAddress = socketAddress(DefaultPort)) {
    private def init() = {
      val jetty = new JettyServer(listen)

      val resource_handler = new ResourceHandler()
      resource_handler.setDirectoriesListed(true)
      resource_handler.setResourceBase(dir.toAbsolutePath().toString)

      val servlets = new ServletHandler()
      servlets.addServletWithMapping(new ServletHolder(new LatestVersion), "/latest-version.do")

      val handlers = new HandlerList()
      handlers.setHandlers(Array(servlets, resource_handler))

      jetty.setHandler(handlers)

      jetty
    }

    private val http = init()

    def start() {
      http.start()
    }
    def stop() {
      http.stop()
      http.join()
    }
  }

  class LatestVersion extends HttpServlet {
    override def doGet(req: HttpServletRequest, res: HttpServletResponse) {
      dist.tree.history.latest() match {
        case Some(Dir(version, _)) =>
          res.setStatus(HttpServletResponse.SC_OK)
          res.getWriter.println(version.toString)
        case _ =>
          res.setStatus(HttpServletResponse.SC_NOT_FOUND)
      }
    }
  }
}
