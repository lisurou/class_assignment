package org.example.classAssignment.pojo;

import java.util.Set;

public final class MaterialCategory {
    public static final String STUDY = "学习资料";
    public static final String MOOC = "慕课资料";
    public static final String RECORDING = "录屏录像";
    public static final String LIVE_RECORDING = "直播录像";

    private static final Set<String> ALLOWED = Set.of(STUDY, MOOC, RECORDING, LIVE_RECORDING);

    private MaterialCategory() {
    }

    public static String normalize(String category) {
        if (category == null) {
            throw new IllegalArgumentException("资料分区不能为空");
        }
        String normalized = category.trim();
        if (!ALLOWED.contains(normalized)) {
            throw new IllegalArgumentException("资料分区不合法，仅支持：学习资料、慕课资料、录屏录像、直播录像");
        }
        return normalized;
    }
}
