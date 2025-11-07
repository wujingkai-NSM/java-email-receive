package org.example.receiver.kit.config;

import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

@Data
@Builder
public class SessionProviderConfiguration {
	private String username;
	private String protocol;
	private String host;
	private Integer port;
	private Boolean sslEnabled;
	private String password;
}
