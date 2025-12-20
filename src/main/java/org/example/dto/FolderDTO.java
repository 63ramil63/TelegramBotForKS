package org.example.dto;

public class FolderDTO {
    private long id;
    private String folder;

    public static Builder builder() {
        return new Builder();
    }

    private FolderDTO(Builder builder) {
        this.id = builder.id;
        this.folder = builder.folder;
    }

    public long getId() {
        return id;
    }

    public String getFolder() {
        return folder;
    }

    public static class Builder {
        private long id;
        private String folder;

        private Builder() {}

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder folder(String folder) {
            this.folder = folder;
            return this;
        }

        public FolderDTO build() {
            return new FolderDTO(this);
        }
    }
}
