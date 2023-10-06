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

## How to use it?
---

### Include it into your project dependencies

If you're using Maven:
```xml
<dependency>
  <groupId>io.github.jarlah</groupId>
  <artifactId>testcontainers-ceph</artifactId>
  <version>VERSION</version>
</dependency>
```

or if you're using Gradle:

```groovy
dependencies {
    testImplementation 'io.github.jarlah:testcontainers-ceph:<VERSION>'
}
```