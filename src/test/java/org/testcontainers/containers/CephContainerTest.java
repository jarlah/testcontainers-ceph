package org.testcontainers.containers;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CephContainerTest {

    @Test
    public void testBasicUsage() throws Exception {
        try (
            // minioContainer {
            CephContainer container = new CephContainer()
            // }
        ) {
            container.start();
            // We should not modify demo script by default
            assertThat(getDemoScriptFromContainer(container)).doesNotContain(
                    "rgw frontends = ${RGW_FRONTEND}\n" +
                    "rgw verify ssl = false\n" +
                    "rgw crypt require ssl = false"
            );
            assertThat(container.getCephAccessKey()).isEqualTo("demo");
            assertThat(container.getCephSecretKey()).isEqualTo("demo");
            assertThat(container.getCephBucket()).isEqualTo("demo");

            AmazonS3 s3client = getS3client(container);

            assertThat(s3client.doesBucketExistV2("demo")).isTrue();

            URL file = this.getClass().getResource("/object_to_upload.txt");
            assertThat(file).isNotNull();
            s3client.putObject("demo", "my-objectname", file.getFile());

            List<S3ObjectSummary> objets = s3client.listObjectsV2("demo").getObjectSummaries();
            assertThat(objets.size()).isEqualTo(1);
            assertThat(objets.get(0).getKey()).isEqualTo("my-objectname");
        }
    }

    @Test
    public void testOverrides() throws Exception {
        try (
            // cephOverrides {
            CephContainer container = new CephContainer("quay.io/ceph/demo:latest")
                .withCephAccessKey("testuser123")
                .withCephSecretKey("testpassword123")
                .withCephBucket("testbucket123")
                .withSslDisabled()
            // }
        ) {
            container.start();
            // we should have modified the demo script
            assertThat(getDemoScriptFromContainer(container)).contains(
                    "rgw frontends = ${RGW_FRONTEND}\n" +
                    "rgw verify ssl = false\n" +
                    "rgw crypt require ssl = false"
            );
            assertThat(container.getCephAccessKey()).isEqualTo("testuser123");
            assertThat(container.getCephSecretKey()).isEqualTo("testpassword123");
            assertThat(container.getCephBucket()).isEqualTo("testbucket123");
            AmazonS3 s3client = getS3client(container);
            assertThat(s3client.doesBucketExistV2("testbucket123")).isTrue();
        }
    }

    @Test
    public void testSpecificDaemonImage() throws URISyntaxException {
        DockerImageName daemonImage =
                DockerImageName.parse("quay.io/ceph/daemon:v7.0.3-stable-7.0-quincy-centos-stream8-x86_64")
                        .asCompatibleSubstituteFor("quay.io/ceph/demo");
        try (
                // cephOverrides {
                CephContainer container = new CephContainer(daemonImage)
                        .withCephAccessKey("testuser123")
                        .withCephSecretKey("testpassword123")
                        .withCephBucket("testbucket123")
                        .withCommand("demo")
                // }
        ) {
            container.start();
            assertThat(container.getCephAccessKey()).isEqualTo("testuser123");
            assertThat(container.getCephSecretKey()).isEqualTo("testpassword123");
            assertThat(container.getCephBucket()).isEqualTo("testbucket123");
            AmazonS3 s3client = getS3client(container);
            assertThat(s3client.doesBucketExistV2("testbucket123")).isTrue();
        }
    }

    // configuringClient {

    private static AmazonS3 getS3client(CephContainer container) throws URISyntaxException {
        AWSCredentials credentials = new BasicAWSCredentials(
                container.getCephAccessKey(),
                container.getCephSecretKey()
        );
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
                container.getCephUrl().toString(),
                ""
        );
        return AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withPathStyleAccessEnabled(true)
                .build();
    }
    // }

    private static String getDemoScriptFromContainer(CephContainer container) throws IOException {
        container.copyFileFromContainer("/opt/ceph-container/bin/demo", "demo-script");
        return new String(Files.readAllBytes(Paths.get("demo-script")));
    }
}
