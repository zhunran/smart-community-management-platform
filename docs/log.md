property-admin-api
异常：
Error starting ApplicationContext. To display the condition evaluation report re-run your application with 'debug' enabled.
2026-09-03T13:38:10.065+08:00 ERROR 38604 --- [property-admin-api] [ main] o.s.boot.SpringApplication : Application run failed

org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'adminPaymentController' defined in file [D:\.workspace\javaproject\property-management-system\property-management\property-admin-api\target\classes\com\property\adminapi\controller\AdminPaymentController.class]: Failed to instantiate [com.property.adminapi.controller.AdminPaymentController]: Constructor threw exception
at org.springframework.beans.factory.support.ConstructorResolver.instantiate(ConstructorResolver.java:321) ~[spring-beans-7.0.8.jar:7.0.8]
at org.springframework.beans.factory.support.ConstructorResolver.autowireConstructor(ConstructorResolver.java:309) ~[spring-beans-7.0.8.jar:7.0.8]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.autowireConstructor(AbstractAutowireCapableBeanFactory.java:1380) ~[spring-beans-7.0.8.jar:7.0.8]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBeanInstance(AbstractAutowireCapableBeanFactory.java:1219) ~[spring-beans-7.0.8.jar:7.0.8]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:565) ~[spring-beans-7.0.8.jar:7.0.8]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:525) ~[spring-beans-7.0.8.jar:7.0.8]
at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:333) ~[spring-beans-7.0.8.jar:7.0.8]
at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:371) ~[spring-beans-7.0.8.jar:7.0.8]
at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:331) ~[spring-beans-7.0.8.jar:7.0.8]
at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:196) ~[spring-beans-7.0.8.jar:7.0.8]
at org.springframework.beans.factory.support.DefaultListableBeanFactory.instantiateSingleton(DefaultListableBeanFactory.java:1225) ~[spring-beans-7.0.8.jar:7.0.8]
at org.springframework.beans.factory.support.DefaultListableBeanFactory.preInstantiateSingleton(DefaultListableBeanFactory.java:1191) ~[spring-beans-7.0.8.jar:7.0.8]
at org.springframework.beans.factory.support.DefaultListableBeanFactory.preInstantiateSingletons(DefaultListableBeanFactory.java:1121) ~[spring-beans-7.0.8.jar:7.0.8]
at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:994) ~[spring-context-7.0.8.jar:7.0.8]
at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:621) ~[spring-context-7.0.8.jar:7.0.8]
at org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:143) ~[spring-boot-web-server-4.1.0.jar:4.1.0]
at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:756) ~[spring-boot-4.1.0.jar:4.1.0]
at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:445) ~[spring-boot-4.1.0.jar:4.1.0]
at org.springframework.boot.SpringApplication.run(SpringApplication.java:321) ~[spring-boot-4.1.0.jar:4.1.0]
at org.springframework.boot.SpringApplication.run(SpringApplication.java:1365) ~[spring-boot-4.1.0.jar:4.1.0]
at org.springframework.boot.SpringApplication.run(SpringApplication.java:1354) ~[spring-boot-4.1.0.jar:4.1.0]
at com.property.adminapi.PropertyAdminApplication.main(PropertyAdminApplication.java:27) ~[classes/:na]
Caused by: org.springframework.beans.BeanInstantiationException: Failed to instantiate [com.property.adminapi.controller.AdminPaymentController]: Constructor threw exception
at org.springframework.beans.BeanUtils.instantiateClass(BeanUtils.java:220) ~[spring-beans-7.0.8.jar:7.0.8]
at org.springframework.beans.factory.support.SimpleInstantiationStrategy.instantiate(SimpleInstantiationStrategy.java:129) ~[spring-beans-7.0.8.jar:7.0.8]
at org.springframework.beans.factory.support.ConstructorResolver.instantiate(ConstructorResolver.java:318) ~[spring-beans-7.0.8.jar:7.0.8]
... 21 common frames omitted
Caused by: java.lang.Error: Unresolved compilation problems:
The import com.property.module.payment cannot be resolved
PaymentReconciliationService cannot be resolved to a type

    at com.property.adminapi.controller.AdminPaymentController.<init>(AdminPaymentController.java:11) ~[classes/:na]
    at java.base/jdk.internal.reflect.DirectConstructorHandleAccessor.newInstance(DirectConstructorHandleAccessor.java:62) ~[na:na]
    at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:499) ~[na:na]
    at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:483) ~[na:na]
    at org.springframework.beans.BeanUtils.instantiateClass(BeanUtils.java:207) ~[spring-beans-7.0.8.jar:7.0.8]
    ... 23 common frames omitted

