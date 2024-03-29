## _使用注意事项：_

* 被注入`bean`必须含有无参构造方法：   
即被`@Component`、`@Configuration`或者自定义注入注解标记的类必须含有无参构造方法。

## Quick Start

_详情请阅览_[DemoApplication.java](src%2Ftest%2Fjava%2Forg%2Ffhm%2Fsubstrate%2FDemoApplication.java)

### `maven`引入

```xml
    <dependency>
        <groupId>cn.org.forhuman.substrate</groupId>
        <artifactId>fhm-jSubstrate</artifactId>
        <version>1.0.0</version>
    </dependency>
```

### 示例：

#### ***程序启动类***

```java
   @ScanPackageConfig("scan.package.name")
   public class DemoApplication {
       public static void main(String[] args) {
           Bootstrap.open(args, DemoStarter.class);
       }
   
   }
```  

#### ***`IStarter`接口实现***

```java
@Component // Inject into the IOC
public class DemoStarter implements IStarter {

    @Setup // Load from the IOC 
    private DemoAttach attach;
    
    @Setup("Demo") 
    // Load from the IOC by interface or abstract-class.
    // Multiple implementations need to be annotated with values that 
    // correspond to the injection names of their respective implementation objects.
    private IDemoTest demo;

    @Setup("->test.demo.bean.name") 
    // Specifies that the reference of the test.demo.bean.name
    // attribute in the configuration
    // file is the name of the loading object
    private IDemoTest demoAttach;

    @Setup // Mapping loads bean mechanisms
    private Map<String, IDemoTest> iDemoTestMap;

    @Override
    public List<Class<? extends Annotation>> newManageMembers() {
        ArrayList<Class<? extends Annotation>> classes = new ArrayList<>();
        classes.add(DemoComponent.class);
        classes.add(DemoTestComponent.class);
        return classes; // Returns a collection of annotations for custom injection containers
    }

    @Override
    public void manageNotify(List<?> beans, Class<? extends Annotation> clazz) {
        if (DemoComponent.class.isAssignableFrom(clazz)) { // Determines whether the bean is marked by the DemoComponent annotation
            // Beans marked with DemoComponent annotations are treated independently
        }
    }

    @Override
    public void start(String[] args) throws Exception {
        demo.test(); // Runs test method of the Demo
        demoAttach.test(); // Runs test method of the DemoAttach
        iDemoTestMap.forEach((k, v) -> v.test());
    }

    @Override
    public void close() throws Exception {
        // Runs before the IOC ends
    }

}
```

#### ***注入`bean`***

```java
   @Component("Demo")
   public class Demo implements IDemoTest {
   
      private final ILogger logger = LoggerHandler.getLogger(Demo.class);
   
      @Setup
      private TestDemoConfiguration testDemoConfiguration;
   
      @Override
      public void test() {
         logger.info("demo test successful");
         logger.info("desc: {}, lucky number: {}", testDemoConfiguration.getDesc(), testDemoConfiguration.getLuckyNumber());
      }
   
   
      @BeanInitial
      private void beanInitial() throws Exception {
         // The bean to do initial
         logger.info("demo start initialize");
         logger.info("desc: {}, lucky number: {}", testDemoConfiguration.getDesc(), testDemoConfiguration.getLuckyNumber());
      }
   
      @BeanEnable
      private void beanEnable() throws Exception {
         // The bean to do enable
         logger.info("demo start enable");
         logger.info("desc: {}, lucky number: {}", testDemoConfiguration.getDesc(), testDemoConfiguration.getLuckyNumber());
      }
   
   }
```

#### ***注入`bean`***

```java
   @Component("DemoAttach")
   public class DemoAttach implements IDemoTest {
   
       private final ILogger logger = LoggerHandler.getLogger(DemoAttach.class);
   
       @Override
       public void test() {
           logger.info("demoAttach test successful");
       }
   
   }
```

### 说明：

#### ***类***

|         类         |       说明       | 类型  |
|:-----------------:|:--------------:|:---:|
| `DemoApplication` |     程序启动类      | 普通类 |
|   `DemoStarter`   | `IStarter`接口实现 | 普通类 |  
|      `Demo`       |    注入`bean`    | 普通类 |
|   `DemoAttach`    |    注入`bean`    | 普通类 |

#### ***方法***

