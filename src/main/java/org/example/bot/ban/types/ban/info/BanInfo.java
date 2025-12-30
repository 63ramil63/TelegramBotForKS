package org.example.bot.ban.types.ban.info;

public class BanInfo {
    private String reason;
    private String banType;
    private long adminChatId;

    private BanInfo(Builder builder) {
        this.banType = builder.banType;
        this.reason = builder.reason;
        this.adminChatId = builder.adminChatId;
    }

    public String getBanType() {
        return banType;
    }

    public String getReason() {
        return reason;
    }

    public Long getAdminChatId() {
        return adminChatId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String reason;
        private String banType;
        private long adminChatId;

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder banType(String banType) {
            this.banType = banType;
            return this;
        }

        public Builder adminChatId(long adminChatId) {
            this.adminChatId = adminChatId;
            return this;
        }

        public BanInfo build() {
            return new BanInfo(this);
        }
    }
}
