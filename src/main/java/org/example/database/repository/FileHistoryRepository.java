package org.example.database.repository;

import org.example.database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class FileHistoryRepository {
    private final Database databaseConnection = Database.getInstance();
    private static final String filesHistoryTable = "files_history";

    private static final String PUT_FILE_INFO_TO_FILES_HISTORY = "INSERT INTO " + filesHistoryTable + " (ChatId, FilePath) values (? ,?)";

    public void putFileInfoToFilesHistory(long chatId, String folder, String fileName) {
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(PUT_FILE_INFO_TO_FILES_HISTORY)) {
            // Для таблицы истории файлов
            preparedStatement.setLong(1, chatId);
            preparedStatement.setString(2, folder + "/" + fileName);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Add file info in history for file " + folder + "/" + fileName);
            } else {
                System.err.println("Error add file info in history  " + folder + "/" + fileName);
            }
        } catch (SQLException e) {
            System.err.printf("Error (FileTrackerRepositoryClass (method putFileInfoToFilesHistory(chatId : %d, folder : %s, fileName : %s)))%n",
                    chatId, folder, fileName);
        }
    }
}
