# Ant-Wrapper

A small command line wrapper for [Apache Ant](http://ant.apache.org/) similar to the
[Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html).

It can be used to run Ant projects without the need of any Apache installation and therefore is not
platform dependent.

# Building & verification

Run the following command to build Ant wrapper and run tests / verification.

```shell
gradlew build
```

# Usage

To use the Ant wrapper run the following command inside you Ant project root directory

```shell
antw <target>
```

instead of

```shell
ant <target>
```

which relies on the Ant version installed manually.
