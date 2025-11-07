package org.example.receiver.kit.component;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;

import java.io.IOException;

/**
 * 邮件信息提取器定义提取邮件信息的方法
 * 邮件信息提取以后会通过这个MessageTake接口获取邮件信息
 * 从而进行关键词分析
 * @author wujingkai
 */
public interface MessageTake {
	/**
	 * 获取邮件信息
	 * @return 需要被分析的邮件信息
	 */
	String take() throws MessagingException, IOException;

	default String take(MessageProcesser messageProcesser) {
		try {
			return messageProcesser.getContent();
		} catch (MessagingException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 获取此次正在处理的邮件
	 * @return 邮件
	 */
	Message message();
}
