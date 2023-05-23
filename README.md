# iiq-rule-testing
A library for writing unit test against IIQ rules, scripts, etc.

Publishing package to GitHub Packages:

```shell
GITHUB_ACTOR=jtgasper3 GITHUB_TOKEN=<pat_with_package_write> ./gradlew -Pversion=0.0.4 clean test publish
```

Publishing locally for testing:

```shell
./gradlew clean test publishToMavenLocal
```