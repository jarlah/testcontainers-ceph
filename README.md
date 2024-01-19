Testcontainers Ceph
===

![main](https://github.com/jarlah/testcontainers-ceph/actions/workflows/maven.yml/badge.svg?branch=main)

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.jarlah/testcontainers-ceph/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.jarlah/testcontainers-ceph)

A [Testcontainers](https://www.testcontainers.org/) implementation for [Ceph](https://ceph.io).

How to use
---

You can use the `@Container` annotation to start a Ceph container.

### Default image

```java
@Container
CephContainer container = new CephContainer();
```

### Custom image

```java
@Container
CephContainer container = new CephContainer(DockerImageName.parse("quay.io/ceph/demo"));
```

or override with a non-standard, but yet compliant image, for ex if you make a new image based on quay.io/ceph/demo

```java
@Container
CephContainer container = new CephContainer(
        DockerImageName.parse("our-prebuilt-ceph-demo-image")
            .asCompatibleSubstituteFor("quay.io/ceph/demo")
);
```

### Configure access key and secret key

```java
@Container
CephContainer container = new CephContainer("quay.io/ceph/demo")
    .withCephAccessKey("accessKey")
    .withCephAccessKey("secretKey");
```

## How to use it?
---

### Include it into your project dependencies

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers-bom</artifactId>
            <version>1.19.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
    </dependency>
    <dependency>
        <groupId>io.github.jarlah</groupId>
        <artifactId>testcontainers-ceph</artifactId>
        <scope>test</scope>
        <version>1.2.5</version>
    </dependency>
</dependencies>
```

The package will no longer be published to Sonatype nexus, e.g. maven central. Latest version in maven central is 1.2.5. To access newer versions or even bleeding edge directly from source tree, use version main-SNAPSHOT.