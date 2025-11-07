package org.example.receiver.kit.config;

import org.example.receiver.kit.FeishuMailSessionProvider;
import org.example.receiver.kit.GoogleMailSessionProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfiguration {
	@Bean(name = "wujingkai-mail-client")
	public FeishuMailSessionProvider feishuMailSessionProvider(@Qualifier(value = "wujingkai-feishuConfig") SessionProviderConfiguration feishuConfig) {
		return new FeishuMailSessionProvider(feishuConfig);
	}

	@Bean(name = "wujingkai-feishuConfig")
	public SessionProviderConfiguration feishuConfig() {
		return SessionProviderConfiguration.builder()
				.host("imap.feishu.cn")
				.port(993)
				.protocol("imap")
				.sslEnabled(true)
				.build();
	}


	@Bean(name = "lele-mail-client")
	public FeishuMailSessionProvider leleFeishuMailSessionProvider(@Qualifier(value = "lele-feishuConfig") SessionProviderConfiguration feishuConfig) {
		return new FeishuMailSessionProvider(feishuConfig);
	}

	@Bean(name = "lele-feishuConfig")
	public SessionProviderConfiguration leleFeishuConfig() {
		return SessionProviderConfiguration.builder()
				.host("imap.feishu.cn")
				.port(993)
				.protocol("imap")
				.sslEnabled(true)
				.build();
	}

	@Bean("wujingkai-gmail-client")
	public GoogleMailSessionProvider googleMailSessionProvider(SessionProviderConfiguration gmailConfig) {
		return new GoogleMailSessionProvider(gmailConfig);
	}

	@Bean(name = "gmailConfig")
	public SessionProviderConfiguration gmailConfig() {
		return SessionProviderConfiguration.builder()
				.host("imap.gmail.com")
				.port(993)
				.protocol("imap")
				.sslEnabled(true)
				.build();
	}

}
