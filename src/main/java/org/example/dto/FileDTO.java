package org.example.dto;

public class FileDTO {
    private long id;
    private String folder;
    private String fileName;

    private FileDTO(Builder builder) {
        this.id = builder.id;
        this.folder = builder.folder;
        this.fileName = builder.fileName;
    }

    public long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFolder() {
        return folder;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long id;
        private String folder;
        private String fileName;

        private Builder(){}

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder folder(String folder) {
            this.folder = folder;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public FileDTO build() {
            return new FileDTO(this);
        }
    }
}
