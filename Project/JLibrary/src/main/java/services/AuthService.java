package services;

import dao.UserDAO;
import models.User;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;

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
            if (user == null || !BCrypt.checkpw(password, user.getPassword())) {
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

            String hashed = BCrypt.hashpw(password, BCrypt.gensalt());

            User newUser = new User(username, hashed, "READER");
            UserDAO.addUser(newUser);
        } catch (SQLException e) {
            throw new RuntimeException("Błąd rejestracji");
        }
    }
}