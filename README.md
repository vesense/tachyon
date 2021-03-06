Tachyon
=======

The master branch is in version 0.7.0-SNAPSHOT:

- [Tachyon Homepage](http://www.tachyonproject.org)
- [Contribute to Tachyon](http://tachyon-project.org/master/Contributing-to-Tachyon.html) and
[New Contributor's Tasks](https://tachyon.atlassian.net/issues/?jql=project%20%3D%20TACHYON%20AND%20labels%20%3D%20NewContributor%20AND%20status%20%3D%20Open)
  - Please limit 2 tasks per New Contributor. Afterwards, try some Beginner/Intermediate tasks,
  or ask in the [User Mailing List](https://groups.google.com/forum/?fromgroups#!forum/tachyon-users).
- [Releases](https://github.com/amplab/tachyon/tags)
- [Tachyon JIRA](https://tachyon.atlassian.net/browse/TACHYON)
- [Master Branch Document](http://tachyon-project.org/master/)
- [User Group](https://groups.google.com/forum/?fromgroups#!forum/tachyon-users)
- [Meetup Group](http://www.meetup.com/Tachyon)

## Building applications with Tachyon

### Dependency Information

#### Apache Maven
```xml
<dependency>
  <groupId>org.tachyonproject</groupId>
  <artifactId>tachyon-client</artifactId>
  <version>0.6.4</version>
</dependency>
```

#### Gradle

```groovy
compile 'org.tachyonproject:tachyon-client:0.6.4'
```

#### Apache Ant
```xml
<dependency org="org.tachyonproject" name="tachyon" rev="0.6.4">
  <artifact name="tachyon-client" type="jar" />
</dependency>
```

#### SBT
```
libraryDependencies += "org.tachyonproject" % "tachyon-client" % "0.6.4"
```
