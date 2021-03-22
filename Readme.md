# springboot-example

## What is it?

A spring boot example that demonstrates some basic capabilities for Spring boot 2.4 with Vault and Consul using both  the config  data api as well as legacy bootstrap processing.

## Dependencies

### OpenJDK  15
https://docs.aws.amazon.com/corretto/latest/corretto-15-ug/downloads-list.html

#### Mac
https://www.macports.org/ports.php?by=name&substr=jdk
https://github.com/AdoptOpenJDK/homebrew-openjdk


### Maven 3.5.4
https://archive.apache.org/dist/maven/maven-3/3.5.4/binaries/

## Build

### Maven

```
mvn clean install
```

### Run

#### spring boot
```
mvn clean install spring-boot:run
mvn clean install spring-boot:run -Dspring-boot.run.profiles=test
```
### Open issue 

https://github.com/spring-projects/spring-boot/issues/25705

#### Tests for consul ACL token

```
 mvn test -Dtest=AppTest
```

#### Tests for Legacy bootstrap processing

```
 mvn test -Dspring.config.use-legacy-processing=true -Dtest=AppTest
```