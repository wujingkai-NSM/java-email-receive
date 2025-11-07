package org.example.receiver.kit.component;

import jakarta.mail.Session;

import java.util.Properties;

/**
 * 邮件会话提供者接口，不同的邮件服务提供者实现此接口，提供邮件会话对象
 * @author wujingkai
 */
public interface Provider {
	/**
	 * 获取邮件会话对象
	 * 不同的邮件服务器提供者实现此接口，提供邮件会话对象
	 * <pre>{@code
	 * 	Session session = Session.getInstance(properties, new Authenticator() {})
	 * }
	 * </pre>
	 * @return 邮件会话对象
	 * @see Session 邮件会话对象，相查阅对应的<a href="https://jakarta.ee/specifications/mail/2.1/jakarta-mail-spec-2.1#a823">官方文档</a>
	 */
	Session provide();

	/**
	 * 获取当前链接配置
	 * @implNote 如果修改这个配置响应的链接也会因此而改变
	 * @return 邮件链接配置
	 */
	Properties getConnectionProperties();
}
