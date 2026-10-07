package app.rekord.application;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

/** Fails any container start in a JVM that names it as the Testcontainers image substitutor. */
public class ContainerTripwire extends ImageNameSubstitutor {

    public ContainerTripwire() {}

    @Override
    public DockerImageName apply(DockerImageName original) {
        throw new IllegalStateException("a resource test must not start a container: " + original);
    }

    @Override
    protected String getDescription() {
        return "fails every container start of a resource test";
    }
}
