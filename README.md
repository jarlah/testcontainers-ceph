Testcontainers Ceph
===

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

## How to get it?
---

### Include it into your project dependencies

Its available in Maven Central.

```xml
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
        <version>2.0.0</version>
    </dependency>
</dependencies>
```

For snapshot version, e.g. latest bleeding edge version, use jitpack by adding the jitpack repository.

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

then change version of testcontainers-ceph to `main-SNAPSHOT`