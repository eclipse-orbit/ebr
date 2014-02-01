import org.apache.commons.io.IOUtils
import static org.junit.Assert.*

assert new File( basedir, "target/MANIFEST.MF" ).isFile()
assert new File( basedir, "target/simple-it-1.0.0-SNAPSHOT.jar" ).isFile()
assert new File( basedir, "target/MANIFEST-SRC.MF" ).isFile()
assert new File( basedir, "target/simple-it-1.0.0-SNAPSHOT-sources.jar" ).isFile()


InputStream is = new FileInputStream(new File( basedir, "target/MANIFEST.MF" ))
def manifestContent = IOUtils.toString(is)
IOUtils.closeQuietly(is)

assert !manifestContent.contains("SNAPSHOT")

