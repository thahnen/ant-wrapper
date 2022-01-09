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

# Installation (using Ant installed)

Run the following command to create an Ant wrapper installation in your Ant project

```shell
gradlew installWithAntInstalled -Pout=/home/user/ant/testProject
```

After that the Ant project root directory will contain another directory called *ant* with another
directory inside called *wrapper*. There you'll find the Ant wrapper installation consisting of Jar
and properties file.

Also there will be two new files located inside your Ant project root directory. One called *antw*
and one called *antw.bat* (Unix / Windows wrapper invoker).

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
