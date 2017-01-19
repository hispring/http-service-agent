# http-service-agent
http-service-agent 是一个 Java 工具库，被设计用于简化对 HTTP Restful API 的调用。

一般地，访问一个发布的 http 服务的可选方法有：

1. 直接使用 JDK 的 URL 类建立 http 连接进行调用。
2. 直接使用 apache 的 http-client 建立 http 连接进行调用。
3. 使用 spring 的 rest template 库进行调用。

这几种方法对开发者而言，都有一些不如人意的地方：

- 需要拼接要访问的目标地址 URL；
- 在发起请求和处理回复时，开发者需要自行处理 java 数据结构与 http 传输的字符内容的转换，这里包括了典型的对象-JSON转换，原生数据类型转为 http 参数或者表单字段，等等；

于是，调用一个 http 服务这样一个语义上很简单的请求-回复的过程，却需要编写很多的转换处理的代码。

如果有一种简单的方法，简单到就像 spring mvc 的 controller 声明一个 restful 接口一样，用几个 Annotation 标注一下即可，那必能很招人喜欢。

http-service-agent 就是为了达到这样的效果而诞生的。

下面展示 http-service-agent 的一个示例：

1. 服务器定义了如下的 controller
        @Controller
        public class HelloController {

            @RequestMapping(method = RequestMethod.GET, path = "/hello/{name}")
            public @ResponseBody String hello(@PathVariable("name") String name) {
                return "hello " + name + "!";
            }

        }

2. 使用 http-service-client 访问该服务是很简单的，首先为这个 Controller 抽象一个访问接口 HelloService
        @HttpService
        public interface HelloService {

            @HttpAction(method = HttpMethod.GET, path="/hello/{name}")
            public String hello(@PathParam(name="name") String name);

        }

3. 建立连接进行调用，获取返回结果；
        public static void main(String[] args) {
            ServiceAddress host = new ServiceAddress("10.1.1.10", 80, false);
            HelloService helloService = HttpServiceAgent.createService(HelloService.class, host);
            String responseText = helloService.hello("John");
            System.out.println(responseText);
        }

So easy!