|                                  方法                                   |                  参数                  |     返回值     |  异常   |         说明          |
|:---------------------------------------------------------------------:|:------------------------------------:|:-----------:|:-----:|:-------------------:|
|               `Bootstrap.open(args, DemoStarter.class)`               |   1. 程序入口参数；2. 自定义接口`IStarter`实现类    |      无      |   无   |      `IOC`启动方法      |
|        `List<Class<? extends Annotation>> newManageMembers()`         |                  无                   | 不使用返回`null` |  处理   | 请阅览特性 **`IOC`仓库规划** |
| `void manageNotify(List<?> beans, Class<? extends Annotation> clazz)` | 1. 被自定义注入注解管理`bean`集合； 2. 自定义注入注解类对象 |      无      | 处理或抛出 | 请阅览特性 **`IOC`仓库规划** |
|             `void start(String[] args) throws Exception`              |              1. 程序入口参数               |      无      | 处理或抛出 |      开始执行用户程序       |
|                    `void close() throws Exception`                    |              `IOC`关闭回调               |      无      | 处理或抛出 |      `IOC`关闭回调      |
|                         `void beanInitial()`                          |                  无                   |      无      | 处理或抛出 |    [生命周期](#生命周期)    |
|                          `void beanEnable()`                          |                  无                   |      无      | 处理或抛出 |    [生命周期](#生命周期)    |

#### ***注解***

|                    注解                     |         描述          |       值说明       |
|:-----------------------------------------:|:-------------------:|:---------------:|
| `@ScanPackageConfig("scan.package.name")` | [扫描包匹配规则](#扫描包匹配规则) |      包扫描路径      |
|               `@Component`                |     `IOC`默认注入注解     |   `bean`注入名称    |
|                 `@Setup`                  | [`bean`装载](#bean装载) | 需要装载的`bean`注入名称 |
|              `@BeanInitial`               |    标记`bean`初始化方法    |        无        |
|               `@BeanEnable`               |    标记`bean`启动方法     |        无        |


### 关键点详解：

#### ***生命周期***

```mermaid
flowchart LR
 A(开始)
 B(文件扫描)
 C(获取需求beans)
 D(获取配置文件数据)
 E(装载bean对象)
 F(关联beans)
 G(配置bean赋值)
 H(bean初始化)
 Y(bean启动)
 J(IOC仓库规划)
 K(用户程序)
 C --> B
 A --> B
 D --> B
 B --> E
 E --> F
 F --> G
 G --> H
 H --> Y
 Y --> J
 J --> K
```

#### ***扫描包匹配规则***

* 注解`@ScanPackageConfig`必须在程序入口方法对应的类上。
* 注解`@ScanPackageConfig`的值必须至少包含被扫描包第一级目录。  
   如`scan.package.name`必须含有`scan`。
* `**`表示任意级任意字符目录；`*`表示一级任意字符目录。  
   如匹配`scan.package.name.bean`、`scan.package.name.config`、`scan.package.name.xxx`，  
   值可以设置为`scan.**`、`scan.**.name.*`、`scan.package.name.*`、`scan.*.name.*`

#### ***`bean`装载***

* 根据类装载：
```java
    @Setup 
    // 通过类装载
    // 注解值可以选择性赋值
    private DemoAttach attach;
```

* 根据接口或者抽象类装载：
```java
    @Setup("Demo")
    // 通过接口或者抽象类装载
    // 多个实现需要赋值注解值为其某个实现对象的注入名称
    private IDemoTest demo;
```

* 根据配置文件属性值装载：
```java
    @Setup("->test.demo.bean.name") 
    // 指定配置文件中test.demo.bean.name属性值为对象的注入名称
    private IDemoTest demoAttach;
```

* 根据映射机制装载：
```java
    @Setup 
    // 注解值不填
    // map容器映射装载，key为接口或者抽象类实现的类名称，value为实现对象
    private Map<String, IDemoTest> iDemoTestMap;
```

### 运行结果：
```text
_____.__                           __  _________    ___.             __                 __  
_/ ____\  |__   _____               |__|/   _____/__ _\_ |__   _______/  |_____________ _/  |_  ____  
\   __\|  |  \ /     \   ______     |  |\_____  \|  |  \ __ \ /  ___/\   __\_  __ \__  \\   __\/ __ \  
|  |  |   Y  \  Y Y  \ /_____/     |  |/        \  |  / \_\ \\___ \  |  |  |  | \// __ \|  | \  ___/  
|__|  |___|  /__|_|  /         /\__|  /_______  /____/|___  /____  > |__|  |__|  (____  /__|  \___  >  
\/      \/          \______|       \/          \/     \/                   \/          \/  
===============================================================================version 1.0.0 release==  
```
11:16:14.856 [main] INFO org.fhm.substrate.manager.Bootstrap - read VM parameter  
11:16:14.864 [main] INFO org.fhm.substrate.manager.Bootstrap - start collect configuration file and class file  
11:16:14.869 [main] INFO org.fhm.substrate.manager.Bootstrap - start initialize resource scanner  
11:16:14.870 [main] INFO org.fhm.substrate.service.ResourceScanner - start configure resource scanner  
11:16:14.889 [main] INFO org.fhm.substrate.manager.Bootstrap - start filter out the required CP  
11:16:14.889 [main] INFO org.fhm.substrate.manager.Bootstrap - start fixed-point scanning  
11:16:14.890 [main] INFO org.fhm.substrate.manager.Bootstrap - start scan the path to obtain the required resources and class files  
11:16:15.025 [main] INFO org.fhm.substrate.manager.Bootstrap - start clear cache and create beans  
11:16:15.033 [main] INFO org.fhm.substrate.manager.Bootstrap - start auto setup bean  
11:16:15.033 [main] INFO org.fhm.substrate.manager.Bootstrap - initial auto setup container  
11:16:15.033 [main] INFO org.fhm.substrate.manager.Bootstrap - auto setup obj  
11:16:15.046 [main] INFO org.fhm.substrate.manager.Bootstrap - auto setup map obj  
11:16:15.052 [main] INFO org.fhm.substrate.manager.Bootstrap - distribute bean  
11:16:15.054 [main] INFO org.fhm.substrate.manager.Bootstrap - start initial configuration  
11:16:15.063 [main] INFO org.fhm.substrate.manager.Bootstrap - start optimize bean  
11:16:15.063 [main] INFO org.fhm.substrate.manager.Bootstrap - clear not necessary implement and cache  
11:16:15.070 [main] INFO org.fhm.substrate.manager.Bootstrap - start bean initial  
11:16:15.073 [main] INFO org.fhm.substrate.bean.Demo - `demo start initialize` // bean初始化调用  
11:16:15.073 [main] INFO org.fhm.substrate.bean.Demo - `desc: hello,reality, lucky number: 66` // bean初始化调用获取配置文件信息  
11:16:15.076 [main] INFO org.fhm.substrate.manager.Bootstrap - start bean enable  
11:16:15.076 [main] INFO org.fhm.substrate.bean.Demo - `demo start enable` // bean启动调用  
11:16:15.076 [main] INFO org.fhm.substrate.bean.Demo - `desc: hello,reality, lucky number: 66` // bean启动调用获取配置文件信息  
11:16:15.076 [main] INFO org.fhm.substrate.manager.Bootstrap - clear cache data  
11:16:15.083 [main] INFO org.fhm.substrate.manager.Bootstrap - current the number of available processors : 16  
11:16:15.083 [main] INFO org.fhm.substrate.manager.Bootstrap - current maximum heap memory: 3890MB  
11:16:15.083 [main] INFO org.fhm.substrate.manager.Bootstrap - current cost memory: 2MB 945KB  
11:16:15.083 [main] INFO org.fhm.substrate.service.IOCCostTimer - enable project cost: 0s 232ms  
11:16:15.083 [main] INFO org.fhm.substrate.manager.Bootstrap - enable project complete  
11:16:15.085 [main] INFO org.fhm.substrate.bean.Demo - `demo test successful`  // 调用bean测试方法  以下为接口方式装载调用  
11:16:15.085 [main] INFO org.fhm.substrate.bean.Demo - `desc: hello,reality, lucky number: 66`  // 配置文件信息  
11:16:15.085 [main] INFO org.fhm.substrate.bean.DemoAttach - `demoAttach demo test successful`  // 调用bean测试方法  
11:16:15.085 [main] INFO org.fhm.substrate.bean.Demo - `demo test successful`  // 调用bean测试方法  以下为Map方式装载调用  
11:16:15.085 [main] INFO org.fhm.substrate.bean.Demo - `desc: hello,reality, lucky number: 66`  // 配置文件信息  
11:16:15.085 [main] INFO org.fhm.substrate.bean.DemoAttach - `demoAttach demo test successful`  // 调用bean测试方法  
