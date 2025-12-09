package org.example.dto;

public class LinkDTO {
    private long id;
    private String linkName;
    private String link;
    private String groupName;
    private long usersChatId;

    private LinkDTO(Builder builder) {
        this.id = builder.id;
        this.linkName = builder.linkName;
        this.link = builder.link;
        this.groupName = builder.groupName;
        this.usersChatId = builder.usersChatId;
    }

    public long getId() {
        return id;
    }

    public String getLinkName() {
        return linkName;
    }

    public String getLink() {
        return link;
    }

    public String getGroupName() {
        return groupName;
    }

    public long getUsersChatId() {
        return usersChatId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long id;
        private String linkName;
        private String link;
        private String groupName;
        private long usersChatId;

        private Builder() {}

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder linkName(String linkName) {
            this.linkName = linkName;
            return this;
        }

        public Builder link(String link) {
            this.link = link;
            return this;
        }

        public Builder groupName(String groupName) {
            this.groupName = groupName;
            return this;
        }

        public Builder usersChatId(long usersChatId) {
            this.usersChatId = usersChatId;
            return this;
        }

        public LinkDTO build() {
            return new LinkDTO(this);
        }
    }
}
