package salvo.util

import java.lang.{ ProcessBuilder, Process }
import ProcessBuilder.Redirect

object PBZip2 {
  lazy val defaultExe = System.getProperty("os.name") match {
    case "Mac OS X" => "/usr/local/bin/pbzip2"
    case "Linux"    => "/usr/bin/pbzip2"
    case what       => sys.error("'"+what+"' is not a supported operating system")
  }
}

class PBZip2(exe: Path = PBZip2.defaultExe, err: Redirect = Redirect.INHERIT) extends Logging {
  def pack(in: Path, out: Path): ProcessBuilder = {
    logger.info("pack "+in+" ==> "+out)
    new ProcessBuilder(exe.toAbsolutePath().toString, "-1" /*, "-v"*/ , "-c", in.toAbsolutePath().toString)
      .redirectInput(Redirect.from("/dev/null": Path))
      .redirectOutput(Redirect.to(out.toAbsolutePath()))
      .redirectError(err)
  }

  def pack_!(in: Path, out: Path) = pack(in, out).start().waitFor()

  def unpack(in: Path, out: Path): ProcessBuilder = {
    logger.info("unpack "+in+" ==> "+out)
    new ProcessBuilder(exe.toAbsolutePath().toString, "-d" /*, "-v"*/ , "-c", in.toAbsolutePath().toString)
      .redirectInput(Redirect.from("/dev/null": Path))
      .redirectOutput(Redirect.to(out.toAbsolutePath()))
      .redirectError(err)
  }

  def unpack_!(in: Path, out: Path) = unpack(in, out).start().waitFor()
}
