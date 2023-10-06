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

```java
@Container
CephContainer container = new CephContainer("quay.io/ceph/demo:latest");
```

### Configure access key and secret key

```java
@Container
CephContainer container = new CephContainer()
    .withCephAccessKey("accessKey")
    .withCephAccessKey("secretKey");
```

Setup
---

TODO