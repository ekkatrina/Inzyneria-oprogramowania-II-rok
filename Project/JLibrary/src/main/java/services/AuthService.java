package services;

import dao.UserDAO;
import models.User;
import java.sql.SQLException;

public class AuthService {
    private static User currentUser;

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User login(String username, String password) {
        try {
            User user = UserDAO.findUserByUsername(username);
            if (user == null || !user.getPassword().equals(password)) {
                throw new RuntimeException("Nieprawidłowy login lub hasło");
            }
            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Błąd połączenia z bazą danych");
        }
    }

    public static void register(String username, String password) {
        try {
            if (UserDAO.findUserByUsername(username) != null) {
                throw new RuntimeException("Użytkownik już istnieje");
            }
            User newUser = new User(username, password, "READER"); // Только читатели
            UserDAO.addUser(newUser);
        } catch (SQLException e) {
            throw new RuntimeException("Błąd rejestracji");
        }
    }
}