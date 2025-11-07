# MailSessionProvider

# MailSessionProvider Abstract Class Documentation



## Core Class Positioning



An abstract class for mail session providers, which offers standardized abstraction capabilities for creating Jakarta Mail sessions (`jakarta.mail.Session`). It supports retrieving configured mail properties via predefined property keys (`PropertyKey`) and provides multiple flexible extension methods for customizing the construction logic of `Session` objects.



For detailed specifications and usage instructions of `jakarta.mail.Session`, please refer to the official documentation:  



[Jakarta Mail 2.1 Specification - Session](https://jakarta.ee/specifications/mail/2.1/jakarta-mail-spec-2.1#a823)  



## Core Extension Methods



Subclasses can customize the Session construction logic through the following 3 methods and choose the appropriate scheme based on actual needs:  



### 1. Override `getConnectionProperties()` to Provide Custom Properties



By constructing a new `Properties` object, define the configuration parameters required for the mail connection (such as SMTP server, port, encryption method, etc.). The parent class's `provide()` method will automatically use this set of properties to initialize the Session.  



```Java
class CustomMailSessionProvider extends MailSessionProvider {
    @Override
    protected Properties getConnectionProperties() {
        Properties properties = new Properties();
        // Example: Configure SMTP server address and port
        properties.setProperty("mail.smtp.host", "smtp.example.com");
        properties.setProperty("mail.smtp.port", "587");
        // Other custom configurations (e.g., enabling TLS encryption, setting timeout)
        properties.setProperty("mail.smtp.starttls.enable", "true");
        properties.setProperty("mail.smtp.connectiontimeout", "30000");
        return properties;
    }
}

```



### 2. Directly Override `provide()` for Full Customization of Session



Suitable for complex scenarios (such as custom authentication logic, combination of multiple property sets, etc.). The pre-configured password can be obtained via the `getPassword()` method.  



```Java
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;

class FullCustomMailSessionProvider extends MailSessionProvider {
    @Override
    public Session provide() {
        // Construct custom property set
        Properties customProps = new Properties();
        customProps.setProperty("mail.smtp.auth", "true");
        customProps.setProperty("mail.smtp.starttls.enable", "true");

        // Custom authenticator (using the password retrieval method provided by the parent class)
        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                    getUsername(), // Assume the parent class provides a username retrieval method (supplement as needed)
                    getPassword()  // Retrieve pre-configured password from the parent class
                );
            }
        };

        // Directly return the custom-built Session
        return Session.getInstance(customProps, authenticator);
    }
}

```



### 3. Extend the Parent Class's Existing Property Set (Based on `setProperty(String, String)`)



Append or override configurations on top of the parent class's default properties without completely rewriting the property set. Suitable for simple extension scenarios.  



```Java
class ExtendedMailSessionProvider extends MailSessionProvider {
    @Override
    public Session provide() {
        // Override or append individual properties (existing properties of the parent class are retained)
        super.setProperty("mail.smtp.connectiontimeout", "30000"); // Connection timeout: 30 seconds
        super.setProperty("mail.smtp.timeout", "60000");          // Read timeout: 60 seconds
        super.setProperty("mail.smtp.writetimeout", "60000");     // Write timeout: 60 seconds

        // Call the parent class's provide() method to build the Session based on the extended properties
        return super.provide();
    }
}

```



## Key Notes



- **Property Retrieval**: Standard property keys for mail configurations can be obtained through the `PropertyKey` enumeration class to avoid hardcoding errors.  

- **Password Security**: Passwords are uniformly retrieved via the `getPassword()` method. The parent class has handled the password storage and access logic, so subclasses can directly call this method.  

- **Compatibility**: Developed based on the Jakarta Mail 2.1 specification. Ensure that the corresponding version of the Jakarta Mail API is included in the project dependencies.
