package mago.study.global.util;

import org.apache.commons.text.StringEscapeUtils;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * 트윗/소셜 텍스트 정제 유틸.
 * - HTML 엔티티 디코딩
 * - 흔한 인코딩 깨짐(모지바케) 보정
 * - 유니코드 NFC 정규화
 * - URL/해시태그/멘션/이모지·심볼 제거(옵션)
 * - 제로폭/사설영역/제어문자 정리
 * - 공백/개행 정리
 */
public class TextCleaner {

    /** http(s)://, www., example.com/path 같은 URL 전반 제거 */
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)\\b((?:https?://|www\\.)\\S+|(?:[a-z0-9-]+\\.)+[a-z]{2,}(?:/\\S*)?)"
    );

    /** 개행/탭을 제외한 제어문자 제거 */
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cc}&&[^\\n\\t]]");

    /** 제로폭/보이지 않는 공백류 제거 (ZWSP, ZWNJ, ZWJ, WJ, BOM 등) */
    private static final Pattern INVISIBLE_CHARS = Pattern.compile("[\\u200B\\u200C\\u200D\\u2060\\uFEFF]");

    /** 다중 공백/개행 정리 */
    private static final Pattern MULTI_SPACE = Pattern.compile("[ \\t\\x0B\\f\\r]+");
    private static final Pattern MULTI_NEWLINE = Pattern.compile("\\n{3,}");

    /** 해시태그/멘션 */
    private static final Pattern HASHTAG = Pattern.compile("(?<!\\w)#[\\p{L}0-9_]+");
    private static final Pattern MENTION = Pattern.compile("(?<!\\w)@[\\p{L}0-9_]+");

    /**
     * 광범위 심볼 제거용: \p{S} (수학/화폐/기타 심볼 전반) + \p{Cn}(할당되지 않은 코드 포인트)
     * 이모지 대부분이 \p{So}에 포함되며, 기타 특수기호도 함께 제거됨.
     * 필요 시 문장부호(\p{P})까지 삭제하는 옵션은 별도 요청 권장(의미 있는 구두점까지 사라질 수 있음).
     */
    private static final Pattern SYMBOLS = Pattern.compile("[\\p{S}\\p{Cn}]", Pattern.UNICODE_CHARACTER_CLASS);

    /**
     * 텍스트 정제.
     *
     * @param raw            원본 문자열
     * @param removeHashtag  해시태그 제거 여부
     * @param removeMention  멘션 제거 여부
     * @param removeEmoji    이모지/심볼(광범위 \p{S}) 제거 여부
     * @return 정제된 문자열(내용 없으면 null)
     */
    public static String clean(String raw,
                               boolean removeHashtag,
                               boolean removeMention,
                               boolean removeEmoji) {
        if (raw == null) return null;

        String s = raw;

        // 1) HTML 엔티티 → 문자 (예: &amp; → &, &#39; → ')
        s = StringEscapeUtils.unescapeHtml4(s);

        // 2) 흔한 인코딩 깨짐 보정
        s = fixMojibake(s);

        // 3) 유니코드 정규화 (NFC)
        s = Normalizer.normalize(s, Normalizer.Form.NFC);

        // 4) URL 제거
        s = URL_PATTERN.matcher(s).replaceAll("");

        // 5) 해시태그/멘션 제거(심볼 제거 전에 수행해야 @/# 인식 가능)
        if (removeHashtag) s = HASHTAG.matcher(s).replaceAll("");
        if (removeMention) s = MENTION.matcher(s).replaceAll("");

        // 6) 보이지 않는 제로폭 문자 제거
        s = INVISIBLE_CHARS.matcher(s).replaceAll("");

        // 7) 이모지/심볼 등 제거(옵션) — \p{S} 전반
        if (removeEmoji) s = SYMBOLS.matcher(s).replaceAll("");

        // 8) 제어문자 제거(개행/탭 제외)
        s = CONTROL_CHARS.matcher(s).replaceAll("");

        // 9) 라인 단위 트림 + 내부 다중 공백 축소
        String[] lines = s.split("\\R");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                trimmed = MULTI_SPACE.matcher(trimmed).replaceAll(" ");
                sb.append(trimmed).append("\n");
            }
        }
        s = sb.toString();

        // 10) 과도한 개행 축소(최대 2줄)
        s = MULTI_NEWLINE.matcher(s).replaceAll("\n\n");

        // 11) 전체 트림
        s = s.trim();

        return s.isEmpty() ? null : s;
    }

    /** 트위터/CSV에서 자주 보이는 모지바케(인코딩 깨짐) 보정 */
    private static String fixMojibake(String s) {
        return s
                // 대시/따옴표/말줄임표 계열
                .replace("‚Äì", "–")
                .replace("â€“", "–")
                .replace("â€”", "—")
                .replace("â€˜", "‘")
                .replace("â€™", "’")
                .replace("â€œ", "“")
                .replace("â€", "”")
                .replace("â€¦", "…")
                .replace("â„¢", "™")

                // 라틴 확장 흔한 케이스(필요 시 추가)
                .replace("Ã¡", "á")
                .replace("Ã©", "é")
                .replace("Ã­", "í")
                .replace("Ã³", "ó")
                .replace("Ãº", "ú")
                .replace("Ã±", "ñ")

                // NBSP → 일반 공백
                .replace("\u00A0", " ");
    }
}