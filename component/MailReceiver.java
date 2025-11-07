package org.example.receiver.kit.component;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.event.MessageCountAdapter;
import jakarta.mail.event.MessageCountEvent;
import lombok.NonNull;
import org.eclipse.angus.mail.imap.IMAPFolder;
import org.eclipse.angus.mail.imap.IdleManager;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class MailReceiver implements Receiver {
	public static final Logger logger = Logger.getLogger(MailReceiver.class.getName());
	public static final String INBOX = "INBOX";
	public ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = Executors.newScheduledThreadPool(1);

	protected static class CustomMessageCountAdapter extends MessageCountAdapter {
		private final IdleManager idleManager;
		private final Folder inbox;
		private final Consumer<Message[]> messageConsumer;

		public CustomMessageCountAdapter(IdleManager idleManager, Folder inbox, Consumer<Message[]> messageConsumer) {
			this.idleManager = idleManager;
			this.inbox = inbox;
			this.messageConsumer = messageConsumer;
		}

		@Override
		public void messagesAdded(MessageCountEvent e) {
			Message[] messages = e.getMessages();
			logger.info("收到" + messages.length + "封邮件");
			messageConsumer.accept(messages);

			moveOnWatch();
		}

		private void moveOnWatch() {
			try {
				idleManager.watch(inbox);
			} catch (MessagingException ex) {
				throw new RuntimeException(ex);
			}
		}

		@Override
		public void messagesRemoved(MessageCountEvent e) {
//			if (e.isRemoved()) {
//				for (Message message : e.getMessages()) {
//					Folder folder = message.getFolder();
//					try {
//						folder.expunge();
//					} catch (MessagingException ex) {
//						throw new RuntimeException(ex);
//					}
//				}
//			}
			logger.info("删除" + e.getMessages().length + "封邮件");
			messageConsumer.accept(e.getMessages());

			moveOnWatch();
		}
	}

	public record MailProcessCleaner(Store mailStore, Folder inbox, ExecutorService es) implements Closeable {
		public static final Logger logger = Logger.getLogger(MailReceiver.class.getName());

		@Override
		public void close() {
			try {
				if (inbox.isOpen()) {
					inbox.close();
					logger.info("关闭邮箱");
				}
				if (mailStore.isConnected()) {
					mailStore.close();
					logger.info("断开连接");
				}
				es.shutdown();
				logger.info("关闭线程池");
			} catch (MessagingException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public MailProcessCleaner receive(Consumer<Message[]> messageConsumer, @NonNull MailSessionProvider sessionProvider) throws MessagingException {
		Session session = sessionProvider.provide();
		ExecutorService es = Executors.newCachedThreadPool();
		try {
			IdleManager idleManager = new IdleManager(session, es);

			Store mailStore = session.getStore();
			mailStore.connect();
			Folder inbox = mailStore.getFolder(INBOX);
			inbox.open(Folder.READ_ONLY);

			// 添加邮件监听
			inbox.addMessageCountListener(getMessageCountAdapter(idleManager, inbox, messageConsumer));

			// 监听一次邮件接收
			idleManager.watch(inbox);

			SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(
					() -> keepAlive(mailStore, inbox, idleManager), 0, 5, TimeUnit.SECONDS);


			// TODO: 根据业务决定要不要获取全量的邮箱数据
			// messageConsumer.accept(inbox.getMessages());
			return new MailProcessCleaner(mailStore, inbox, es);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void keepAlive(Store mailStore, Folder inbox, IdleManager idleManager) {
		MailReceiver mailReceiver = this;
		try {
			((IMAPFolder) inbox).doCommand((cmd) -> {
				cmd.simpleCommand("noop", null);
				logger.info("发送noop消息, " + mailReceiver.getClass().getName());
				return null;
			});
		} catch (MessagingException e) {
			logger.warning("noop失败, 可能遇到连接断开情况，尝试重新连接" + mailReceiver.getClass().getName());
			if (retryConnect(mailStore, inbox)) {
				try {
					idleManager.watch(inbox);
				} catch (MessagingException ex) {
					throw new RuntimeException(ex);
				}
			}

		}
	}

	private boolean retryConnect(Store mailStore, Folder inbox) {
		try {
			mailStore.connect();
			inbox.open(Folder.READ_ONLY);
			return true;
		} catch (MessagingException e) {
			logger.warning("遇到错误 " + e.getMessage() + "重新连接失败, 尝试重新获取邮件" + this.getClass().getName());
			return false;
		}
	}

	protected MessageCountAdapter getMessageCountAdapter(IdleManager idleManager, Folder inbox, Consumer<Message[]> messageConsumer) {
		return new CustomMessageCountAdapter(idleManager, inbox, messageConsumer);
	}

}
