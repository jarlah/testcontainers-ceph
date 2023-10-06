Testcontainers Ceph
===

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
CephContainer container = new CephContainer(DockerImageName.parse("quay.io/ceph/demo:latest"));
```
or
```java
@Container
CephContainer container = new CephContainer("quay.io/ceph/demo:latest");
```
or
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
CephContainer container = new CephContainer()
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
  <version>0.1-SNAPSHOT</version>
</dependency>
```

or if you're using Gradle:

```groovy
dependencies {
    testImplementation 'io.github.jarlah:testcontainers-ceph:0.1-SNAPSHOT'
}
```