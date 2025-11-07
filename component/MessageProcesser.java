package org.example.receiver.kit.component;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import org.eclipse.angus.mail.imap.IMAPMessage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * 邮件信息提取器：封装 Jakarta Mail Message 的信息提取逻辑
 */
public record MessageProcesser(Message message) {
	// 构造器注入 Message 对象

	public static String stripHTML(String html) {
		if (html == null || html.isEmpty()) {
			return "";
		}
		String pureText = Jsoup.clean(html, Safelist.none());
		return Jsoup.parse(pureText).text();
	}

	public static boolean isHTML(String content) {
		if (content == null || content.trim().isEmpty()) {
			return false;
		}
		// 这个解析+IO可能造成一定的瓶颈
		// Jsoup 解析字符串（自动补全不规范标签）
		Document doc = Jsoup.parse(content);

		// 遍历所有节点，判断是否存在非文本/注释节点（即 HTML 标签）
		for (Element element : doc.getAllElements()) {
			String tagName = element.tagName();
			if (!"#text".equals(tagName) && !"#comment".equals(tagName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 获取发件人列表（格式：姓名 <邮箱> 或 纯邮箱）
	 */
	public List<String> getSenders() throws MessagingException {
		Address[] fromAddresses = message.getFrom();
		if (fromAddresses == null || fromAddresses.length == 0) {
			return List.of("未知发件人");
		}
		return Arrays.stream(fromAddresses)
				.map(this::formatAddress)
				.collect(Collectors.toList());
	}

	/**
	 * 获取主要收件人（TO）列表
	 */
	public List<String> getToRecipients() throws MessagingException {
		return getRecipients(Message.RecipientType.TO);
	}

	/**
	 * 获取抄送（CC）列表
	 */
	public List<String> getCcRecipients() throws MessagingException {
		return getRecipients(Message.RecipientType.CC);
	}

	/**
	 * 获取密送（BCC）列表
	 */
	public List<String> getBccRecipients() throws MessagingException {
		return getRecipients(Message.RecipientType.BCC);
	}

	/**
	 * 获取邮件主题（无主题返回"无主题"）
	 */
	public String getSubject() throws MessagingException {
		String subject = message.getSubject();
		return subject != null ? subject : "无主题";
	}

	/**
	 * 获取发送日期（无日期返回 null）
	 */
	public Date getSentDate() throws MessagingException {
		return message.getSentDate();
	}

	/**
	 * 获取发送日期（转为本地时区，无日期返回"未知日期"）
	 */
	public String getLocalSentDate() throws MessagingException {
		Date sentDate = message.getSentDate();
		if (sentDate == null) {
			return "未知日期";
		}
		// 转换为本地时区
		Date localDate = new Date(sentDate.getTime() + TimeZone.getDefault().getRawOffset());
		return localDate.toString();
	}

	/**
	 * 获取附件信息列表（包含文件名和大小）
	 */
	public List<AttachmentInfo> getAttachments() throws MessagingException, IOException {
		if (!message.isMimeType("multipart/*")) {
			return List.of(); // 无附件返回空列表
		}

		MimeMultipart multipart = (MimeMultipart) message.getContent();
		return Arrays.stream(new int[multipart.getCount()])
				.mapToObj(i -> {
					try {
						MimeBodyPart bodyPart = (MimeBodyPart) multipart.getBodyPart(i);
						// 只筛选附件（排除内嵌资源如图片）
						if (MimeBodyPart.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
							String fileName = bodyPart.getFileName();
							long fileSize = bodyPart.getSize();
							return new AttachmentInfo(fileName, fileSize);
						}
						return null;
					} catch (MessagingException e) {
						throw new RuntimeException("提取附件信息失败", e);
					}
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	/**
	 * 获取邮件正文（兼容纯文本/HTML/多部分格式）
	 */
	public String getContent() throws MessagingException, IOException {
		Object content = message.getContent();
		if (content instanceof String c) {
			return MessageProcesser.isHTML(c) ? MessageProcesser.stripHTML(c) : c;
		} else if (content instanceof MimeMultipart) {
			return extractTextFromMultipart((MimeMultipart) content);
		} else {
			return "不支持的正文格式：" + content.getClass().getSimpleName();
		}
	}

	/**
	 * 预览邮件正文（截取前200字符，避免过长）
	 */
	public String getContentPreview() throws MessagingException, IOException {
		String content = getContent();
		return content.length() > 200 ? content.substring(0, 200) + "..." : content;
	}

	/**
	 * 打印完整邮件信息（用于调试/日志输出）
	 */
	public void printFullMailInfo() throws MessagingException, IOException {
		System.out.println("=====================================");
		System.out.println("【邮件核心信息】");
		System.out.println("发件人：" + String.join(", ", getSenders()));
		System.out.println("收件人（TO）：" + String.join(", ", getToRecipients()));

		List<String> ccList = getCcRecipients();
		if (!ccList.isEmpty() && !"无".equals(ccList.get(0))) {
			System.out.println("抄送（CC）：" + String.join(", ", ccList));
		}

		List<String> bccList = getBccRecipients();
		if (!bccList.isEmpty() && !"无".equals(bccList.get(0))) {
			System.out.println("密送（BCC）：" + String.join(", ", bccList));
		}

		System.out.println("主题：" + getSubject());
		System.out.println("发送日期（本地）：" + getLocalSentDate());

		List<AttachmentInfo> attachments = getAttachments();
		System.out.println("附件：");
		if (attachments.isEmpty()) {
			System.out.println("  - 无附件");
		} else {
			attachments.forEach(attach ->
					System.out.printf("  - 文件名：%s（大小：%s）%n",
							attach.getFileName(), formatFileSize(attach.getFileSize()))
			);
		}

		System.out.println("正文预览：" + getContentPreview());
		System.out.println("=====================================\n");
	}

	// ------------------------------ 私有工具方法（内部复用）------------------------------

	/**
	 * 格式化单个地址（姓名 <邮箱> 或 纯邮箱）
	 */
	private String formatAddress(Address address) {
		InternetAddress internetAddress = (InternetAddress) address;
		String personal = internetAddress.getPersonal();
		return personal != null ? personal + " <" + internetAddress.getAddress() + ">" : internetAddress.getAddress();
	}

	/**
	 * 通用获取收件人列表（支持 TO/CC/BCC）
	 */
	private List<String> getRecipients(Message.RecipientType type) throws MessagingException {
		Address[] addresses = message.getRecipients(type);
		if (addresses == null || addresses.length == 0) {
			return List.of("无");
		}
		return Arrays.stream(addresses)
				.map(this::formatAddress)
				.collect(Collectors.toList());
	}

	/**
	 * 从多部分内容中提取文本正文（优先纯文本，再 HTML）
	 */
	private String extractTextFromMultipart(MimeMultipart multipart) throws MessagingException, IOException {
		for (int i = 0; i < multipart.getCount(); i++) {
			MimeBodyPart bodyPart = (MimeBodyPart) multipart.getBodyPart(i);
			if (bodyPart.isMimeType("text/plain")) {
				return bodyPart.getContent().toString();
			} else if (bodyPart.isMimeType("text/html")) {
				return "[HTML格式] " + MessageProcesser.stripHTML(bodyPart.getContent().toString());
			}
		}
		return "无可用文本正文";
	}

	/**
	 * 格式化文件大小（B → KB/MB）
	 */
	private String formatFileSize(long bytes) {
		if (bytes < 1024) {
			return bytes + " B";
		} else if (bytes < 1024 * 1024) {
			return String.format("%.1f KB", bytes / 1024.0);
		} else {
			return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
		}
	}

	public String getMessageID() {
		if (message instanceof IMAPMessage imapMessage) {
			try {
				return imapMessage.getMessageID();
			} catch (MessagingException e) {
				return "__SIMPLE_ID";
			}
		}
		return "__SIMPLE_ID";
	}

	/**
	 * 附件信息实体（存储文件名和大小）
	 */
	public static class AttachmentInfo {
		private final String fileName;
		private final long fileSize; // 单位：字节

		public AttachmentInfo(String fileName, long fileSize) {
			this.fileName = fileName;
			this.fileSize = fileSize;
		}

		// getter 方法
		public String getFileName() {
			return fileName;
		}

		public long getFileSize() {
			return fileSize;
		}

		public String getFormattedFileSize() {
			return new MessageProcesser(null).formatFileSize(fileSize); // 复用格式化方法
		}
	}
}