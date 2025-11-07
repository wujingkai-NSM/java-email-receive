package org.example.receiver.kit.component;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import lombok.Getter;
import org.example.receiver.kit.config.SessionProviderConfiguration;

import java.util.Properties;
import java.util.logging.Logger;

/**
 * 邮件会话提供者抽象类
 * 可以通过{@code PropertyKey}来获取邮件的已经配置的属性<kbd>键</kbd>，通过选择不同的方式来扩展邮件会话的Session对象
 * {@link jakarta.mail.Session}对象的相关说明，查阅对应的<a href="https://jakarta.ee/specifications/mail/2.1/jakarta-mail-spec-2.1#a823">官方文档</a>
 * <h2>覆写{@code getConnectionProperties}使得provide方法获取新的Properties</h2>
 * <pre>{@code
 * class YourClass extends MailSessionProvider {
 *     @Override
 *     protected Properties getConnectionProperties() {
 *     		Properties p = new Properties();
 *     		p.setProperty("xxx", "xxx");
 *         return p;
 *     }
 * }
 * }</pre>
 *
 * <h2>直接覆写{@code provide}方法提供新的Session</h2>
 * 密码可以通过{@code getPassword}方法获取
 * <pre>{@code
 * class YourClass extends MailSessionProvider {
 * 		@Override
 * 		public Session provide() {
 * 		 	return Session.getInstance(YourProperties, YourAuthenticator);
 * 		}
 * }
 *}</pre>
 * <h2>扩展原有的{@code properties}</h2>
 * 可以通过{@code setProperty}达到
 * <pre>{@code
 * class YourClass extends MailSessionProvider {
 * 		@Override
 * 		public Session provide() {
 * 			super.setProperty('xxx', 'xxx')
 * 		 	return super.provide();
 * 		}
 * }
 * }</pre>
 *
 * @author wujingkai
 * @date 2025/11/6 14:05
 */
@Getter
public abstract class MailSessionProvider implements Provider {
	public static final Logger logger = Logger.getLogger(MailSessionProvider.class.getName());
	private final String username;
	private final String password;
	private final String protocol;
	private final String host;
	private final Integer port;
	private final boolean sslEnabled;

	protected Properties defaultProperties = new Properties();

	/**
	 * 邮件会话属性
	 * @see #getDefaultProperties() 获取默认属性
	 * @see #defaultProperties 默认属性对象
	 */
	public static class PropertyKey{
		/**
		 * 邮件协议例如 {@code imap} {@code pop3} {@code smtp}
		 */
		public static final String Protocol = "mail.store.protocol";
		/**
		 * 邮件服务器地址
		 */
		public static final String Host = "mail.imap.host";
		/**
		 * 邮件服务器端口
		 */
		public static final String Port = "mail.imap.port";
		/**
		 * 是否使用 SSL
		 * 有一些服务器强制要求开启 SSL
		 */
		public static final String SSL_Enable = "mail.imap.ssl.enable";
		/**
		 * 是否使用 StartTLS
		 */
		public static final String StartTLS_Enable = "mail.imap.starttls.enable";
		/**
		 * 是否使用 NIO
		 * */
		public static final String Use_Socket_Channels = "mail.imap.usesocketchannels";
	}

	/**
	 * 创建邮件会话提供者实例
	 * @param username 用户名
	 * @param protocol 邮件协议，如imap、pop3等
	 * @param host 邮件服务器主机地址
	 * @param port 邮件服务器端口号
	 * @param sslEnabled 是否启用SSL连接
	 */
	public MailSessionProvider(String username, String password, String protocol, String host, Integer port, boolean sslEnabled) {
		this.username = username;
		this.password = password;
		this.protocol = protocol;
		this.host = host;
		this.port = port;
		this.sslEnabled = sslEnabled;
		createDefaultProperties();
	}

	public MailSessionProvider(SessionProviderConfiguration sessionProviderConfiguration) {
		this(
				sessionProviderConfiguration.getUsername(),
				sessionProviderConfiguration.getPassword(),
				sessionProviderConfiguration.getProtocol(),
				sessionProviderConfiguration.getHost(),
				sessionProviderConfiguration.getPort(),
				sessionProviderConfiguration.getSslEnabled()
		);
	}


	private void createDefaultProperties() {
		// https://jakarta.ee/specifications/mail/2.1/jakarta-mail-spec-2.1#a823
		defaultProperties.setProperty(PropertyKey.Protocol, this.protocol);
		defaultProperties.setProperty(PropertyKey.Host, this.host);
		defaultProperties.setProperty(PropertyKey.Port, String.valueOf(this.port));
		defaultProperties.setProperty(PropertyKey.SSL_Enable, String.valueOf(this.sslEnabled));
		defaultProperties.setProperty(PropertyKey.StartTLS_Enable, "false");
//		defaultProperties.setProperty("mail.imap.nio.enable", "true");
		defaultProperties.setProperty(PropertyKey.Use_Socket_Channels, "true");
	}

	@Override
	public Session provide() {
		Properties properties = getConnectionProperties();

		if (properties == null) {
			properties = defaultProperties;
		}

		return Session.getInstance(
				properties,
				new Authenticator() {
					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(username, password);
					}
				});
	}

	@Override
	public Properties getConnectionProperties() {
		return defaultProperties;
	}

	protected void setDefaultProperty(String key, String value) {
		defaultProperties.setProperty(key, value);
	}
}
