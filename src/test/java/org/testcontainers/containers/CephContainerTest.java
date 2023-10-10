package org.testcontainers.containers;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

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

            S3Client s3client = getS3client(container);

            s3client.headBucket(HeadBucketRequest.builder().bucket("demo").build());

            PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket("demo").key("my-objectname").build();
            s3client.putObject(putObjectRequest, RequestBody.fromFile(getTestFile()));

            ListObjectsV2Response objets = s3client.listObjectsV2((builder) -> builder.bucket("demo"));
            assertThat(objets.contents().size()).isEqualTo(1);
            assertThat(objets.contents().get(0).key()).isEqualTo("my-objectname");
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
            S3Client s3client = getS3client(container);
            s3client.headBucket(HeadBucketRequest.builder().bucket("testbucket123").build());
            PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket("testbucket123").key("my-objectname").build();
            s3client.putObject(putObjectRequest, RequestBody.fromFile(getTestFile()));
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
            S3Client s3client = getS3client(container);
            s3client.headBucket(HeadBucketRequest.builder().bucket("testbucket123").build());
        }
    }


    // configuringClient {
    private static S3Client getS3client(CephContainer container) throws URISyntaxException {
        final AwsBasicCredentials credentials = AwsBasicCredentials.create(
                container.getCephAccessKey(),
                container.getCephSecretKey()
        );
        final StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);
        return S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .endpointOverride(container.getCephUrl())
                .region(Region.US_EAST_1)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    // }
    private static String getDemoScriptFromContainer(CephContainer container) throws IOException {
        container.copyFileFromContainer("/opt/ceph-container/bin/demo", "demo-script");
        return new String(Files.readAllBytes(Paths.get("demo-script")));
    }

    @NotNull
    private File getTestFile() throws URISyntaxException {
        return Paths.get(Objects.requireNonNull(this.getClass().getResource("/object_to_upload.txt")).toURI()).toFile();
    }
}
