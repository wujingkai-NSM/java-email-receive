package org.example.receiver.kit.component;

import jakarta.mail.MessagingException;
import lombok.Getter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageKeywordRefiner {
	/**
	 * 关键词分类枚举（统一管理分类，避免中文硬编码冗余）
	 */
	@Getter
	public enum KeywordCategory {
		/**
		 * 设施维修/维护关闭提醒
		 */
		FACILITY_MAINTENANCE_CLOSED_REMINDER("设施维修/维护关闭提醒"),
		/**
		 * 前台非24小时/无前台
		 */
		FRONT_DESK_NOT_OPEN_24_HOURS("前台非24小时/无前台"),
		/**
		 * 办理入住地点与物业所在地不同
		 */
		CHECK_IN_LOCATION_DIFFERS_FROM_PROPERTY_LOCATION("办理入住地点与物业所在地不同"),
		/**
		 * 办理入住前需要有密码
		 */
		CHECK_IN_NEED_PASSWORD("办理入住前需要有密码"),
		/**
		 * 不能安排预订，可取消或重新安置
		 */
		CANNOT_HONOR_BOOKING("不能安排预订，可取消或重新安置"),
		/**
		 * 受制裁个人/实体
		 */
		SANCTIONED_INDIVIDUAL_OR_ENTITY("受制裁个人/实体");

		// 获取中文描述的getter方法
		// 枚举对应的中文描述（用于直接获取含义，无需重复写中文硬编码）
		private final String chineseDesc;

		KeywordCategory(String chineseDesc) {
			this.chineseDesc = chineseDesc;
		}

	}

	/**
	 * 需要进行提炼的关键词（英文关键词 → 中文含义映射）
	 * 说明：英文关键词兼顾精准匹配和核心语义覆盖，中文含义直接关联枚举，保证一致性
	 */
	public static final List<Map.Entry<String, KeywordCategory>> needToBeRefineKeywords = Arrays.asList(
			// 1. 设施维修/维护关闭提醒相关（覆盖关闭原因、状态描述）
			Map.entry("closed due", KeywordCategory.FACILITY_MAINTENANCE_CLOSED_REMINDER),
			Map.entry("closed for", KeywordCategory.FACILITY_MAINTENANCE_CLOSED_REMINDER),
			Map.entry("maintenance", KeywordCategory.FACILITY_MAINTENANCE_CLOSED_REMINDER),
			Map.entry("renovation", KeywordCategory.FACILITY_MAINTENANCE_CLOSED_REMINDER),
			Map.entry("renovations", KeywordCategory.FACILITY_MAINTENANCE_CLOSED_REMINDER),
			Map.entry("refurbishment", KeywordCategory.FACILITY_MAINTENANCE_CLOSED_REMINDER),
			Map.entry("temporarily closed", KeywordCategory.FACILITY_MAINTENANCE_CLOSED_REMINDER),
			Map.entry("permanently closed", KeywordCategory.FACILITY_MAINTENANCE_CLOSED_REMINDER),
			Map.entry("currently closed", KeywordCategory.FACILITY_MAINTENANCE_CLOSED_REMINDER),

			// 2. 前台非24小时/无前台相关（覆盖营业时间、入住方式、密码获取）
			Map.entry("front desk is not open 24 hours", KeywordCategory.FRONT_DESK_NOT_OPEN_24_HOURS),
			Map.entry("Front Desk has limited hours", KeywordCategory.FRONT_DESK_NOT_OPEN_24_HOURS),
			Map.entry("no after-hours check-in", KeywordCategory.FRONT_DESK_NOT_OPEN_24_HOURS),
			Map.entry("no front desk", KeywordCategory.FRONT_DESK_NOT_OPEN_24_HOURS),
			Map.entry("receive an access code", KeywordCategory.FRONT_DESK_NOT_OPEN_24_HOURS),
			Map.entry("access code", KeywordCategory.FRONT_DESK_NOT_OPEN_24_HOURS), // 补充核心关键词

			// 3. 办理入住地点与物业所在地不同（完整短语精准匹配）
			Map.entry("check-in location differs from the property location", KeywordCategory.CHECK_IN_LOCATION_DIFFERS_FROM_PROPERTY_LOCATION),
			Map.entry("check-in location is not the property location", KeywordCategory.CHECK_IN_LOCATION_DIFFERS_FROM_PROPERTY_LOCATION), // 补充同义表达

			// 4. 办理入住前需要有密码（核心关键词覆盖）
			Map.entry("door code", KeywordCategory.CHECK_IN_NEED_PASSWORD),
			Map.entry("entry code", KeywordCategory.CHECK_IN_NEED_PASSWORD), // 补充同义关键词
			Map.entry("check-in code", KeywordCategory.CHECK_IN_NEED_PASSWORD), // 补充场景化关键词

			// 5. 不能安排预订，可取消或重新安置（修复语法错误，补充核心短语）
			Map.entry("Can't honor your booking", KeywordCategory.CANNOT_HONOR_BOOKING),
			Map.entry("cannot honor booking", KeywordCategory.CANNOT_HONOR_BOOKING), // 补充小写版本
			Map.entry("reschedule your reservation", KeywordCategory.CANNOT_HONOR_BOOKING),
			Map.entry("relocate your reservation", KeywordCategory.CANNOT_HONOR_BOOKING), // 修复原语法错误（relocation→relocate）
			Map.entry("cancel your reservation", KeywordCategory.CANNOT_HONOR_BOOKING),
			Map.entry("reservation cancellation", KeywordCategory.CANNOT_HONOR_BOOKING), // 补充名词形式

			// 6. 受制裁个人/实体（优化分类名称，覆盖完整语义）
			Map.entry("sanctioned individual", KeywordCategory.SANCTIONED_INDIVIDUAL_OR_ENTITY),
			Map.entry("sanctioned entity", KeywordCategory.SANCTIONED_INDIVIDUAL_OR_ENTITY),
			Map.entry("possible match to a sanctioned", KeywordCategory.SANCTIONED_INDIVIDUAL_OR_ENTITY),
			Map.entry("sanctioned person", KeywordCategory.SANCTIONED_INDIVIDUAL_OR_ENTITY) // 补充同义表达
	);


	public static Set<MessageKeywordRefiner.KeywordCategory> refine(MessageTake messageTake) {
		if (messageTake == null) {
			return Collections.emptySet();
		}

		String content = safelyTake(messageTake);
		Set<MessageKeywordRefiner.KeywordCategory> bucket = new HashSet<>();
		for (Map.Entry<String, KeywordCategory> entry : needToBeRefineKeywords) {
			Pattern pattern = Pattern.compile(entry.getKey(), Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(content);

			if (matcher.find()) {
				bucket.add(entry.getValue());
			}
		}
		return bucket;
	}

	public static String safelyTake(MessageTake messageTake) {
		try {
			return messageTake.take();
		} catch (MessagingException | IOException e) {
			return "";
		}
	}
}
