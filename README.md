# MailSessionProvider Abstract Class Documentation


## Core Class Positioning



An abstract class for mail session provider, which provides standardized abstraction capabilities for creating Jakarta Mail sessions (`jakarta.mail.Session`). It supports obtaining configured mail properties through predefined property keys (`PropertyKey`) and provides multiple flexible extension methods for customizing the construction logic of `Session` objects.



For detailed specifications and usage instructions of `jakarta.mail.Session`, please refer to the official documentation:  



[Jakarta Mail 2.1 Specification - Session](https://jakarta.ee/specifications/mail/2.1/jakarta-mail-spec-2.1#a823)



## Core Extension Methods



Subclasses can customize the Session construction logic through the following 3 methods, and select the appropriate scheme according to actual needs:



### 1. Override `getConnectionProperties()` to Provide Custom Properties



By constructing a new `Properties` object, define the configuration parameters required for mail connection (such as SMTP server, port, encryption method, etc.). The parent class's `provide()` method will automatically use this property set to initialize the Session.



```Java
class CustomMailSessionProvider extends MailSessionProvider {
    @Override
    protected Properties getConnectionProperties() {
        Properties properties = new Properties();
        // 示例：配置 SMTP 服务器地址与端口
        properties.setProperty("mail.smtp.host", "smtp.example.com");
        properties.setProperty("mail.smtp.port", "587");
        // 其他自定义配置（如启用 TLS 加密、设置超时时间）
        properties.setProperty("mail.smtp.starttls.enable", "true");
        properties.setProperty("mail.smtp.connectiontimeout", "30000");
        return properties;
    }
}
```



### 2. 直接覆写 `provide()` 完全自定义 Session



适用于复杂场景（如自定义认证逻辑、多属性集组合等），可通过 `getPassword()` 方法获取预配置的密码。



```Java
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;

class FullCustomMailSessionProvider extends MailSessionProvider {
    @Override
    public Session provide() {
        // 构建自定义属性集
        Properties customProps = new Properties();
        customProps.setProperty("mail.smtp.auth", "true");
        customProps.setProperty("mail.smtp.starttls.enable", "true");

        // 自定义认证器（使用父类提供的密码获取方法）
        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                    getUsername(), // 假设父类提供用户名获取方法（可根据实际补充）
                    getPassword()  // 从父类获取预配置密码
                );
            }
        };

        // 直接返回自定义构建的 Session
        return Session.getInstance(customProps, authenticator);
    }
}
```



### 3. 扩展父类原有属性集（基于 `setProperty(String, String)`）



在父类默认属性的基础上追加或覆盖配置，无需完全重写属性集，适用于简单扩展场景。



```Java
class ExtendedMailSessionProvider extends MailSessionProvider {
    @Override
    public Session provide() {
        // 覆盖或追加单个属性（父类原有属性仍保留）
        super.setProperty("mail.smtp.connectiontimeout", "30000"); // 连接超时 30 秒
        super.setProperty("mail.smtp.timeout", "60000");          // 读取超时 60 秒
        super.setProperty("mail.smtp.writetimeout", "60000");     // 写入超时 60 秒

        // 调用父类 provide() 方法，基于扩展后的属性构建 Session
        return super.provide();
    }
}
```



## 关键说明



- **属性获取**：可通过 `PropertyKey` 枚举类获取邮件配置的标准属性键，避免硬编码错误。

- **密码安全**：密码通过 `getPassword()` 方法统一获取，父类已处理密码的存储与访问逻辑，子类直接调用即可。

- **兼容性**：基于 Jakarta Mail 2.1 规范开发，需确保项目依赖中引入对应版本的 Jakarta Mail API。

