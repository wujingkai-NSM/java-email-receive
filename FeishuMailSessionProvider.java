package org.example.receiver.kit;

import org.example.receiver.kit.component.MailSessionProvider;
import org.example.receiver.kit.config.MailConfiguration;
import org.example.receiver.kit.config.SessionProviderConfiguration;

public class FeishuMailSessionProvider extends MailSessionProvider {


	/**
	 * 创建邮件会话提供者实例
	 *
	 * @param username   用户名
	 * @param password
	 * @param protocol   邮件协议，如imap、pop3等
	 * @param host       邮件服务器主机地址
	 * @param port       邮件服务器端口号
	 * @param sslEnabled 是否启用SSL连接
	 */
	public FeishuMailSessionProvider(String username, String password, String protocol, String host, Integer port, boolean sslEnabled) {
		super(username, password, protocol, host, port, sslEnabled);
	}

	public FeishuMailSessionProvider(SessionProviderConfiguration configuration) {
		super(
				configuration.getUsername(),
				configuration.getPassword(),
				configuration.getProtocol(),
				configuration.getHost(),
				configuration.getPort(),
				configuration.getSslEnabled()
		);
	}
}
