package org.example.controller;

import org.example.database.repository.FileHistoryRepository;
import org.example.database.repository.FileTrackerRepository;
import org.example.database.repository.FolderRepository;
import org.example.dto.FileDTO;
import org.example.dto.FolderDTO;

import java.util.List;

public class FilesAndFoldersController {
    private final FileTrackerRepository fileTrackerRepository;
    private final FileHistoryRepository fileHistoryRepository;
    private final FolderRepository folderRepository;

    public FilesAndFoldersController() {
        fileTrackerRepository = new FileTrackerRepository();
        fileHistoryRepository = new FileHistoryRepository();
        folderRepository = new FolderRepository();
    }

    public void putFileInfo(long chatId, String folder, String fileName) {
        fileTrackerRepository.putFileInfo(chatId, folder, fileName);
        fileHistoryRepository.putFileInfoToFilesHistory(chatId, folder, fileName);
    }

    public long getFilesChatIdById(long fileId) {
        return fileTrackerRepository.getFilesChatIdById(fileId);
    }

    public List<FileDTO> getAllUserFiles(long chatId) {
        return fileTrackerRepository.getAllUserFiles(chatId);
    }

    public FileDTO getFileInfoByFileId(long fileId) {
        return fileTrackerRepository.getFileInfoByFileId(fileId);
    }

    public boolean deleteUserFileFromRepository(long fileId) {
        return fileTrackerRepository.deleteUserFileFromRepository(fileId);
    }

    public List<FileDTO> getFilesByFolderName(String folder) {
        return fileTrackerRepository.getFilesByFolderName(folder);
    }

    public boolean addFolder(long chatId, String folderName) {
        fileHistoryRepository.putFileInfoToFilesHistory(chatId, folderName, "");
        return folderRepository.addFolder(chatId, folderName);
    }

    public boolean checkFolderByName(String folder) {
        return folderRepository.checkFolderByName(folder);
    }

    public String getFolderNameById(long id) {
        return folderRepository.getFolderNameById(id);
    }

    public List<FolderDTO> getFolders() {
        return folderRepository.getFolders();
    }

    public boolean deleteFolderByName(String folder) {
        return folderRepository.deleteFolderByName(folder);
    }

    public boolean deleteFolderById(long id) {
        return folderRepository.deleteFolderById(id);
    }

    public void checkAndAddFolderIfNotExistsByName(String folderName) {
        if (!checkFolderByName(folderName)) {
            addFolder(0, folderName);
        }
    }
}
