# Bremersee's Spring Cloud Eureka Server

This eureka server is more or less a plain Spring Cloud Eureka Server:

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

It's resources are protected with Basic Authentication. You can define three users: 
A client user can access the web page and the `/eureka` endpoints,
an actuator user can access the actuator endpoints and an administration user can do both.

```yaml
bremersee:
  access:
    application-access: ${APPLICATION_ACCESS:hasIpAddress('127.0.0.1') or hasIpAddress('::1')}
    actuator-access: ${ACTUATOR_ACCESS:hasIpAddress('127.0.0.1') or hasIpAddress('::1')}
    client-user-name: ${CLIENT_USER_NAME:eureka}
    client-user-password: ${CLIENT_USER_PASSWORD:changeit}
    actuator-user-name: ${ACTUATOR_USER_NAME:}
    actuator-user-password: ${ACTUATOR_USER_PASSWORD:}
    admin-user-name: ${ADMIN_USER_NAME:}
    admin-user-password: ${ADMIN_USER_PASSWORD:}
```

Via 'application-access' and 'actuator-access' you can grant access to the resources of the server 
without authentication.