进程已结束，退出代码为 1
property-owner-api:

Error starting ApplicationContext. To display the condition evaluation report re-run your application with 'debug' enabled.
2026-09-03T13:38:10.760+08:00 ERROR 38804 --- [property-owner-api] [ main] o.s.boot.SpringApplication : Application run failed

org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'ownerPaymentController': Lookup method resolution failed
at org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor.checkLookupMethods(AutowiredAnnotationBeanPostProcessor.java:482) ~[spring-beans-7.0.8.jar:7.0.8]
at org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor.determineCandidateConstructors(AutowiredAnnotationBeanPostProcessor.java:352) ~[spring-beans-7.0.8.jar:7.0.8]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.determineConstructorsFromBeanPostProcessors(AbstractAutowireCapableBeanFactory.java:1319) ~[spring-beans-7.0.8.jar:7.0.8]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBeanInstance(AbstractAutowireCapableBeanFactory.java:1216) ~[spring-beans-7.0.8.jar:7.0.8]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:565) ~[spring-beans-7.0.8.jar:7.0.8]
at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:525) ~[spring-beans-7.0.8.jar:7.0.8]
at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:333) ~[spring-beans-7.0.8.jar:7.0.8]
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:371) ~[spring-beans-7.0.8.jar:7.0.8]
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:331) ~[spring-beans-7.0.8.jar:7.0.8]
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:196) ~[spring-beans-7.0.8.jar:7.0.8]
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.instantiateSingleton(DefaultListableBeanFactory.java:1225) ~[spring-beans-7.0.8.jar:7.0.8]
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.preInstantiateSingleton(DefaultListableBeanFactory.java:1191) ~[spring-beans-7.0.8.jar:7.0.8]
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.preInstantiateSingletons(DefaultListableBeanFactory.java:1121) ~[spring-beans-7.0.8.jar:7.0.8]
	at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:994) ~[spring-context-7.0.8.jar:7.0.8]
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:621) ~[spring-context-7.0.8.jar:7.0.8]
	at org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:143) ~[spring-boot-web-server-4.1.0.jar:4.1.0]
	at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:756) ~[spring-boot-4.1.0.jar:4.1.0]
	at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:445) ~[spring-boot-4.1.0.jar:4.1.0]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:321) ~[spring-boot-4.1.0.jar:4.1.0]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1365) ~[spring-boot-4.1.0.jar:4.1.0]
	at org.springframework.boot.SpringApplication.run(SpringApplication.java:1354) ~[spring-boot-4.1.0.jar:4.1.0]
	at com.property.ownerapi.PropertyOwnerApplication.main(PropertyOwnerApplication.java:13) ~[classes/:na]
Caused by: java.lang.IllegalStateException: Failed to introspect Class [com.property.ownerapi.controller.OwnerPaymentController] from ClassLoader [jdk.internal.loader.ClassLoaders$AppClassLoader@73d16e93]
at org.springframework.util.ReflectionUtils.getDeclaredMethods(ReflectionUtils.java:483) ~[spring-core-7.0.8.jar:7.0.8]
at org.springframework.util.ReflectionUtils.doWithLocalMethods(ReflectionUtils.java:320) ~[spring-core-7.0.8.jar:7.0.8]
at org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor.checkLookupMethods(AutowiredAnnotationBeanPostProcessor.java:460) ~[spring-beans-7.0.8.jar:7.0.8]
... 21 common frames omitted
Caused by: java.lang.NoClassDefFoundError: PayOrderRequest
at java.base/java.lang.Class.getDeclaredMethods0(Native Method) ~[na:na]
at java.base/java.lang.Class.privateGetDeclaredMethods(Class.java:3011) ~[na:na]
at java.base/java.lang.Class.getDeclaredMethods(Class.java:2330) ~[na:na]
at org.springframework.util.ReflectionUtils.getDeclaredMethods(ReflectionUtils.java:465) ~[spring-core-7.0.8.jar:7.0.8]
... 23 common frames omitted
Caused by: java.lang.ClassNotFoundException: PayOrderRequest
at java.base/jdk.internal.loader.BuiltinClassLoader.loadClass(BuiltinClassLoader.java:580) ~[na:na]
at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:502) ~[na:na]
... 27 common frames omitted

进程已结束，退出代码为 1
