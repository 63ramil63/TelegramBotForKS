package org.example.controller;

import org.example.database.repository.AdminRepository;
import org.example.database.repository.UserRepository;
import org.example.role.AdminRole;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

public class UserController {
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    public UserController(UserRepository userRepository, AdminRepository adminRepository) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
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

    public boolean checkUser(long chatId) {
        return userRepository.getUser(chatId);
    }

    public void addUser(long chatId) {
        userRepository.addUser(chatId);
    }

    public void updateUserName(long chatId, String userName) {
        userRepository.updateUserName(chatId, userName);
    }

    public String getUsername(long chatId) {
        return userRepository.getUserName(chatId);
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

    // Получаем username пользователя
    private String getUsernameFromUpdate(Update update) {
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getFrom().getUserName();
        } else if (update.hasMessage()){
            return update.getMessage().getChat().getUserName();
        }
        return "undefined";
    }

    public void checkAndAddUser(Update update) {
        long chatId = getChatId(update);
        if (!checkUser(chatId)) {
            String userName = getUsernameFromUpdate(update);
            addUser(chatId);
            updateUserName(chatId, userName);
        }
    }
}
