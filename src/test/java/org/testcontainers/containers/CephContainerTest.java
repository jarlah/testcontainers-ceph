package org.testcontainers.containers;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CephContainerTest {

    @Test
    void testBasicUsage() throws Exception {
        try (
                CephContainer container = new CephContainer()
        ) {
            container.start();
            // We should not modify demo script by default
            assertThat(getDemoScriptFromContainer(container)).doesNotContain(
                    "rgw frontends = ${RGW_FRONTEND}\n" +
                            "rgw verify ssl = false\n" +
                            "rgw crypt require ssl = false"
            );
            assertThat(container.getCephAccessKey()).isEqualTo("demo");
            assertThat(container.getCephSecretKey()).isSameAs("b36361c4-1589-42f7-a369-d9dafb926d55");
            assertThat(container.getCephBucket()).isEqualTo("demo");
            assertThat(container.getWaitStrategy()).isInstanceOf(LogMessageWaitStrategy.class);

            S3Client s3client = getS3client(container);

            HeadBucketResponse headBucketResponse = s3client.headBucket(HeadBucketRequest.builder().bucket("demo").build());
            assertThat(headBucketResponse.sdkHttpResponse().isSuccessful())
                    .isTrue();
            assertThat(headBucketResponse.sdkHttpResponse().headers().get("X-RGW-Quota-Max-Buckets").get(0))
                    .isEqualTo("1000");

            final String key = "my-objectname";

            PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket("demo").key(key).build();
            PutObjectResponse putObjectResponse = s3client.putObject(putObjectRequest, RequestBody.fromFile(getTestFile()));
            assertThat(putObjectResponse.sdkHttpResponse().isSuccessful()).isTrue();
            assertThat(putObjectResponse.eTag()).isNotNull();
            assertThat(putObjectResponse.expiration()).isNull();

            ResponseInputStream<GetObjectResponse>
                    responseInputStream = s3client.getObject(request -> request.bucket("demo").key(key));
            assertThat(responseInputStream).hasContent("This is a file");

            ListObjectsV2Response objets = s3client.listObjectsV2(builder -> builder.bucket("demo"));
            assertThat(objets.contents()).hasSize(1);
            assertThat(objets.contents().get(0).key()).isEqualTo(key);

            assertThatThrownBy(() ->
                    s3client.putObject(
                            getSSECPutObjectRequest("demo", key),
                            RequestBody.fromFile(getTestFile())
                    )
            )
                    .isInstanceOf(software.amazon.awssdk.services.s3.model.S3Exception.class)
                    .hasMessageContaining("Service: S3, Status Code: 400, Request ID: ");
        }
    }

    @Test
    void testOverrides() throws Exception {
        try (
                CephContainer container = new CephContainer("quay.io/ceph/demo:latest")
                        .withCephAccessKey("testuser123")
                        .withCephSecretKey("testpassword123")
                        .withCephBucket("testbucket123")
                        .withSslDisabled()

        ) {
            container.start();
            // we should have modified the demo script
            assertThat(getDemoScriptFromContainer(container)).contains(
                    "rgw frontends = ${RGW_FRONTEND}\n" +
                            "rgw verify ssl = false\n" +
                            "rgw crypt require ssl = false"
            );
            assertThat(container.getWaitStrategy()).isInstanceOf(LogMessageWaitStrategy.class);
            assertThat(container.getCephAccessKey()).isEqualTo("testuser123");
            assertThat(container.getCephSecretKey()).isEqualTo("testpassword123");
            assertThat(container.getCephBucket()).isEqualTo("testbucket123");
            S3Client s3client = getS3client(container);
            s3client.headBucket(HeadBucketRequest.builder().bucket("testbucket123").build());
            PutObjectResponse sseCPutObjectResponse = s3client.putObject(
                    getSSECPutObjectRequest("testbucket123", "some-objectname"),
                    RequestBody.fromFile(getTestFile())
            );
            assertThat(sseCPutObjectResponse).isNotNull();
            PutObjectResponse normalPutObjectResponse = s3client.putObject(
                    PutObjectRequest.builder().key("some-other-key").bucket("testbucket123").build(),
                    RequestBody.fromFile(getTestFile())
            );
            assertThat(normalPutObjectResponse).isNotNull();
        }
    }

    @Test
    void testSpecificDaemonImage() throws URISyntaxException {
        DockerImageName daemonImage =
                DockerImageName.parse("quay.io/ceph/daemon:v7.0.3-stable-7.0-quincy-centos-stream8-x86_64")
                        .asCompatibleSubstituteFor("quay.io/ceph/demo");
        try (
                CephContainer container = new CephContainer(daemonImage)
                        .withCephAccessKey("testuser123")
                        .withCephSecretKey("testpassword123")
                        .withCephBucket("testbucket123")
                        .withCommand("demo")
        ) {
            container.start();
            assertThat(container.getWaitStrategy()).isInstanceOf(LogMessageWaitStrategy.class);
            assertThat(container.getCephAccessKey()).isEqualTo("testuser123");
            assertThat(container.getCephSecretKey()).isEqualTo("testpassword123");
            assertThat(container.getCephBucket()).isEqualTo("testbucket123");
            S3Client s3client = getS3client(container);
            s3client.headBucket(HeadBucketRequest.builder().bucket("testbucket123").build());
        }
    }

    /**
     * Test that startupStrategy override works
     * Keep validating issue<a href="https://github.com/jarlah/testcontainers-ceph/issues/176"> #176</a>
     */
    @Test
    void testOverrideStartupStrategy() {
        DockerImageName daemonImage =
                DockerImageName.parse("quay.io/ceph/daemon:v7.0.3-stable-7.0-quincy-centos-stream8-x86_64")
                        .asCompatibleSubstituteFor("quay.io/ceph/demo");
        try (
                CephContainer container = new CephContainer(daemonImage)
                        .withCephAccessKey("testuser123")
                        .withCephSecretKey("testpassword123")
                        .withCephBucket("testbucket123")
                        .withCommand("demo")
        ) {
            container.setWaitStrategy(Wait.forListeningPort());
            container.start();
            assertThat(container.isRunning()).isTrue();
            assertThat(container.getWaitStrategy()).isInstanceOf(HostPortWaitStrategy.class);
        }
    }

    /**
     * Test that WaitingFor override works
     * Keep validating issue<a href="https://github.com/jarlah/testcontainers-ceph/issues/176"> #176</a>
     */
    @Test
    void testOverrideWaitingFor() {
        DockerImageName daemonImage =
                DockerImageName.parse("quay.io/ceph/daemon:v7.0.3-stable-7.0-quincy-centos-stream8-x86_64")
                        .asCompatibleSubstituteFor("quay.io/ceph/demo");
        try (
                CephContainer container = new CephContainer(daemonImage)
                        .withCephAccessKey("testuser123")
                        .withCephSecretKey("testpassword123")
                        .withCephBucket("testbucket123")
                        .withCommand("demo")
        ) {
            container.waitingFor(Wait.forListeningPort());
            container.start();
            assertThat(container.isRunning()).isTrue();
            assertThat(container.getWaitStrategy()).isInstanceOf(HostPortWaitStrategy.class);
        }
    }

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


    private static String getDemoScriptFromContainer(CephContainer container) throws IOException {
        container.copyFileFromContainer("/opt/ceph-container/bin/demo", "demo-script");
        return new String(Files.readAllBytes(Paths.get("demo-script")));
    }

    @NotNull
    private File getTestFile() throws URISyntaxException {
        return Paths.get(Objects.requireNonNull(this.getClass().getResource("/object_to_upload.txt")).toURI()).toFile();
    }

    private static SecretKey getPasswordBasedKey(char[] password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] salt = new byte[100];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password, salt, 1000, 256);
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(pbeKeySpec);
    }

    private static PutObjectRequest getSSECPutObjectRequest(String bucket, String key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKey secretKey = getPasswordBasedKey("testtesttesttesttesttest123".toCharArray());
        byte[] customerKeyBytes = secretKey.getEncoded();
        String customerKeyMD5;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5Bytes = md.digest(customerKeyBytes);
            customerKeyMD5 = Base64.getEncoder().encodeToString(md5Bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
        String sseCKeyBase64 = Base64.getEncoder().encodeToString(customerKeyBytes);
        return PutObjectRequest.builder()
                .key(key)
                .bucket(bucket)
                .sseCustomerAlgorithm("AES256")
                .sseCustomerKey(sseCKeyBase64)
                .sseCustomerKeyMD5(customerKeyMD5)
                .build();
    }
}
