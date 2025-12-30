package org.example.controller;

import org.example.database.repository.AdminRepository;
import org.example.database.repository.UserRepository;
import org.example.role.AdminRole;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

public class UserController {
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    public UserController() {
        userRepository = new UserRepository();
        adminRepository = new AdminRepository();
    }

    public boolean checkAdmin(String username) {
        return adminRepository.getAdmin(username);
    }

    public boolean checkAdminByChatId(long chatId) {
        return adminRepository.getAdmin(userRepository.getUserName(chatId));
    }

    // Для суперадминов
    public void addAdminsFromProperty(List<String> adminsUserName) {
        for (String adminUsername : adminsUserName) {
            if (!checkAdmin(adminUsername)) {
                adminRepository.addAdmin(adminUsername, AdminRole.Main.toString());
            }
        }
    }

    // Для обычных админов
    public boolean addBaseAdmin(String username) {
        if (!checkAdmin(username)) {
            return adminRepository.addAdmin(username, AdminRole.Base.toString());
        }
        return false;
    }

    // Возвращает chatId, независимо от того, какое это сообщение
    public long getChatId(Update update) {
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        } else if (update.hasMessage()) {
            return update.getMessage().getChatId();
        }
        return 0;
    }

    private String getUserNameFromUpdateWithCallbackQ(Update update) {
        return update.getCallbackQuery().getFrom().getUserName();
    }

    private String getUserNameFromUpdateWithoutCallbackQ(Update update) {
        return update.getMessage().getChat().getUserName();
    }

    private String getFirstNameFromUpdateWithCallbackQ(Update update) {
        return update.getCallbackQuery().getFrom().getFirstName();
    }

    private String getFirstNameFromUpdateWithoutCallbackQ(Update update) {
        return update.getMessage().getChat().getFirstName();
    }

    private String getLastNameFromUpdateWithCallbackQ(Update update) {
        return update.getCallbackQuery().getFrom().getLastName();
    }

    private String getLastNameFromUpdateWithoutCallbackQ(Update update) {
        return update.getMessage().getChat().getLastName();
    }

    // Ищет пользователя по Id
    public void checkAndAddUser(Update update) {
        long chatId = getChatId(update);
        if (!getUser(chatId)) {
            addUser(chatId);
            String username;
            String firstName;
            String lastName;
            if (update.hasCallbackQuery()) {
                username = getUserNameFromUpdateWithCallbackQ(update);
                firstName = getFirstNameFromUpdateWithCallbackQ(update);
                lastName = getLastNameFromUpdateWithCallbackQ(update);
            } else {
                username = getUserNameFromUpdateWithoutCallbackQ(update);
                firstName = getFirstNameFromUpdateWithoutCallbackQ(update);
                lastName = getLastNameFromUpdateWithoutCallbackQ(update);
            }
            if (username != null && !username.isEmpty()) {
                updateUserName(chatId, username);
            }
            if (firstName != null && !firstName.isEmpty()) {
                updateFirstName(chatId, firstName);
            }
            if (lastName != null && !lastName.isEmpty()) {
                updateLastName(chatId, lastName);
            }
        }
    }

    private void addUser(long chatId) {
        userRepository.addUser(chatId);
    }

    private boolean getUser(long chatId) {
        return userRepository.getUser(chatId);
    }

    public String getUserName(long chatId) {
        return userRepository.getUserName(chatId);
    }

    public String getFirstName(long chatId) {
        return userRepository.getFirstName(chatId);
    }

    public String getLastName(long chatId) {
        return userRepository.getLastName(chatId);
    }

    public String getFilePath(long chatId) {
        return userRepository.getFilePath(chatId);
    }

    public String getGroupId(long chatId) {
        return userRepository.getGroupId(chatId);
    }

    public String getGroupForLinks(long chatId) {
        return userRepository.getGroupForLinks(chatId);
    }

    public boolean getCanAddFolder(long chatId) {
        return userRepository.getCanAddFolder(chatId);
    }

    public boolean getCanAddGroup(long chatId) {
        return userRepository.getCanAddGroup(chatId);
    }

    public boolean getCanAddLink(long chatId) {
        return userRepository.getCanAddLink(chatId);
    }

    public List<Long> getAllUsersChatId() {
        return userRepository.getAllUsersChatId();
    }

    public void updateGroupId(long chatId, String groupId) {
        userRepository.updateGroupId(chatId, groupId);
    }

    public void updateFilePath(long chatId, String filePath) {
        userRepository.updateFilePath(chatId, filePath);
    }

    public void updateCanAddFolder(long chatId, byte bool) {
        userRepository.updateCanAddFolder(chatId, bool);
    }

    public void updateCanAddGroup(long chatId, byte bool) {
        userRepository.updateCanAddGroup(chatId, bool);
    }

    public void updateCanAddLink(long chatId, byte bool) {
        userRepository.updateCanAddLink(chatId, bool);
    }

    public void updateUserName(long chatId, String userName) {
        userRepository.updateUserName(chatId, userName);
    }

    public void updateGroupForLinks(long chatId, String groupForLinks) {
        userRepository.updateGroupForLinks(chatId, groupForLinks);
    }

    public void updateFirstName(long chatId, String firstName) {
        userRepository.updateFirstName(chatId, firstName);
    }

    public void updateLastName(long chatId, String lastName) {
        userRepository.updateLastName(chatId, lastName);
    }

    public String getAdminRole(long chatId) {
        String userName = getUserName(chatId);
        return adminRepository.getAdminRole(userName);
    }

    public List<String> getAdminsUsername() {
        return adminRepository.getAdminsUsername();
    }

    public String getUserInfo(long chatId) {
        String userName = getUserName(chatId);
        String firstName = getFirstName(chatId);
        String lastName = getLastName(chatId);
        if (userName == null || userName.isEmpty()) {
            userName = "Не найдено";
        }
        if (firstName == null || firstName.isEmpty()) {
            firstName = "Не найдено";
        }
        if (lastName == null || lastName.isEmpty()) {
            lastName = "Не найдено";
        }
        return ("Данный файл отправлен пользователем:" +
                "\nusername : " + userName +
                "\nfirst name : " + firstName +
                "\nlast name : " + lastName +
                "\nChatId: " + chatId);
    }
}
