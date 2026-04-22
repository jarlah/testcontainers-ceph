# Testcontainers Ceph

![main](https://github.com/jarlah/testcontainers-ceph/actions/workflows/maven.yml/badge.svg?branch=main)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.jarlah/testcontainers-ceph?logo=apachemaven)](https://central.sonatype.com/artifact/io.github.jarlah/testcontainers-ceph)
[![License](https://img.shields.io/github/license/jarlah/testcontainers-ceph)](https://github.com/jarlah/testcontainers-ceph/blob/main/license.txt)

A [Testcontainers](https://www.testcontainers.org/) module for [Ceph](https://ceph.io) — spin up a throwaway S3-compatible object store in your JVM integration tests without standing up a real cluster.

Backed by the official [`quay.io/ceph/demo`](https://quay.io/repository/ceph/demo) image. Works with any AWS S3 SDK (path-style URLs, sigv4, etc.).

---

## Why use it?

- **Real Ceph, not a mock.** If your code talks to S3 in production and you want your tests to exercise bucket ops, SSE-C, multipart uploads, presigned URLs, etc., a stubbed S3 fakes won't catch the same bugs.
- **Isolated per test.** Each container is ephemeral. Buckets, objects, users — all gone when the test ends.
- **No setup.** Only requires a working Docker daemon (or Podman / Colima / Testcontainers Cloud).

## Requirements

- Java 11+
- Docker (or a compatible daemon — Podman / Colima / Docker Desktop / Rancher Desktop / Testcontainers Cloud)

## Installation

### Maven

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers-bom</artifactId>
            <version>2.0.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>io.github.jarlah</groupId>
        <artifactId>testcontainers-ceph</artifactId>
        <version>2.0.7</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Gradle

```groovy
testImplementation platform("org.testcontainers:testcontainers-bom:2.0.1")
testImplementation "org.testcontainers:testcontainers"
testImplementation "io.github.jarlah:testcontainers-ceph:2.0.7"
```

Check [Maven Central](https://central.sonatype.com/artifact/io.github.jarlah/testcontainers-ceph) for the latest version.

## Quick start

A minimal JUnit 5 test that creates a bucket, uploads an object, and reads it back:

```java
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.CephContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class CephExampleTest {

    @Container
    static CephContainer ceph = new CephContainer();

    @Test
    void uploadsAndReadsObject() throws Exception {
        S3Client s3 = S3Client.builder()
            .endpointOverride(ceph.getCephUrl())
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(ceph.getCephAccessKey(), ceph.getCephSecretKey())))
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true)    // Ceph RGW requires path-style
                .build())
            .build();

        s3.putObject(
            PutObjectRequest.builder()
                .bucket(ceph.getCephBucket())    // defaults to "demo"
                .key("hello.txt")
                .build(),
            RequestBody.fromString("hello from Ceph"));

        String body = s3.getObjectAsBytes(b -> b.bucket(ceph.getCephBucket()).key("hello.txt"))
            .asUtf8String();

        assertThat(body).isEqualTo("hello from Ceph");
    }
}
```

`CephContainer` ships sensible defaults: bucket `demo`, access key `demo`, and a pre-generated secret key. Everything is overridable.

> **AWS SDK 2.30+**: the SDK now sends flexible-checksum headers on PutObject by default. Older Ceph RGW (`latest-quincy` / v17) rejects them with HTTP 400. If you pin to the default image and see 400 errors on uploads, set `.requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)` and `.responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)` on the `S3Client` builder, or switch to the `quay.io/ceph/demo:latest` image.

## Configuration

### Custom image tag

```java
new CephContainer("quay.io/ceph/demo:latest-reef");    // Ceph v18

new CephContainer(DockerImageName.parse("quay.io/ceph/demo:latest-squid"));    // Ceph v19
```

### Prebuilt substitute image (air-gapped mirrors, custom builds)

```java
new CephContainer(
    DockerImageName.parse("my-registry.internal/ceph-demo:1.2.3")
        .asCompatibleSubstituteFor("quay.io/ceph/demo"));
```

### Credentials and bucket name

```java
new CephContainer()
    .withCephAccessKey("my-access-key")
    .withCephSecretKey("my-secret-key")
    .withCephBucket("my-bucket");
```

Read them back from the container:

```java
ceph.getCephAccessKey();
ceph.getCephSecretKey();
ceph.getCephBucket();
ceph.getCephPort();     // host port mapped to RGW's 8080
ceph.getCephUrl();      // http://host:port — pass straight to endpointOverride()
```

### Disable SSL (needed for SSE-C tests)

By default, the demo image enforces SSL on server-side-encryption-with-customer-keys operations. For local integration tests that use SSE-C over HTTP, disable it:

```java
new CephContainer().withSslDisabled();
```

### Customize RGW hostname (for container-to-container access)

By default, `RGW_NAME=localhost`, which is what you want when your test code runs on the host and reaches Ceph via the container's mapped port.

If **another container** on the same Docker network needs to talk to Ceph by network alias (e.g. your app under test is itself containerized), override the RGW name to match the alias:

```java
try (
    var network = Network.newNetwork();
    var ceph = new CephContainer()
        .withNetwork(network)
        .withNetworkAliases("ceph")
        .withRgwName("ceph");                         // match the alias
    var app = new GenericContainer<>("my-app:latest")
        .withNetwork(network)
        .withEnv("S3_ENDPOINT", "http://ceph:8080")   // reach by alias
) {
    ceph.start();
    app.start();
    // ...
}
```

> ⚠️ The demo image's RGW can only serve one hostname at a time. If you set `withRgwName(...)` to something other than `localhost`, host-based access via `getCephUrl()` will stop working for that container. Pick whichever path your test needs.

## Lifecycle

### JUnit 5 — shared across all tests in the class

```java
@Testcontainers
class MyTest {
    @Container
    static CephContainer ceph = new CephContainer();

    // all tests share one container — fast, but tests are not fully isolated
}
```

### JUnit 5 — fresh per test

```java
@Testcontainers
class MyTest {
    @Container
    CephContainer ceph = new CephContainer();    // non-static

    // each test gets a clean container — slower but isolated
}
```

### Plain Java (try-with-resources)

```java
try (CephContainer ceph = new CephContainer()) {
    ceph.start();
    // ...
}
```

### Custom wait strategy

By default, the container waits for the demo bucket-created log line with a 5-minute timeout. Override if needed:

```java
new CephContainer().waitingFor(Wait.forListeningPort());
```

## Debugging test failures

- `container.getLogs()` — full container stdout/stderr
- Run your test with `TESTCONTAINERS_RYUK_DISABLED=true` + `docker ps` to inspect the container while it's still alive
- Set the `-Dorg.testcontainers.containers.output.OutputFrame.LOGGER=DEBUG` property to stream container logs live

## Contributing

Issues and PRs welcome. The repo ships a Nix dev shell — `nix develop` (or `direnv allow`) gives you JDK 11, Maven, and the Docker CLI, so you don't need to install the toolchain on your host.

Run the test suite with:

```bash
mvn test
```

Tests spin up real Ceph containers and take ~2–5 minutes depending on your machine.

## License

Apache 2.0 — see [license.txt](license.txt).
