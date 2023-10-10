package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

/**
 * Testcontainers implementation for Ceph.
 * <br>
 * Supported image: {@code quay.io/ceph/demo}
 * <br>
 * Defaults to {@code quay.io/ceph/demo:latest-quincy} aka v17 of Ceph
 * <br>
 * Exposed ports:
 * <ul>
 *     <li>Ceph: 8080</li>
 *     <li>Monitor: 3300</li>
 * </ul>
 */
public class CephContainer extends GenericContainer<CephContainer> {
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("quay.io/ceph/demo");

    private static final String DEFAULT_IMAGE_TAG = "latest-quincy";

    private static final String CEPH_RGW_DEFAULT_ACCESS_KEY = "demo";

    private static final String CEPH_RGW_DEFAULT_SECRET_KEY = "demo";

    private static final Integer CEPH_MON_DEFAULT_PORT = 3300;

    private static final Integer CEPH_RGW_DEFAULT_PORT = 8080;

    private static final String CEPH_DEMO_UID = "demo";

    private static final String CEPH_DEMO_BUCKET = "demo";

    private static final String CEPH_END_START_REGEX_FORMAT = ".*Bucket 's3://%s/' created\n.*";

    private String cephAccessKey;

    private String cephSecretKey;

    private String cephBucket;

    public CephContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_IMAGE_TAG));
    }

    public CephContainer(final String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public CephContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
    }

    @Override
    public void configure() {
        addExposedPorts(CEPH_MON_DEFAULT_PORT, CEPH_RGW_DEFAULT_PORT);

        addEnv("CEPH_DEMO_UID", CEPH_DEMO_UID);
        addEnv(
            "CEPH_DEMO_BUCKET",
            this.cephBucket != null
                ? this.cephBucket
                : (this.cephBucket = CEPH_DEMO_BUCKET)
        );
        addEnv(
            "CEPH_DEMO_ACCESS_KEY",
            this.cephAccessKey != null
                ? this.cephAccessKey
                : (this.cephAccessKey = CEPH_RGW_DEFAULT_ACCESS_KEY)
        );
        addEnv(
            "CEPH_DEMO_SECRET_KEY",
            this.cephSecretKey != null
                ? this.cephSecretKey
                : (this.cephSecretKey = CEPH_RGW_DEFAULT_SECRET_KEY)
        );
        addEnv("CEPH_PUBLIC_NETWORK", "0.0.0.0/0");
        // This needs to be 127.0.0.1, if not the demo image will not start properly
        addEnv("MON_IP", "127.0.0.1");
        // This is important because without it, we cant access ceph from http://localhost:<PORT>
        addEnv("RGW_NAME", "localhost");

        setWaitStrategy(Wait.forLogMessage(String.format(CEPH_END_START_REGEX_FORMAT, this.cephBucket), 1)
                .withStartupTimeout(Duration.ofMinutes(5)));
    }

    public CephContainer withCephAccessKey(String cephAccessKey) {
        this.cephAccessKey = cephAccessKey;
        return this;
    }

    public CephContainer withCephSecretKey(String cephSecretKey) {
        this.cephSecretKey = cephSecretKey;
        return this;
    }

    public CephContainer withCephBucket(String cephBucket) {
        this.cephBucket = cephBucket;
        return this;
    }

    public int getCephPort() {
        return getMappedPort(CEPH_RGW_DEFAULT_PORT);
    }

    public URI getCephUrl() throws URISyntaxException {
        return new URI(String.format("http://%s:%s", this.getHost(), getCephPort()));
    }

    public String getCephAccessKey() {
        return cephAccessKey;
    }

    public String getCephSecretKey() {
        return cephSecretKey;
    }

    public String getCephBucket() {
        return cephBucket;
    }
}
