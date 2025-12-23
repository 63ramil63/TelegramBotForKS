package org.example.dto;

public class GroupDTO {
    private long id;
    private String groupName;

    private GroupDTO(Builder builder) {
        id = builder.id;
        groupName = builder.groupName;
    }

    public long getId() {
        return id;
    }

    public String getGroupName() {
        return groupName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long id;
        private String groupName;

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder groupName(String groupName) {
            this.groupName = groupName;
            return this;
        }

        public GroupDTO build() {
            return new GroupDTO(this);
        }
    }
}
