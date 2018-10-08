[![Build Status](https://api.travis-ci.org/coveo/spring-boot-parameter-store-integration.svg?branch=master)](https://travis-ci.org/coveo/spring-boot-parameter-store-integration)
[![MIT license](http://img.shields.io/badge/license-MIT-brightgreen.svg)](https://github.com/coveo/spring-boot-parameter-store-integration/blob/master/LICENSE)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.coveo/spring-boot-parameter-store-integration/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.coveo/spring-boot-parameter-store-integration)

# Spring Boot Parameter Store Integration

The Spring Boot Parameter Store Integration is a tiny library used to integrate AWS Parameter Store in Spring Boot's powerful property injection. For example, it allows you to fetch a property directly using the `@Value` annotation. In fact, it simply adds a PropertySource with highest precedence to the existing ones (see [Spring Boot's External Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html)).

## Requirements
The library uses:

- [Spring Boot](https://spring.io/projects/spring-boot) 

Those can be overridden in your `pom.xml`.  

The library was tested and worked properly with:

- [Spring Boot](https://spring.io/projects/spring-boot) 2.0.x

## Unleashing the Magic

#### For your pom.xml:
```
<dependency>
    <groupId>tga.aws</groupId>
    <artifactId>aws-parameter-store-spring-boot-starter</artifactId>
    <version>2.0.2.RELEASE</version>
</dependency>
```

#### Enabling:
- Set `psSpringProfiles` environment property with some custom profiles that should integrate the AWS Parameter Store using a comma-separated list such as `Prod,PreProd,IntegrationTests`  
- You can set`psSpringProfiles` environment property to `'ANY'` value - in this case the integration will be activated for any profile.   
**Important**: using other list injecting methods like a yaml list won't work because this property gets loaded too early in the boot process.

        // the best way to setup the psSpringProfiles is:
        @SpringBootApplication
        public class App {
            public static void main(String[] args) {
                System.setProperty("psSpringProfiles","ANY"); // activated for any profile
                System.setProperty("psRoots","/myapp,/common"); // 2 root folders will be used for reading properties (see bellow in the documentation)
        
                SpringApplication.run(App.class, args);
           }
        }
 

#### Using the lib 'by default':
By default, the library will read property values from AWS Parameter store using the simple name convention:
* all dots `.` will be replaced with slashes `/`
* one slash will be added at the beginning
* **Example:** 
    * Spring `my.super.property` will be will be readed from AWS by name: `/my/super/property`   

#### Using the lib 'with roots':
'By default' mode is ok if your are using only a single application with AWS Parameter Store.
But, In the case you are using several applications and want to separate groups of parameters 
for your application (for security reasons, or to make your parameters store more maintainable),
you can:
* put parameters for your aplication to one ore several 'root folders'
    * For instance: `/my-app` - for my application only, `/common` - common parameters for all applications    
* tell the library, what root folders it has to use, via the `psRoots` environment property:
    * `psRoots` <-- `/my-app,/common`
    * the library will try read parameter `my.super.property` in the following order:
        1. `/my-app/my/super/property`
        1. `/common/my/super/property`   

#### Logging
The library do not use any logging library.

Several simple log messages will be rendered via `System.out`. So - you can't switch off logs from the library. 

It's going becouse the libray starts BEFORE any logging context will be created by Spring Boot.

The library will logs all loaded properties in the following format:

    AWS Parameter Store loaded: {"springProperty": "<property key>", "name" = "<aws parameter name>", "value" = "<loaded value>"} 

## AWS Credentials

The lib uses the [DefaultAWSCredentialProviderChain](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html). This means if your code is running on an EC2 instance that has access to a Parameter Store property and its associated KMS key, the library should be able to fetch it without any configuration.
