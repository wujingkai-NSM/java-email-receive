package org.example.receiver.kit.component;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import lombok.NonNull;

import java.io.Closeable;
import java.util.function.Consumer;

public interface Receiver {
	/**
	 * 接收邮件，接受一个消费者，消费邮件
	 * 消费者会在有新邮件的时候一直消费他，直到调用{@code close}方法 或者说Java线程被关闭
	 *
	 * @param messageConsumer 消费者
	 * @param sessionProvider 会话提供者
	 * @throws MessagingException 邮件接受异常，或者是邮件服务器不支持某一种操作, {@code getContent}/{@code getContentType}等
	 */
	Closeable receive(Consumer<Message[]> messageConsumer, @NonNull MailSessionProvider sessionProvider) throws MessagingException;
}
